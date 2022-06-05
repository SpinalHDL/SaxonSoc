package saxon.board.blackice.peripheral

import spinal.core._
import spinal.lib.generator._
import saxon.{Apb3DecoderGenerator, InterruptCtrl}
import spinal.lib.com.i2c._

case class Apb3I2cGenerator(apbOffset : Handle[BigInt] = Unset)
                           (implicit decoder: Apb3DecoderGenerator) extends Generator {
  val parameter = createDependency[I2cSlaveMemoryMappedGenerics]
  val i2c = produceIo(logic.io.i2c)
  val apb = produce(logic.io.apb)
  val interrupt = produce(logic.io.interrupt)
  val logic = add task Apb3I2cCtrl(parameter)

  decoder.addSlave(apb, apbOffset)

  def connectInterrupt(ctrl : InterruptCtrl, id : Int): Unit = {
    ctrl.addInterrupt(interrupt, id)
  }
}
