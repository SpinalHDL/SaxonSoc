package saxon

import spinal.core._
import spinal.core.fiber._
import spinal.lib.IMasterSlave
import spinal.lib.bus.amba3.apb.{Apb3, Apb3CC, Apb3Config, Apb3SlaveFactory}
import spinal.lib.bus.bmb.{Bmb, BmbAccessCapabilities, BmbAccessParameter, BmbArbiter, BmbEg4S20Bram32K, BmbExclusiveMonitor, BmbIce40Spram, BmbImplicitPeripheralDecoder, BmbInvalidateMonitor, BmbInvalidationParameter, BmbOnChipRam, BmbOnChipRamMultiPort, BmbParameter, BmbInterconnectGenerator, BmbToApb3Bridge}
import spinal.lib.bus.misc.{AddressMapping, DefaultMapping, SizeMapping}
import spinal.lib.generator.{Dependable, Generator, MemoryConnection}
import spinal.lib.memory.sdram.SdramLayout
import spinal.lib.memory.sdram.sdr._
import spinal.lib.memory.sdram.xdr._
import spinal.lib.memory.sdram.xdr.phy.{Ecp5Sdrx2Phy, RtlPhy, SdrInferedPhy, XilinxS7Phy}

import scala.collection.mutable.ArrayBuffer


case class RtlPhyGenerator()extends Generator{
  val layout = createDependency[PhyLayout]
  val io = produceIo(logic.io.write)
  val logic = add task RtlPhy(layout)

  def connect(ctrl : SdramXdrBmbGenerator): Unit = {
    layout.produce{ ctrl.phyParameter.load(layout.get) }
    Dependable(ctrl, logic){ ctrl.logic.io.phy <> logic.io.ctrl }
  }
}


case class BmbOnChipRamGenerator(val address: Handle[BigInt] = Unset)
                                (implicit interconnect: BmbInterconnectGenerator) extends Generator {
  val size      = Handle[BigInt]
  val dataWidth = Handle[Int]
  var hexOffset = BigInt(0)
  val hexInit = Handle[String]
  val requirements = Handle[BmbAccessParameter]
  val ctrl = Handle(logic.io.bus)

  dependencies += address

  interconnect.addSlave(
    accessCapabilities = Handle(BmbOnChipRam.busCapabilities(size, dataWidth)),
    accessRequirements = requirements,
    bus = ctrl,
    mapping = Handle(SizeMapping(address, BigInt(1) << log2Up(size)))
  )


  val logic = Handle(BmbOnChipRam(
    p = requirements.toBmbParameter(),
    size = size,
    hexOffset = address.get + hexOffset,
    hexInit = hexInit
  ))
}





case class  BmbToApb3Decoder(address : Handle[BigInt] = Unset)(implicit interconnect: BmbInterconnectGenerator, apbDecoder : Apb3DecoderGenerator) extends Generator {
  val input = produce(logic.bridge.io.input)
  val requirements = createDependency[BmbAccessParameter]

  dependencies += address

  interconnect.addSlave(
    accessCapabilities = apbDecoder.inputConfig produce BmbToApb3Bridge.busCapabilities(
      addressWidth = apbDecoder.inputConfig.addressWidth,
      dataWidth = apbDecoder.inputConfig.dataWidth
    ),
    accessRequirements = requirements,
    bus = input,
    mapping = apbDecoder.inputConfig produce SizeMapping(address, BigInt(1) << apbDecoder.inputConfig.addressWidth)
  )

  dependencies += requirements
  dependencies += apbDecoder

  val logic = add task new Area {
    val bridge = BmbToApb3Bridge(
      apb3Config = apbDecoder.inputConfig,
      bmbParameter = requirements.toBmbParameter,
      pipelineBridge = false
    )
    apbDecoder.input << bridge.io.output
  }


  tags += new MemoryConnection(input, apbDecoder.input, 0)
}



