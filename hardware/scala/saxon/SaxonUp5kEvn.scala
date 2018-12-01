package saxon

import spinal.core._
import spinal.lib.com.spi.SpiMaster
import spinal.lib._
import spinal.lib.com.jtag.Jtag
import spinal.lib.com.spi.ddr.SpiXdrMaster
import spinal.lib.com.uart.Uart
import spinal.lib.io.TriStateArray



//Up5kAreaEvn creative board specific toplevel.
case class SaxonUp5kEvn(p : SaxonSocParameters) extends Component{
  val io = new Bundle {
    val iceClk  = in  Bool()
    val uart = master(Uart())
    val flash = master(SpiXdrMaster(p.flashCtrl.ctrl.spi))
    val jtag = slave(Jtag())
    val leds = new Bundle {
      val r,g,b = out Bool()
    }
    val switches = in Bits(4 bits)
  }

  val clkBuffer = SB_GB()
  clkBuffer.USER_SIGNAL_TO_GLOBAL_BUFFER <> io.iceClk

  val soc = SaxonSoc(p)

  soc.io.clk   <> clkBuffer.GLOBAL_BUFFER_OUTPUT
  soc.io.reset <> False
  soc.io.flash <> io.flash
  soc.io.uartA <> io.uart
  soc.io.gpioA.read := io.switches ## B"0000"
  soc.io.jtag <> io.jtag

  val ledDriver = SB_RGBA_DRV()
  ledDriver.CURREN   := True
  ledDriver.RGBLEDEN := True
  ledDriver.RGB0PWM  := soc.io.gpioA.write(0)
  ledDriver.RGB1PWM  := soc.io.gpioA.write(1)
  ledDriver.RGB2PWM  := soc.io.gpioA.write(2)

  ledDriver.RGB0 <> io.leds.b
  ledDriver.RGB1 <> io.leds.g
  ledDriver.RGB2 <> io.leds.r
}




//Scala main used to generate the Up5kAreaEvn toplevel
object SaxonUp5kEvn{
  def main(args: Array[String]) {
    SpinalRtlConfig.generateVerilog(SaxonUp5kEvn(SaxonSocParameters(
      ioClkFrequency = 12 MHz,
      ioSerialBaudRate = 115200
    ).withArgs(args)))
  }
}
