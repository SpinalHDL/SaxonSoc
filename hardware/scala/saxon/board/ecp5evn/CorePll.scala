package saxon.board.ecp5evn

import spinal.core._
import spinal.lib._

case class CorePll() extends BlackBox { 
  val io = new Bundle {
    val clkin     = in  Bool
    val clkout0    = out Bool
    val locked     = out Bool
  }
  noIoPrefix()
  setBlackBoxName("core_pll")
}
