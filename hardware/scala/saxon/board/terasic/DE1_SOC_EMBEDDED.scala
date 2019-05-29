package saxon.board.terasic

import saxon.ResetSourceKind.EXTERNAL
import saxon._
import spinal.core._
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.generator._
import spinal.lib.io.{Gpio, InOutWrapper}


class DE1_SOC_EMBEDDED extends Generator{
  val clocking = add task new Area{
    val CLOCK_50 = in Bool()
    val resetN = in Bool()

    clockCtrl.clock.load(CLOCK_50)
    clockCtrl.reset.load(!resetN)
  }

  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetSourceKind.load(EXTERNAL)
  clockCtrl.powerOnReset.load(true)

  val system = new SaxonSocLinux {
    onClockDomain(clockCtrl.clockDomain)

    //Add components
    val ramA = BmbOnChipRamGenerator(0x80000000l)
    ramA.dataWidth.load(32)

    val gpioA = Apb3GpioGenerator(0x00000)

    //Interconnect specification
    interconnect.addConnection(
      cpu.iBus -> List(ramA.bmb),
      cpu.dBus -> List(ramA.bmb)
    )
  }
}


object DE1_SOC_EMBEDDED {

  //Function used to configure the SoC
  def default(g : DE1_SOC_EMBEDDED) = g{
    import g._
    clockCtrl.clkFrequency.load(50 MHz)

    system {
      import system._

      cpu.config.load(VexRiscvConfigs.linux)
      cpu.enableJtag(clockCtrl)

      ramA.size.load(32 KiB)
      ramA.hexInit.load(null)

      uartA.parameter load UartCtrlMemoryMappedConfig(
        baudrate = 115200,
        txFifoDepth = 1,
        rxFifoDepth = 1
      )


      gpioA.parameter load Gpio.Parameter(
        width = 8,
        interrupt = List(0, 1)
      )

      plic.addInterrupt(source = gpioA.produce(gpioA.logic.io.interrupt(0)), id = 4)
      plic.addInterrupt(source = gpioA.produce(gpioA.logic.io.interrupt(1)), id = 5)
    }
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    SpinalRtlConfig.generateVerilog(InOutWrapper(default(new DE1_SOC_EMBEDDED()).toComponent()))
  }
}

