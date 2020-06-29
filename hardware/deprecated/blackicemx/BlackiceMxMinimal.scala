package saxon.board.blackicemx

import saxon._
import spinal.core._
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.generator._
import spinal.lib.io.{Gpio, InOutWrapper}
import saxon.board.blackice.peripheral.{Apb3SpiMasterGenerator}
import spinal.lib.com.jtag.sim.JtagTcp
import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}
import saxon.board.blackice.IceStormInOutWrapper
import spinal.lib.com.spi._

class BlackiceMxMinimalSystem extends BmbApbVexRiscvGenerator{
  //Add components
  val ramA = BmbOnChipRamGenerator(0x80000000l)
  val uartA = Apb3UartGenerator(0x10000)
  val gpioA = Apb3GpioGenerator(0x00000)
  val spiA = Apb3SpiMasterGenerator(0x20000)
  val spiB = Apb3SpiMasterGenerator(0x30000)

  ramA.dataWidth.load(32)

  //Interconnect specification
  interconnect.addConnection(
    cpu.iBus -> List(ramA.bmb),
    cpu.dBus -> List(ramA.bmb, peripheralBridge.input)
  )
}


class BlackiceMxMinimal extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(255)
  clockCtrl.powerOnReset.load(true)
  clockCtrl.clkFrequency.load(25 MHz)

  val system = new BlackiceMxMinimalSystem
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val clk_25M = in Bool()

    clockCtrl.clock.load(clk_25M)
  }
}

object BlackiceMxMinimalSystem{
  def default(g : BlackiceMxMinimalSystem, clockCtrl : ClockDomainGenerator) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.minimal)
    cpu.enableJtag(clockCtrl)

    ramA.size.load(8 KiB)
    ramA.hexInit.load("software/standalone/readSdcard/build/readSdcard.hex")

    uartA.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 32,
      rxFifoDepth = 32
    )

    // Configure spiA
    spiA.parameter load SpiMasterCtrlMemoryMappedConfig(
      SpiMasterCtrlGenerics(
        dataWidth = 8,
        timerWidth = 32,
        ssWidth = 1
      )
    )

    // Configure spiB
    spiB.parameter load SpiMasterCtrlMemoryMappedConfig(
      SpiMasterCtrlGenerics(
        dataWidth = 8,
        timerWidth = 32,
        ssWidth = 1
      )
    )

    gpioA.parameter load Gpio.Parameter(width = 8)

    g
  }
}


object BlackiceMxMinimal {
  //Function used to configure the SoC
  def default(g : BlackiceMxMinimal) = g{
    import g._
    BlackiceMxMinimalSystem.default(system, clockCtrl)
    clockCtrl.resetSensitivity load(ResetSensitivity.NONE)
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(IceStormInOutWrapper(default(new BlackiceMxMinimal()).toComponent()))
    BspGenerator("BlackiceMxMinimal", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
  }
}

object BlackiceMxMinimalSystemSim {
  import spinal.core.sim._

  def main(args: Array[String]): Unit = {

    val simConfig = SimConfig
    simConfig.allOptimisation
    simConfig.withWave
    simConfig.compile(new BlackiceMxMinimalSystem(){
      val clockCtrl = ClockDomainGenerator()
      this.onClockDomain(clockCtrl.clockDomain)
      clockCtrl.makeExternal(ResetSensitivity.FALL)
      clockCtrl.powerOnReset.load(true)
      clockCtrl.clkFrequency.load(25 MHz)
      BlackiceMxMinimalSystem.default(this, clockCtrl)
    }.toComponent()).doSimUntilVoid("test", 42){dut =>
      val systemClkPeriod = (1e12/dut.clockCtrl.clkFrequency.toDouble).toLong
      //val jtagClkPeriod = systemClkPeriod*4
      val uartBaudRate = 115200
      val uartBaudPeriod = (1e12/uartBaudRate).toLong

      val clockDomain = ClockDomain(dut.clockCtrl.clock, dut.clockCtrl.reset)
      clockDomain.forkStimulus(systemClkPeriod)

      //val tcpJtag = JtagTcp(
      //  jtag = dut.cpu.jtag,
      //  jtagClkPeriod = jtagClkPeriod
      //)

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

