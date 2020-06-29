package saxon.board.fomu

import spinal.core._

case class FomuPll() extends BlackBox{
  setDefinitionName("fomu_pll")
  val clock_in = in Bool()
  val clock_out = out Bool()
  val locked = out Bool()
}

