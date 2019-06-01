package saxon.board.terasic


import saxon._
import spinal.core._
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.generator._
import spinal.lib.io.{Gpio, InOutWrapper}


class De1SocMinimalSystem extends BmbApbVexRiscvGenerator{
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


class De1SocMinimal extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(255)
  clockCtrl.resetSynchronous.load(false)
  clockCtrl.powerOnReset.load(true)
  clockCtrl.clkFrequency.load(50 MHz)

  val system = new De1SocMinimalSystem
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val CLOCK_50 = in Bool()
    val resetN = in Bool()

    clockCtrl.clock.load(CLOCK_50)
    clockCtrl.reset.load(resetN)
  }
}

object De1SocMinimalSystem{
  def default(g : De1SocMinimalSystem, clockCtrl : ClockDomainGenerator) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.minimal)
    cpu.enableJtag(clockCtrl)

    ramA.size.load(32 KiB)
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


object De1SocMinimal {
  //Function used to configure the SoC
  def default(g : De1SocMinimal) = g{
    import g._
    De1SocMinimalSystem.default(system, clockCtrl)
    clockCtrl.ResetSensitivity.load(ResetSensitivity.LOW) //TODO better error reporting if forgot this line
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    SpinalRtlConfig.generateVerilog(InOutWrapper(default(new De1SocMinimal()).toComponent()))
  }
}

