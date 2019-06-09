package saxon.board.blackice.peripheral

import spinal.core._
import spinal.lib.generator.Generator
import saxon.Apb3DecoderGenerator
import spinal.lib.com.i2c._

case class Apb3I2cGenerator(apbOffset : BigInt)
                           (implicit decoder: Apb3DecoderGenerator) extends Generator {
  val parameter = createDependency[I2cSlaveMemoryMappedGenerics]
  val i2c = produceIo(logic.io.i2c)
  val apb = produce(logic.io.apb)
  val logic = add task Apb3I2cCtrl(parameter)

  decoder.addSlave(apb, apbOffset)
}
