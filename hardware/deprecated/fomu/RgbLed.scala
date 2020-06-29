package saxon.board.fomu

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb._
import spinal.lib.bus.misc._
import saxon.SB_RGBA_DRV

import spinal.lib.generator.Generator
import saxon.Apb3DecoderGenerator

case class RgbLed() extends Bundle with IMasterSlave {
  val red, green, blue = Bool

  override def asMaster(): Unit = {
    out(red, green, blue)
  }
}

case class RgbLedCtrl() extends Component {
  val io = new Bundle() {
    val rgbLed = master(RgbLed())
    val leds = in Bits(3 bits)
  }

  val ledDriver = SB_RGBA_DRV()
  ledDriver.CURREN   := True
  ledDriver.RGBLEDEN := True
  ledDriver.RGB0PWM  := io.leds(0)
  ledDriver.RGB1PWM  := io.leds(1)
  ledDriver.RGB2PWM  := io.leds(2)

  ledDriver.RGB0 <> io.rgbLed.blue
  ledDriver.RGB1 <> io.rgbLed.green
  ledDriver.RGB2 <> io.rgbLed.red

  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : Int = 0) () = new Area {
    busCtrl.drive(io.leds, baseAddress + 0)
  }
}

/*
 * Leds  -> 0x00 Write register to set leds
 **/
case class Apb3RgbLedCtrl() extends Component {
  val io = new Bundle {
    val apb = slave(Apb3(Apb3Config(addressWidth = 8, dataWidth = 32)))
    val rgbLed = master(RgbLed())
  }

  val busCtrl = Apb3SlaveFactory(io.apb)
  val rgbLedCtrl = RgbLedCtrl()
  io.rgbLed <> rgbLedCtrl.io.rgbLed

  rgbLedCtrl.driveFrom(busCtrl)()
}

case class Apb3RgbLedGenerator(apbOffset : BigInt)
                             (implicit decoder: Apb3DecoderGenerator) extends Generator{
  val rgbLed = produceIo(logic.io.rgbLed)
  val apb = produce(logic.io.apb)
  val logic = add task Apb3RgbLedCtrl()

  decoder.addSlave(apb, apbOffset)
}

