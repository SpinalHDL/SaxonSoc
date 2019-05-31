package saxon.board.blackice

import saxon.ResetSourceKind.EXTERNAL
import saxon._
import spinal.core._
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.generator._
import spinal.lib.io.{Gpio, InOutWrapper}

case class BlackicePll() extends BlackBox{
  setDefinitionName("blackice_pll")
  val clock_in = in Bool()
  val clock_out = out Bool()
  val locked = out Bool()
}

class BlackiceSocMinimalSystem extends BmbApbVexRiscvGenerator{
  //Add components
  val ramA = BmbOnChipRamGenerator(0x80000000l)
  val uartA = Apb3UartGenerator(0x10000)
  val gpioA = Apb3GpioGenerator(0x00000)

  ramA.dataWidth.load(32)

  //Interconnect specification
  interconnect.addConnection(
    cpu.iBus -> List(ramA.bmb),
    cpu.dBus -> List(ramA.bmb)
  )
}


class BlackiceSocMinimal extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetSourceKind.load(EXTERNAL)
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
    clockCtrl.reset.load(!GRESET)
  }
}

object BlackiceSocMinimalSystem{
  def default(g : BlackiceSocMinimalSystem, clockCtrl : ClockDomainGenerator) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.minimal)
    cpu.enableJtag(clockCtrl)

    ramA.size.load(12 KiB)
    ramA.hexInit.load(null)

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
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    SpinalRtlConfig.generateVerilog(IceStormInOutWrapper(default(new BlackiceSocMinimal()).toComponent()))
  }
}

