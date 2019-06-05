package saxon.board.blackice.peripheral

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb._
import spinal.lib.bus.misc._

import spinal.lib.generator.Generator
import saxon.Apb3DecoderGenerator

case class Pwm(width:Int) extends Bundle with IMasterSlave {
  val pins = Bits(width bits)

  override def asMaster(): Unit = {
    out(pins)
  }
}

case class PwmCtrl(width: Int) extends Component {
  val io = new Bundle {
    val pwm = master(Pwm(width))
    val duty = in Vec(UInt(8 bits), width)
  }

  val counter = Reg(UInt(8 bits))
  counter := counter + 1

  for(i <- 0 until width) {
    io.pwm.pins(i) := counter <= io.duty(i) && io.duty(i) =/= 0
  }

  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : Int = 0) () = new Area {
    for (i <- 0 until width) {
      busCtrl.drive(io.duty(i), baseAddress + (i << 2))
    }
  }
}

/*
 * Duty -> 4 *n Write register to set the duty cycle value for PWM pin n
 **/
case class Apb3PwmCtrl(width : Int) extends Component {
  val io = new Bundle {
    val apb = slave(Apb3(Apb3Config(addressWidth = 8, dataWidth = 32)))
    val pwm = master(Pwm(width))
  }

  val busCtrl = Apb3SlaveFactory(io.apb)
  val pwmCtrl = PwmCtrl(width)
  io.pwm <> pwmCtrl.io.pwm

  pwmCtrl.driveFrom(busCtrl)()
}

case class  Apb3PwmGenerator(apbOffset : BigInt)
                             (implicit decoder: Apb3DecoderGenerator) extends Generator{
  val width = createDependency[Int]
  val pwm = produceIo(logic.io.pwm)
  val apb = produce(logic.io.apb)
  val logic = add task Apb3PwmCtrl(width)

  decoder.addSlave(apb, apbOffset)
}
