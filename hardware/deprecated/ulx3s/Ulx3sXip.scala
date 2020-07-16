package saxon.board.ulx3s

import saxon._
import spinal.core._
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.generator._
import spinal.lib.io.{Gpio, InOutWrapper}
import spinal.lib.com.jtag.sim.JtagTcp
import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}
import spinal.lib.com.spi.ddr.{SpiXdrMasterCtrl, SpiXdrParameter}
import spinal.lib.com.spi._

class Ulx3sXipSystem extends BmbApbVexRiscvGenerator{
  //Add components
  val ramA = BmbOnChipRamGenerator(0x80000000l)
  val uartA = Apb3UartGenerator(0x10000)
  val gpioA = Apb3GpioGenerator(0x00000)
  val spiA = Apb3SpiGenerator(0x20000, xipOffset = 0x20000000)
  val noReset = Ulx3sNoResetGenerator()

  ramA.dataWidth.load(32)

  //Interconnect specification
  val bridge = BmbBridgeGenerator()
  interconnect.addConnection(
    cpu.iBus -> List(bridge.bmb),
    cpu.dBus -> List(bridge.bmb),
    bridge.bmb -> List(ramA.ctrl, peripheralBridge.input)
  )
}

class Ulx3sXip extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(255)
  clockCtrl.powerOnReset.load(true)
  clockCtrl.clkFrequency.load(25 MHz)

  val system = new Ulx3sXipSystem
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val clk_25M = in Bool()

    clockCtrl.clock.load(clk_25M)
  }
}

object Ulx3sXipSystem{
  def default(g : Ulx3sXipSystem, clockCtrl : ClockDomainGenerator) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.xip.fast)
    cpu.enableJtag(clockCtrl)

    ramA.size.load(8 KiB)

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
        timerWidth = 12,
        spi = SpiXdrParameter(
          dataWidth = 2,
          ioRate= 1,
          ssWidth = 1
        )
      ).addFullDuplex(id = 0, rate = 1).addHalfDuplex(id=1, rate=1, ddr=false, spiWidth=2),
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
    spiA.inferSpiSdrIo()
    spiA.spi.produce {
      val sclk = spiA.spi.get.asInstanceOf[SpiHalfDuplexMaster].sclk
      sclk.setAsDirectionLess()
      val usrMclk = Ulx3sUsrMclk()
      usrMclk.USRMCLKTS := False
      usrMclk.USRMCLKI := sclk
    }
    
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


object Ulx3sXip {
  //Function used to configure the SoC
  def default(g : Ulx3sXip) = g{
    import g._
    Ulx3sXipSystem.default(system, clockCtrl)
    clockCtrl.resetSensitivity load(ResetSensitivity.NONE)
    
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(InOutWrapper(default(new Ulx3sXip()).toComponent()))
    BspGenerator("Ulx3sXip", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
  }
}

object Ulx3sXipSystemSim {
  import spinal.core.sim._

  def main(args: Array[String]): Unit = {

    val simConfig = SimConfig
    simConfig.allOptimisation
    simConfig.withWave
    simConfig.compile(new Ulx3sXipSystem(){
      val clockCtrl = ClockDomainGenerator()
      this.onClockDomain(clockCtrl.clockDomain)
      clockCtrl.makeExternal(ResetSensitivity.FALL)
      clockCtrl.powerOnReset.load(true)
      clockCtrl.clkFrequency.load(25 MHz)
      Ulx3sXipSystem.default(this, clockCtrl)
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

