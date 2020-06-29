package saxon.board.fomu

import saxon._
import saxon.board.blackice.IceStormInOutWrapper
import spinal.core._
import spinal.lib.com.jtag.sim.JtagTcp
import spinal.lib.com.spi.ddr.{SpiXdrMasterCtrl, SpiXdrParameter}
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}
import spinal.lib.generator.Generator
import spinal.lib.io.{Gpio, InOutWrapper}
import spinal.lib.misc.plic.PlicMapping

class FomuMinimalSystem extends BmbApbVexRiscvGenerator{
  //Add components
  val ramA = BmbOnChipRamGenerator(0x80000000l)
  val gpioA = Apb3GpioGenerator(0x00000)
  val rgbA = Apb3RgbLedGenerator(0x10000)

  ramA.dataWidth.load(32)

  //Interconnect specification
  interconnect.addConnection(
    cpu.iBus -> List(ramA.bmb),
    cpu.dBus -> List(ramA.bmb, peripheralBridge.input)
  )
}

class FomuMinimal extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(255)
  clockCtrl.powerOnReset.load(true)
  clockCtrl.clkFrequency.load(16 MHz)

  val system = new FomuMinimalSystem
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val clk_48M = in Bool()

    val pll = FomuPll()
    pll.clock_in := clk_48M

    clockCtrl.clock.load(pll.clock_out)
  }
}

object FomuMinimalSystem{
  def default(g : FomuMinimalSystem, clockCtrl : ClockDomainGenerator) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.minimal)
    cpu.disableDebug()

    ramA.size.load(8 KiB)
    ramA.hexInit.load("software/standalone/blinkRgb/build/blinkRgb.hex")

    gpioA.parameter load Gpio.Parameter(
      width = 4
    )

    g
  }
}

object FomuMinimal {
  //Function used to configure the SoC
  def default(g : FomuMinimal) = g{
    import g._
    FomuMinimalSystem.default(system, clockCtrl)
    clockCtrl.resetSensitivity.load(ResetSensitivity.NONE)
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(IceStormInOutWrapper(default(new FomuMinimal()).toComponent()))
    BspGenerator("FomuMinimal", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
  }
}

