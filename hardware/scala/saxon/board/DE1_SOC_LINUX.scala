package saxon.board

import saxon.ResetSourceKind.EXTERNAL
import saxon.{Apb3GpioGenerator, ClockDomainGenerator, SaxonSocLinux, SaxonSocSdram, SdramSdrBmbGenerator, SpinalRtlConfig, VexRiscvConfigs}
import spinal.core._
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.generator._
import spinal.lib.io.{Gpio, InOutWrapper}
import spinal.lib.memory.sdram.IS42x320D


case class DE1_SOC_LINUX_PLL() extends BlackBox{
  setDefinitionName("pll_0002")
  val refclk = in Bool()
  val rst = in Bool()
  val outclk_0 = out Bool()
  val outclk_1 = out Bool()
  val outclk_2 = out Bool()
  val outclk_3 = out Bool()
  val locked = out Bool()
}



class DE1_SOC_LINUX extends Generator{
  val clocking = add task new Area{
    val CLOCK_50 = in Bool()
    val resetN = in Bool()
    val sdramClk = out Bool()

    val pll = DE1_SOC_LINUX_PLL()
    pll.refclk := CLOCK_50
    pll.rst := False
    sdramClk := pll.outclk_1
    clockCtrl.clock.load(pll.outclk_0)
    clockCtrl.reset.load(!resetN)
  }


  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetSourceKind.load(EXTERNAL)
  clockCtrl.powerOnReset.load(true)

  val system = new SaxonSocLinux {
    onClockDomain(clockCtrl.clockDomain)

    //Add components
    val sdramA = SdramSdrBmbGenerator(address = 0x80000000l)
    val gpioA = Apb3GpioGenerator(0x00000)

    //Interconnect specification
    interconnect.addConnection(
      cpu.iBus -> List(sdramA.bmb),
      cpu.dBus -> List(sdramA.bmb)
    )
  }
}


object DE1_SOC_LINUX {
  //Function used to configure the SoC
  def configure(g : DE1_SOC_LINUX) ={
    g{
      import g._
      clockCtrl.clkFrequency.load(100 MHz)

      system {
        import g.system._

        cpu.config.load(VexRiscvConfigs.linux)
        cpu.enableJtag(clockCtrl)

        sdramA.layout.load(IS42x320D.layout)
        sdramA.timings.load(IS42x320D.timingGrade7)

        uartA.parameter.load(
          UartCtrlMemoryMappedConfig(
            baudrate = 1000000,
            txFifoDepth = 128,
            rxFifoDepth = 128
          )
        )

        gpioA.parameter.load(
          Gpio.Parameter(
            width = 8,
            interrupt = List(0, 1)
          )
        )

        plic.addInterrupt(source = gpioA.produce(gpioA.logic.io.interrupt(0)), id = 4)
        plic.addInterrupt(source = gpioA.produce(gpioA.logic.io.interrupt(1)), id = 5)
      }
    }
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    SpinalRtlConfig.generateVerilog(InOutWrapper(configure(new DE1_SOC_LINUX()).toComponent()))
  }
}

