package saxon.board.ulx3s


import saxon._
import spinal.core._
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.generator._
import spinal.lib.io.{Gpio, InOutWrapper}


class Ulx3sMinimalSystem extends BmbApbVexRiscvGenerator{
  //Add components
  val ramA = BmbOnChipRamGenerator(0x80000000l)
  val uartA = Apb3UartGenerator(0x10000)
  val gpioA = Apb3GpioGenerator(0x00000)
  val noReset = Ulx3sNoResetGenerator()

  ramA.dataWidth.load(32)

  //Interconnect specification
  interconnect.addConnection(
    cpu.iBus -> List(ramA.bmb),
    cpu.dBus -> List(ramA.bmb, peripheralBridge.input)
  )
}


class Ulx3sMinimal extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(255)
  clockCtrl.resetSynchronous.load(false)
  clockCtrl.powerOnReset.load(true)
  clockCtrl.clkFrequency.load(25 MHz)

  val system = new Ulx3sMinimalSystem
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val clk_25mhz = in Bool()

    clockCtrl.clock.load(clk_25mhz)
  }
}

object Ulx3sMinimalSystem{
  def default(g : Ulx3sMinimalSystem, clockCtrl : ClockDomainGenerator) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.minimal)
    cpu.enableJtag(clockCtrl)

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


object Ulx3sMinimal {
  //Function used to configure the SoC
  def default(g : Ulx3sMinimal) = g{
    import g._
    Ulx3sMinimalSystem.default(system, clockCtrl)
    clockCtrl.resetSensitivity.load(ResetSensitivity.NONE)
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(InOutWrapper(default(new Ulx3sMinimal()).toComponent()))
    BspGenerator("Ulx3sMinimal", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
  }

}

