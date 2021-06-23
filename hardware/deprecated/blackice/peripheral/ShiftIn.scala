package saxon.board.blackice.perpheral

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb._
import spinal.lib.bus.misc._

import spinal.lib.generator.Generator
import saxon.Apb3DecoderGenerator

case class ShiftIn() extends Bundle with IMasterSlave {
  val dataPin = Bool
  val clockPin = Bool

  override def asMaster(): Unit = {
    in(dataPin)
    out(clockPin)
  }
}

case class ShiftInCtrl() extends Component {
  val io = new Bundle {
    val shiftIn = master(ShiftIn())
    val value = out UInt(8 bits)
    val req = in Bool()
    val preScale = in UInt(12 bits)
    val bitOrder = in Bool()
  }

  val shiftReg = Reg(UInt(8 bits))
  io.value := shiftReg

  val bitCounter = Reg(UInt(4 bits))
  val clockReg = Reg(Bool)
  val prescaler = Reg(UInt(12 bits))

  io.shiftIn.clockPin := clockReg

  when (io.req) {
    bitCounter := 8
    prescaler := 0
    shiftReg := 0
  }

  when (bitCounter > 0) {
    prescaler := prescaler + 1
    when (prescaler === (io.preScale - 1)) {
      prescaler := 0
      clockReg := !clockReg
      when (clockReg) {
        when (io.bitOrder) {
          shiftReg := shiftReg |<< 1
        } otherwise {
          shiftReg := shiftReg |>> 1
        } 
      } otherwise {
        bitCounter := bitCounter - 1
        when (io.bitOrder) {
          shiftReg(0) := io.shiftIn.dataPin
        } otherwise {
          shiftReg(7) := io.shiftIn.dataPin
        }
      }
    }
  } otherwise {
    clockReg := True
  }


  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : Int = 0) () = new Area {
    val busSetting = False 
    busCtrl.read(io.value,baseAddress)
    busCtrl.drive(io.preScale,baseAddress + 4)
    busCtrl.drive(io.bitOrder,baseAddress + 8)

    busSetting setWhen(busCtrl.isWriting(baseAddress + 8))

    io.req := busSetting
  }
}

/*
 * Value       -> 0x00 Read register to get the value of byte
 * Prescale    -> 0x04 Write register to set prescaler
 * Bitorder    -> 0x08 Write register to set the bit order
 **/
case class Apb3ShiftInCtrl() extends Component {
  val io = new Bundle {
    val apb = slave(Apb3(Apb3Config(addressWidth = 8, dataWidth = 32)))
    val shiftIn = master(ShiftIn())
  }

  val busCtrl = Apb3SlaveFactory(io.apb)
  val shiftInCtrl = ShiftInCtrl()
  io.shiftIn <> shiftInCtrl.io.shiftIn

  shiftInCtrl.driveFrom(busCtrl)()
}

case class  Apb3ShiftInGenerator(apbOffset : BigInt)
                               (implicit decoder: Apb3DecoderGenerator) extends Generator{
  val shiftIn = produceIo(logic.io.shiftIn)
  val apb = produce(logic.io.apb)
  val logic = add task Apb3ShiftInCtrl()

  decoder.addSlave(apb, apbOffset)
}

