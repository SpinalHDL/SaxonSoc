package saxon.board.blackice.peripheral

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb._
import spinal.lib.bus.misc._

import spinal.lib.generator.Generator
import saxon.Apb3DecoderGenerator

case class PulseIn(width: Int) extends Bundle with IMasterSlave {
  val pins = Bits(width bits)

  override def asMaster(): Unit = {
    in(pins)
  }
}

case class PulseInCtrl(width: Int) extends Component {
  val io = new Bundle {
    val pulseIn = master(PulseIn(width))
    val timeout = in Vec(UInt(32 bits), width)
    val value = in Bits(width bits)
    val req = in Bits(width bits)
    val pulseLength = out Vec(UInt(32 bits), width)
  }

  val clockMhz = 50
  val req = Reg(Bits(width bits))

  for (i <- 0 until width) {
    val pulseOut = Reg(Vec(UInt(32 bits), width))
    io.pulseLength(i) := pulseOut(i)

    val state = Reg(Vec(UInt(2 bits), width))
    val counter = Reg(Vec(UInt(8 bits), width))
    val micros = Reg(Vec(UInt(32 bits), width))

    when (io.req(i)) {
      req(i) := True
      pulseOut(i) := 0
    }

    when (req(i)) {
      counter(i) := counter(i) + 1

      when (counter(i) === (clockMhz - 1)) {
        micros(i) := micros(i) + 1
        counter(i) := 0
      }

      when (io.timeout(i) > 0 && (micros(i) >= (io.timeout(i) -1))) {
        req(i) := False
        pulseOut(i) := U"32'hFFFFFFFF"
      } otherwise {
        when (state(i) === 0 && io.pulseIn.pins(i) =/= io.value(i)) {
          state(i) := 1
        } elsewhen (state(i) === 1 && io.pulseIn.pins(i) === io.value(i)) {
          state(i) := 2
          counter(i) := 0
          micros(i) := 0
        } elsewhen (state(i) === 2 && io.pulseIn.pins(i) =/= io.value(i)) {
          when (micros(i) === 0) {
            pulseOut(i) := U"32'hFFFFFFFF"
          } otherwise {
            pulseOut(i) := micros(i)
          }
          req(i) := False
        }
      }
    } otherwise {
      counter(i) := 0
      micros(i) := 0
      state(i) := 0
    }
  }

  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : Int = 0) () = new Area {

    for(i <- 0 until width) {
      val j = i * 16
      val busSetting = False
      busCtrl.drive(io.value(i), baseAddress + j)
      busCtrl.drive(io.timeout(i), baseAddress + j + 4)
      busCtrl.read(io.pulseLength(i) ,baseAddress + j + 8)

      busSetting setWhen(busCtrl.isWriting(baseAddress + j))

      io.req(i) := busSetting
    }
  }
}

/*
 * Value       -> 0x00 Write register to set the value of the pulse (HIGH or LOW)
 * Timeout     -> 0x04 Write register to set the timeout in microseconds
 * PulseLength -> 0x08 Read register to read the pulse length in microseconds
 **/
case class Apb3PulseInCtrl(width: Int) extends Component {
  val io = new Bundle {
    val apb = slave(Apb3(Apb3Config(addressWidth = 8, dataWidth = 32)))
    val pulseIn = master(PulseIn(width))
  }

  val busCtrl = Apb3SlaveFactory(io.apb)
  val pulseInCtrl = PulseInCtrl(width)
  io.pulseIn <> pulseInCtrl.io.pulseIn

  pulseInCtrl.driveFrom(busCtrl)()
}

case class  Apb3PulseInGenerator(apbOffset : BigInt)
                               (implicit decoder: Apb3DecoderGenerator) extends Generator{
  val width = createDependency[Int]
  val pulseIn = produceIo(logic.io.pulseIn)
  val apb = produce(logic.io.apb)
  val logic = add task Apb3PulseInCtrl(width)

  decoder.addSlave(apb, apbOffset)
}

