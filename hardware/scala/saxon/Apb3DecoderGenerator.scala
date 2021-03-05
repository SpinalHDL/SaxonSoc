package saxon

import spinal.core._
import spinal.core.fiber._
import spinal.lib.bus.amba3.apb.{Apb3, Apb3Config, Apb3Decoder}
import spinal.lib.bus.misc.SizeMapping
import spinal.lib.generator.{Generator, MemoryConnection, SimpleBus}
//import vexriscv.plugin.{CfuBus, CfuParameter}

import scala.collection.mutable.ArrayBuffer


case class Apb3DecoderGenerator() extends Generator {

  case class SlaveModel(slave: Handle[Apb3], config : Handle[Apb3Config], address: Handle[BigInt]) {
    configGenerator.dependencies += config.setCompositeName(slave, "AAAAmiaou")
    configGenerator.dependencies += address.setCompositeName(slave, "BBBBmiaou")
    def mapping = SizeMapping(address, (BigInt(1)) << config.addressWidth)
  }

  val models = ArrayBuffer[SlaveModel]()

  val configGenerator = new Generator{val dummy = 0}
  val inputConfig = configGenerator produce Apb3Config(
    addressWidth = log2Up(models.map(m => m.mapping.end + 1).max),
    dataWidth = models.head.config.dataWidth
  )

  inputConfig produce(tags += new SimpleBus(input, BigInt(1) << inputConfig.addressWidth))

  val input = inputConfig.produce{
    for (m <- models) assert(m.config.dataWidth == inputConfig.dataWidth)
    Apb3(inputConfig)
  }


  def addSlave(slave: Handle[Apb3], address: Handle[BigInt]): Unit = addSlave(slave, slave.produce(slave.config), address)

  def addSlave(slave: Handle[Apb3], config : Handle[Apb3Config], address: Handle[BigInt]): Unit = {
    dependencies += slave
    dependencies += address
    models += SlaveModel(slave, config, address)
    tags += new MemoryConnection(input, slave, address)
  }

  dependencies += input
  val logic = add task new Area {

    val decoder = Apb3Decoder(
      master = input,
      slaves = models.map(m => (m.slave.get, m.mapping))
    )
  }
}

/*
case class CfuDecoderGenerator() extends Generator {

  case class SlaveModel(slave: Handle[CfuBus], config : Handle[CfuParameter], address: BigInt) {
    configGenerator.dependencies += config
    def mapping = SizeMapping(address, (BigInt(1)) << config.addressWidth)
  }

  val models = ArrayBuffer[SlaveModel]()

  val configGenerator = new Generator{val dummy = 0}
  val inputConfig = createDependency[CfuParameter]

  val input = inputConfig.produce{
    for (m <- models) assert(m.config.dataWidth == inputConfig.dataWidth)
    Apb3(inputConfig)
  }


  def addSlave(slave: Handle[Apb3], address: BigInt): Unit = addSlave(slave, slave.produce(slave.config), address)

  def addSlave(slave: Handle[Apb3], config : Handle[Apb3Config], address: BigInt): Unit = {
    dependencies += slave
    models += SlaveModel(slave, config, address)
    tags += new MemoryConnection(input, slave, address)
  }

  dependencies += input
  val logic = add task new Area {

    val decoder = Apb3Decoder(
      master = input,
      slaves = models.map(m => (m.slave.get, m.mapping))
    )
  }
}*/
