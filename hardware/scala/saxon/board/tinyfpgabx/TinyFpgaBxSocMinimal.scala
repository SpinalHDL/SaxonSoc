package saxon.board.tinyfpgabx

import saxon._
import spinal.core._
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.generator._
import spinal.lib.io.{Gpio, InOutWrapper}
import saxon.board.blackice.IceStormInOutWrapper
import saxon.board.blackice.peripheral._
import spinal.lib.com.spi._

class TinyFpgaBxSocMinimalSystem extends BmbApbVexRiscvGenerator{
  //Add components
  val ramA = BmbOnChipRamGenerator(0x80000000l)
  val uartA = Apb3UartGenerator(0x10000)
  val gpioA = Apb3GpioGenerator(0x00000)
  val spiA = Apb3SpiMasterGenerator(0x20000)

  ramA.dataWidth.load(32)

  //Interconnect specification
  interconnect.addConnection(
    cpu.iBus -> List(ramA.bmb),
    cpu.dBus -> List(ramA.bmb, peripheralBridge.input)
  )
}


class TinyFpgaBxSocMinimal extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(255)
  clockCtrl.resetSynchronous.load(false)
  clockCtrl.powerOnReset.load(true)
  clockCtrl.clkFrequency.load(20 MHz)

  val system = new TinyFpgaBxSocMinimalSystem
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val CLOCK_16 = in Bool()
    val GRESET = in Bool()

    val pll = TinyFpgaBxPll()
    pll.clock_in := CLOCK_16

    clockCtrl.clock.load(pll.clock_out)
    clockCtrl.reset.load(GRESET)
  }
}

object TinyFpgaBxSocMinimalSystem{
  def default(g : TinyFpgaBxSocMinimalSystem, clockCtrl : ClockDomainGenerator) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.minimal)
    cpu.enableJtag(clockCtrl)

    ramA.size.load(12 KiB)
    ramA.hexInit.load("software/standalone/blinkAndEcho/build/blinkAndEcho.hex")
    //ramA.hexInit.load(null)

    uartA.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 1,
      rxFifoDepth = 1
    )

    gpioA.parameter load Gpio.Parameter(width = 8)

    // Configure spi
    spiA.parameter load SpiMasterCtrlMemoryMappedConfig(
      SpiMasterCtrlGenerics(
        dataWidth = 8,
        timerWidth = 32,
        ssWidth = 1
      )
    )

    g
  }
}


object TinyFpgaBxSocMinimal {
  //Function used to configure the SoC
  def default(g : TinyFpgaBxSocMinimal) = g{
    import g._
    TinyFpgaBxSocMinimalSystem.default(system, clockCtrl)
    clockCtrl.resetSensitivity load(ResetSensitivity.FALL)
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(IceStormInOutWrapper(default(new TinyFpgaBxSocMinimal()).toComponent()))
    BspGenerator("TinyFpgaBxSocMinimal", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
}
}

