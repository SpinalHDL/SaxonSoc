package saxon.board.blackice.peripheral

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb._
import spinal.lib.bus.misc._

import spinal.lib.generator.Generator
import saxon.Apb3DecoderGenerator

case class ShiftOut() extends Bundle with IMasterSlave {
  val dataPin = Bool
  val clockPin = Bool

  override def asMaster(): Unit = {
    out(dataPin)
    out(clockPin)
  }
}

case class ShiftOutCtrl() extends Component {
  val io = new Bundle {
    val shiftOut = master(ShiftOut())
    val bitOrder = in Bool()
    val value = in UInt(8 bits)
    val preScale = in UInt(32 bits)
    val set = in Bool()
  }

  val prescaler = Reg(UInt(32 bits))
  val shiftReg = Reg(UInt(8 bits))
  val bitCounter = Reg(UInt(4 bits))
  val clockReg = Reg(Bool)

  io.shiftOut.dataPin := io.bitOrder ? shiftReg(7) | shiftReg(0)
  io.shiftOut.clockPin := clockReg

  when (io.set) {
    bitCounter := 8
    clockReg := False
    shiftReg := io.value
  }

  when (!(bitCounter === 0)) {
    prescaler := prescaler + 1
    when (prescaler === (io.preScale - 1)) {
      prescaler := 0
      clockReg := !clockReg
      when (clockReg) {
        bitCounter := bitCounter - 1
        when (io.bitOrder) {
          shiftReg := shiftReg |<< 1
        } otherwise {
          shiftReg := shiftReg |>> 1
        }
      }
    }
  }

  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : Int = 0) () = new Area {
    val busSetting = False

    busCtrl.drive(io.value, baseAddress)
    busCtrl.drive(io.bitOrder, baseAddress + 4)
    busCtrl.drive(io.preScale, baseAddress + 8)

    busSetting setWhen(busCtrl.isWriting(baseAddress))

    io.set := busSetting
  }
}

/*
 * Value    -> 0x00 Write register to set the value of the byte to shift out
 * Bitorder -> 0x04 Write register to set the bit order, MSBFIRST or LSBFIRST
 * Prescale -> 0x08 Prescaler for clock speed
 **/
case class Apb3ShiftOutCtrl() extends Component {
  val io = new Bundle {
    val apb = slave(Apb3(Apb3Config(addressWidth = 8, dataWidth = 32)))
    val shiftOut = master(ShiftOut())
  }

  val busCtrl = Apb3SlaveFactory(io.apb)
  val shiftOutCtrl = ShiftOutCtrl()
  io.shiftOut <> shiftOutCtrl.io.shiftOut

  shiftOutCtrl.driveFrom(busCtrl)()
}

case class  Apb3ShiftOutGenerator(apbOffset : BigInt)
                               (implicit decoder: Apb3DecoderGenerator) extends Generator{
  val shiftOut = produceIo(logic.io.shiftOut)
  val apb = produce(logic.io.apb)
  val logic = add task Apb3ShiftOutCtrl()

  decoder.addSlave(apb, apbOffset)
}

