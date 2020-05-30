package saxon

import spinal.core._
import spinal.lib.bus.bmb.{Bmb, BmbAccessParameter, BmbSlaveFactory}
import spinal.lib.bus.misc.{BusSlaveFactoryConfig, SizeMapping}
import spinal.lib.com.uart.{BmbUartCtrl, UartCtrlMemoryMappedConfig}
import spinal.lib.generator.{BmbSmpInterconnectGenerator, Export, Generator, Handle, Unset}
import spinal.lib.memory.sdram.SdramLayout
import spinal.lib.memory.sdram.xdr.phy.XilinxS7Phy
import spinal.lib.memory.sdram.xdr.{BmbPortParameter, CoreParameter, CtrlParameter, CtrlWithPhy, CtrlWithoutPhy, CtrlWithoutPhyBmb, PhyLayout}
import spinal.lib.misc.{BmbClint, Clint}
import spinal.lib.misc.plic.{PlicGateway, PlicGatewayActiveHigh, PlicMapper, PlicMapping, PlicTarget}

import scala.collection.mutable.ArrayBuffer

case class BmbUartGenerator(apbOffset : Handle[BigInt] = Unset)
                            (implicit interconnect: BmbSmpInterconnectGenerator) extends Generator {
  val parameter = createDependency[UartCtrlMemoryMappedConfig]
  val interrupt = produce(logic.io.interrupt)
  val uart = produceIo(logic.io.uart)
  val bus = produce(logic.io.bus)

  val accessSource = Handle[BmbAccessParameter]
  val accessRequirements = createDependency[BmbAccessParameter]
  val logic = add task BmbUartCtrl(parameter, accessRequirements.toBmbParameter())

  val txd = uart.produce(uart.txd)
  val rxd = uart.produce(uart.rxd)

  def connectInterrupt(ctrl : InterruptCtrl, id : Int): Unit = {
    ctrl.addInterrupt(interrupt, id)
  }

  interconnect.addSlave(
    accessSource = accessSource,
    accessCapabilities = accessSource.derivate(BmbUartCtrl.getBmbCapabilities),
    accessRequirements = accessRequirements,
    bus = bus,
    mapping = apbOffset.derivate(SizeMapping(_, 1 << BmbUartCtrl.addressWidth))
  )
  export(parameter)
}

case class BmbClintGenerator(apbOffset : Handle[BigInt] = Unset)
                             (implicit interconnect: BmbSmpInterconnectGenerator) extends Generator {
  val bus = produce(logic.io.bus)
  val cpuCount = createDependency[Int]

  val accessSource = Handle[BmbAccessParameter]
  val accessRequirements = createDependency[BmbAccessParameter]
  val logic = add task BmbClint(accessRequirements.toBmbParameter(), cpuCount)
  def timerInterrupt(id : Int) = logic.derivate(_.io.timerInterrupt(id))
  def softwareInterrupt(id : Int) = logic.derivate(_.io.softwareInterrupt(id))

  interconnect.addSlave(
    accessSource = accessSource,
    accessCapabilities = accessSource.derivate(Clint.getBmbCapabilities),
    accessRequirements = accessRequirements,
    bus = bus,
    mapping = apbOffset.derivate(SizeMapping(_, 1 << Clint.addressWidth))
  )

  val hz = export(produce(ClockDomain.current.frequency))
}



