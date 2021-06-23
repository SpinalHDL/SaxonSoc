package saxon.board.ulx3s.peripheral

import spinal.core._
import spinal.lib._

class Ps2ScanToAscii extends Component {
  val io = new Bundle {
    val shift = in Bool()
    val ctrl = in Bool()
    val alt = in Bool()
    val extend = in Bool()
    val scanCode = in Bits(8 bits)
    val ascii = out Bits(8 bits)
  }

  when (io.extend) {
    switch(io.scanCode) {
      is(0x75) (io.ascii := 0x90) // up
      is(0x74) (io.ascii := 0x91) // right
      is(0x72) (io.ascii := 0x92) // down
      is(0x6b) (io.ascii := 0x93) // left
      is(0x6c) (io.ascii := 0x94) // home
      is(0x69) (io.ascii := 0x95) // end
      is(0x7d) (io.ascii := 0x96) // pg up
      is(0x7a) (io.ascii := 0x97) // pg down
      is(0x70) (io.ascii := 0x98) // insert
      is(0x71) (io.ascii := 0x99) // delete
      is(0x05) (io.ascii := 0xa1) // F1
      is(0x06) (io.ascii := 0xa2) // F2
      is(0x04) (io.ascii := 0xa3) // F3
      default  (io.ascii := 0x2e)
    }
  } elsewhen (io.ctrl) {
    switch(io.scanCode) {
      is(0x0d) (io.ascii := 0x09)
      is(0x0e) (io.ascii := 0x7e) // ~
      is(0x15) (io.ascii := 0x11) // Q
      is(0x16) (io.ascii := 0x21) // !
      is(0x1b) (io.ascii := 0x13) // S
      is(0x1a) (io.ascii := 0x1a) // Z
      is(0x1c) (io.ascii := 0x01) // A
      is(0x1d) (io.ascii := 0x17) // W
      is(0x1e) (io.ascii := 0x40) // @
      is(0x21) (io.ascii := 0x03) // C
      is(0x22) (io.ascii := 0x18) // X
      is(0x23) (io.ascii := 0x04) // D
      is(0x24) (io.ascii := 0x05) // E
      is(0x25) (io.ascii := 0x24) // $
      is(0x26) (io.ascii := 0x23) // #
      is(0x29) (io.ascii := 0x20) // space
      is(0x2a) (io.ascii := 0x16) // V
      is(0x2b) (io.ascii := 0x06) // F
      is(0x2c) (io.ascii := 0x14) // T
      is(0x2d) (io.ascii := 0x12) // R
      is(0x2e) (io.ascii := 0x25) // %
      is(0x31) (io.ascii := 0x0e) // N
      is(0x32) (io.ascii := 0x02) // B
      is(0x33) (io.ascii := 0x08) // H
      is(0x34) (io.ascii := 0x07) // G
      is(0x35) (io.ascii := 0x19) // Y
      is(0x36) (io.ascii := 0x5e) // ^
      is(0x3a) (io.ascii := 0x0d) // M
      is(0x3b) (io.ascii := 0x0a) // J
      is(0x3c) (io.ascii := 0x15) // U
      is(0x3d) (io.ascii := 0x26) // &
      is(0x3e) (io.ascii := 0x2a) // *
      is(0x41) (io.ascii := 0x3c) // <
      is(0x42) (io.ascii := 0x0b) // K
      is(0x43) (io.ascii := 0x09) // I
      is(0x44) (io.ascii := 0x0f) // O
      is(0x45) (io.ascii := 0x29) // )
      is(0x46) (io.ascii := 0x28) // (
      is(0x49) (io.ascii := 0x3e) // >
      is(0x4a) (io.ascii := 0x3f) // ?
      is(0x4b) (io.ascii := 0x0c) // L
      is(0x4c) (io.ascii := 0x3a) // :
      is(0x4d) (io.ascii := 0x10) // P
      is(0x4e) (io.ascii := 0x5f) // _
      is(0x52) (io.ascii := 0x22) // "
      is(0x54) (io.ascii := 0x7b) // {
      is(0x55) (io.ascii := 0x2b) // +
      is(0x5a) (io.ascii := 0x0a) // Enter
      is(0x5b) (io.ascii := 0x7d) // }
      is(0x5d) (io.ascii := 0x7c) // |
      is(0x66) (io.ascii := 0x08)
      is(0x76) (io.ascii := 0x1b)
      is(0x71) (io.ascii := 0x7f) // del
      default  (io.ascii := 0x2e)
    }
  } elsewhen (io.shift) {
    switch(io.scanCode) {
      is(0x0d) (io.ascii := 0x09)
      is(0x0e) (io.ascii := 0x7e) // ~
      is(0x15) (io.ascii := 0x51) // Q
      is(0x16) (io.ascii := 0x21) // !
      is(0x1b) (io.ascii := 0x53) // S
      is(0x1a) (io.ascii := 0x5a) // Z
      is(0x1c) (io.ascii := 0x41) // A
      is(0x1d) (io.ascii := 0x57) // W
      is(0x1e) (io.ascii := 0x40) // @
      is(0x21) (io.ascii := 0x43) // C
      is(0x22) (io.ascii := 0x58) // X
      is(0x23) (io.ascii := 0x44) // D
      is(0x24) (io.ascii := 0x45) // E
      is(0x25) (io.ascii := 0x24) // $
      is(0x26) (io.ascii := 0x23) // #
      is(0x29) (io.ascii := 0x20) // space
      is(0x2a) (io.ascii := 0x56) // V
      is(0x2b) (io.ascii := 0x46) // F
      is(0x2c) (io.ascii := 0x54) // T
      is(0x2d) (io.ascii := 0x52) // R
      is(0x2e) (io.ascii := 0x25) // %
      is(0x31) (io.ascii := 0x4e) // N
      is(0x32) (io.ascii := 0x42) // B
      is(0x33) (io.ascii := 0x48) // H
      is(0x34) (io.ascii := 0x47) // G
      is(0x35) (io.ascii := 0x59) // Y
      is(0x36) (io.ascii := 0x5e) // ^
      is(0x3a) (io.ascii := 0x4d) // M
      is(0x3b) (io.ascii := 0x4a) // J
      is(0x3c) (io.ascii := 0x55) // U
      is(0x3d) (io.ascii := 0x26) // &
      is(0x3e) (io.ascii := 0x2a) // *
      is(0x41) (io.ascii := 0x3c) // <
      is(0x42) (io.ascii := 0x4b) // K
      is(0x43) (io.ascii := 0x49) // I
      is(0x44) (io.ascii := 0x4f) // O
      is(0x45) (io.ascii := 0x29) // )
      is(0x46) (io.ascii := 0x28) // (
      is(0x49) (io.ascii := 0x3e) // >
      is(0x4a) (io.ascii := 0x3f) // ?
      is(0x4b) (io.ascii := 0x4c) // L
      is(0x4c) (io.ascii := 0x3a) // :
      is(0x4d) (io.ascii := 0x50) // P
      is(0x4e) (io.ascii := 0x5f) // _
      is(0x52) (io.ascii := 0x22) // "
      is(0x54) (io.ascii := 0x7b) // {
      is(0x55) (io.ascii := 0x2b) // +
      is(0x5a) (io.ascii := 0x0a) // Enter
      is(0x5b) (io.ascii := 0x7d) // }
      is(0x5d) (io.ascii := 0x7c) // |
      is(0x66) (io.ascii := 0x08)
      is(0x76) (io.ascii := 0x1b)
      is(0x71) (io.ascii := 0x7f) // del
      default  (io.ascii := 0x2e)
    }
  } otherwise {
    switch(io.scanCode) {
      is(0x05) (io.ascii := 0xa1) // F1
      is(0x06) (io.ascii := 0xa2) // F2
      is(0x04) (io.ascii := 0xa3) // F3
      is(0x0C) (io.ascii := 0xa4) // F4
      is(0x03) (io.ascii := 0xa5) // F5
      is(0x0B) (io.ascii := 0xa6) // F6
      is(0x83) (io.ascii := 0xA7) // F7
      is(0x0A) (io.ascii := 0xA8) // F8
      is(0x01) (io.ascii := 0xA9) // F9
      is(0x09) (io.ascii := 0xAA) // F10
      is(0x78) (io.ascii := 0xAB) // F11
      is(0x07) (io.ascii := 0xAC) // F12
      is(0x0d) (io.ascii := 0x09) // tab
      is(0x0e) (io.ascii := 0x60) // `
      is(0x15) (io.ascii := 0x71) // q
      is(0x16) (io.ascii := 0x31) // 1
      is(0x1a) (io.ascii := 0x7a) // z
      is(0x1b) (io.ascii := 0x73) // s
      is(0x1c) (io.ascii := 0x61) // a
      is(0x1d) (io.ascii := 0x77) // w
      is(0x1e) (io.ascii := 0x32) // 2
      is(0x21) (io.ascii := 0x63) // c
      is(0x22) (io.ascii := 0x78) // x
      is(0x23) (io.ascii := 0x64) // d
      is(0x24) (io.ascii := 0x65) // e
      is(0x25) (io.ascii := 0x34) // 4
      is(0x26) (io.ascii := 0x33) // 3
      is(0x29) (io.ascii := 0x20) // space
      is(0x2a) (io.ascii := 0x76) // v
      is(0x2b) (io.ascii := 0x66) // f
      is(0x2c) (io.ascii := 0x74) // t
      is(0x2d) (io.ascii := 0x72) // r
      is(0x2e) (io.ascii := 0x35) // 5
      is(0x31) (io.ascii := 0x6e) // n
      is(0x32) (io.ascii := 0x62) // b
      is(0x33) (io.ascii := 0x68) // h
      is(0x34) (io.ascii := 0x67) // g
      is(0x35) (io.ascii := 0x79) // y
      is(0x36) (io.ascii := 0x36) // 6
      is(0x3a) (io.ascii := 0x6d) // m
      is(0x3b) (io.ascii := 0x6a) // j
      is(0x3c) (io.ascii := 0x75) // u
      is(0x3d) (io.ascii := 0x37) // 7
      is(0x3e) (io.ascii := 0x38) // 8
      is(0x41) (io.ascii := 0x2c) // ,
      is(0x42) (io.ascii := 0x6b) // k
      is(0x43) (io.ascii := 0x69) // i
      is(0x44) (io.ascii := 0x6f) // o
      is(0x45) (io.ascii := 0x30) // 0
      is(0x46) (io.ascii := 0x39) // 9
      is(0x49) (io.ascii := 0x2e) // .
      is(0x4a) (io.ascii := 0x2f) // /
      is(0x4b) (io.ascii := 0x6c) // l
      is(0x4c) (io.ascii := 0x3b) // ;
      is(0x4d) (io.ascii := 0x70) // p
      is(0x4e) (io.ascii := 0x2d) // -
      is(0x52) (io.ascii := 0x27) // '
      is(0x54) (io.ascii := 0x5b) // [
      is(0x55) (io.ascii := 0x3d) // =
      is(0x58) (io.ascii := 0xAD) // CAPS lock
      is(0x7E) (io.ascii := 0xAE) // Scroll lock
      is(0x77) (io.ascii := 0xAF) // Num lock
      is(0x5a) (io.ascii := 0x0a) // Enter
      is(0x5b) (io.ascii := 0x5d) // ]
      is(0x5d) (io.ascii := 0x5c) // \
      is(0x66) (io.ascii := 0x08) // backspace
      is(0x69) (io.ascii := 0x95) // end
      is(0x6b) (io.ascii := 0x93) // left
      is(0x6c) (io.ascii := 0x94) // home
      is(0x70) (io.ascii := 0x98) // insert
      is(0x71) (io.ascii := 0x7f) // del
      is(0x72) (io.ascii := 0x92) // down
      is(0x74) (io.ascii := 0x91) // right
      is(0x75) (io.ascii := 0x90) // up
      is(0x76) (io.ascii := 0x1b) // escape
      is(0x7a) (io.ascii := 0x97) // pg down
      is(0x7d) (io.ascii := 0x96) // pg up
      is(0xFA) (io.ascii := 0xFA) // keyboard ACK code
      default  (io.ascii := 0x2e) // '.' used for unlisted characters.
    }
  }
}

