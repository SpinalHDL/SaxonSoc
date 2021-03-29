package saxon.board.radiona.ulx3s

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

// Define a SoC abstract enough to be used for simulation
class Ulx3sMinimalAbstract extends Area{
  implicit val interconnect = BmbInterconnectGenerator()
  implicit val bmbPeripheral = BmbBridgeGenerator(mapping = SizeMapping(0x10000000, 16 MiB)).peripheral(dataWidth = 32)
  implicit val peripheralDecoder = bmbPeripheral.asPeripheralDecoder() //Will be used by peripherals as default bus to connect to
  implicit val cpu = VexRiscvBmbGenerator()

  interconnect.setDefaultArbitration(BmbInterconnectGenerator.STATIC_PRIORITY)
  interconnect.setPriority(cpu.iBus, 1)
  interconnect.setPriority(cpu.dBus, 2)

  val clint = BmbClintGenerator(0xB00000) //Used as a time reference only
  clint.cpuCount.load(0)

  //Add components
  val ramA = BmbOnChipRamGenerator(0x80000000l)
  val gpioA = BmbGpioGenerator(0x00000)
  val uartA = BmbUartGenerator(0x10000)

  //Interconnect specification
  interconnect.addConnection(
    cpu.iBus -> List(ramA.ctrl),
    cpu.dBus -> List(ramA.ctrl, bmbPeripheral.bmb)
  )
}

class Ulx3sMinimal extends Component{
  // Define the clock domains used by the SoC
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

  val system = systemCd on new Ulx3sMinimalAbstract(){
    cpu.enableJtag(debugCdCtrl, systemCdCtrl)

    interconnect.setPipelining(bmbPeripheral.bmb)(cmdHalfRate = true, rspHalfRate = true)
    interconnect.setPipelining(cpu.dBus)(cmdValid = true)
  }
}

object Ulx3sMinimalAbstract{
  def default(g : Ulx3sMinimalAbstract) = g.rework {
    import g._

    cpu.config.load(VexRiscvConfigs.minimal)

    ramA.size.load(32 KiB)
    ramA.hexInit.load("software/standalone/blinkAndEcho/build/blinkAndEcho.hex")

    uartA.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 1,
      rxFifoDepth = 1
    )

    gpioA.parameter load Gpio.Parameter(width = 8)

    g
  }
}

object Ulx3sMinimal extends App{
  //Function used to configure the SoC
  def default(g : Ulx3sMinimal) = g.rework {
    import g._
    Ulx3sMinimalAbstract.default(system)
    g
  }

  //Generate the SoC
  val report = SpinalRtlConfig.generateVerilog(InOutWrapper(default(new Ulx3sMinimal())))
  BspGenerator("radiona/ulx3s/minimal", report.toplevel, report.toplevel.system.cpu.dBus)
}


object Ulx3sMinimalSim extends App{
  import spinal.core.sim._

  val config = SimConfig
  config.compile{
    val dut = new Ulx3sMinimal()
    Ulx3sMinimal.default(dut) //Configure the system
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

//  val plic = BmbPlicGenerator(0xC00000)
//  plic.priorityWidth.load(2)
//  plic.mapping.load(PlicMapping.sifive)
//  plic.addTarget(cpu.externalInterrupt)
//
//  val clint = BmbClintGenerator(0xB00000)
//  clint.cpuCount.load(1)
//  cpu.setTimerInterrupt(clint.timerInterrupt(0))
