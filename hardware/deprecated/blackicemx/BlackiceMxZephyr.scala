package saxon.board.blackicemx

import saxon._
import spinal.core._
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.generator._
import spinal.lib.io.{Gpio, InOutWrapper}
import spinal.lib.com.jtag.sim.JtagTcp
import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}
import saxon.board.blackice._
import saxon.board.blackice.peripheral._
import spinal.lib.com.spi.ddr.{SpiXdrMasterCtrl, SpiXdrParameter}
import spinal.lib.memory.sdram.sdr._
import spinal.lib.memory.sdram.sdr.sim.SdramModel
import spinal.lib.misc.plic.PlicMapping

class BlackiceMxZephyrSystem extends BmbApbVexRiscvGenerator{
  //Add components
  val sdramA = SdramSdrBmbGenerator(0x80000000l)
  val uartA = Apb3UartGenerator(0x10000)
  val gpioA = Apb3GpioGenerator(0x00000)
  val spiA = Apb3SpiGenerator(0x20000, xipOffset = 0x20000000)
  val machineTimer = Apb3MachineTimerGenerator(0x08000)
  val plic = Apb3PlicGenerator(0xC00000)

  //Interconnect specification
  val bridge = BmbBridgeGenerator()
  interconnect.addConnection(
    cpu.iBus -> List(bridge.bmb),
    cpu.dBus -> List(bridge.bmb),
    bridge.bmb -> List(sdramA.bmb, peripheralBridge.input)
  )

  interconnect.setConnector(sdramA.bmb){(m,s) =>
    m.cmd >-> s.cmd
    m.rsp << s.rsp
  }
}


class BlackiceMxZephyr extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(255)
  clockCtrl.powerOnReset.load(true)
  clockCtrl.clkFrequency.load(16 MHz)

  val system = new BlackiceMxZephyrSystem
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val clk_25M = in Bool()
    val sdramClk = out Bool()

    val pll = BlackiceMxPll()
    pll.clock_in := clk_25M

    sdramClk := pll.sdram_clock_out

    clockCtrl.clock.load(pll.clock_out)
  }
}

object BlackiceMxZephyrSystem{
  def default(g : BlackiceMxZephyrSystem, clockCtrl : ClockDomainGenerator) = g {
    import g._

    cpu.setTimerInterrupt(machineTimer.interrupt)
    cpu.config.load(VexRiscvConfigs.xip.fastWithCsr(0x20050000))
    cpu.enableJtag(clockCtrl)

    sdramA.layout.load(IS42S16100H.layout)
    sdramA.timings.load(IS42S16100H.timingGrade7)

    // Configure Platform Interrupt Controller
    plic.priorityWidth.load(2)
    plic.mapping.load(PlicMapping.sifive)
    plic.addTarget(cpu.externalInterrupt)

    uartA.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 32,
      rxFifoDepth = 32
    )

    uartA.connectInterrupt(plic, 1)

    gpioA.parameter load Gpio.Parameter(width = 8)

    spiA.parameter load SpiXdrMasterCtrl.MemoryMappingParameters(
      SpiXdrMasterCtrl.Parameters(
        dataWidth = 8,
        timerWidth = 0,
        spi = SpiXdrParameter(
          dataWidth = 2,
          ioRate= 2,
          ssWidth = 1
        )
      ).addFullDuplex(id = 0, rate = 2).addHalfDuplex(id=1, rate=2, ddr=false, spiWidth=2),
      cmdFifoDepth = 64,
      rspFifoDepth = 64,
      cpolInit = false,
      cphaInit = false,
      modInit = 0,
      sclkToogleInit = 0,
      ssSetupInit = 0,
      ssHoldInit = 0,
      ssDisableInit = 0,
      xipConfigWritable = false,
      xipEnableInit = true,
      xipInstructionEnableInit = true,
      xipInstructionModInit = 0,
      xipAddressModInit = 0,
      xipDummyModInit = 0,
      xipPayloadModInit = 1,
      xipInstructionDataInit = 0x3B,
      xipDummyCountInit = 0,
      xipDummyDataInit = 0xFF
    )
    spiA.withXip.load(true)
    cpu.hardwareBreakpointCount.load(4)

    interconnect.addConnection(
      bridge.bmb -> List(spiA.bmb)
    )

    //Cut dBus address path
    interconnect.setConnector(bridge.bmb){(m,s) =>
      m.cmd >-> s.cmd
      m.rsp << s.rsp
    }

    g
  }
}


object BlackiceMxZephyr {
  //Function used to configure the SoC
  def default(g : BlackiceMxZephyr) = g{
    import g._
    BlackiceMxZephyrSystem.default(system, clockCtrl)
    clockCtrl.resetSensitivity load(ResetSensitivity.NONE)
    system.spiA.inferSpiIce40()

    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(IceStormInOutWrapper(default(new BlackiceMxZephyr()).toComponent()))
    BspGenerator("BlackiceMxZephyr", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
  }
}

object BlackiceMxZephyrSystemSim {
  import spinal.core.sim._

  def main(args: Array[String]): Unit = {

    val simConfig = SimConfig
    simConfig.allOptimisation
    simConfig.withWave
    simConfig.compile(new BlackiceMxZephyrSystem(){
      val clockCtrl = ClockDomainGenerator()
      this.onClockDomain(clockCtrl.clockDomain)
      clockCtrl.makeExternal(ResetSensitivity.FALL)
      clockCtrl.powerOnReset.load(true)
      clockCtrl.clkFrequency.load(16 MHz)
      BlackiceMxZephyrSystem.default(this, clockCtrl)
    }.toComponent()).doSimUntilVoid("test", 42){dut =>
      val systemClkPeriod = (1e12/dut.clockCtrl.clkFrequency.toDouble).toLong
      val jtagClkPeriod = systemClkPeriod*4
      val uartBaudRate = 115200
      val uartBaudPeriod = (1e12/uartBaudRate).toLong

      val clockDomain = ClockDomain(dut.clockCtrl.clock, dut.clockCtrl.reset)
      clockDomain.forkStimulus(systemClkPeriod)

      val tcpJtag = JtagTcp(
        jtag = dut.cpu.jtag,
        jtagClkPeriod = jtagClkPeriod
      )

      val uartTx = UartDecoder(
        uartPin =  dut.uartA.uart.txd,
        baudPeriod = uartBaudPeriod
      )

      val uartRx = UartEncoder(
        uartPin = dut.uartA.uart.rxd,
        baudPeriod = uartBaudPeriod
      )

      val sdram = SdramModel(
        io = dut.sdramA.sdram,
        layout = dut.sdramA.logic.layout,
        clockDomain = clockDomain
      )

      val flash = new FlashModel(dut.spiA.phy, clockDomain)
      flash.loadBinary("../zephyr/samples/philosophers/build/zephyr/zephyr.bin", 0x50000)
    }
  }
}

