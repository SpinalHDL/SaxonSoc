package saxon

import spinal.core._
import spinal.lib.IMasterSlave
import spinal.lib.bus.amba3.apb.{Apb3, Apb3CC, Apb3Config, Apb3SlaveFactory}
import spinal.lib.bus.bmb.{Bmb, BmbAccessParameter, BmbArbiter, BmbEg4S20Bram32K, BmbExclusiveMonitor, BmbIce40Spram, BmbInvalidateMonitor, BmbInvalidationParameter, BmbOnChipRam, BmbOnChipRamMultiPort, BmbParameter, BmbToApb3Bridge}
import spinal.lib.bus.misc.{DefaultMapping, SizeMapping}
import spinal.lib.generator.{BmbInterconnectGenerator, BmbSmpInterconnectGenerator, Dependable, Generator, Handle, MemoryConnection, Unset}
import spinal.lib.memory.sdram.SdramLayout
import spinal.lib.memory.sdram.sdr._
import spinal.lib.memory.sdram.xdr._
import spinal.lib.memory.sdram.xdr.phy.{Ecp5Sdrx2Phy, RtlPhy, SdrInferedPhy, XilinxS7Phy}

import scala.collection.mutable.ArrayBuffer

/*
object BmbInterconnectStdGenerators {
  def bmbOnChipRam(address: BigInt,
                   size: BigInt,
                   dataWidth: Int,
                   hexInit: String = null)
                  (implicit interconnect: BmbInterconnectGenerator) = wrap(new Generator {
    val requirements = Handle[BmbParameter]()
    val bmb = produce(logic.io.bus)

    dependencies += requirements

    interconnect.addSlave(
      capabilities = BmbOnChipRam.busCapabilities(size, dataWidth),
      requirements = requirements,
      bus = bmb,
      mapping = SizeMapping(address, BigInt(1) << log2Up(size))
    )

    val logic = add task BmbOnChipRam(
      p = requirements,
      size = size,
      hexOffset = address,
      hexInit = hexInit
    )
  })

  def bmbOnChipRamMultiPort( portCount : Int,
                             address: BigInt,
                             size: BigInt,
                             dataWidth: Int,
                             hexInit: String = null)
                           (implicit interconnect: BmbInterconnectGenerator) = wrap(new Generator {
    val requirements = List.fill(portCount)(Handle[BmbParameter]())
    val busses = List.tabulate(portCount)(id => produce(logic.io.buses(id)))

    dependencies ++= requirements

    for(portId <- 0 until portCount) interconnect.addSlave(
      capabilities = BmbOnChipRamMultiPort.busCapabilities(size, dataWidth),
      requirements = requirements(portId),
      bus = busses(portId),
      mapping = SizeMapping(address, BigInt(1) << log2Up(size))
    )

    val logic = add task BmbOnChipRamMultiPort(
      portsParameter = requirements,
      size = size,
      hexOffset = address,
      hexInit = hexInit
    )
  })


  def addSdramSdrCtrl(address: BigInt)
                     (implicit interconnect: BmbInterconnectGenerator) = wrap(new Generator {


    val layout = createDependency[SdramLayout]
    val timings = createDependency[SdramTimings]
    val requirements = createDependency[BmbParameter]

    val bmb   = produce(logic.io.bmb)
    val sdram = produceIo(logic.io.sdram)

    layout.produce{
      interconnect.addSlave(
        capabilities = BmbSdramCtrl.bmbCapabilities(layout),
        requirements = requirements,
        bus = bmb,
        mapping = SizeMapping(address, layout.capacity)
      )
    }

    val logic = add task BmbSdramCtrl(
      bmbParameter = requirements,
      layout = layout,
      timing = timings,
      CAS = 3
    )
  })

  def bmbToApb3Decoder(address : BigInt)
                      (implicit interconnect: BmbInterconnectGenerator, apbDecoder : Apb3DecoderGenerator) = wrap(new Generator {
    val input = produce(logic.bridge.io.input)
    val requirements = Handle[BmbParameter]()

    val requirementsGenerator = Dependable(apbDecoder.inputConfig){
      interconnect.addSlave(
        capabilities = BmbToApb3Bridge.busCapabilities(
          addressWidth = apbDecoder.inputConfig.addressWidth,
          dataWidth = apbDecoder.inputConfig.dataWidth
        ),
        requirements = requirements,
        bus = input,
        mapping = SizeMapping(address, BigInt(1) << apbDecoder.inputConfig.addressWidth)
      )
    }

    dependencies += requirements
    dependencies += apbDecoder

    val logic = add task new Area {
      val bridge = BmbToApb3Bridge(
        apb3Config = apbDecoder.inputConfig,
        bmbParameter = requirements,
        pipelineBridge = false
      )
      apbDecoder.input << bridge.io.output
    }

    //    dependencies += output
  })
}



*/


