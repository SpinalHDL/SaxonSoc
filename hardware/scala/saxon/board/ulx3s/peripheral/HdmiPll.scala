package saxon.board.ulx3s.peripheral

import spinal.core._
import spinal.lib._

case class HdmiPll() extends BlackBox {
  val io = new Bundle {
    val clkin     = in  Bool
    val clkout0    = out Bool
    val clkout1    = out Bool
    val locked     = out Bool
  }
  noIoPrefix()
  setBlackBoxName("hdmi_pll")
}
