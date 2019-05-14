package saxon.experimental

import saxon.{SpinalRtlConfig}
import spinal.core.{Area, dontName, log2Up}
import spinal.lib.bus.bmb._
import spinal.lib.bus.misc._
import spinal.lib._

import scala.annotation.meta.field
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object BmbInterconnectGenerator{
  class ArbitrationKind
  val ROUND_ROBIN = new ArbitrationKind
  val STATIC_PRIORITY = new ArbitrationKind
}

case class BmbInterconnectGenerator() extends Generator{
  var defaultArbitration : BmbInterconnectGenerator.ArbitrationKind = BmbInterconnectGenerator.ROUND_ROBIN
  def setDefaultArbitration(kind : BmbInterconnectGenerator.ArbitrationKind): Unit ={
    defaultArbitration = kind
  }
  def setPriority(m : Handle[Bmb], priority : Int) = getMaster(m).priority = priority

  case class MasterModel(@dontName bus : Handle[Bmb]) extends Generator{
    var requirements = Handle[BmbParameter]
    var connector : (Bmb,Bmb) => Unit = defaultConnector
    var priority = 0

    dependencies += bus
    val logic = add task new Area{
      val busConnections = connections.filter(_.m == bus)
      val busSlaves = busConnections.map(c => slaves(c.s))
      val decoder = new BmbDecoder(bus.p, busSlaves.map(_.mapping.get), busSlaves.map(_.capabilities.get))
      decoder.setCompositeName(bus, "decoder")
      connector(bus, decoder.io.input)
      for((connection, decoderOutput) <- (busConnections, decoder.io.outputs).zipped) {
        connection.decoder.load(decoderOutput)
      }
    }
  }

  case class SlaveModel(@dontName bus : Handle[Bmb]) extends Generator{
    val capabilities = Handle[BmbParameter]
    val requirements = Handle[BmbParameter]
    var arbiterRequirements = Handle[BmbParameter]
    val mapping = Handle[AddressMapping]
    var connector: (Bmb, Bmb) => Unit = defaultConnector
    var requireUnburstify = false

    dependencies ++= List(bus, mapping)
    val logic = add task new Area{
      val busConnections = connections.filter(_.s == bus).sortBy(connection => getMaster(connection.m).priority).reverse
      val arbiter = new BmbArbiter(arbiterRequirements, busConnections.size, 3, lowerFirstPriority = defaultArbitration == BmbInterconnectGenerator.STATIC_PRIORITY)
      arbiter.setCompositeName(bus, "arbiter")
      val requireBurstSpliting = arbiterRequirements.lengthWidth != requirements.lengthWidth
      @dontName var busPtr = arbiter.io.output
      val burstSpliter = if(requireUnburstify){
        val c = BmbUnburstify(arbiterRequirements.get).setCompositeName(bus, "burstUnburstifier")
        c.io.input << busPtr
        busPtr = c.io.output
        c
      }
      connector(busPtr, bus)
      for((connection, arbiterInput) <- (busConnections, arbiter.io.inputs).zipped) {
        connection.arbiter.load(arbiterInput)
      }
    }

    val requirementsGenerator = this add new Generator{
      dependencies += capabilities

      add task {
        val busConnections = connections.filter(_.s == bus)
        val busMasters = busConnections.map(c => masters(c.m))
        val routerBitCount = log2Up(busConnections.size)
        val inputSourceWidth = busMasters.map(_.requirements.sourceWidth).max
        val inputContextWidth = busMasters.map(_.requirements.contextWidth).max
        val inputLengthWidth = busMasters.map(_.requirements.lengthWidth).max
        val inputAllowUnalignedByteBurst = busMasters.exists(_.requirements.allowUnalignedByteBurst)
        val inputAllowUnalignedWordBurst = busMasters.exists(_.requirements.allowUnalignedWordBurst)
        val outputLengthWidth = Math.min(capabilities.lengthWidth, inputLengthWidth)
        val outputSourceWidth = inputSourceWidth + routerBitCount

        assert(outputSourceWidth <= capabilities.sourceWidth)
        assert(inputContextWidth <= capabilities.contextWidth)


        val requireBurstSpliting = outputLengthWidth != inputLengthWidth
        if(requireBurstSpliting){
          assert(outputLengthWidth == log2Up(capabilities.get.byteCount) && !capabilities.allowUnalignedByteBurst)
          requireUnburstify = true
        }

        requirements.load(capabilities.copy(
          sourceWidth = outputSourceWidth,
          lengthWidth = outputLengthWidth,
          contextWidth = inputContextWidth + (if(requireUnburstify) 2 else 0)
        ))
        arbiterRequirements.load(capabilities.copy(
          sourceWidth = outputSourceWidth,
          lengthWidth = inputLengthWidth,
          contextWidth = inputContextWidth,
          allowUnalignedByteBurst = inputAllowUnalignedByteBurst,
          allowUnalignedWordBurst = inputAllowUnalignedWordBurst
        ))
      }
    }
  }

  class ConnectionModel(@dontName val m : Handle[Bmb],@dontName val s : Handle[Bmb]) extends Generator{
    var connector : (Bmb,Bmb) => Unit = defaultConnector
    @dontName val decoder, arbiter = Handle[Bmb]()

    dependencies ++= List(decoder, arbiter)
    val logic = add task new Area{
      connector(decoder, arbiter)
    }
  }

  def defaultConnector(m : Bmb, s : Bmb) : Unit = s << m

  @dontName val masters = mutable.LinkedHashMap[Handle[Bmb], MasterModel]()
  @dontName val slaves = mutable.LinkedHashMap[Handle[Bmb], SlaveModel]()
  @dontName val connections = ArrayBuffer[ConnectionModel]()

  def getMaster(key : Handle[Bmb]) = masters.getOrElseUpdate(key, new MasterModel(key))
  def getSlave(key : Handle[Bmb]) = slaves.getOrElseUpdate(key, new SlaveModel(key))

  def setConnector(bus : Handle[Bmb])( connector : (Bmb,Bmb) => Unit): Unit = (masters.get(bus), slaves.get(bus)) match {
    case (Some(m), _) =>    m.connector = connector
    case (None, Some(s)) => s.connector = connector
    case _ => ???
  }

  def setConnector(m : Handle[Bmb], s : Handle[Bmb])(connector : (Bmb,Bmb) => Unit): Unit = connections.find(e => e.m == m && e.s == s) match {
    case Some(c) => c.connector = connector
    case _ => ???
  }

  def addSlave(capabilities : Handle[BmbParameter],
               requirements : Handle[BmbParameter],
               bus : Handle[Bmb],
               mapping: Handle[AddressMapping]) : Unit = {
    val model = getSlave(bus)
    model.capabilities.merge(capabilities)
    model.requirements.merge(requirements)
    model.mapping.merge(mapping)
  }

  def addSlave(capabilities : Handle[BmbParameter],
               requirements : Handle[BmbParameter],
               bus : Handle[Bmb],
               address: BigInt) : Unit = {
    val model = getSlave(bus)
    model.capabilities.merge(capabilities)
    model.requirements.merge(requirements)
    Dependable(capabilities){
      model.mapping.load(SizeMapping(address, BigInt(1) << capabilities.addressWidth))
    }
  }


  def addMaster(requirements : Handle[BmbParameter], bus : Handle[Bmb], priority : Int = 0) : Unit = {
    val model = getMaster(bus)
    model.requirements = requirements
    model.priority = priority
  }

  def addConnection(m : Handle[Bmb], s : Handle[Bmb]) : this.type = {
    connections += new ConnectionModel(m, s)
    getMaster(m).dependencies += getSlave(s).mapping
    getMaster(m).dependencies += getSlave(s).capabilities
    getSlave(s).requirementsGenerator.dependencies += getMaster(m).requirements
    this
  }

  def addConnection(m : Handle[Bmb], s : Seq[Handle[Bmb]]) : this.type = {
    for(e <- s) addConnection(m, e)
    this
  }
  def addConnection(l : (Handle[Bmb], Seq[Handle[Bmb]])*) : this.type = {
    for((m, s) <- l) addConnection(m, s)
    this
  }
}


