package saxon.board.blackice.peripheral

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb._
import spinal.lib.bus.misc._

import spinal.lib.generator.Generator
import saxon.Apb3DecoderGenerator

// Assumes Hitachi HD44780 compatible chip
case class LcdText() extends Bundle with IMasterSlave {
  val e = Bool
  val rs = Bool
  val dout = Bits(8 bits)

  override def asMaster(): Unit = {
    out(e, rs, dout)
  }
}

case class LcdTextCtrl(clockHz : Int = 50000000) extends Component {
  val io = new Bundle {
    val lcdText = master(LcdText())
    val enable = in Bool
    val data = in(Bits(9 bits))
  }

  val e = Reg(Bool) init False
  io.lcdText.e := e

  io.lcdText.rs := !io.data(8)
  io.lcdText.dout := io.data(7 downto 0)

  // Enable must me set for 220 nanoseconds
  val cycles = ((22 * clockHz) / 100000000) + 1
  val counter = Reg(UInt(log2Up(cycles) bits)) init 0

  when (counter >  0) {
    counter := counter - 1
  } otherwise {
    e := False
  }
 
  when (io.enable) {
    e := True
    counter := cycles
  }

  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : Int = 0) () = new Area {
    val busEnable  = False

    busCtrl.drive(io.data, baseAddress + 0)
    busEnable setWhen(busCtrl.isWriting(baseAddress + 0))

    io.enable := busEnable
  }
}

/*
 * Data   -> 0x00 Write register to set data or instruction - bit 8 set for instruction
 **/
case class Apb3LcdTextCtrl(clockHz : Int = 50000000) extends Component {
  val io = new Bundle {
    val apb = slave(Apb3(Apb3Config(addressWidth = 8, dataWidth = 32)))
    val lcdText = master(LcdText())
  }

  val busCtrl = Apb3SlaveFactory(io.apb)
  val lcdTextCtrl = LcdTextCtrl(clockHz)
  io.lcdText <> lcdTextCtrl.io.lcdText

  lcdTextCtrl.driveFrom(busCtrl)()
}

case class  Apb3LcdTextGenerator(apbOffset : BigInt)
                             (implicit decoder: Apb3DecoderGenerator) extends Generator{
  val clockHz = createDependency[Int]
  val lcdText = produceIo(logic.io.lcdText)
  val apb = produce(logic.io.apb)
  val logic = add task Apb3LcdTextCtrl(clockHz)

  decoder.addSlave(apb, apbOffset)
}

