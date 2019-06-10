package saxon.board.blackice.peripheral

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb._
import spinal.lib.bus.misc._

import spinal.lib.generator.Generator
import saxon.Apb3DecoderGenerator

case class Tone() extends Bundle with IMasterSlave {
  val pin = Bool

  override def asMaster(): Unit = {
    out(pin)
  }
}

case class ToneCtrl(clockHz : Int = 50000000) extends Component {
  val io = new Bundle {
    val tone = master(Tone())
    val period = in UInt(32 bits)
    val duration = in UInt(32 bits)
    val clear = in Bool
    val done = out Bool
  }

  val prescaler = Reg(UInt(8 bits))
  val counter = Reg(UInt(32 bits))
  val timeCounter = Reg(UInt(32 bits))
  val millis = Reg(UInt(32 bits))
  val clockKHz = clockHz / 1000
  val clockMHz = clockKHz / 1000
  val toneOut = Reg(Bool)

  io.tone.pin := toneOut
  io.done := (millis === io.duration)

  when (io.clear) {
    millis := 0
  }

  when (!io.done) {
    // Count milliseconds
    when (timeCounter === (clockKHz - 1)) {
      millis := millis + 1
      timeCounter := 0
    } otherwise {
      timeCounter := timeCounter + 1
    }

    // Play tone for specified duration im millseconds
    prescaler := prescaler + 1
    when (prescaler === (clockMHz / 2 - 1)) {
      prescaler := 0
      counter := counter + 1
      when (counter === (io.period - 1)) {
        counter := 0
        toneOut := !toneOut
      }
    }
  } otherwise {
    toneOut := False
  }

  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : Int = 0) () = new Area {
    val busClearing  = False

    busCtrl.drive(io.period, baseAddress + 0)
    busCtrl.drive(io.duration, baseAddress + 4)
    busClearing setWhen(busCtrl.isWriting(baseAddress + 4))

    io.clear := busClearing
  }
}

/*
 * Period    -> 0x00 Write register set the period in microseconds
 * Duration  -> 0x04 Write register to set the duration
 **/
case class Apb3ToneCtrl(clockHz : Int = 50000000) extends Component {
  val io = new Bundle {
    val apb = slave(Apb3(Apb3Config(addressWidth = 8, dataWidth = 32)))
    val tone = master(Tone())
  }

  val busCtrl = Apb3SlaveFactory(io.apb)
  val toneCtrl = ToneCtrl()
  io.tone <> toneCtrl.io.tone

  toneCtrl.driveFrom(busCtrl)()
}

case class  Apb3ToneGenerator(apbOffset : BigInt)
                             (implicit decoder: Apb3DecoderGenerator) extends Generator{
  val clockHz = createDependency[Int]
  val tone = produceIo(logic.io.tone)
  val apb = produce(logic.io.apb)
  val logic = add task Apb3ToneCtrl(clockHz)

  decoder.addSlave(apb, apbOffset)
}

