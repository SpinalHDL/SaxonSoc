package saxon.board.blackice.peripheral

import spinal.core._
import spinal.lib.generator.Generator
import saxon.Apb3DecoderGenerator
import spinal.lib.com.spi._

case class Apb3SpiMasterGenerator(apbOffset : BigInt)
                           (implicit decoder: Apb3DecoderGenerator) extends Generator {
  val parameter = createDependency[SpiMasterCtrlMemoryMappedConfig]
  val spi = produceIo(logic.io.spi)
  val apb = produce(logic.io.apb)
  val logic = add task Apb3SpiMasterCtrl(parameter)

  decoder.addSlave(apb, apbOffset)
}