case class SdramSdrBmbGenerator(address: BigInt)
                               (implicit interconnect: BmbInterconnectGenerator) extends Generator {

  val layout = createDependency[SdramLayout]
  val timings = createDependency[SdramTimings]
  val requirements = createDependency[BmbParameter]

  val bmb   = produce(logic.io.bmb)
  val sdram = produceIo(logic.io.sdram)


  interconnect.addSlave(
    capabilities = layout.produce(BmbSdramCtrl.bmbCapabilities(layout)),
    requirements = requirements,
    bus = bmb,
    mapping = layout.produce(SizeMapping(address, layout.capacity))
  )

  val logic = add task BmbSdramCtrl(
    bmbParameter = requirements,
    layout = layout,
    timing = timings,
    CAS = 3
  )
}


case class SdramXdrBmbGenerator(memoryAddress: BigInt)
                               (implicit interconnect: BmbInterconnectGenerator/*, decoder: Apb3DecoderGenerator*/) extends Generator {

  val phyParameter = createDependency[PhyLayout]
  val coreParameter = createDependency[CoreParameter]
  val portsParameter = ArrayBuffer[Handle[BmbPortParameter]]()
  val phyPort = produce(logic.io.phy)
  val apb = produce(logic.io.apb)

  def mapApbAt(address : BigInt)(implicit decoder: Apb3DecoderGenerator) : this.type = {
    decoder.addSlave(apb, address)
    this
  }

  def addPort() = new Generator {
    val requirements = createDependency[BmbParameter]
    val portId = portsParameter.length
    val bmb = SdramXdrBmbGenerator.this.produce(logic.io.bmb(portId))

    portsParameter += SdramXdrBmbGenerator.this.createDependency[BmbPortParameter]

    interconnect.addSlave(
      capabilities = phyParameter.produce(CtrlWithPhy.bmbCapabilities(phyParameter)),
      requirements = requirements,
      bus = bmb,
      mapping = phyParameter.produce(SizeMapping(memoryAddress, phyParameter.sdram.capacity))
    )

    add task {
      portsParameter(portId).load(
        BmbPortParameter(
          bmb = requirements,
          clockDomain = ClockDomain.current,
          cmdBufferSize = 16,
          dataBufferSize = 32,
          rspBufferSize = 32
        )
      )
    }
  }




  val logic = add task new CtrlWithoutPhy(
    p =  CtrlParameter(
      core = coreParameter,
      ports = portsParameter.map(_.get)
    ),
    pl = phyParameter
  )
}



case class SdramXdrBmbSmpGenerator(memoryAddress: BigInt)
                               (implicit interconnect: BmbSmpInterconnectGenerator/*, decoder: Apb3DecoderGenerator*/) extends Generator {

  val phyParameter = createDependency[PhyLayout]
  val coreParameter = createDependency[CoreParameter]
  val portsParameter = ArrayBuffer[Handle[BmbPortParameter]]()
  val phyPort = produce(logic.io.phy)
  val apb = produce(logic.io.apb)

  def mapApbAt(address : BigInt)(implicit decoder: Apb3DecoderGenerator) : this.type = {
    decoder.addSlave(apb, address)
    this
  }

  def addPort() = new Generator {
    val requirements = createDependency[BmbAccessParameter]
    val portId = portsParameter.length
    val bmb = SdramXdrBmbSmpGenerator.this.produce(logic.io.bmb(portId))

    portsParameter += SdramXdrBmbSmpGenerator.this.createDependency[BmbPortParameter]

    interconnect.addSlave(
      accessCapabilities = phyParameter.produce(CtrlWithPhy.bmbCapabilities(phyParameter).toAccessParameter),
      accessRequirements = requirements,
      bus = bmb,
      mapping = phyParameter.produce(SizeMapping(memoryAddress, phyParameter.sdram.capacity))
    )

    add task {
      portsParameter(portId).load(
        BmbPortParameter(
          bmb = requirements.toBmbParameter(),
          clockDomain = ClockDomain.current,
          cmdBufferSize = 16,
          dataBufferSize = 32,
          rspBufferSize = 32
        )
      )
    }
  }




  val logic = add task new CtrlWithoutPhy(
    p =  CtrlParameter(
      core = coreParameter,
      ports = portsParameter.map(_.get)
    ),
    pl = phyParameter
  )
}

