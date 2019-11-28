package saxon

import spinal.core._
import spinal.lib._
import spinal.lib.bus.wishbone._
import spinal.lib.com.spi.ddr.SpiXdrMaster
import spinal.lib.generator._
import spinal.lib.io.TriStateArray

object sd_top{
  def wishboneConfig = WishboneConfig(
    addressWidth = 32,
    dataWidth = 32,
    selWidth = 4,
    useBTE = true,
    useCTI = true
  )
}




case class sd_top() extends BlackBox{
  val clk_50 = in  Bool()
  val clk_100 = in  Bool()
  val clk_200 = in  Bool()

  val reset_n = in  Bool()
  val sd_clk = in  Bool()
  val sd_cmd_i = in  Bool()
  val sd_cmd_o = out Bool()
  val sd_cmd_t = out Bool()
  val sd_dat_i = in  Bits(4 bits)
  val sd_dat_o = out Bits(4 bits)
  val sd_dat_t = out Bits(4 bits)

  val wbm_clk_o = out Bool()
  val wbm_adr_o = out Bits(32 bits)
  val wbm_dat_i = in  Bits(32 bits)
  val wbm_dat_o = out Bits(32 bits)
  val wbm_sel_o = out Bits(4 bits)
  val wbm_cyc_o = out Bool()
  val wbm_stb_o = out Bool()
  val wbm_we_o = out  Bool()
  val wbm_ack_i = in  Bool()
  val wbm_cti_o = out Bits(3 bits)
  val wbm_bte_o = out Bits(2 bits)

  val opt_enable_hs = in Bool()

  def toWishbone(): Wishbone ={
    val bus = Wishbone(sd_top.wishboneConfig)
    bus.CYC <> wbm_clk_o
    bus.STB <> wbm_stb_o
    bus.ACK <> wbm_ack_i
    bus.WE <> wbm_we_o
    bus.ADR := wbm_adr_o.asUInt
    bus.DAT_MISO <> wbm_dat_i
    bus.DAT_MOSI <> wbm_dat_o
    bus.BTE <> wbm_bte_o
    bus.CTI <> wbm_cti_o
    bus.SEL <> wbm_sel_o
    bus
  }
}


case class SdcardEmulatorIo() extends Bundle {
  val reset_n = in  Bool()
  val clk_50 = in  Bool()
  val clk_100 = in  Bool()
  val clk_200 = in  Bool()
  val wishbone = master(Wishbone(sd_top.wishboneConfig))
  val opt_enable_hs = in Bool()
}

case class SdcardEmulatorGenerator() extends Generator{
  val io = produce(logic.io)

  val logic = add task new Area{
    val ctrl = sd_top()
    val io = SdcardEmulatorIo()
    io.reset_n <> ctrl.reset_n
    io.clk_50 <> ctrl.clk_50
    io.clk_100 <> ctrl.clk_100
    io.clk_200 <> ctrl.clk_200
    ctrl.toWishbone().connectTo(io.wishbone)
    io.opt_enable_hs <> ctrl.opt_enable_hs
  }

  def connect(phy : Handle[SpiXdrMaster], ss : Handle[Bool]): Unit = Dependable(phy, logic, ss){
    val spi = phy.setAsDirectionLess.toSpi()
    logic.ctrl.sd_clk := spi.sclk
    logic.ctrl.sd_cmd_i := spi.data(0).write
    logic.ctrl.sd_dat_i := ss ## logic.ctrl.sd_dat_o(2 downto 0)
    spi.data.read := logic.ctrl.sd_dat_o(0) ## True
  }
}