package saxon.board.efinix.xyloni

import saxon._
import spinal.core._
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.bus.bmb._
import spinal.lib.bus.misc.SizeMapping
import spinal.lib.com.jtag.sim.JtagTcp
import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}
import spinal.lib.generator._
import spinal.lib.io.{Gpio, InOutWrapper}
import spinal.lib.misc.plic.PlicMapping
import vexriscv.VexRiscvBmbGenerator

object Const {
  final val GENERATED_VERILOG_DIR : String  = "hardware/synthesis/efinix/xyloni/rtl"
  final val BSP_DIR : String                = "efinix/xyloni"                             // sbt root/bsp/...
  final val RISCV_APP_PRJ_DIR : String      = "software/efinix/xyloni"                    // RiscV software. The Eclipse Project Dir
  final val RAM_A_HEX_FILE : String         = RISCV_APP_PRJ_DIR +"/build/app.hex"
}
class SoC extends Component{

  //---- Define the clock domains used by the SoC
  val debugCdCtrl = ClockDomainResetGenerator()
  debugCdCtrl.holdDuration.load(4095)
  debugCdCtrl.enablePowerOnReset()
  debugCdCtrl.makeExternal(FixedFrequency(25 MHz), resetActiveLevel = LOW)

  val systemCdCtrl = ClockDomainResetGenerator()
  systemCdCtrl.holdDuration.load(63)
  systemCdCtrl.asyncReset(debugCdCtrl)
  systemCdCtrl.setInput(
    debugCdCtrl.outputClockDomain,
    omitReset = true
  )

  val debugCd  = debugCdCtrl.outputClockDomain
  val systemCd = systemCdCtrl.outputClockDomain


  //---- Create the rest of the SoC (cpu, interconnect, ram, pheripherals etc) on systemCd
  val system = new ClockingArea(systemCd) {
    val interconnect = BmbInterconnectGenerator()             // removed implicits as it is both ambiguous and depreciated in Scala 3.0
    val periphBridge = BmbBridgeGenerator(mapping = SizeMapping(0x10000000, 16 MiB))(interconnect).peripheral(dataWidth = 32)
    val periphDecoder = periphBridge.asPeripheralDecoder()
    implicit val cpu = VexRiscvBmbGenerator()(interconnect)

    cpu.config.load(VexRiscvConfigs.minimal)
    cpu.enableJtag(debugCdCtrl, systemCdCtrl)

    interconnect.setDefaultArbitration(BmbInterconnectGenerator.STATIC_PRIORITY)
    interconnect.setPriority(cpu.iBus, 1)
    interconnect.setPriority(cpu.dBus, 2)

    // Main on chip RAM for code and data. Note: This is linked to Reset Address in linker script!!
    val ramA = BmbOnChipRamGenerator(0x80000000l)(interconnect)
    ramA.size.load(32 KiB)
    ramA.hexInit.load(Const.RAM_A_HEX_FILE)
    //ramA.hexInit.loadNothing()


    // ---- components on Peripheral Bridge
    val clint = BmbClintGenerator(0xB00000)(interconnect,periphDecoder)
    clint.cpuCount.load(0)


    val gpioA = BmbGpioGenerator(0x00000)(interconnect,periphDecoder)
    gpioA.parameter load Gpio.Parameter(width = 8)


    val uartA = BmbUartGenerator(0x10000)(interconnect,periphDecoder)
    uartA.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 1,
      rxFifoDepth = 1
      )

    //Interconnect specification
    interconnect.addConnection(
      cpu.iBus -> List(ramA.ctrl),
      cpu.dBus -> List(ramA.ctrl, periphBridge.bmb)
    )

    interconnect.setPipelining(periphBridge.bmb)(cmdHalfRate = true, rspHalfRate = true)
    interconnect.setPipelining(cpu.dBus)(cmdValid = true)

  }
}

object xyloni extends App{
  //Generate the SoC
  def mySpinalConfig = SpinalConfig(targetDirectory = Const.GENERATED_VERILOG_DIR)

  val report = mySpinalConfig.generateVerilog(InOutWrapper(new SoC()))
  BspGenerator(Const.BSP_DIR, report.toplevel, report.toplevel.system.cpu.dBus)
}

object xyloniSim extends App{
  import spinal.core.sim._

  val config = SimConfig
  config.compile{
    val dut = new SoC()
    //xyloni.default(dut) //Configure the system
    dut
  }.doSimUntilVoid(seed=42){dut =>
    val debugClkPeriod = (1e12/dut.debugCdCtrl.inputClockDomain.frequency.getValue.toDouble).toLong
    val jtagClkPeriod = debugClkPeriod*8
    val uartBaudRate = 115200
    val uartBaudPeriod = (1e12/uartBaudRate).toLong

    dut.debugCdCtrl.inputClockDomain.forkStimulus(debugClkPeriod)

    JtagTcp(
      jtag = dut.system.cpu.jtag,
      jtagClkPeriod = jtagClkPeriod
    )

    UartDecoder(
      uartPin =  dut.system.uartA.uart.txd,
      baudPeriod = uartBaudPeriod
    )

    UartEncoder(
      uartPin = dut.system.uartA.uart.rxd,
      baudPeriod = uartBaudPeriod
    )
  }
}

