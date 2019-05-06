package saxon

import spinal.core._
import spinal.lib._
import spinal.lib.blackbox.lattice.ice40.{SB_PLL40_CORE, SB_PLL40_PAD, SB_PLL40_PAD_CONFIG}
import spinal.lib.io.{InOutWrapper, TriState}



//Up5kAreaEvn creative board specific toplevel.
case class SaxonUp5kEvn(p : SaxonSocParameters) extends Component{
  val io = new Bundle {
    val ICE_CLK  = in  Bool()

    val ICE_SS   = out Bool()
    val ICE_SCK  = out Bool()
    val ICE_MOSI = inout(Analog(Bool))
    val ICE_MISO = inout(Analog(Bool))

    //Switches
    val IOT_37A = inout(Analog(Bool))
    val IOT_36B = inout(Analog(Bool))
    val IOT_44B = inout(Analog(Bool))
    val IOT_49A = inout(Analog(Bool))

    //Gpio
    val IOB_22A = inout(Analog(Bool))
    val IOB_23B = inout(Analog(Bool))
    val IOB_24A = inout(Analog(Bool))
    val IOB_25B = inout(Analog(Bool))


    val IOT_13B = out Bool()
    val IOT_16A = in  Bool()

    val IOB_29B = in  Bool()
    val IOB_31B = in  Bool()
    val IOB_20A = out Bool()
    val IOB_18A = in  Bool()

    val LED_BLUE  = out Bool()
    val LED_GREEN = out Bool()
    val LED_RED   = out Bool()
  }
  noIoPrefix()

  val clkBuffer = SB_GB()
  clkBuffer.USER_SIGNAL_TO_GLOBAL_BUFFER <> io.ICE_CLK


//  val pll = SB_PLL40_CORE(SB_PLL40_PAD_CONFIG(
//    DIVR = B"0000",
//    DIVF = B"0111111",
//    DIVQ = B"100",
//    FILTER_RANGE = "001",
//    FEEDBACK_PATH = "SIMPLE",
//    DELAY_ADJUSTMENT_MODE_FEEDBACK = "FIXED",
//    FDA_FEEDBACK = B"0000",
//    DELAY_ADJUSTMENT_MODE_RELATIVE = "FIXED",
//    FDA_RELATIVE = B"0000",
//    SHIFTREG_DIV_MODE = B"00",
//    PLLOUT_SELECT = "GENCLK",
//    ENABLE_ICEGATE = False
//  ))
//
//  pll.REFERENCECLK <> io.ICE_CLK
//  pll.RESETB := True
//  pll.BYPASS := False
//
//  val pllGb = SB_GB(pll.PLLOUTGLOBAL)
//  val clkReg = ClockDomain(clkBuffer.GLOBAL_BUFFER_OUTPUT)(Reg(Bool))
//  clkReg := !clkReg
//  val pllReg = ClockDomain(pllGb)(BufferCC(clkReg))
//  io.IOB_24A := pllReg


//  val testGb = SB_GB(io.IOT_37A)
//  val divider = ClockDomain(testGb) (Reg(UInt(2 bits)))
//  divider := divider + 1
//  val clks = divider.asBools.map(SB_GB(_))
//
//  val clkReg = ClockDomain(testGb)(Reg(Bool))
//  clkReg := !clkReg
//
//
//  val chain : List[Bool] = List.tabulate(2)(i => ClockDomain(clks(i))(Reg(Bool)))
//  (chain, clkReg :: chain.dropRight(1)).zipped.foreach(_ := _.addTag(crossClockDomain))
//  io.IOB_24A := chain.last


//  val soc = SaxonSoc(p)
  val soc = new PluginComponent(new SaxonDocDefault)
  import soc.generator._
  clockCtrl.io.clk   <> clkBuffer.GLOBAL_BUFFER_OUTPUT
  clockCtrl.io.reset <> False
  uartA.logic.uart.txd <> io.IOT_13B
  uartA.logic.uart.rxd <> io.IOT_16A


  def ioSbComb(io : Bool, design : TriState[Bool]): Unit ={
    val bb = SB_IO("101001").setCompositeName(io, "SB")
    bb.PACKAGE_PIN <> io
    bb.D_IN_0 <> design.read
    bb.D_OUT_0 <> design.write
    bb.OUTPUT_ENABLE <> design.writeEnable
  }

  ioSbComb(io.IOT_37A, gpioA.io.gpio(0))
  ioSbComb(io.IOT_36B, gpioA.io.gpio(1))
  ioSbComb(io.IOT_44B, gpioA.io.gpio(2))
  ioSbComb(io.IOT_49A, gpioA.io.gpio(3))
  ioSbComb(io.IOB_22A, gpioA.io.gpio(4))
  ioSbComb(io.IOB_23B, gpioA.io.gpio(5))
  ioSbComb(io.IOB_24A, gpioA.io.gpio(6))
  ioSbComb(io.IOB_25B, gpioA.io.gpio(7))
//  soc.io.gpioA.read := io.IOT_49A ## io.IOT_44B ## io.IOT_36B ## io.IOT_37A ## B"0000"

  if(p.withJtag) {
    system.cpu.jtag.tms <> io.IOB_29B
    system.cpu.jtag.tdi <> io.IOB_31B
    system.cpu.jtag.tdo <> io.IOB_20A
    system.cpu.jtag.tck <> io.IOB_18A
  } else {
    io.IOB_20A := False
  }


  val ledDriver = SB_RGBA_DRV()
  ledDriver.CURREN   := True
  ledDriver.RGBLEDEN := True
  ledDriver.RGB0PWM  := gpioA.io.gpio.write(0)
  ledDriver.RGB1PWM  := gpioA.io.gpio.write(1)
  ledDriver.RGB2PWM  := gpioA.io.gpio.write(2)

  ledDriver.RGB0 <> io.LED_BLUE
  ledDriver.RGB1 <> io.LED_GREEN
  ledDriver.RGB2 <> io.LED_RED


  List(io.ICE_SS,
    io.ICE_SCK,
    io.ICE_MOSI,
    io.ICE_MISO).flatMap(_.flatten).filter(_.isOutput).foreach(_.assignDontCare())
  val xipIo = new ClockingArea(xip.implicitCd) {
    RegNext(xip.logic.flash.ss.asBool) <> io.ICE_SS

    val sclkIo = SB_IO_SCLK()
    sclkIo.PACKAGE_PIN <> io.ICE_SCK
    sclkIo.CLOCK_ENABLE := True

    sclkIo.OUTPUT_CLK := ClockDomain.current.readClockWire
    sclkIo.D_OUT_0 <> xip.logic.flash.sclk.write(0)
    sclkIo.D_OUT_1 <> RegNext(xip.logic.flash.sclk.write(1))

    val datas = for ((data, pin) <- (xip.logic.flash.data, List(io.ICE_MOSI, io.ICE_MISO).reverse).zipped) yield new Area {
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




//Scala main used to generate the Up5kAreaEvn toplevel
object SaxonUp5kEvn{
  def main(args: Array[String]) {
    SpinalRtlConfig.generateVerilog(InOutWrapper(SaxonUp5kEvn(SaxonSocParameters.up5kEvnDefault.withArgs(args))))
  }
}