case class XilinxS7PhyGenerator(configAddress : BigInt)(implicit decoder: Apb3DecoderGenerator) extends Generator{
  val sdramLayout = createDependency[SdramLayout]
  val apb = produce(logic.apb)
  val sdram = produceIo(logic.phy.io.sdram)
  val clk90 = createDependency[ClockDomain]
  val serdesClk0 = createDependency[ClockDomain]
  val serdesClk90 = createDependency[ClockDomain]

  decoder.addSlave(apb, configAddress)

  val logic = add task new Area{
    val apb = Apb3(12, 32)
    val phy = XilinxS7Phy(
      sl = sdramLayout,
      clkRatio = 2,
      clk90 = clk90,
      serdesClk0 = serdesClk0,
      serdesClk90 = serdesClk90
    )
    phy.driveFrom(Apb3SlaveFactory(apb))
  }

  def connect(ctrl : SdramXdrBmbGenerator): Unit = {
    this.produce{
      ctrl.phyParameter.load(logic.phy.pl)
    }
    ctrl.produce{
      ctrl.logic.io.phy <> logic.phy.io.ctrl
    }
  }
  def connect(ctrl : SdramXdrBmbSmpGenerator): Unit = {
    this.produce{
      ctrl.phyParameter.load(logic.phy.pl)
    }
    ctrl.produce{
      ctrl.logic.io.phy <> logic.phy.io.ctrl
    }
  }
}



case class SdrInferedPhyGenerator(implicit decoder: Apb3DecoderGenerator) extends Generator{
  val sdramLayout = createDependency[SdramLayout]
  val sdram = produceIo(logic.phy.io.sdram)

  val logic = add task new Area{
    val phy = SdrInferedPhy(sdramLayout)
  }

  def connect(ctrl : SdramXdrBmbGenerator): this.type = {
    this.produce{ctrl.phyParameter.load(logic.phy.pl)}
    ctrl.produce{ctrl.logic.io.phy <> logic.phy.io.ctrl}
    this
  }
}


case class Ecp5Sdrx2PhyGenerator(implicit decoder: Apb3DecoderGenerator) extends Generator{
  val sdramLayout = createDependency[SdramLayout]
  val sdram = produceIo(logic.phy.io.sdram)

  val logic = add task new Area{
    val phy = Ecp5Sdrx2Phy(sdramLayout)
  }

  def connect(ctrl : SdramXdrBmbGenerator): this.type = {
    this.produce{ctrl.phyParameter.load(logic.phy.pl)}
    ctrl.produce{ctrl.logic.io.phy <> logic.phy.io.ctrl}
    this
  }
}




case class RtlPhyGenerator()extends Generator{
  val layout = createDependency[PhyLayout]
  val io = produceIo(logic.io.write)
  val logic = add task RtlPhy(layout)

  def connect(ctrl : SdramXdrBmbGenerator): Unit = {
    layout.produce{ ctrl.phyParameter.load(layout.get) }
    Dependable(ctrl, logic){ ctrl.logic.io.phy <> logic.io.ctrl }
  }

  def connect(ctrl : SdramXdrBmbSmpGenerator): Unit = {
    layout.produce{ ctrl.phyParameter.load(layout.get) }
    Dependable(ctrl, logic){ ctrl.logic.io.phy <> logic.io.ctrl }
  }
}

case class BmbOnChipRamGenerator(val address: Handle[BigInt] = Unset)
                                (implicit interconnect: BmbInterconnectGenerator) extends Generator {
  val size      = Handle[BigInt]
  val dataWidth = Handle[Int]
  val hexInit = createDependency[String]
  val requirements = createDependency[BmbParameter]
  val bmb = produce(logic.io.bus)

  dependencies += address

  interconnect.addSlave(
    capabilities = Dependable(size, dataWidth)(BmbOnChipRam.busCapabilities(size, dataWidth)),
    requirements = requirements,
    bus = bmb,
    mapping = Dependable(address, size)(SizeMapping(address, BigInt(1) << log2Up(size)))
  )


  val logic = add task BmbOnChipRam(
    p = requirements,
    size = size,
    hexOffset = address,
    hexInit = hexInit
  )
}

