package saxon

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb.{Apb3, Apb3SlaveFactory}


case class MachineTimer() extends Component{
  val io = new Bundle{
    val bus = slave(Apb3(4, 32))
    val mTimeInterrupt = out Bool()
  }

  val mapper = Apb3SlaveFactory(io.bus, dontCareReadData = true)
  val counter = Reg(UInt(32 bits)) init(0)
  val cmp = Reg(UInt(32 bits)) init(0)
  val interrupt = RegInit(False) setWhen(!(counter - cmp).msb) clearWhen(mapper.isWriting(0x8))
  counter := counter + 1
  io.mTimeInterrupt := interrupt
  mapper.read(counter, 0x0)
  mapper.write(cmp, 0x8)
}