package saxon.board.tinyfpgabx

import spinal.core._
import spinal.lib._
import spinal.lib.generator.Generator

case class TinyFpgaBxUsbOff() extends Component {
 val io = new Bundle {
   val usb_pu = out Bool()
 }

 io.usb_pu := False
}

case class TinyFpgaBxUsbOffGenerator() extends Generator {
  val usbPu = produceIo(logic.io.usb_pu)

  val logic = add task TinyFpgaBxUsbOff()
}

