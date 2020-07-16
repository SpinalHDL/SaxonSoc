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

class BlackiceMxXipSystem extends BmbApbVexRiscvGenerator{
  //Add components
  val ramA = BmbOnChipRamGenerator(0x80000000l)
  val uartA = Apb3UartGenerator(0x10000)
  val gpioA = Apb3GpioGenerator(0x00000)
  val spiA = Apb3SpiGenerator(0x20000, xipOffset = 0x20000000)

  ramA.dataWidth.load(32)

  //Interconnect specification
  val bridge = BmbBridgeGenerator()
  interconnect.addConnection(
    cpu.iBus -> List(bridge.bmb),
    cpu.dBus -> List(bridge.bmb),
    bridge.bmb -> List(ramA.ctrl, peripheralBridge.input)
  )
}


class BlackiceMxXip extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(255)
  clockCtrl.powerOnReset.load(true)
  clockCtrl.clkFrequency.load(25 MHz)

  val system = new BlackiceMxXipSystem
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val clk_25M = in Bool()

    clockCtrl.clock.load(clk_25M)
  }
}

object BlackiceMxXipSystem{
  def default(g : BlackiceMxXipSystem, clockCtrl : ClockDomainGenerator) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.xip.fast(0x20050000))
    cpu.enableJtag(clockCtrl)

    ramA.size.load(8 KiB)

    //ramA.hexInit.load("software/standalone/writeFlash/build/writeFlash.hex")
    ramA.hexInit.load(null)

    uartA.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 1,
      rxFifoDepth = 1
    )

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


object BlackiceMxXip {
  //Function used to configure the SoC
  def default(g : BlackiceMxXip) = g{
    import g._
    BlackiceMxXipSystem.default(system, clockCtrl)
    clockCtrl.resetSensitivity load(ResetSensitivity.NONE)
    system.spiA.inferSpiIce40()

    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(IceStormInOutWrapper(default(new BlackiceMxXip()).toComponent()))
    BspGenerator("BlackiceMxXip", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
  }
}

object BlackiceMxXipSystemSim {
  import spinal.core.sim._

  def main(args: Array[String]): Unit = {

    val simConfig = SimConfig
    simConfig.allOptimisation
    simConfig.withWave
    simConfig.compile(new BlackiceMxXipSystem(){
      val clockCtrl = ClockDomainGenerator()
      this.onClockDomain(clockCtrl.clockDomain)
      clockCtrl.makeExternal(ResetSensitivity.FALL)
      clockCtrl.powerOnReset.load(true)
      clockCtrl.clkFrequency.load(25 MHz)
      BlackiceMxXipSystem.default(this, clockCtrl)
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
    }
  }
}

