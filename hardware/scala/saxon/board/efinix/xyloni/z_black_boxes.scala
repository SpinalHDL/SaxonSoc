package saxon.board.efinix.xyloni_demoEx.bb

import spinal.core._


case class DcDpr(addrWidth: Int) extends BlackBox {
    val a_clk       = in  Bool()
    val a_wr        = in  Bool()
    val a_addr      = in  Bits(addrWidth bits)
    val a_din       = in  Bits(16 bits)
    val a_dout      = out Bits(16 bits)

    val b_clk       = in  Bool()
    val b_wr        = in  Bool()
    val b_addr      = in  Bits(addrWidth bits)
    val b_din       = in  Bits(16 bits)
    val b_dout      = out Bits(16 bits)
}

case class mixed_width_ram() extends BlackBox{

    val waddr = in  Bits(8 bits)
    val wdata = in  Bits(16 bits)
    val we, clk = in  Bool()
    val raddr = in  Bits(9 bits)
    val q = out Bits(8 bits)

}

case class DcFifo() extends BlackBox{
    
    val wr_clk_i        = in  Bool() 
    val rd_clk_i        = in  Bool()
    val wr_en_i         = in  Bool()
    val rd_en_i         = in  Bool()
    val rdata           = out Bits(16 bits)
    val wdata           = in  Bits(16 bits) 
    val prog_full_o     = out Bool()
    val prog_empty_o    = out Bool()
    val a_rst_i         = in  Bool()
    
    val full_o          = out Bool()
    val empty_o         = out Bool()
    val wr_datacount_o  = out Bits(10 bits)
    val rd_datacount_o  = out Bits(10 bits)
    val rst_busy        = out Bool()
}