case class  Apb3CCGenerator() extends Generator {
  val inputClockDomain, outputClockDomain = createDependency[ClockDomain]
  val apbConfig = createDependency[Apb3Config]

  val input = produce(logic.io.input)
  val output = produce(logic.io.output)

  val logic = add task Apb3CC(
    config = apbConfig,
    inputClock = inputClockDomain,
    outputClock = outputClockDomain
  )

  tags += new MemoryConnection(input, output, 0)
//  tags += new SimpleBus(input, BigInt(1) << inputConfig.addressWidth)

  def mapAt(apbOffset : BigInt)(implicit apbDecoder : Apb3DecoderGenerator) = apbDecoder.addSlave(input, apbOffset)
  def setOutput(apb : Handle[Apb3]): Unit = {
    apb.produce(apbConfig.load(apb.config))
    Dependable(apb, output){
      output >> apb
      tags += new MemoryConnection(output, apb, 0)
    }
  }
}



class Dummy{
  val interconnect = new {
    def addSlave(emitInvalidate : Handle[Boolean], canRead : Handle[Boolean]): Unit ={

    }
    def addMaster(emitRead : Handle[Boolean], canInvalidate : Handle[Boolean]): Unit ={

    }


  }

  val interconnect2 = new {
    case class BmbMasterRequirements(addressWidth : Int,
                                     dataWidth : Int,
                                     lengthWidth : Int,
                                     sourceWidth : Int,
                                     contextWidth : Int,
                                     alignment : BmbParameter.BurstAlignement.Kind = BmbParameter.BurstAlignement.WORD,
                                     alignmentMin : Int = 0,
                                     canRead : Boolean = true,
                                     canWrite : Boolean = true,
                                     canExclusive : Boolean = false)

    case class BmbSlaveRequirements(canInvalidate : Boolean = false,
                                    canSync : Boolean = false,
                                    invalidateLength : Int = 0,
                                    invalidateAlignment : BmbParameter.BurstAlignement.Kind = BmbParameter.BurstAlignement.WORD)

    case class BmbMasterCapabilities(canInvalidate : Boolean = false,
                                     canSync : Boolean = false,
                                     invalidateLength : Int = 0,
                                     invalidateAlignment : BmbParameter.BurstAlignement.Kind = BmbParameter.BurstAlignement.WORD)

    case class BmbSlaveCapabilities(addressWidth : Int,
                                    dataWidth : Int,
                                    lengthWidth : Int,
                                    sourceWidth : Int,
                                    contextWidth : Int,
                                    alignment : BmbParameter.BurstAlignement.Kind = BmbParameter.BurstAlignement.WORD,
                                    alignmentMin : Int = 0,
                                    canRead : Boolean = true,
                                    canWrite : Boolean = true)

//    def addSlave(requirements : Handle[BmbSlaveRequirements], capabilities : Handle[BmbSlaveCapabilities], config : Handle[BmbParameter]): Unit ={
//
//    }
//    def addMaster(requirements : Handle[BmbMasterRequirements], capabilities : Handle[BmbMasterCapabilities], config : Handle[BmbParameter]): Unit ={
//
//    }

    def addMaster(masterRequirements : Handle[BmbMasterRequirements],
                  slaveRequirements : Handle[BmbSlaveRequirements],
                  config : Handle[BmbParameter]): Unit ={

    }

    def addSlave(slaveRequirements : Handle[BmbSlaveRequirements],
                 masterRequirements : Handle[BmbMasterRequirements],
                 config : Handle[BmbParameter]): Unit ={

    }


  }
}

//
//case class BmbArbiterGenerator()
//                                        (implicit interconnect: BmbInterconnectGenerator) extends Generator {
//  val inputs = produce(arbiter.io.inputs)
//  val output = produce(arbiter.io.output)
//
//
//  val inputRequirements = createDependency[BmbParameter]
//  val outputRequirements = inputRequirements.derivate(BmbExclusiveMonitor.outputParameter)
//
//  interconnect.addSlave(
//    capabilities = BmbParameter(
//      addressWidth  = 32,
//      dataWidth     = 32,
//      lengthWidth   = Int.MaxValue,
//      sourceWidth   = Int.MaxValue,
//      contextWidth  = Int.MaxValue,
//      canRead       = true,
//      canWrite      = true,
//      canExclusive  = false,
//      canInvalidate = true,
//      canSync       = true,
//      alignment     = BmbParameter.BurstAlignement.BYTE,
//      maximumPendingTransactionPerId = Int.MaxValue
//    ),
//    requirements = inputRequirements,
//    bus = input,
//    mapping = DefaultMapping
//  )
//
//  interconnect.addMaster(outputRequirements, output)
//
//  val arbiter = add task BmbArbiter(
//    p
//    portCount =
//    lowerFirstPriority = true
//  )
//}

