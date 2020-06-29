package saxon.board.ulx3s.peripheral

import spinal.core._
import spinal.lib._

class UsbHid2Ascii extends Component {
  val io = new Bundle {
    val hidReport = in Bits(16 bits)
    val ascii = out Bits(8 bits)
  }

  val key = io.hidReport(15 downto 8).asUInt
  val shift = io.hidReport(1) || io.hidReport(5)

  when (key >= U(0x04, 8 bits) && key <= U(0x1d, 8 bits)) {
    io.ascii := (shift ? (key + U(0x3D, 8 bits)) | (key + U(0x5D, 8 bits))).asBits
  } elsewhen (key >= U(0x1e, 8 bits) && key <= U(0x26, 8 bits)) {
    io.ascii := (shift ? (key + U(0x03, 8 bits)) | (key + U(0x13, 8 bits))).asBits
  } elsewhen (key === U(0x27)) {
    io.ascii := (shift ? (key + U(0x02, 8 bits)) | (key + U(0x09, 8 bits))).asBits
  } elsewhen (key === U(0x28)) {
    io.ascii := 0x0a // Enter
  } elsewhen (key === U(0x29)) {
    io.ascii := 0x1b // Esc
  } elsewhen (key === U(0x35)) {
    io.ascii := 0x60 // `
  } elsewhen (key === U(0x2a)) {
    io.ascii := 0x08 // backspace
  } elsewhen (key === U(0x2b)) {
    io.ascii := 0x09 // tab
  } elsewhen (key === U(0x2c)) {
    io.ascii := 0x20 // space
  } elsewhen (key === U(0x33)) {
    io.ascii := (shift ? B(0x3a, 8 bits) | B(0x3b, 8 bits)) // ; :
  } elsewhen (key === U(0x34)) {
    io.ascii := (shift ? B(0x40, 8 bits) | B(0x27, 8 bits)) // ' @
  } elsewhen (key === U(0x2d)) {
    io.ascii := (shift ? B(0x5f, 8 bits) | B(0x2d, 8 bits)) // - _
  } elsewhen (key === U(0x36)) {
    io.ascii := (shift ? B(0x3c, 8 bits) | B(0x2c, 8 bits)) // , <
  } elsewhen (key === U(0x37)) {
    io.ascii := (shift ? B(0x3e, 8 bits) | B(0x2e, 8 bits)) // . >
  } elsewhen (key === U(0x38)) {
    io.ascii := (shift ? B(0x3f, 8 bits) | B(0x2f, 8 bits)) // / ? /
  } elsewhen (key === U(0x2e)) {
    io.ascii := (shift ? B(0x2b, 8 bits) | B(0x3d, 8 bits)) // / = +
  } elsewhen (key === U(0x2f)) {
    io.ascii := (shift ? B(0x7b, 8 bits) | B(0x5b, 8 bits)) // / [ {
  } elsewhen (key === U(0x30)) {
    io.ascii := (shift ? B(0x7d, 8 bits) | B(0x5d, 8 bits)) // / ] }
  } elsewhen (key === U(0x31)) {
    io.ascii := (shift ? B(0x7c, 8 bits) | B(0x5c, 8 bits)) // / ?
  } otherwise {
    io.ascii := 0
  }
}

