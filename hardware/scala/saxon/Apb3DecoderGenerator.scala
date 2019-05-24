package saxon

import spinal.core.{Area, assert, log2Up}
import spinal.lib.bus.amba3.apb.{Apb3, Apb3Decoder}
import spinal.lib.bus.misc.SizeMapping
import spinal.lib.generator.{Generator, Handle}

import scala.collection.mutable.ArrayBuffer


case class Apb3DecoderGenerator() extends Generator {

  case class SlaveModel(slave: Handle[Apb3], address: BigInt) {
    def mapping = SizeMapping(address, (BigInt(1)) << slave.config.addressWidth)
  }

  val models = ArrayBuffer[SlaveModel]()
  val input = productOf(logic.inputBus)
  val inputConfig = productOf(logic.inputBus.config)

  def addSlave(slave: Handle[Apb3], address: BigInt): Unit = {
    dependencies += slave
    models += SlaveModel(slave, address)
  }

  val logic = add task new Area {
    val inputBus = Apb3(
      addressWidth = log2Up(models.map(m => m.mapping.end + 1).max),
      dataWidth = models.head.slave.config.dataWidth
    )
    for (m <- models) assert(m.slave.config.dataWidth == inputBus.config.dataWidth)
    val decoder = Apb3Decoder(
      master = inputBus,
      slaves = models.map(m => (m.slave.get, m.mapping))
    )
  }
}
