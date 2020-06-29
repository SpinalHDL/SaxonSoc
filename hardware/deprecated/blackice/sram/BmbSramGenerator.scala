package saxon.board.blackice.sram

import spinal.lib.bus.misc.SizeMapping
import spinal.lib.generator.{BmbInterconnectGenerator, Dependable, Generator, Handle}
import spinal.lib.bus.bmb.{BmbParameter}


case class BmbSramGenerator(address: BigInt)
                           (implicit interconnect: BmbInterconnectGenerator) extends Generator {

  val layout = createDependency[SramLayout]
  val requirements = createDependency[BmbParameter]

  val bmb   = produce(logic.io.bus)
  val sram = produceIo(logic.io.sram)

  layout.produce{
    interconnect.addSlave(
      capabilities = BmbSramCtrl.bmbCapabilities(layout),
      requirements = requirements,
      bus = bmb,
      mapping = SizeMapping(address, layout.capacity)
    )
  }

  val logic = add task BmbSramCtrl(
    bmbParameter = requirements,
    sramLayout = layout
  )
}
