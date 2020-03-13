package saxon.board.ulx3s.peripheral

import spinal.core._
import spinal.lib._
import spinal.lib.graphic._
import spinal.lib.graphic.vga._
import spinal.lib.bus.amba3.apb.{Apb3, Apb3Config, Apb3SlaveFactory}
import spinal.lib.bus.misc.BusSlaveFactory

import spinal.lib.generator.Generator
import saxon.Apb3DecoderGenerator

case class Hdmi() extends Bundle with IMasterSlave{
  val gpdi_dp = Bits(4 bits)
  val gpdi_dn = Bits(4 bits)

  override def asMaster() = this.asOutput()
}

case class Apb3HdmiConsoleGenerator(apbOffset : BigInt)
                             (implicit decoder: Apb3DecoderGenerator) extends Generator{
  val hdmi = produceIo(logic.io.hdmi)
  val apb = produce(logic.io.apb)
  val logic = add task Apb3HdmiConsoleCtrl()
  val pixclk = produce(logic.io.pixclk)
  val pixclk_x5 = produce(logic.io.pixclk_x5)
  val resetn = produce(logic.io.resetn)

  decoder.addSlave(apb, apbOffset)
}

object Apb3HdmiConsoleCtrl{
  def getApb3Config = Apb3Config(
    addressWidth = 4,
    dataWidth    = 32
  )
}

case class Apb3HdmiConsoleCtrl() extends Component{
  val io = new Bundle {
    val apb = slave(Apb3(Apb3HdmiConsoleCtrl.getApb3Config))
    val hdmi = master(Hdmi())
    val pixclk = in Bool
    val pixclk_x5 = in Bool
    val resetn = in Bool
  }

  // Instantiate an HDMI console controller
  val hdmiConsoleCtrl = new HdmiConsoleCtrl()
  hdmiConsoleCtrl.io.pixclk := io.pixclk
  hdmiConsoleCtrl.io.resetn := io.resetn

  val vga2Hdmi = new Vga2Hdmi()
  vga2Hdmi.io.pixclk := io.pixclk
  vga2Hdmi.io.pixclk_x5 := io.pixclk_x5

  io.hdmi <> vga2Hdmi.io.hdmi

  vga2Hdmi.io.vga := hdmiConsoleCtrl.io.vga
  // VGA to HDMI converter needs signals blanked when colorEn is false
  when(!hdmiConsoleCtrl.io.vga.colorEn){
    vga2Hdmi.io.vga.color.r := 0
    vga2Hdmi.io.vga.color.g := 0
    vga2Hdmi.io.vga.color.b := 0
  }

  val busCtrl = Apb3SlaveFactory(io.apb)

  hdmiConsoleCtrl.driveFrom(busCtrl)
}

// Controller for sending characters to VGA/HDMI screen
// Has uart-style interface
class HdmiConsoleCtrl(rgbConfig: RgbConfig = RgbConfig(8, 8, 8)) extends Component{
  val io = new Bundle{
    val vga = master(Vga(rgbConfig))
    val chars = slave Flow(Bits(8 bits))
    val led = out Bits(8 bits)
    val pixclk = in Bool
    val resetn = in Bool
    val attributes = in Bits(9 bits)
  }

  // 8x8 character font
  val font = Mem(Bits(8 bits), wordCount= 8 * 256)
  font.initialContent = Tools.readmemh("hardware/synthesis/ulx3s/font.hex")

  val w = 80            // Width of screen in characters
  val h = 60            // Height of screen in characters
  val wBits = log2Up(w) // Number of bits for width
  val hBits = log2Up(h) // Nimber of bits for height

  // 80 x 60 character frame buffer
  val frameBuffer = Mem(Bits(17 bits), wordCount = w * h)

  val attributes = Reg(Bits(9 bits))
  attributes := ((io.attributes(2 downto 0) === io.attributes(5 downto 3)) 
                   ? B"000000111" 
                   | io.attributes)
  
  val lineStart = Vec(UInt(hBits + wBits bits), h) // Pointers to start of lines in the frame buffer
  val lineLength = Reg(Vec(UInt(wBits bits), h))   // Array of line lengths
  val currLine = Reg(UInt(hBits bits)) init 0 addTag(crossClockDomain) // The line being written to
  val linePos = Reg(UInt(wBits bits)) init 0       // The character position in the line being written

  // The line after the current one with wraparound. 
  // This is also the start line of the screen,
  // as the line being written to is always at the bottom of the screen
  val nextLine = (currLine < h - 1) ? (currLine + U(1, hBits bits)) | U(0, hBits bits)
  val startLine = (currLine < h - 5) ? (currLine + U(5, hBits bits)) | U(0, hBits bits)

  // Set up the line start and lengths arrays
  for (i <- 0 to h - 1) {
    lineStart(i) := i * w
    lineLength(i).init(0)
  }

