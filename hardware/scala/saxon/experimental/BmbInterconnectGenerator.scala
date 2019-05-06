package saxon.experimental

import saxon.{PluginComponent, SaxonDocDefault, SpinalRtlConfig}
import spinal.core.{Area, log2Up}
import spinal.lib.bus.bmb._
import spinal.lib.bus.misc._
import spinal.lib._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class BmbInterconnectGenerator extends Generator{
  case class MasterModel(requirements : Handle[BmbParameter],
                         bus : Handle[Bmb]) extends Generator{
    var connector : (Bmb,Bmb) => Unit = defaultConnector

    dependencies += bus
    val logic = add task new Area{
      val busConnections = connections.filter(_.m == bus)
      val busSlaves = busConnections.map(c => slaves(c.s))
      val decoder = new BmbDecoder(bus.p, busSlaves.map(_.mapping.get))
      decoder.setCompositeName(bus, "decoder")
      connector(bus, decoder.io.input)
      for((connection, decoderOutput) <- (busConnections, decoder.io.outputs).zipped) {
        connection.decoder.load(decoderOutput)
      }
    }
  }

  case class SlaveModel(capabilities : Handle[BmbParameter],
                        requirements : Handle[BmbParameter],
                        bus : Handle[Bmb],
                        mapping: Handle[AddressMapping]) extends Generator{
    var connector: (Bmb, Bmb) => Unit = defaultConnector

    dependencies ++= List(bus, mapping)
    val logic = add task new Area{
      val busConnections = connections.filter(_.s == bus)
      val busMasters = busConnections.map(c => masters(c.m))
      val arbiter = new BmbArbiter(bus.p, busMasters.size, 3)
      arbiter.setCompositeName(bus, "arbiter")
      connector(arbiter.io.output, bus)
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
        val inputLengthWidth = busMasters.map(_.requirements.lengthWidth).max
        val inputContextWidth = busMasters.map(_.requirements.contextWidth).max
        val outputSourceWidth = inputSourceWidth + routerBitCount
        requirements.load(capabilities.copy(
          sourceWidth = outputSourceWidth,
          lengthWidth = inputLengthWidth,
          contextWidth = inputContextWidth
        ))
      }
    }
  }

  case class ConnectionModel(m : Handle[Bmb], s : Handle[Bmb]) extends Generator{
    var connector : (Bmb,Bmb) => Unit = defaultConnector
    val decoder, arbiter = Handle[Bmb]()
    dependencies ++= List(decoder, arbiter)
    val logic = add task new Area{
      connector(decoder, arbiter)
    }
  }

  def defaultConnector(m : Bmb, s : Bmb) : Unit = s << m

  val masters = mutable.LinkedHashMap[Handle[Bmb], MasterModel]()
  val slaves = mutable.LinkedHashMap[Handle[Bmb], SlaveModel]()
  val connections = ArrayBuffer[ConnectionModel]()

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
    slaves(bus) = SlaveModel(capabilities, requirements, bus, mapping)
  }

//  def addSlaves(orders : (Bmb,AddressMapping)*) : this.type = {
//    orders.foreach(order => addSlave(order._1,order._2))
//    this
//  }

    def addMaster(requirements : Handle[BmbParameter], bus : Handle[Bmb]) : Unit = {
      val model = new MasterModel(requirements, bus)
      masters(bus) = model
    }

//  def addMaster(bus : Handle[Bmb], accesses : Seq[Bmb] = Nil) : this.type = {
//    masters(bus) = MasterModel()
//    for(s <- accesses) connections += ConnectionModel(bus, s)
//    this
//  }
//
//  def addMasters(specs : (Bmb,Seq[Bmb])*) : this.type = {
//    specs.foreach(spec => addMaster(spec._1,spec._2))
//    this
//  }
//
  def addConnection(m : Handle[Bmb], s : Handle[Bmb]) : this.type = {
    connections += ConnectionModel(m, s)
    masters(m).dependencies += slaves(s).mapping
    slaves(s).requirementsGenerator.dependencies += masters(m).requirements
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
      allowUnalignedBurst = false,
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
      allowUnalignedBurst = false,
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

object BmpTopLevel{
  def main(args: Array[String]): Unit = {
    SpinalRtlConfig.generateVerilog(new PluginComponent(new BmpTopLevel))
  }
}
