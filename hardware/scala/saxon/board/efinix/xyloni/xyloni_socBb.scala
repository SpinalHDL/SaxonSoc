package saxon.board.efinix.xyloni_soc.bb

import spinal.core._


case class SoC() extends BlackBox{
    
    val ClkCore = in  Bool()
    val nReset = in  Bool()

    val jtag_inst1_tck = in  Bool()
    val jtag_inst1_tdi = in  Bool()
    val jtag_inst1_enable = in  Bool()
    val jtag_inst1_capture = in  Bool()
    val jtag_inst1_shift = in  Bool()
    val jtag_inst1_update = in  Bool()
    val jtag_inst1_reset = in  Bool()
    val jtag_inst1_tdo = out Bool()

    val system_gpioA_gpio_read = in  Bits(8 bits)
    val system_gpioA_gpio_write = out Bits(8 bits)
    val system_gpioA_gpio_writeEnable = out Bits(8 bits)
    val system_uartA_uart_txd = out Bool()
    val system_uartA_uart_rxd = in  Bool()

    val Bus32_wr = out Bool()
    val Bus32_rd = out Bool()
    val Bus32_A = out Bits(12 bits)
    val Bus32_Din = in  Bits(32 bits)
    val Bus32_Dout = out Bits(32 bits)
        
    val UsbDpr_b_clk = in  Bool()
    val UsbDpr_b_wr = in  Bool()
    val UsbDpr_b_addr = in  Bits(9 bits)
    val UsbDpr_b_din = in  Bits(16 bits)
    val UsbDpr_b_dout = out Bits(16 bits)

    val Bus16_wr = out Bool()
    val Bus16_A = out Bits(12 bits)
    val Bus16_Din = in  Bits(16 bits)
    val Bus16_Dout = out Bits(16 bits)
}