class BmpTopLevel extends Generator{
  val interconnect = new BmbInterconnectGenerator

  def addRam(mapping: AddressMapping) = this add new Generator{
    val capabilities = BmbParameter(
      addressWidth  = 16,
      dataWidth     = 32,
      lengthWidth   = Int.MaxValue,
      sourceWidth   = Int.MaxValue,
      contextWidth  = Int.MaxValue,
      canRead       = true,
      canWrite      = true,
      allowUnalignedWordBurst = false,
      allowUnalignedByteBurst = false,
      maximumPendingTransactionPerId = Int.MaxValue
    )

    val requirements = Handle[BmbParameter]()
    val bus = Handle[Bmb]()

    interconnect.addSlave(
      capabilities  = capabilities,
      requirements  = requirements,
      bus           = bus,
      mapping       = mapping
    )

    dependencies += requirements
    val logic = add task new Area {
      val io = bus.load(master(Bmb(requirements)))

    }
  }

  def addCpu() = this add new Generator{
    val requirements = BmbParameter(
      addressWidth  = 16,
      dataWidth     = 32,
      lengthWidth   = 3,
      sourceWidth   = 0,
      contextWidth  = 0,
      canRead       = true,
      canWrite      = true,
      allowUnalignedWordBurst = false,
      allowUnalignedByteBurst = false,
      maximumPendingTransactionPerId = Int.MaxValue
    )

    val bus = Handle[Bmb]()

    interconnect.addMaster(
      requirements  = requirements,
      bus           = bus
    )


    val logic = add task new Area {
      val io = bus.load(slave(Bmb(requirements)))
    }
  }



  val ram0 = addRam(SizeMapping(0x00, 0x10))
  val ram1 = addRam(SizeMapping(0x10, 0x10))

  val cpu0 = addCpu()
  val cpu1 = addCpu()

  interconnect.addConnection(cpu0.bus, ram0.bus)
  interconnect.addConnection(cpu0.bus, ram1.bus)
  interconnect.addConnection(cpu1.bus, ram0.bus)
  interconnect.addConnection(cpu1.bus, ram1.bus)
}

//object BmpTopLevel{
//  def main(args: Array[String]): Unit = {
//    SpinalRtlConfig.generateVerilog(new PluginComponent(new BmpTopLevel))
//  }
//}
