package saxon

import java.io.RandomAccessFile

import spinal.core._
import spinal.lib._
import spinal.lib.bus.wishbone._
import spinal.lib.com.spi.SpiHalfDuplexMaster
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
    bus.CYC <> wbm_cyc_o
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


case class SdcardEmulatorIoSpinalSim(io : SdcardEmulatorIo,
                                     nsPeriod : Int,
                                     storagePath : String,
                                     c50 : Boolean = true,
                                     c100 : Boolean = false,
                                     c200 : Boolean = false,
                                     hs : Boolean = false) {
  import spinal.core.sim._

  fork{
    io.reset_n #= true
    sleep(0)
    io.reset_n #= false
    sleep(nsPeriod*100)
    io.reset_n #= true
  }

  ClockDomain(io.clk_50).forkStimulus(nsPeriod*20)
  if(c100) ClockDomain(io.clk_100).forkStimulus(nsPeriod*10)
  if(c200) ClockDomain(io.clk_200).forkStimulus(nsPeriod*5)
  io.opt_enable_hs #= hs

  val fs = new RandomAccessFile(storagePath, "rw")
  var fsAddress = -1l;
  io.wishbone.ACK #= false
  ClockDomain(io.wbm_clk_o, io.reset_n, config = ClockDomainConfig(clockEdge = FALLING, resetActiveLevel = LOW)).onSamplings{
    io.wishbone.ACK #= io.wishbone.STB.toBoolean
    if(io.wishbone.STB.toBoolean){
      val address = io.wishbone.ADR.toLong
      if(address != fsAddress) {
        fs.seek(address)
        fsAddress = address;
      }
      if(io.wishbone.WE.toBoolean){
        fs.writeInt(io.wishbone.DAT_MOSI.toLong.toInt)
      } else {
        io.wishbone.DAT_MISO #= (fs.readInt().toLong & 0xFFFFFFFFl)
      }
      fsAddress += 4
    }
  }
}

case class SdcardEmulatorIo() extends Bundle {
  val reset_n = in  Bool()
  val clk_50 = in  Bool()
  val clk_100 = in  Bool()
  val clk_200 = in  Bool()
  val wbm_clk_o = out Bool()
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
    io.wbm_clk_o <> ctrl.wbm_clk_o
    ctrl.toWishbone().connectTo(io.wishbone)
    io.opt_enable_hs <> ctrl.opt_enable_hs
  }

  def connectSpi(spi : Handle[SpiHalfDuplexMaster], ss : Handle[Bool]): Unit = Dependable(spi, logic, ss){
    spi.setAsDirectionLess
    logic.ctrl.sd_clk := spi.sclk
    logic.ctrl.sd_cmd_i := spi.data(0).write
    logic.ctrl.sd_dat_i := ss ## logic.ctrl.sd_dat_o(2 downto 0)
    spi.data.read := logic.ctrl.sd_dat_o(0) ## True
  }

  def connect(phy : Handle[SpiXdrMaster], ss : Handle[Bool]): Unit = {
    connectSpi(phy.produce(phy.toSpi()), ss)
  }

}