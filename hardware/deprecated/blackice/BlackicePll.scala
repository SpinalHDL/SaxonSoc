package saxon.board.blackice

import spinal.core._

case class BlackicePll() extends BlackBox{
  setDefinitionName("blackice_pll")
  val clock_in = in Bool()
  val clock_out = out Bool()
  val locked = out Bool()
}
