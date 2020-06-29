package saxon.board.tinyfpgabx

import spinal.core._

case class TinyFpgaBxPll() extends BlackBox{
  setDefinitionName("tinyfpgabx_pll")
  val clock_in = in Bool()
  val clock_out = out Bool()
  val locked = out Bool()
}


