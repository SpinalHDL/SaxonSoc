package saxon.board.ulx3s

import spinal.core._
import spinal.lib._

case class Ulx3sUsrMclk() extends BlackBox{
  setDefinitionName("USRMCLK")

  val USRMCLKI = in Bool()
  val USRMCLKTS = in Bool()
}

