package saxon.board.ulx3s.peripheral

import spinal.core._
import spinal.lib._

class UsbCrc16 extends Component {
  val io = new Bundle {
    val crc_i = in Bits(16 bits)
    val data_i = in Bits(8 bits)
    val crc_o = out Bits(16 bits)
  }
 
io.crc_o(15) := io.data_i(0) ^ io.data_i(1) ^ io.data_i(2) ^ io.data_i(3) ^ io.data_i(4) ^
                io.data_i(5) ^ io.data_i(6) ^ io.data_i(7) ^ io.crc_i(7) ^ io.crc_i(6) ^
                io.crc_i(5) ^ io.crc_i(4) ^ io.crc_i(3) ^ io.crc_i(2) ^
                io.crc_i(1) ^ io.crc_i(0)
io.crc_o(14) := io.data_i(0) ^ io.data_i(1) ^ io.data_i(2) ^ io.data_i(3) ^ io.data_i(4) ^ io.data_i(5) ^
                io.data_i(6) ^ io.crc_i(6) ^ io.crc_i(5) ^ io.crc_i(4) ^
                io.crc_i(3) ^ io.crc_i(2) ^ io.crc_i(1) ^ io.crc_i(0)
io.crc_o(13) := io.data_i(6) ^ io.data_i(7) ^ io.crc_i(7) ^ io.crc_i(6)
io.crc_o(12) := io.data_i(5) ^ io.data_i(6) ^ io.crc_i(6) ^ io.crc_i(5)
io.crc_o(11) := io.data_i(4) ^ io.data_i(5) ^ io.crc_i(5) ^ io.crc_i(4)
io.crc_o(10) := io.data_i(3) ^ io.data_i(4) ^ io.crc_i(4) ^ io.crc_i(3)
io.crc_o(9)  := io.data_i(2) ^ io.data_i(3) ^ io.crc_i(3) ^ io.crc_i(2)
io.crc_o(8)  := io.data_i(1) ^ io.data_i(2) ^ io.crc_i(2) ^ io.crc_i(1)
io.crc_o(7)  := io.data_i(0) ^ io.data_i(1) ^ io.crc_i(15) ^ io.crc_i(1) ^ io.crc_i(0)
io.crc_o(6)  := io.data_i(0) ^ io.crc_i(14) ^ io.crc_i(0)
io.crc_o(5)  := io.crc_i(13)
io.crc_o(4)  := io.crc_i(12)
io.crc_o(3)  := io.crc_i(11)
io.crc_o(2)  := io.crc_i(10)
io.crc_o(1)  := io.crc_i(9)
io.crc_o(0)  := io.data_i(0) ^ io.data_i(1) ^ io.data_i(2) ^ io.data_i(3) ^ io.data_i(4) ^ io.data_i(5) ^
                io.data_i(6) ^ io.data_i(7) ^ io.crc_i(8) ^ io.crc_i(7) ^ io.crc_i(6) ^
                io.crc_i(5) ^ io.crc_i(4) ^ io.crc_i(3) ^ io.crc_i(2) ^
                io.crc_i(1) ^ io.crc_i(0)
}
 
