package saxon.board.blackice.peripheral

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb._
import spinal.lib.bus.misc._

import spinal.lib.generator.Generator
import saxon.Apb3DecoderGenerator

case class Quadrature() extends Bundle with IMasterSlave {
  val quadA = Bool
  val quadB = Bool

  override def asMaster(): Unit = {
    in(quadA, quadB)
  }
}

case class QuadratureCtrl(width : Int) extends Component {
  val io = new Bundle {
    val quadrature = master(Quadrature())
    val position = out UInt(width bits)
  }

  val positionR = Reg(UInt(width bits))
  io.position := positionR

  val quadAr = Reg(Bits(3 bits))
  val quadBr = Reg(Bits(3 bits))

  quadAr := quadAr(1 downto 0) ## io.quadrature.quadA.asBits
  quadBr := quadBr(1 downto 0) ## io.quadrature.quadB.asBits

  when (quadAr(2) ^ quadAr(1) ^ quadBr(2) ^ quadBr(1)) {
    when (quadBr(2) ^ quadAr(1)) {
      when (!positionR.andR) {
        positionR := positionR + 1
      }
    } otherwise {
      when (positionR.orR) {
        positionR := positionR - 1
      }
    }
  }

  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : Int = 0) () = new Area {
    busCtrl.read(io.position,baseAddress)
  }
}

/*
 * Position -> 0x00 Read register to read the position
 **/
case class Apb3QuadratureCtrl(width: Int) extends Component {
  val io = new Bundle {
    val apb = slave(Apb3(Apb3Config(addressWidth = 8, dataWidth = 32)))
    val quadrature = master(Quadrature())
  }

  val busCtrl = Apb3SlaveFactory(io.apb)
  val quadratureCtrl = QuadratureCtrl(width)
  io.quadrature <> quadratureCtrl.io.quadrature

  quadratureCtrl.driveFrom(busCtrl)()
}

case class Apb3QuadratureGenerator(apbOffset : BigInt)
                             (implicit decoder: Apb3DecoderGenerator) extends Generator{
  val width = createDependency[Int]
  val quadrature = produceIo(logic.io.quadrature)
  val apb = produce(logic.io.apb)
  val logic = add task Apb3QuadratureCtrl(width)

  val quadA = produce(quadrature.quadA)
  val quadB = produce(quadrature.quadB)

  decoder.addSlave(apb, apbOffset)
}

