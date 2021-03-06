package saxon

import spinal.core._
import spinal.core.fiber._
import spinal.lib.bus.bmb.{Bmb, BmbAccessCapabilities, BmbAccessParameter, BmbImplicitPeripheralDecoder, BmbInterconnectGenerator, BmbParameter, BmbSlaveFactory}
import spinal.lib.bus.misc.{BusSlaveFactoryConfig, SizeMapping}
import spinal.lib.com.eth._
import spinal.lib.com.i2c.{BmbI2cCtrl, I2cSlaveMemoryMappedGenerics}
import spinal.lib.com.spi.ddr.SpiXdrMasterCtrl.XipBusParameters
import spinal.lib.com.spi.ddr.{BmbSpiXdrMasterCtrl, SpiXdrMasterCtrl}
import spinal.lib.com.uart.{BmbUartCtrl, UartCtrlMemoryMappedConfig}
import spinal.lib.generator.{Dependable, Export, Generator, InterruptCtrlGeneratorI}
import spinal.lib.io.{BmbGpio2, Gpio}
import spinal.lib.master
import spinal.lib.memory.sdram.SdramLayout
import spinal.lib.memory.sdram.xdr.phy.{Ecp5Sdrx2Phy, XilinxS7Phy}
import spinal.lib.memory.sdram.xdr.{BmbPortParameter, CoreParameter, CtrlParameter, CtrlWithPhy, CtrlWithoutPhy, CtrlWithoutPhyBmb, PhyLayout}
import spinal.lib.misc.{BmbClint, Clint}
import spinal.lib.misc.plic.{PlicGateway, PlicGatewayActiveHigh, PlicMapper, PlicMapping, PlicTarget}

import scala.collection.mutable.ArrayBuffer

case class BmbUartGenerator(apbOffset : Handle[BigInt] = Unset)
                            (implicit interconnect: BmbInterconnectGenerator, decoder : BmbImplicitPeripheralDecoder = null) extends Generator {
  val parameter          = Handle[UartCtrlMemoryMappedConfig]
  val accessSource       = Handle[BmbAccessCapabilities]
  val accessRequirements = Handle[BmbAccessParameter]

  val logic     = Handle(BmbUartCtrl(parameter, accessRequirements.toBmbParameter()))
  val ctrl      = Handle(logic.io.bus)
  val uart      = Handle(logic.io.uart.toIo)
  val interrupt = Handle(logic.io.interrupt)

  def connectInterrupt(ctrl : InterruptCtrlGeneratorI, id : Int): Unit = {
    ctrl.addInterrupt(interrupt, id)
  }

  interconnect.addSlave(
    accessSource       = accessSource,
    accessCapabilities = Handle(BmbUartCtrl.getBmbCapabilities(accessSource)),
    accessRequirements = accessRequirements,
    bus                = ctrl,
    mapping            = Handle(SizeMapping(apbOffset, 1 << BmbUartCtrl.addressWidth))
  )
  export(parameter)
  if(decoder != null) interconnect.addConnection(decoder.bus, ctrl)
}

abstract case class BmbPeripheralGenerator(apbOffset : BigInt, addressWidth : Int)
                                          (implicit interconnect: BmbInterconnectGenerator, decoder : BmbImplicitPeripheralDecoder = null) extends Generator {



  val accessSource = Handle[BmbAccessCapabilities]
  val accessRequirements = createDependency[BmbAccessParameter]

  val ctrl = accessRequirements.produce(Bmb(accessRequirements.toBmbParameter()))
  dependencies += ctrl

  interconnect.addSlave(
    accessSource = accessSource,
    accessCapabilities = accessSource.produce(BmbSlaveFactory.getBmbCapabilities(
      accessSource,
      addressWidth = addressWidth,
      dataWidth = 32
    )),
    accessRequirements = accessRequirements,
    bus = ctrl,
    mapping = SizeMapping(apbOffset, 1 << addressWidth)
  )
  if(decoder != null) interconnect.addConnection(decoder.bus, ctrl)
}

