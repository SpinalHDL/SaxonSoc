package saxon.board.blackice.peripheral

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb._
import spinal.lib.bus.misc._

import spinal.lib.generator.Generator
import saxon.Apb3DecoderGenerator

case class Keypad() extends Bundle with IMasterSlave {
  val cols = Bits(4 bits)
  val rows = Bits(4 bits)

  override def asMaster(): Unit = {
    in(rows)
    out(cols)
  }
}

case class KeypadCtrl() extends Component {
  val io = new Bundle {
    val keypad = master(Keypad())
    val keys = out Bits(16 bits)
  }

  val cols = Reg(Bits(4 bits)) init B"1110"
  io.keypad.cols := cols

  val column = Reg(UInt(2 bits)) init 0

  val keys = Reg(Bits(16 bits)) init 0
  io.keys := keys

  for(i <- 0 until 4) {
    keys(i*4 + column) := !io.keypad.rows(i)
  }

  val prescaler = Reg(UInt(12 bits))
  prescaler := prescaler + 1

  when (prescaler === 1000) {
    column := column + 1
    cols := cols.rotateLeft(1)
    prescaler := 0
  }

  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : Int = 0) () = new Area {
    busCtrl.read(io.keys,baseAddress)
  }
}

class KeypadTest extends Component {
  val io = new Bundle {
    val keypad= master(Keypad())
    val leds = out Bits(16 bits)
  }

  val keypadCtrl = new KeypadCtrl()
  keypadCtrl.io.keypad <> io.keypad
  io.leds := keypadCtrl.io.keys
}

object KeypadTest {
  def main(args: Array[String]): Unit = {
    SpinalVerilog(new KeypadTest)
  }
}

/*
 * Keys -> 0x00 Read register to read the keys
 **/
case class Apb3KeypadCtrl() extends Component {
  val io = new Bundle {
    val apb = slave(Apb3(Apb3Config(addressWidth = 8, dataWidth = 32)))
    val keypad = master(Keypad())
  }

  val busCtrl = Apb3SlaveFactory(io.apb)
  val keypadCtrl = KeypadCtrl()
  io.keypad <> keypadCtrl.io.keypad

  keypadCtrl.driveFrom(busCtrl)()
}

case class  Apb3KeypadGenerator(apbOffset : BigInt)
                               (implicit decoder: Apb3DecoderGenerator) extends Generator{
  val keypad = produceIo(logic.io.keypad)
  val apb = produce(logic.io.apb)
  val logic = add task Apb3KeypadCtrl()

  decoder.addSlave(apb, apbOffset)
}

