package saxon.board.efinix.xyloni_soc

import saxon._
import spinal.core._
import spinal.core.fiber._
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.bus.bmb._
import spinal.lib.bus.misc.SizeMapping
import spinal.lib.com.jtag.sim.JtagTcp
import spinal.lib.com.jtag.{Jtag, JtagTap, JtagTapInstructionCtrl}
import spinal.lib.com.jtag.{JtagInstructionDebuggerGenerator, JtagTapDebuggerGenerator}
import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}
import spinal.lib.generator._
import spinal.lib.io.{Gpio, InOutWrapper}
import spinal.lib.misc.plic.PlicMapping
import vexriscv.VexRiscvBmbGenerator

object Const {
  final val GENERATED_VERILOG_DIR : String  = "hardware/synthesis/efinix/xyloni/a_verilog"
  final val RISCV_APP_PRJ_DIR : String      = "software/efinix/xyloni"                    // RiscV software. The Eclipse Project Dir
  final val BSP_DIR : String                = RISCV_APP_PRJ_DIR + "/bsp"                  
  final val RAM_A_HEX_FILE : String         = RISCV_APP_PRJ_DIR + "/build/xyloni.hex"
}
class SoC extends Component{

  //---- Define the clock domains used by the SoC
  val debugCdCtrl = ClockDomainResetGenerator()
  debugCdCtrl.holdDuration.load(4095)
  debugCdCtrl.enablePowerOnReset()
  debugCdCtrl.makeExternal(FixedFrequency(19.96 MHz), resetActiveLevel = LOW)

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
    val periphBridge = BmbBridgeGenerator(mapping = SizeMapping(0x10000000, 0x20000))(interconnect).peripheral(dataWidth = 32)
    val periphDecoder = periphBridge.asPeripheralDecoder()
    val cpu = VexRiscvBmbGenerator()(interconnect)

    Handle (cpu.config.load(VexRiscvConfigs.minimalWithCodeCompressAndMult (ramA.address)))

    interconnect.setDefaultArbitration(BmbInterconnectGenerator.STATIC_PRIORITY)
    interconnect.setPriority(cpu.iBus, 1)
    interconnect.setPriority(cpu.dBus, 2)

    // ---- Main on chip RAM for code and data. Note: This is linked to Reset Address in linker script!!
    val ramA = BmbOnChipRamGenerator(0x8000L)(interconnect) // Also the Reset Address! Manually set reset address and size in default.ld to e.g ORIGIN = 0x8000, LENGTH = 4k
    ramA.size.load(4 KiB)
    ramA.hexInit.load(Const.RAM_A_HEX_FILE)
    //ramA.hexInit.loadNothing()                              // chicken - egg situation. If hex file is not available/correct use this to get compilation going...

    
    // ---- our 16 bit Bridge
    val dBus16 = BmbBridgeGenerator(mapping = SizeMapping(0x0000L, 8 KiB))(interconnect)
    dBus16.dataWidth(16)

    // -- Dual Clock Dual Port Ram
    val dpr = BmbOnChipDpRamGenerator(0x0000L)(interconnect)
    dpr.size.load (BigInt (1024))
    //dpr.hexInit.loadNothing()
    Handle(dpr.logic.io.portB.b_clk.toIo.setName("UsbDpr_b_clk"))
    Handle(dpr.logic.io.portB.b_wr.toIo.setName("UsbDpr_b_wr"))
    Handle(dpr.logic.io.portB.b_addr.toIo.setName("UsbDpr_b_addr"))
    Handle(dpr.logic.io.portB.b_din.toIo.setName("UsbDpr_b_din"))
    Handle(dpr.logic.io.portB.b_dout.toIo.setName("UsbDpr_b_dout"))    

    // -- An exported 16 bit Pipeline Bus
    val Bus16 = BmbBusExportGenerator(0x1000L)(interconnect)
    Bus16.size.load (4 KiB)
    Handle(Bus16.logic.io.wr.toIo.setName("Bus16_wr"))
    Handle(Bus16.logic.io.addr.toIo.setName("Bus16_A"))
    Handle(Bus16.logic.io.din.toIo.setName("Bus16_Din"))
    Handle(Bus16.logic.io.dout.toIo.setName("Bus16_Dout"))


    // -- An exported 32 bit Pipeline Bus
    val Bus32 = BmbBusExportGenerator(0x2000L)(interconnect)
    Bus32.size.load (4 KiB)
    Handle(Bus32.logic.io.wr.toIo.setName("Bus32_wr"))
    Handle(Bus32.logic.io.rd.toIo.setName("Bus32_rd"))
    Handle(Bus32.logic.io.addr.toIo.setName("Bus32_A"))
    Handle(Bus32.logic.io.din.toIo.setName("Bus32_Din"))
    Handle(Bus32.logic.io.dout.toIo.setName("Bus32_Dout"))


    // ---- components on Peripheral Bridge
    val clint = BmbClintGenerator(0x00000)(interconnect,periphDecoder)
    clint.cpuCount.load(0)


    val gpioA = BmbGpioGenerator(0x10000)(interconnect,periphDecoder)
    gpioA.parameter load Gpio.Parameter(width = 8)


    val uartA = BmbUartGenerator(0x10100)(interconnect,periphDecoder)
    uartA.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 1,
      rxFifoDepth = 1
    )

    //Interconnect specification
    interconnect.addConnection(
      cpu.iBus -> List(ramA.ctrl),
      cpu.dBus -> List(ramA.ctrl, periphBridge.bmb, dBus16.bmb, Bus32.ctrl),
      dBus16.bmb  -> List(dpr.ctrl, Bus16.ctrl)
    )

    //---- Do you need a debugger?
    // cpu.disableDebug()                         // disable JTAG in production
    //* enable Hard Jatag in T8 FPGA which works for downloading
    val hardJtag =  new Area {
      val ctrl = debugCd on BmbBridgeGenerator()(interconnect)
      cpu.enableDebugBmb(debugCd, systemCdCtrl, SizeMapping(0xF000, 0x1000))    // this value goes to OCD debug.cfg target create -dbgbase 0xF000
      interconnect.addConnection(ctrl.bmb, cpu.debugBmb)

      val tap = debugCd on JtagInstructionDebuggerGenerator(ignoreWidth=0)(interconnect)
      interconnect.addConnection(tap.bmb, ctrl.bmb)
      val debug = tap


      val jtag_inst1 = Handle(debug.logic.jtagBridge.io.ctrl.toIo).setName("jtag_inst1")
      val jtag_inst1_tck = in(Bool()) setName("jtag_inst1_tck")
      debug.jtagClockDomain.load(ClockDomain(jtag_inst1_tck))
    } //*/    

    //interconnect.setPipelining(periphBridge.bmb)(cmdHalfRate = true, rspHalfRate = true)
    interconnect.setPipelining(cpu.dBus)(cmdValid = true)

    debugCdCtrl.inputClockDomain.clock.setName("ClkCore")
    debugCdCtrl.inputClockDomain.reset.setName("nReset")

  }
}

object xyloni_soc extends App{
  //Generate the SoC
  def mySpinalConfig = SpinalConfig(targetDirectory = Const.GENERATED_VERILOG_DIR)

//  val report = mySpinalConfig.generateVerilog(InOutWrapper(new SoC()))
  val report = mySpinalConfig.generateVerilog(new SoC())
  BspGenerator(Const.BSP_DIR, report.toplevel, report.toplevel.system.cpu.dBus)
}

object xyloni_socSim extends App{
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

