package saxon

import spinal.core.{Area, log2Up}
import spinal.lib.bus.bmb.{Bmb, BmbIce40Spram, BmbOnChipRam, BmbOnChipRamMultiPort, BmbParameter, BmbToApb3Bridge}
import spinal.lib.bus.misc.{DefaultMapping, SizeMapping}
import spinal.lib.generator.{BmbInterconnectGenerator, Dependable, Generator, Handle, MemoryConnection}
import spinal.lib.memory.sdram.{BmbSdramCtrl, SdramLayout, SdramTimings}


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


case class BmbOnChipRamGenerator(address: BigInt)
                           (implicit interconnect: BmbInterconnectGenerator) extends Generator {
  val size      = Handle[BigInt]
  val dataWidth = Handle[Int]
  val hexInit = createDependency[String]
  val requirements = createDependency[BmbParameter]
  val bmb = produce(logic.io.bus)


  interconnect.addSlave(
    capabilities = Dependable(size, dataWidth)(BmbOnChipRam.busCapabilities(size, dataWidth)),
    requirements = requirements,
    bus = bmb,
    mapping = Dependable(size)(SizeMapping(address, BigInt(1) << log2Up(size)))
  )


  val logic = add task BmbOnChipRam(
    p = requirements,
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





case class  BmbToApb3Decoder(address : BigInt)
                            (implicit interconnect: BmbInterconnectGenerator, apbDecoder : Apb3DecoderGenerator) extends Generator {
  val input = produce(logic.bridge.io.input)
  val requirements = createDependency[BmbParameter]

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
