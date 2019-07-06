package saxon.board.blackicemx

import spinal.core._

case class BlackiceMxPll() extends BlackBox{
  setDefinitionName("blackice_mx_pll")
  val clock_in = in Bool()
  val clock_out = out Bool()
  val sdram_clock_out = out Bool()

  val locked = out Bool()
}