  // Write incoming character to the next position in the current line in the frame buffer
  when (io.chars.valid) {
    // Put the character in the frame buffer, unless a newline or backspace
    when (io.chars.payload =/= 0x0a && io.chars.payload =/= 0x08) {
      frameBuffer(lineStart(currLine) + linePos) := attributes ## io.chars.payload
      linePos := linePos + 1
      lineLength(currLine) := lineLength(currLine) + 1
    }

    // Start new line when current full or newline character received
    when (linePos === w - 1 || io.chars.payload === 0x0a) {
      linePos := 0
      lineLength(nextLine) := U(0, wBits bits)
      currLine := nextLine
    } elsewhen (io.chars.payload === 0x08 && lineLength(currLine) > 0) {
      lineLength(currLine) := lineLength(currLine) - 1 // backspace
      linePos := linePos - 1
    }
  }

  val vgaClockDomain = ClockDomain(io.pixclk, io.resetn,
                                   config = ClockDomainConfig(
                                             resetKind = ASYNC,
                                             resetActiveLevel = LOW))
  val vgaClockingArea = new ClockingArea(vgaClockDomain){
    // Generate the VGA/HDMI screen
    val x = Reg(UInt(wBits + 3 bits)) init 0  // Pixel x coordinate on screen
    val y = Reg(UInt(hBits + 3 bits)) init 0  // Pixel y co-ordinate

    val black = Rgb(rgbConfig)
    val red = Rgb(rgbConfig)
    val green = Rgb(rgbConfig)
    val yellow = Rgb(rgbConfig)
    val blue = Rgb(rgbConfig)
    val magenta = Rgb(rgbConfig)
    val cyan = Rgb(rgbConfig)
    val white = Rgb(rgbConfig)

    black.r := U"00000000"
    black.g := U"00000000"
    black.b := U"00000000"

    red.r := U"11111111"
    red.g := U"00000000"
    red.b := U"00000000"

    green.r := U"00000000"
    green.g := U"11111111"
    green.b := U"00000000"

    yellow.r := U"11111111"
    yellow.g := U"11111111"
    yellow.b := U"00000000"

    blue.r := U"00000000"
    blue.g := U"00000000"
    blue.b := U"11111111"

    magenta.r := U"11111111"
    magenta.g := U"00000000"
    magenta.b := U"11111111"

    cyan.r := U"00000000"
    cyan.g := U"11111111"
    cyan.b := U"11111111"

    white.r := U"11111111"
    white.g := U"11111111"
    white.b := U"11111111"

    val colors = Vec(Rgb(rgbConfig), 8)
    colors(0) := black
    colors(1) := red
    colors(2) := green
    colors(3) := yellow
    colors(4) := blue
    colors(5) := magenta
    colors(6) := cyan
    colors(7) := white

    val screenStartLine = Reg(UInt(hBits bits)) init 1 // The line at the top of the screen

    io.led := screenStartLine.asBits.resized

    // The current row being output
    val currY = ((screenStartLine + y(hBits + 2 downto 3)) < h) ? 
          (screenStartLine + y(hBits + 2 downto 3)) | 
          (screenStartLine + y(hBits + 2 downto 3) - h)

    // Get the index to the character in the frame buffer
    val charIndex = RegNext(lineStart(currY) + x(wBits + 2 downto 3))

    // The current character (plus attributes) from the frame buffer
    val currChar = frameBuffer(charIndex)

    // The current row of the font or blank if past the end of the line
    val fontLine = (x(wBits + 2 downto 3) < lineLength(currY)) ? 
          font(currChar(7 downto 0).asUInt @@ y(2 downto 0)) | B(0, 8 bits)

    val foreColor = Reg(Rgb(rgbConfig))
    foreColor := colors(currChar(10 downto 8).asUInt)

    val backColor = Reg(Rgb(rgbConfig))
    backColor := colors(currChar(13 downto 11).asUInt)

    val pixel = fontLine(x(2 downto 0))     // Set for pixel visible 
    val color = Rgb(rgbConfig)

    color.r := pixel ? foreColor.r | backColor.r
    color.g := pixel ? foreColor.g | backColor.g
    color.b := pixel ? foreColor.b | backColor.b

    // The VGA/HDMI Controller at 640x480 60Hz resolution, 24-bit color
    val ctrl = new VgaCtrl(rgbConfig)
    ctrl.io.softReset := False
    ctrl.io.timings.setAs_h640_v480_r60
    ctrl.io.pixels.valid := True
    ctrl.io.pixels.payload := color
    ctrl.io.vga <> io.vga

    // Update x, y pixel co-ordinates on screen
    when (ctrl.io.pixels.ready) {
      x := x + 1
      when (x === w * 8 - 1) {
        x := 0
        y := y + 1
        when (y === h * 8 -1) {
          y := 0
          // Reset the line at the top of the screen, as it might have changed
          screenStartLine := startLine
        }
      }
    }
  }

  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : Int = 0) = new Area {
    busCtrl.createAndDriveFlow(Bits(8 bits), address = 0) >-> io.chars
    busCtrl.drive(io.attributes, address = 4)
  }
}

