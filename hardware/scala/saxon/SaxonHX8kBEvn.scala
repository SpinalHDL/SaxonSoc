package saxon

import spinal.core._
import spinal.lib._

//HX8kBEvn breakout board specific toplevel.
case class SaxonHX8kBEvn(p : SaxonSocParameters) extends Component{
  val io = new Bundle {
    val ICE_CLK  = in  Bool()

    val ICE_SS   = out Bool()
    val ICE_SCK  = out Bool()
    val ICE_MOSI = inout(Analog(Bool))
    val ICE_MISO = inout(Analog(Bool))

    val ICE_TXD = out Bool()
    val ICE_RXD = in  Bool()

    val ICE_TMS = in  Bool()
    val ICE_TDI = in  Bool()
    val ICE_TDO = out Bool()
    val ICE_TCK = in  Bool()

    val LED = out Bits(8 bits)
  }.setName("")

  val clkBuffer = SB_GB()
  clkBuffer.USER_SIGNAL_TO_GLOBAL_BUFFER <> io.ICE_CLK

  val soc = SaxonSoc(p)

  soc.io.clk   <> clkBuffer.GLOBAL_BUFFER_OUTPUT
  soc.io.reset <> False

  soc.io.uartA.txd <> io.ICE_TXD
  soc.io.uartA.rxd <> io.ICE_RXD

  soc.io.gpioA.read := B"00000000"
  io.LED := soc.io.gpioA.write

  soc.io.jtag.tms <> io.ICE_TMS
  soc.io.jtag.tdi <> io.ICE_TDI
  soc.io.jtag.tdo <> io.ICE_TDO
  soc.io.jtag.tck <> io.ICE_TCK

  val xip = new ClockingArea(soc.systemClockDomain) {
    RegNext(soc.io.flash.ss.asBool) <> io.ICE_SS

    val sclkIo = SB_IO_SCLK()
    sclkIo.PACKAGE_PIN <> io.ICE_SCK
    sclkIo.CLOCK_ENABLE := True

    sclkIo.OUTPUT_CLK := ClockDomain.current.readClockWire
    sclkIo.D_OUT_0 <> soc.io.flash.sclk.write(0)
    sclkIo.D_OUT_1 <> RegNext(soc.io.flash.sclk.write(1))

    val datas = for ((data, pin) <- (soc.io.flash.data, List(io.ICE_MOSI, io.ICE_MISO).reverse).zipped) yield new Area {
      val dataIo = SB_IO_DATA()
      dataIo.PACKAGE_PIN := pin
      dataIo.CLOCK_ENABLE := True

      dataIo.OUTPUT_CLK := ClockDomain.current.readClockWire
      dataIo.OUTPUT_ENABLE <> data.writeEnable
      dataIo.D_OUT_0 <> data.write(0)
      dataIo.D_OUT_1 <> RegNext(data.write(1))

      dataIo.INPUT_CLK := ClockDomain.current.readClockWire
      data.read(0) := dataIo.D_IN_0
      data.read(1) := RegNext(dataIo.D_IN_1)
    }
  }
}

//Scala main used to generate the toplevel
object SaxonHX8kBEvn{
  def main(args: Array[String]) {
    SpinalRtlConfig.generateVerilog(SaxonHX8kBEvn(SaxonSocParameters.up5kEvnDefault.withArgs(args)))
  }
}
