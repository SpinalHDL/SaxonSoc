package saxon.board.blackice.peripheral

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb._
import spinal.lib.bus.misc._

import spinal.lib.generator.Generator
import saxon.Apb3DecoderGenerator

case class SB_WARMBOOT() extends BlackBox {
  val S0 = in Bool()
  val S1 = in Bool()
  val BOOT = in Bool()
}

case class WarmbootCtrl() extends Component {
  val io = new Bundle {
    val boot = in Bool()
  }

  val warmBoot = SB_WARMBOOT()
  warmBoot.S0 := True
  warmBoot.S1 := False
  warmBoot.BOOT := io.boot

  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : Int = 0) () = new Area {
    busCtrl.drive(io.boot, baseAddress, bitOffset=0)
  }
}

/*
 * Boot    -> 0x00 Write register to cause a warm boot
 **/
case class Apb3WarmbootCtrl() extends Component {
  val io = new Bundle {
    val apb = slave(Apb3(Apb3Config(addressWidth = 8, dataWidth = 32)))
    
  }

  val busCtrl = Apb3SlaveFactory(io.apb)
  val warmbootCtrl = WarmbootCtrl()

  warmbootCtrl.driveFrom(busCtrl)()
}

case class  Apb3WarmbootGenerator(apbOffset : BigInt)
                             (implicit decoder: Apb3DecoderGenerator) extends Generator{
  val apb = produce(logic.io.apb)
  val logic = add task Apb3WarmbootCtrl()

  decoder.addSlave(apb, apbOffset)
}