case class BmbSmpOnChipRamGenerator(val address: Handle[BigInt] = Unset)
                                (implicit interconnect: BmbSmpInterconnectGenerator) extends Generator {
  val size      = Handle[BigInt]
  val dataWidth = Handle[Int]
  val hexInit = createDependency[String]
  val requirements = createDependency[BmbAccessParameter]
  val bmb = produce(logic.io.bus)

  dependencies += address

  interconnect.addSlave(
    accessCapabilities = Dependable(size, dataWidth)(BmbOnChipRam.busCapabilities(size, dataWidth).toAccessParameter),
    accessRequirements = requirements,
    bus = bmb,
    mapping = Dependable(address, size)(SizeMapping(address, BigInt(1) << log2Up(size)))
  )


  val logic = add task BmbOnChipRam(
    p = requirements.toBmbParameter(),
    size = size,
    hexOffset = address,
    hexInit = hexInit
  )
}





case class BmbIce40SpramGenerator(address: BigInt)
                                 (implicit interconnect: BmbInterconnectGenerator) extends Generator {
  val size = Handle[BigInt]
  val requirements = createDependency[BmbParameter]
  val bmb = produce(logic.io.bus)


  interconnect.addSlave(
    capabilities = size.produce(BmbIce40Spram.busCapabilities(size)),
    requirements = requirements,
    bus = bmb,
    mapping = size.produce(SizeMapping(address, BigInt(1) << log2Up(size)))
  )


  val logic = add task BmbIce40Spram(
    p = requirements
  )
}


case class BmbEg4S20Bram32Generator
            (address: BigInt)
            (implicit interconnect: BmbInterconnectGenerator)
            extends Generator {

  val size = Handle[BigInt]
  val hexInit = createDependency[String]
  val requirements = createDependency[BmbParameter]
  val bmb = produce(logic.io.bus)

  interconnect.addSlave(
    capabilities = size.produce(BmbEg4S20Bram32K.busCapabilities(size)),
    requirements = requirements,
    bus = bmb,
    mapping = size.produce(SizeMapping(address, BigInt(1) << log2Up(size)))
  )

  val logic = add task BmbEg4S20Bram32K(
    p = requirements,
    hexInit = hexInit
  )
}


object BmbBridgeGenerator{
  def busCapabilities(addressWidth : Int, dataWidth : Int) = BmbParameter(
    addressWidth  = addressWidth,
    dataWidth     = dataWidth,
    lengthWidth   = Int.MaxValue,
    sourceWidth   = Int.MaxValue,
    contextWidth  = Int.MaxValue,
    canRead       = true,
    canWrite      = true,
    alignment     = BmbParameter.BurstAlignement.BYTE,
    maximumPendingTransactionPerId = Int.MaxValue
  )
}

case class BmbBridgeGenerator()
                              (implicit interconnect: BmbInterconnectGenerator) extends Generator {
  val requirements = createDependency[BmbParameter]
  val bmb = add task Bmb(requirements)

  interconnect.addSlave(
    capabilities = BmbBridgeGenerator.busCapabilities(32, 32), //TODO
    requirements = requirements,
    bus = bmb,
    mapping = DefaultMapping
  )

  interconnect.addMaster(requirements, bmb)
}


case class BmbSmpBridgeGenerator()
                             (implicit interconnect: BmbSmpInterconnectGenerator) extends Generator {
  val accessSource = createDependency[BmbAccessParameter]
  val accessRequirements = createDependency[BmbAccessParameter]
  val invalidationSource = createDependency[BmbInvalidationParameter]
  val invalidationRequirements = createDependency[BmbInvalidationParameter]
  val bmb = add task Bmb(accessRequirements, invalidationRequirements)

  interconnect.addSlave(
    accessSource = accessSource,
    accessCapabilities = accessSource,
    accessRequirements = accessRequirements,
    invalidationRequirements = invalidationRequirements,
    bus = bmb,
    mapping = DefaultMapping
  )

  interconnect.addMaster(
    accessRequirements = accessRequirements,
    invalidationSource = invalidationSource,
    invalidationCapabilities = invalidationSource,
    invalidationRequirements = invalidationRequirements,
    bus = bmb
  )
}


