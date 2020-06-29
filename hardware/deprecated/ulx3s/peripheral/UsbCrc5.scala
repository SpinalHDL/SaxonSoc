package saxon.board.ulx3s.peripheral

import spinal.core._
import spinal.lib._

class UsbCrc5 extends Component {
  val io = new Bundle {
    val crc_i = in Bits(5 bits)
    val data_i = in Bits(11 bits)
    val crc_o = out Bits(5 bits)
  }
  
  io.crc_o(0) := io.data_i(10) ^ io.data_i(9) ^ io.data_i(6) ^ 
                 io.data_i(5) ^ io.data_i(3) ^ io.data_i(0) ^ 
                 io.crc_i(0) ^ io.crc_i(3) ^ io.crc_i(4)
 
  io.crc_o(1) := io.data_i(10) ^ io.data_i(7) ^ io.data_i(6) ^ 
                 io.data_i(4) ^ io.data_i(1) ^ 
                 io.crc_i(0) ^ io.crc_i(1) ^ io.crc_i(4)
 
  io.crc_o(2) := io.data_i(10) ^ io.data_i(9) ^ io.data_i(8) ^ 
                 io.data_i(7) ^ io.data_i(6) ^ io.data_i(3) ^ 
                 io.data_i(2) ^ io.data_i(0) ^ io.crc_i(0) ^ 
                 io.crc_i(1) ^ io.crc_i(2) ^ io.crc_i(3) ^ io.crc_i(4)
 
  io.crc_o(3) := io.data_i(10) ^ io.data_i(9) ^ io.data_i(8) ^ 
                 io.data_i(7) ^ io.data_i(4) ^ io.data_i(3) ^ io.data_i(1) ^ 
                 io.crc_i(1) ^ io.crc_i(2) ^ io.crc_i(3) ^ io.crc_i(4)
 
  io.crc_o(4) := io.data_i(10) ^ io.data_i(9) ^ io.data_i(8) ^ 
                 io.data_i(5) ^ io.data_i(4) ^ io.data_i(2) ^ 
                 io.crc_i(2) ^ io.crc_i(3) ^ io.crc_i(4)
}
 
