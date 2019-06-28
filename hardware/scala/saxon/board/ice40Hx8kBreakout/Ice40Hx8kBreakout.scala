package saxon.board.ice40Hx8kBreakout

import saxon._
import saxon.board.blackice.IceStormInOutWrapper
import spinal.core._
import spinal.lib.com.jtag.sim.JtagTcp
import spinal.lib.com.spi.ddr.{SpiXdrMasterCtrl, SpiXdrParameter}
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}
import spinal.lib.generator.Generator
import spinal.lib.io.{Gpio, InOutWrapper}


class Ice40Hx8kBreakoutSystem extends BmbApbVexRiscvGenerator{
  //Add components
  val ramA = BmbOnChipRamGenerator(0x80000000l)
  val uartA = Apb3UartGenerator(0x10000)
  val gpioA = Apb3GpioGenerator(0x00000)
  val spiA = Apb3SpiGenerator(0x20000, xipOffset = 0x20000000)

  ramA.dataWidth.load(32)

  //Interconnect specification
  interconnect.addConnection(
    cpu.iBus -> List(ramA.bmb),
    cpu.dBus -> List(ramA.bmb)
  )
}



class Ice40Hx8kBreakout extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(255)
  clockCtrl.resetSynchronous.load(true)
  clockCtrl.powerOnReset.load(true)
  clockCtrl.clkFrequency.load(12 MHz)

  val system = new Ice40Hx8kBreakoutSystem
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val clk_12M = in Bool()

    clockCtrl.clock.load(clk_12M)
    clockCtrl.reset.load(False) // TODO improve
  }
}


object Ice40Hx8kBreakoutSystem{
  def default(g : Ice40Hx8kBreakoutSystem, clockCtrl : ClockDomainGenerator) = g {
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
        timerWidth = 8,
        spi = SpiXdrParameter(
          dataWidth = 2,
          ioRate = 1, //2
          ssWidth = 1
        )
      ) .addFullDuplex(id = 0),//.addHalfDuplex(id=1, rate=2, ddr=false, spiWidth=2),
      cmdFifoDepth = 64,
      rspFifoDepth = 64,
      xipConfigWritable = true,
      xipEnableInit = true
    )
    spiA.withXip.load(true)

    cpu.hardwareBreakpointCount.load(4)

    interconnect.addConnection(
      cpu.iBus -> List(spiA.bmb),
      cpu.dBus -> List(spiA.bmb)
    )

    g
  }
}


object Ice40Hx8kBreakout {
  //Function used to configure the SoC
  def default(g : Ice40Hx8kBreakout) = g{
    import g._
    Ice40Hx8kBreakoutSystem.default(system, clockCtrl)
    clockCtrl.resetSensitivity.load(ResetSensitivity.HIGH)
    system(system.spiA.inferSpiSdrIo())
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(IceStormInOutWrapper(default(new Ice40Hx8kBreakout()).toComponent()))
    BspGenerator("Ice40Hx8kBreakout", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
  }
}




object Ice40Hx8kBreakoutSystemSim {
  import spinal.core.sim._

  def main(args: Array[String]): Unit = {

    val simConfig = SimConfig
    simConfig.allOptimisation
//    simConfig.withWave
    simConfig.compile(new Ice40Hx8kBreakoutSystem(){
      val clockCtrl = ClockDomainGenerator()
      this.onClockDomain(clockCtrl.clockDomain)
      clockCtrl.makeExternal(ResetSensitivity.HIGH)
      clockCtrl.powerOnReset.load(true)
      clockCtrl.clkFrequency.load(12 MHz)

      Ice40Hx8kBreakoutSystem.default(this, clockCtrl)
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

      val flash = new FlashModel(dut.spiA.phy, clockDomain)
//      flash.loadBinary("software/standalone/blinkAndEcho/build/blinkAndEcho.bin", 0)
    }
  }
}