case class  BmbToApb3Decoder(address : Handle[BigInt] = Unset)(implicit interconnect: BmbInterconnectGenerator, apbDecoder : Apb3DecoderGenerator) extends Generator {
  val input = produce(logic.bridge.io.input)
  val requirements = createDependency[BmbParameter]

  dependencies += address

  interconnect.addSlave(
    capabilities = apbDecoder.inputConfig produce BmbToApb3Bridge.busCapabilities(
      addressWidth = apbDecoder.inputConfig.addressWidth,
      dataWidth = apbDecoder.inputConfig.dataWidth
    ),
    requirements = requirements,
    bus = input,
    mapping = apbDecoder.inputConfig produce SizeMapping(address, BigInt(1) << apbDecoder.inputConfig.addressWidth)
  )

  dependencies += requirements
  dependencies += apbDecoder

  val logic = add task new Area {
    val bridge = BmbToApb3Bridge(
      apb3Config = apbDecoder.inputConfig,
      bmbParameter = requirements,
      pipelineBridge = false
    )
    apbDecoder.input << bridge.io.output
  }


  tags += new MemoryConnection(input, apbDecoder.input, 0)
}



case class  BmbSmpToApb3Decoder(address : Handle[BigInt] = Unset)(implicit interconnect: BmbSmpInterconnectGenerator, apbDecoder : Apb3DecoderGenerator) extends Generator {
  val input = produce(logic.bridge.io.input)
  val requirements = createDependency[BmbAccessParameter]

  dependencies += address

  interconnect.addSlave(
    accessCapabilities = apbDecoder.inputConfig produce BmbToApb3Bridge.busCapabilities(
      addressWidth = apbDecoder.inputConfig.addressWidth,
      dataWidth = apbDecoder.inputConfig.dataWidth
    ).toAccessParameter,
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



case class BmbExclusiveMonitorGenerator()
                             (implicit interconnect: BmbInterconnectGenerator) extends Generator {
  val input = produce(exclusiveMonitor.io.input)
  val output = produce(exclusiveMonitor.io.output)


  val inputRequirements = createDependency[BmbParameter]
  val outputRequirements = inputRequirements.derivate(BmbExclusiveMonitor.outputParameter)

  interconnect.addSlave(
    capabilities = BmbParameter(
      addressWidth  = 32,
      dataWidth     = 32,
      lengthWidth   = Int.MaxValue,
      sourceWidth   = Int.MaxValue,
      contextWidth  = Int.MaxValue,
      canRead       = true,
      canWrite      = true,
      canExclusive  = true,
      canInvalidate = true,
      canSync       = true,
      alignment     = BmbParameter.BurstAlignement.BYTE,
      maximumPendingTransactionPerId = Int.MaxValue
    ),
    requirements = inputRequirements,
    bus = input,
    mapping = DefaultMapping
  )

  interconnect.addMaster(outputRequirements, output)

  val exclusiveMonitor = add task BmbExclusiveMonitor(
    inputParameter = inputRequirements,
    pendingWriteMax = 64
  )
}

case class BmbInvalidateMonitorGenerator()
                                       (implicit interconnect: BmbInterconnectGenerator) extends Generator {
  val input = produce(exclusiveMonitor.io.input)
  val output = produce(exclusiveMonitor.io.output)


  val inputRequirements = createDependency[BmbParameter]
  val outputRequirements = inputRequirements.derivate(BmbExclusiveMonitor.outputParameter)

  interconnect.addSlave(
    capabilities = BmbParameter(
      addressWidth  = 32,
      dataWidth     = 32,
      lengthWidth   = Int.MaxValue,
      sourceWidth   = Int.MaxValue,
      contextWidth  = Int.MaxValue,
      canRead       = true,
      canWrite      = true,
      canExclusive  = false,
      canInvalidate = true,
      canSync       = true,
      alignment     = BmbParameter.BurstAlignement.BYTE,
      maximumPendingTransactionPerId = Int.MaxValue
    ),
    requirements = inputRequirements,
    bus = input,
    mapping = DefaultMapping
  )

  interconnect.addMaster(outputRequirements, output)

  val exclusiveMonitor = add task BmbInvalidateMonitor(
    inputParameter = inputRequirements,
    pendingInvMax = 16
  )
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

