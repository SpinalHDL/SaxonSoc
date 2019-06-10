package saxon.board.blackice.peripheral

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb._
import spinal.lib.bus.misc._

import spinal.lib.generator.Generator
import saxon.Apb3DecoderGenerator

case class PS2Keyboard() extends Bundle with IMasterSlave {
  val ps2Clk = Bool
  val ps2Data = Bool

  override def asMaster(): Unit = {
    in(ps2Clk, ps2Data)
  }
}

case class PS2KeyboardCtrl() extends Component {
  val io = new Bundle {
    val ps2 = master(PS2Keyboard())
    val read = master(Flow(Bits(8 bits)))
  }

  val dataOut = Reg(Bits(8 bits)) init 0
  val validOut = Reg(Bool) init False
  val errorOut = Reg(Bool) init False

  val ps2ClkIn = Reg(Bool) init True
  val ps2DataIn = Reg(Bool) init True
  ps2DataIn := io.ps2.ps2Data

  val clkFilter = Reg(Bits(8 bits)) init 0xff
  clkFilter := io.ps2.ps2Clk ## clkFilter(7 downto 1)

  val bitCount = Reg(UInt(4 bits)) init 0
  val shiftReg = Reg(Bits(9 bits)) init 0
  val parity = Reg(Bool) init False
  val clkEdge = Reg(Bool) init False

  clkEdge := False

  when (clkFilter === 0xFF) {
    ps2ClkIn := True
  } elsewhen (clkFilter === 0) {
    when (ps2ClkIn) {
      clkEdge := True
    }
    ps2ClkIn := False
  }

  validOut := False
  errorOut := False

  when (clkEdge) {
    when (bitCount === 0) {
      parity := False
      when (!ps2DataIn) {
        bitCount := bitCount + 1
      }
    } otherwise {
      when (bitCount < 10) {
        bitCount := bitCount + 1
        shiftReg := ps2DataIn ## shiftReg(8 downto 1)
        parity := parity ^ ps2DataIn
      } elsewhen (ps2DataIn) {
        bitCount := 0
        when (parity) {
          dataOut := shiftReg(7 downto 0)
          validOut := True
        } otherwise {
          errorOut := True
        }
      } otherwise {
        bitCount := 0
        errorOut := True
      }
    }
  }

  io.read.payload := dataOut
  io.read.valid := validOut

  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : Int = 0) () = new Area {
    val (stream, fifoOccupancy) = io.read.queueWithOccupancy(16)

    busCtrl.readStreamNonBlocking(stream, baseAddress, validBitOffset = 8, payloadBitOffset = 0)
  }
}

class PS2Test extends Component {
  val io = new Bundle {
    val ps2 = master(PS2Keyboard())
    val leds = out Bits(8 bits)
  }

  val ps2Ctrl = new PS2KeyboardCtrl()
  ps2Ctrl.io.ps2 <> io.ps2
  io.leds := ps2Ctrl.io.read.toReg()
}

object PS2Test {
  def main(args: Array[String]): Unit = {
    SpinalVerilog(new PS2Test)
  }
}

/*
 * Data -> 0x00 Read register to read the next byte of data
 **/
case class Apb3PS2KeyboardCtrl() extends Component {
  val io = new Bundle {
    val apb = slave(Apb3(Apb3Config(addressWidth = 8, dataWidth = 32)))
    val ps2 = master(PS2Keyboard())
  }

  val busCtrl = Apb3SlaveFactory(io.apb)
  val ps2KeyboardCtrl = PS2KeyboardCtrl()
  io.ps2 <> ps2KeyboardCtrl.io.ps2

  ps2KeyboardCtrl.driveFrom(busCtrl)()
}

case class  Apb3Ps2KeyboardGenerator(apbOffset : BigInt)
                             (implicit decoder: Apb3DecoderGenerator) extends Generator{
  val ps2 = produceIo(logic.io.ps2)
  val apb = produce(logic.io.apb)
  val logic = add task Apb3PS2KeyboardCtrl()

  decoder.addSlave(apb, apbOffset)
}

