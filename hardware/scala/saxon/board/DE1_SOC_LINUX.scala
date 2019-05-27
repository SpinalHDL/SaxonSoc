package saxon.board

import saxon.ResetSourceKind.EXTERNAL
import saxon.{SaxonSocSdram, SpinalRtlConfig, VexRiscvConfigs}
import spinal.core._
import spinal.lib.io.InOutWrapper


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



class DE1_SOC_LINUX extends SaxonSocSdram{
  clockCtrl.resetSourceKind.load(EXTERNAL)
  clockCtrl.clkFrequency.load(50 MHz)
  clockCtrl.powerOnReset.load(true)

  system.cpu.config.load(VexRiscvConfigs.linux)
  system.cpu.enableJtag(clockCtrl)

  val clocking = add task new Area{
    val CLOCK_50 = in Bool()
    val resetN = in Bool()
    val sdramClk = out Bool()

    val pll = DE1_SOC_LINUX_PLL()
    pll.refclk := CLOCK_50
    pll.rst := False
    clockCtrl.clock.load(pll.outclk_0)
    clockCtrl.reset.load(!resetN)
    sdramClk := pll.outclk_1
  }
}


object DE1_SOC_LINUX {
  def main(args: Array[String]): Unit = {
    SpinalRtlConfig.generateVerilog(InOutWrapper(new DE1_SOC_LINUX().toComponent()))
  }
}

