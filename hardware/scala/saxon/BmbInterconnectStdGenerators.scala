package saxon

import spinal.core._
import spinal.core.fiber._
import spinal.lib._
import spinal.lib.bus.amba3.apb.{Apb3, Apb3CC, Apb3Config, Apb3SlaveFactory}
import spinal.lib.bus.bmb.{Bmb, BmbAccessCapabilities, BmbAccessParameter, BmbArbiter, BmbEg4S20Bram32K, BmbExclusiveMonitor, BmbIce40Spram, BmbImplicitPeripheralDecoder, BmbInvalidateMonitor, BmbInvalidationParameter, BmbOnChipRam, BmbOnChipRamMultiPort, BmbParameter, BmbInterconnectGenerator, BmbToApb3Bridge}
import spinal.lib.bus.misc.{AddressMapping, DefaultMapping, SizeMapping}
import spinal.lib.generator.{Dependable, Generator, MemoryConnection}
import spinal.lib.memory.sdram.SdramLayout
import spinal.lib.memory.sdram.sdr._
import spinal.lib.memory.sdram.xdr._
import spinal.lib.memory.sdram.xdr.phy.{Ecp5Sdrx2Phy, RtlPhy, SdrInferedPhy, XilinxS7Phy}

import scala.collection.mutable.ArrayBuffer


case class RtlPhyGenerator()extends Area{
  val layout = Handle[PhyLayout]
  val io = Handle(logic.io.write.toIo)
  val logic = Handle(RtlPhy(layout))

  def connect(ctrl : SdramXdrBmbGenerator): Unit = {
    ctrl.phyParameter.load(layout.get)
    Handle{ ctrl.logic.io.phy <> logic.io.ctrl }
  }
}


case class BmbOnChipRamGenerator(val address: Handle[BigInt] = Unset)
                                (implicit interconnect: BmbInterconnectGenerator) extends Area {
  val size      = Handle[BigInt]
  val dataWidth = Handle[Int]
  var hexOffset = BigInt(0)
  val hexInit = Handle[String]
  val requirements = Handle[BmbAccessParameter]
  val ctrl = Handle(logic.io.bus)

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





case class  BmbToApb3Decoder(address : Handle[BigInt] = Unset)(implicit interconnect: BmbInterconnectGenerator, apbDecoder : Apb3DecoderGenerator) extends Area {
  val input = Handle(logic.bridge.io.input)
  val requirements = Handle[BmbAccessParameter]

  interconnect.addSlave(
    accessCapabilities = apbDecoder.inputConfig produce BmbToApb3Bridge.busCapabilities(
      addressWidth = apbDecoder.inputConfig.addressWidth,
      dataWidth = apbDecoder.inputConfig.dataWidth
    ),
    accessRequirements = requirements,
    bus = input,
    mapping = apbDecoder.inputConfig produce SizeMapping(address, BigInt(1) << apbDecoder.inputConfig.addressWidth)
  )

  val logic = Handle(new Area {
    val bridge = BmbToApb3Bridge(
      apb3Config = apbDecoder.inputConfig,
      bmbParameter = requirements.toBmbParameter,
      pipelineBridge = false
    )
    apbDecoder.input << bridge.io.output
  })


  export(new MemoryConnection(input, apbDecoder.input, 0))
}



case class  Apb3CCGenerator() extends Area {
  val inputClockDomain, outputClockDomain = Handle[ClockDomain]
  val apbConfig = Handle[Apb3Config]

  val input = Handle(logic.io.input)
  val output = Handle(logic.io.output)

  val logic = Handle(Apb3CC(
    config = apbConfig,
    inputClock = inputClockDomain,
    outputClock = outputClockDomain
  ))

  export(new MemoryConnection(input, output, 0))

  def mapAt(apbOffset : BigInt)(implicit apbDecoder : Apb3DecoderGenerator) = apbDecoder.addSlave(input, apbOffset)
  def setOutput(apb : Handle[Apb3]): Unit = {
    apb.produce(apbConfig.load(apb.config))
    Dependable(apb, output){
      output >> apb
      export(new MemoryConnection(output, apb, 0))
    }
  }
}


