package saxon.board.blackice

import saxon._
import spinal.core._
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.generator._
import spinal.lib.io.{Gpio, InOutWrapper}
import saxon.board.blackice.peripheral.{Apb3SevenSegmentGenerator, Apb3PwmGenerator}
import spinal.lib.com.jtag.sim.JtagTcp
import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}

class BlackiceSocMinimalSystem extends BmbApbVexRiscvGenerator{
  //Add components
  val ramA = BmbOnChipRamGenerator(0x80000000l)
  val uartA = Apb3UartGenerator(0x10000)
  val gpioA = Apb3GpioGenerator(0x00000)

  ramA.dataWidth.load(32)

  //Interconnect specification
  interconnect.addConnection(
    cpu.iBus -> List(ramA.bmb),
    cpu.dBus -> List(ramA.bmb, peripheralBridge.input)
  )
}


class BlackiceSocMinimal extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(255)
  clockCtrl.resetSynchronous.load(false)
  clockCtrl.powerOnReset.load(true)
  clockCtrl.clkFrequency.load(25 MHz)

  val system = new BlackiceSocMinimalSystem
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val CLOCK_100 = in Bool()
    val GRESET = in Bool()

    val pll = BlackicePll()
    pll.clock_in := CLOCK_100

    clockCtrl.clock.load(pll.clock_out)
    clockCtrl.reset.load(GRESET)
  }
}

object BlackiceSocMinimalSystem{
  def default(g : BlackiceSocMinimalSystem, clockCtrl : ClockDomainGenerator) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.minimal)
    cpu.enableJtag(clockCtrl)

    ramA.size.load(12 KiB)
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


object BlackiceSocMinimal {
  //Function used to configure the SoC
  def default(g : BlackiceSocMinimal) = g{
    import g._
    BlackiceSocMinimalSystem.default(system, clockCtrl)
    clockCtrl.resetSensitivity load(ResetSensitivity.FALL)
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(IceStormInOutWrapper(default(new BlackiceSocMinimal()).toComponent()))
    BspGenerator("BlackiceSocMinimal", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
  }
}

object BlackiceSocMinimalSystemSim {
  import spinal.core.sim._

  def main(args: Array[String]): Unit = {

    val simConfig = SimConfig
    simConfig.allOptimisation
    simConfig.withWave
    simConfig.compile(new BlackiceSocMinimalSystem(){
      val clockCtrl = ClockDomainGenerator()
      this.onClockDomain(clockCtrl.clockDomain)
      clockCtrl.makeExternal(ResetSensitivity.FALL)
      clockCtrl.powerOnReset.load(true)
      clockCtrl.clkFrequency.load(25 MHz)
      BlackiceSocMinimalSystem.default(this, clockCtrl)
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