case class SdramXdrBmbGenerator(memoryAddress: BigInt)
                               (implicit interconnect: BmbInterconnectGenerator) extends Generator {

  val phyParameter = createDependency[PhyLayout]
  val coreParameter = createDependency[CoreParameter]
  val portsParameter = ArrayBuffer[Handle[BmbPortParameter]]()
  val phyPort = produce(logic.io.phy)
  val ctrl = produce(logic.io.ctrl)


  val accessSource = Handle[BmbAccessCapabilities]
  val accessRequirements = createDependency[BmbAccessParameter]
  def mapCtrlAt(address : BigInt)(implicit interconnect: BmbInterconnectGenerator, decoder : BmbImplicitPeripheralDecoder = null) : this.type = {
    interconnect.addSlave(
      accessSource = accessSource,
      accessCapabilities = accessSource.derivate(CtrlWithoutPhyBmb.getBmbCapabilities),
      accessRequirements = accessRequirements,
      bus = ctrl,
      mapping = SizeMapping(address, 1 << CtrlWithoutPhyBmb.addressWidth)
    )
    if(decoder != null) interconnect.addConnection(decoder.bus, ctrl)
    this
  }

  def addPort() = this {
    new Generator {
      val requirements = createDependency[BmbAccessParameter]
      val portId = portsParameter.length
      val bmb = SdramXdrBmbGenerator.this.produce(logic.io.bmb(portId))

      portsParameter += SdramXdrBmbGenerator.this.createDependency[BmbPortParameter]

      interconnect.addSlave(
        accessCapabilities = phyParameter.produce(CtrlWithPhy.bmbCapabilities(phyParameter)),
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
  }

  val logic = add task new CtrlWithoutPhyBmb(
    p =  CtrlParameter(
      core = coreParameter,
      ports = portsParameter.map(_.get)
    ),
    pl = phyParameter,
    ctrlParameter = accessRequirements.toBmbParameter()
  )
//  if(decoder != null) interconnect.addConnection(decoder.bus, ctrlBus)
}

case class XilinxS7PhyBmbGenerator(configAddress : BigInt)(implicit interconnect: BmbInterconnectGenerator, decoder : BmbImplicitPeripheralDecoder = null) extends Generator{
  val sdramLayout = createDependency[SdramLayout]
  val ctrl = produce(logic.ctrl)
  val sdram = produceIo(logic.phy.io.sdram)
  val clk90 = createDependency[ClockDomain]
  val serdesClk0 = createDependency[ClockDomain]
  val serdesClk90 = createDependency[ClockDomain]

  val accessSource = Handle[BmbAccessCapabilities]
  val accessRequirements = createDependency[BmbAccessParameter]

  interconnect.addSlave(
    accessSource = accessSource,
    accessCapabilities = accessSource.derivate(CtrlWithoutPhyBmb.getBmbCapabilities),
    accessRequirements = accessRequirements,
    bus = ctrl,
    mapping = SizeMapping(configAddress, 1 << CtrlWithoutPhyBmb.addressWidth)
  )

  val logic = add task new Area{
    val phy = XilinxS7Phy(
      sl = sdramLayout,
      clkRatio = 2,
      clk90 = clk90,
      serdesClk0 = serdesClk0,
      serdesClk90 = serdesClk90
    )
    val ctrl = Bmb(accessRequirements)
    phy.driveFrom(BmbSlaveFactory(ctrl))
  }


  def connect(ctrl : SdramXdrBmbGenerator): Unit = {
    ctrl.phyParameter.derivatedFrom(sdramLayout)(XilinxS7Phy.phyLayout(_, 2))
//    this.produce{
//      ctrl.phyParameter.load(logic.phy.pl)
//    }
    List(ctrl.logic, logic).produce{
      ctrl.logic.io.phy <> logic.phy.io.ctrl
    }
  }
  if(decoder != null) interconnect.addConnection(decoder.bus, ctrl)
}

case class Ecp5Sdrx2PhyGenerator() extends Generator{
  val sdramLayout = createDependency[SdramLayout]
  val sdram = produceIo(logic.io.sdram)
  val logic = add task Ecp5Sdrx2Phy(sdramLayout)

  def connect(ctrl : SdramXdrBmbGenerator): this.type = {
    ctrl.phyParameter.derivatedFrom(sdramLayout)(Ecp5Sdrx2Phy.phyLayout)
    List(ctrl.logic, logic).produce{
      ctrl.logic.io.phy <> logic.io.ctrl
    }
    this
  }
}



case class  BmbGpioGenerator(apbOffset : Handle[BigInt] = Unset)
                             (implicit interconnect: BmbInterconnectGenerator, decoder : BmbImplicitPeripheralDecoder = null) extends Generator{
  val parameter = createDependency[spinal.lib.io.Gpio.Parameter]
  val gpio = produceIo(logic.io.gpio)
  val ctrl = produce(logic.io.bus)

  val accessSource = Handle[BmbAccessCapabilities]
  val accessRequirements = createDependency[BmbAccessParameter]
  //TODO not having to setCompositeName
  val interrupts : Handle[List[Handle[Bool]]] = parameter.produce(List.tabulate(parameter.width)(i => this.produce(logic.io.interrupt(i)).setCompositeName(interrupts, i.toString)))
  val logic = add task BmbGpio2(parameter, accessRequirements.toBmbParameter())

  @dontName var interruptCtrl : InterruptCtrlGeneratorI = null
  var interruptOffsetId = 0
  def connectInterrupts(ctrl : InterruptCtrlGeneratorI, offsetId : Int): Unit = interrupts.produce{
    for((pinId, localOffset) <- parameter.interrupt.zipWithIndex) ctrl.addInterrupt(interrupts.get(pinId), offsetId + localOffset)
    interruptCtrl = ctrl
    interruptOffsetId = offsetId
  }
  def connectInterrupt(ctrl : InterruptCtrlGeneratorI, pinId : Int, interruptId : Int): Unit = interrupts.produce{
    ctrl.addInterrupt(interrupts.get(pinId), interruptId)
  }
  def pin(id : Int) = gpio.produce(gpio.get.setAsDirectionLess.apply(id))

  interconnect.addSlave(
    accessSource = accessSource,
    accessCapabilities = accessSource.derivate(BmbGpio2.getBmbCapabilities),
    accessRequirements = accessRequirements,
    bus = ctrl,
    mapping = apbOffset.derivate(SizeMapping(_, 1 << Gpio.addressWidth))
  )
  if(decoder != null) interconnect.addConnection(decoder.bus, ctrl)
}





object BmbSpiGenerator{
  def apply(apbOffset : BigInt, xipOffset : BigInt = 0)
           (implicit interconnect: BmbInterconnectGenerator, decoder : BmbImplicitPeripheralDecoder = null): BmbSpiGenerator ={
    new BmbSpiGenerator(apbOffset,xipOffset)
  }
}
class BmbSpiGenerator(apbOffset : Handle[BigInt] = Unset, xipOffset : Handle[BigInt] = 0)
                      (implicit interconnect: BmbInterconnectGenerator, decoder : BmbImplicitPeripheralDecoder = null) extends Generator {
  val parameter = createDependency[SpiXdrMasterCtrl.MemoryMappingParameters]
  val withXip = Handle(false)
  val interrupt = produce(logic.io.interrupt)
  val phy = produce(logic.io.spi)
  val spi = Handle[Nameable]
  val ctrl : Handle[Bmb] = produce(logic.io.ctrl)

  val accessSource = Handle[BmbAccessCapabilities]
  val accessRequirements = createDependency[BmbAccessParameter]

  val logic = add task BmbSpiXdrMasterCtrl(parameter, accessRequirements.toBmbParameter())
//  val logic = add task BmbSpiXdrMasterCtrl(parameter.copy(xip = if(!withXip) null else XipBusParameters(24, bmbRequirements.lengthWidth)), accessRequirements.toBmbParameter())

  val bmbRequirements = Handle[BmbParameter]
  val bmb = product[Bmb]

  dependencies += withXip.produce{
    if(withXip) {
      ???
//      dependencies += bmbRequirements
//      interconnect.addSlaveAt(
//        capabilities = Handle(SpiXdrMasterCtrl.getXipBmbCapabilities()),
//        requirements = bmbRequirements,
//        bus = bmb,
//        address = xipOffset
//      )
//      Dependable(BmbSpiGenerator.this, bmbRequirements){
//        bmb.load(logic.io.xip.fromBmb(bmbRequirements))
//      }
    }
  }


  dependencies += withXip

  @dontName var interruptCtrl : InterruptCtrlGeneratorI = null
  var interruptId = 0
  def connectInterrupt(ctrl : InterruptCtrlGeneratorI, id : Int): Unit = {
    ctrl.addInterrupt(interrupt, id)
    interruptCtrl = ctrl
    interruptId = id
  }

  def inferSpiSdrIo() = this(Dependable(phy)(spi.load(master(phy.toSpi().setPartialName(spi, ""))))) //TODO automated naming
  def inferSpiIce40() = this(Dependable(phy)(spi.load{
    phy.toSpiIce40().asInOut().setPartialName(spi, "")
  }))
  def phyAsIo() = produceIo(phy.get)

  interconnect.addSlave(
    accessSource = accessSource,
    accessCapabilities = accessSource.derivate(BmbSpiXdrMasterCtrl.getBmbCapabilities),
    accessRequirements = accessRequirements,
    bus = ctrl,
    mapping = apbOffset.derivate(SizeMapping(_, 1 << BmbSpiXdrMasterCtrl.addressWidth))
  )
  if(decoder != null) interconnect.addConnection(decoder.bus, ctrl)
}

case class BmbI2cGenerator(apbOffset : Handle[BigInt] = Unset)
                           (implicit interconnect: BmbInterconnectGenerator, decoder : BmbImplicitPeripheralDecoder = null) extends Generator {
  val parameter = createDependency[I2cSlaveMemoryMappedGenerics]
  val i2c = produceIo(logic.io.i2c)
  val ctrl = produce(logic.io.ctrl)
  val interrupt = produce(logic.io.interrupt)

  val accessSource = Handle[BmbAccessCapabilities]
  val accessRequirements = createDependency[BmbAccessParameter]
  interconnect.addSlave(
    accessSource = accessSource,
    accessCapabilities = accessSource.derivate(BmbGpio2.getBmbCapabilities),
    accessRequirements = accessRequirements,
    bus = ctrl,
    mapping = apbOffset.derivate(SizeMapping(_, 1 << Gpio.addressWidth))
  )
  if(decoder != null) interconnect.addConnection(decoder.bus, ctrl)

  val logic = add task BmbI2cCtrl(parameter, accessRequirements.toBmbParameter())

  def connectInterrupt(ctrl : InterruptCtrlGeneratorI, id : Int): Unit = {
    ctrl.addInterrupt(interrupt, id)
  }
}


case class BmbMacEthGenerator(address : Handle[BigInt] = Unset)
                             (implicit interconnect: BmbInterconnectGenerator, decoder : BmbImplicitPeripheralDecoder = null) extends Generator {
  val parameter = createDependency[MacEthParameter]
  val rxCd, txCd = createDependency[ClockDomain]
  val interrupt = produce(logic.io.interrupt)
  val phy = produce(logic.io.phy)
  val ctrl = produce(logic.io.bus)

  val accessSource = Handle[BmbAccessCapabilities]
  val accessRequirements = createDependency[BmbAccessParameter]
  val logic = add task BmbMacEth(
      p            = parameter,
      bmbParameter = accessRequirements.toBmbParameter(),
      txCd         = txCd,
      rxCd         = rxCd
  )

  def connectInterrupt(ctrl : InterruptCtrlGeneratorI, id : Int): Unit = {
    ctrl.addInterrupt(interrupt, id)
  }

  interconnect.addSlave(
    accessSource = accessSource,
    accessCapabilities = accessSource.derivate(BmbMacEth.getBmbCapabilities),
    accessRequirements = accessRequirements,
    bus = ctrl,
    mapping = address.derivate(SizeMapping(_, 1 << BmbMacEth.addressWidth))
  )
  export(parameter)
  if(decoder != null) interconnect.addConnection(decoder.bus, ctrl)


  def withPhyMii() = new Generator {
    val mii = add task master(Mii(
      MiiParameter(
        MiiTxParameter(
          dataWidth = 4,
          withEr    = false
        ),
        MiiRxParameter(
          dataWidth = 4
        )
      )
    ))

    txCd.derivatedFrom(mii)(_ => ClockDomain(mii.TX.CLK))
    rxCd.derivatedFrom(mii)(_ => ClockDomain(mii.RX.CLK))

    List(mii, phy).produce{
      txCd.copy(reset = logic.mac.txReset) on {
        val tailer = MacTxInterFrame(dataWidth = 4)
        tailer.io.input << phy.tx

        mii.TX.EN := RegNext(tailer.io.output.valid)
        mii.TX.D := RegNext(tailer.io.output.data)
      }
      rxCd on {
        phy.rx << mii.RX.toRxFlow().toStream
      }
    }
  }

  def withPhyRmii(ffIn : Bool => Bool = e => e, ffOut : Bool => Bool = e => e, withEr : Boolean = true) = new Generator {
    val mii = add task master(Rmii(
      RmiiParameter(
        RmiiTxParameter(
          dataWidth = 2
        ),
        RmiiRxParameter(
          dataWidth = 2,
          withEr    = withEr
        )
      )
    ))

    List(mii, phy).produce{
      val unpatched = cloneOf(mii.get)
      txCd.copy(reset = logic.mac.txReset) on {
        mii.TX.EN := ffOut(unpatched.TX.EN)
        mii.TX.D(0) := ffOut(unpatched.TX.D(0))
        mii.TX.D(1) := ffOut(unpatched.TX.D(1))
        unpatched.TX.fromTxStream() << phy.tx
      }

      rxCd on {
        unpatched.RX.CRS_DV := ffIn(mii.RX.CRS_DV)
        unpatched.RX.D(0) := ffIn(mii.RX.D(0))
        unpatched.RX.D(1) := ffIn(mii.RX.D(1))
        if(withEr) unpatched.RX.ER := ffIn(mii.RX.ER)
        phy.rx << unpatched.RX.toRxFlow().toStream
      }
    }
  }
}