case class BmbPlicGenerator(apbOffset : Handle[BigInt] = Unset) (implicit interconnect: BmbSmpInterconnectGenerator) extends Generator with InterruptCtrl{
  @dontName val gateways = ArrayBuffer[Handle[PlicGateway]]()
  val bus = produce(logic.bmb)

  val accessSource = Handle[BmbAccessParameter]
  val accessRequirements = createDependency[BmbAccessParameter]

  val priorityWidth = createDependency[Int]
  val mapping = createDependency[PlicMapping]

  val targetsModel = ArrayBuffer[Handle[Bool]]()
  def addTarget(target : Handle[Bool]) = {
    val id = targetsModel.size

    targetsModel += target
    dependencies += target

    //TODO remove the need of delaying stuff for name capture
    add task(tags += new Export(BmbPlicGenerator.this.getName() + "_" + target.getName, id))
  }

  override def addInterrupt(source : Handle[Bool], id : Int) = {
    this.dependencies += new Generator {
      dependencies += source
      add task new Area {
        gateways += PlicGatewayActiveHigh(
          source = source,
          id = id,
          priorityWidth = priorityWidth
        ).setCompositeName(source, "plic_gateway")

        tags += new Export(BmbPlicGenerator.this.getName() + "_" + source.getName, id)
      }
    }
  }

  override def getBus(): Handle[Nameable] = bus

  val logic = add task new Area{
    val bmb = Bmb(accessRequirements.toBmbParameter())
    val bus = BmbSlaveFactory(bmb)

    val targets = targetsModel.map(flag =>
      PlicTarget(
        gateways = gateways.map(_.get),
        priorityWidth = priorityWidth
      ).setCompositeName(flag, "plic_target")
    )

    //    gateways.foreach(_.priority := 1)
    //    targets.foreach(_.threshold := 0)
    //    targets.foreach(_.ie.foreach(_ := True))

    val bridge = PlicMapper(bus, mapping)(
      gateways = gateways.map(_.get),
      targets = targets
    )

    for(targetId <- 0 until targetsModel.length){
      targetsModel(targetId) := targets(targetId).iep
    }
  }


  interconnect.addSlave(
    accessSource = accessSource,
    accessCapabilities = accessSource.derivate(BmbSlaveFactory.getBmbCapabilities(
      _,
      addressWidth = 22,
      dataWidth = 32
    )),
    accessRequirements = accessRequirements,
    bus = bus,
    mapping = apbOffset.derivate(SizeMapping(_, 1 << 22))
  )
}

case class SdramXdrBmb2SmpGenerator(memoryAddress: BigInt)
                                  (implicit interconnect: BmbSmpInterconnectGenerator/*, decoder: Apb3DecoderGenerator*/) extends Generator {

  val phyParameter = createDependency[PhyLayout]
  val coreParameter = createDependency[CoreParameter]
  val portsParameter = ArrayBuffer[Handle[BmbPortParameter]]()
  val phyPort = produce(logic.io.phy)
  val ctrlBus = produce(logic.io.ctrl)


  val accessSource = Handle[BmbAccessParameter]
  val accessRequirements = createDependency[BmbAccessParameter]
  def mapCtrlAt(address : BigInt)(implicit interconnect: BmbSmpInterconnectGenerator) : this.type = {
    interconnect.addSlave(
      accessSource = accessSource,
      accessCapabilities = accessSource.derivate(CtrlWithoutPhyBmb.getBmbCapabilities),
      accessRequirements = accessRequirements,
      bus = ctrlBus,
      mapping = SizeMapping(address, 1 << CtrlWithoutPhyBmb.addressWidth)
    )
    this
  }

  def addPort() = new Generator {
    val requirements = createDependency[BmbAccessParameter]
    val portId = portsParameter.length
    val bmb = SdramXdrBmb2SmpGenerator.this.produce(logic.io.bmb(portId))

    portsParameter += SdramXdrBmb2SmpGenerator.this.createDependency[BmbPortParameter]

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

  val logic = add task new CtrlWithoutPhyBmb(
    p =  CtrlParameter(
      core = coreParameter,
      ports = portsParameter.map(_.get)
    ),
    pl = phyParameter,
    ctrlParameter = accessRequirements.toBmbParameter()
  )
}

case class XilinxS7PhyBmbGenerator(configAddress : BigInt)(implicit interconnect: BmbSmpInterconnectGenerator) extends Generator{
  val sdramLayout = createDependency[SdramLayout]
  val ctrl = produce(logic.ctrl)
  val sdram = produceIo(logic.phy.io.sdram)
  val clk90 = createDependency[ClockDomain]
  val serdesClk0 = createDependency[ClockDomain]
  val serdesClk90 = createDependency[ClockDomain]

  val accessSource = Handle[BmbAccessParameter]
  val accessRequirements = createDependency[BmbAccessParameter]

  interconnect.addSlave(
    accessSource = accessSource,
    accessCapabilities = accessSource.derivate(CtrlWithoutPhyBmb.getBmbCapabilities),
    accessRequirements = accessRequirements,
    bus = ctrl,
    mapping = SizeMapping(configAddress, 1 << CtrlWithoutPhyBmb.addressWidth)
  )

  val logic = add task new Area{
    val ctrl = Bmb(BmbSlaveFactory.getBmbCapabilities(
      accessSource,
      addressWidth = 12,
      dataWidth = 32)
    )
    val phy = XilinxS7Phy(
      sl = sdramLayout,
      clkRatio = 2,
      clk90 = clk90,
      serdesClk0 = serdesClk0,
      serdesClk90 = serdesClk90
    )
    phy.driveFrom(BmbSlaveFactory(ctrl))
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

