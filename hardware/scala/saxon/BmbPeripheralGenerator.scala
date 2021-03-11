package saxon

import spinal.core._
import spinal.lib._
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
                            (implicit interconnect: BmbInterconnectGenerator, decoder : BmbImplicitPeripheralDecoder = null) extends Area {
  val parameter          = Handle[UartCtrlMemoryMappedConfig]
  val accessSource       = Handle[BmbAccessCapabilities]
  val accessCapabilities = Handle(BmbUartCtrl.getBmbCapabilities(accessSource))
  val accessRequirements = Handle[BmbAccessParameter]

  val logic     = Handle(BmbUartCtrl(parameter, accessRequirements.toBmbParameter()))
  val ctrl      = Handle(logic.io.bus)
  val uart      = Handle(logic.io.uart.toIo)
  val interrupt = Handle(logic.io.interrupt)

  interconnect.addSlave(
    accessSource       = accessSource,
    accessCapabilities = accessCapabilities,
    accessRequirements = accessRequirements,
    bus                = ctrl,
    mapping            = Handle(SizeMapping(apbOffset, 1 << BmbUartCtrl.addressWidth))
  )
  export(parameter)
  if(decoder != null) interconnect.addConnection(decoder.bus, ctrl)

  def connectInterrupt(ctrl : InterruptCtrlGeneratorI, id : Int): Unit = {
    ctrl.addInterrupt(interrupt, id)
  }
}

abstract case class BmbPeripheralGenerator(apbOffset : BigInt, addressWidth : Int)
                                          (implicit interconnect: BmbInterconnectGenerator, decoder : BmbImplicitPeripheralDecoder = null) extends Area {



  val accessSource = Handle[BmbAccessCapabilities]
  val accessRequirements = Handle[BmbAccessParameter]

  val ctrl = Handle(Bmb(accessRequirements.toBmbParameter()))

  interconnect.addSlave(
    accessSource = accessSource,
    accessCapabilities = Handle(BmbSlaveFactory.getBmbCapabilities(
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
                               (implicit interconnect: BmbInterconnectGenerator) extends Area {

  val phyParameter = Handle[PhyLayout]
  val coreParameter = Handle[CoreParameter]
  val portsParameter = ArrayBuffer[Handle[BmbPortParameter]]()
  val phyPort = Handle(logic.io.phy)
  val ctrl = Handle(logic.io.ctrl)


  val accessSource = Handle[BmbAccessCapabilities]
  val accessRequirements = Handle[BmbAccessParameter]
  def mapCtrlAt(address : BigInt)(implicit interconnect: BmbInterconnectGenerator, decoder : BmbImplicitPeripheralDecoder = null) : this.type = {
    interconnect.addSlave(
      accessSource = accessSource,
      accessCapabilities = accessSource.map(CtrlWithoutPhyBmb.getBmbCapabilities),
      accessRequirements = accessRequirements,
      bus = ctrl,
      mapping = SizeMapping(address, 1 << CtrlWithoutPhyBmb.addressWidth)
    )
    if(decoder != null) interconnect.addConnection(decoder.bus, ctrl)
    this
  }

  def addPort() = this rework new Area {
    val requirements = Handle[BmbAccessParameter]
    val portId = portsParameter.length
    val bmb = Handle(logic.io.bmb(portId))
    val parameter = Handle(
      BmbPortParameter(
        bmb = requirements.toBmbParameter(),
        clockDomain = ClockDomain.current,
        cmdBufferSize = 16,
        dataBufferSize = 32,
        rspBufferSize = 32
      )
    )

    portsParameter += parameter

    interconnect.addSlave(
      accessCapabilities = phyParameter.map(CtrlWithPhy.bmbCapabilities),
      accessRequirements = requirements,
      bus = bmb,
      mapping = Handle(SizeMapping(memoryAddress, phyParameter.sdram.capacity))
    )
  }

  val logic = Handle(new CtrlWithoutPhyBmb(
    p =  CtrlParameter(
      core = coreParameter,
      ports = portsParameter.map(_.get)
    ),
    pl = phyParameter,
    ctrlParameter = accessRequirements.toBmbParameter()
  ))
}

case class XilinxS7PhyBmbGenerator(configAddress : BigInt)(implicit interconnect: BmbInterconnectGenerator, decoder : BmbImplicitPeripheralDecoder = null) extends Area{
  val sdramLayout = Handle[SdramLayout]
  val ctrl = Handle(logic.ctrl)
  val sdram = Handle(logic.phy.io.sdram.toIo)
  val clk90 = Handle[ClockDomain]
  val serdesClk0 = Handle[ClockDomain]
  val serdesClk90 = Handle[ClockDomain]

  val accessSource = Handle[BmbAccessCapabilities]
  val accessRequirements = Handle[BmbAccessParameter]

  interconnect.addSlave(
    accessSource = accessSource,
    accessCapabilities = accessSource.map(CtrlWithoutPhyBmb.getBmbCapabilities),
    accessRequirements = accessRequirements,
    bus = ctrl,
    mapping = SizeMapping(configAddress, 1 << CtrlWithoutPhyBmb.addressWidth)
  )

  val logic = Handle(new Area{
    val phy = XilinxS7Phy(
      sl = sdramLayout,
      clkRatio = 2,
      clk90 = clk90,
      serdesClk0 = serdesClk0,
      serdesClk90 = serdesClk90
    )
    val ctrl = Bmb(accessRequirements)
    phy.driveFrom(BmbSlaveFactory(ctrl))
  })


  def connect(ctrl : SdramXdrBmbGenerator): Unit = {
    ctrl.phyParameter.loadAsync(XilinxS7Phy.phyLayout(sdramLayout, 2))
    Handle{
      ctrl.logic.io.phy <> logic.phy.io.ctrl
    }
  }
  if(decoder != null) interconnect.addConnection(decoder.bus, ctrl)
}

case class Ecp5Sdrx2PhyGenerator() extends Area{
  val sdramLayout = Handle[SdramLayout]
  val sdram = Handle(logic.io.sdram.toIo)
  val logic = Handle(Ecp5Sdrx2Phy(sdramLayout))

  def connect(ctrl : SdramXdrBmbGenerator): this.type = {
    ctrl.phyParameter.loadAsync(Ecp5Sdrx2Phy.phyLayout(sdramLayout))
    Handle(ctrl.logic.io.phy <> logic.io.ctrl)
    this
  }
}



case class  BmbGpioGenerator(apbOffset : Handle[BigInt] = Unset)
                             (implicit interconnect: BmbInterconnectGenerator, decoder : BmbImplicitPeripheralDecoder = null) extends Area{
  val parameter = Handle[spinal.lib.io.Gpio.Parameter]
  val gpio = Handle(logic.io.gpio.toIo)
  val ctrl = Handle(logic.io.bus)

  val accessSource = Handle[BmbAccessCapabilities]
  val accessRequirements = Handle[BmbAccessParameter]
  //TODO not having to setCompositeName
  val interrupts : Handle[List[Handle[Bool]]] = Handle(List.tabulate(parameter.width)(i => logic.io.interrupt(i).setCompositeName(interrupts, i.toString)))
  val logic = Handle(BmbGpio2(parameter, accessRequirements.toBmbParameter()))

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
                      (implicit interconnect: BmbInterconnectGenerator, decoder : BmbImplicitPeripheralDecoder = null) extends Area {
  val parameter = Handle[SpiXdrMasterCtrl.MemoryMappingParameters]
  val withXip = Handle(false)
  val interrupt = Handle(logic.io.interrupt)
  val phy = Handle(logic.io.spi)
  val spi = Handle[Nameable]
  val ctrl : Handle[Bmb] = Handle(logic.io.ctrl)

  val accessSource = Handle[BmbAccessCapabilities]
  val accessRequirements = Handle[BmbAccessParameter]

  val logic = Handle(BmbSpiXdrMasterCtrl(parameter, accessRequirements.toBmbParameter()))
//  val logic = add task BmbSpiXdrMasterCtrl(parameter.copy(xip = if(!withXip) null else XipBusParameters(24, bmbRequirements.lengthWidth)), accessRequirements.toBmbParameter())

  val bmbRequirements = Handle[BmbParameter]
  val bmb = Handle[Bmb]

//  dependencies += withXip.produce{
//    if(withXip) {
//      ???
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
//    }
//  }


//  dependencies += withXip

  @dontName var interruptCtrl : InterruptCtrlGeneratorI = null
  var interruptId = 0
  def connectInterrupt(ctrl : InterruptCtrlGeneratorI, id : Int): Unit = {
    ctrl.addInterrupt(interrupt, id)
    interruptCtrl = ctrl
    interruptId = id
  }

  def inferSpiSdrIo() = this rework Handle(spi.load(master(phy.toSpi().setPartialName(spi, "")))) //TODO automated naming
  def inferSpiIce40() = this rework Handle(spi.load{
    phy.toSpiIce40().asInOut().setPartialName(spi, "")
  })
  def phyAsIo() = Handle(phy.toIo)

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
                           (implicit interconnect: BmbInterconnectGenerator, decoder : BmbImplicitPeripheralDecoder = null) extends Area {
  val parameter = Handle[I2cSlaveMemoryMappedGenerics]
  val i2c = Handle(logic.io.i2c.toIo)
  val ctrl = Handle(logic.io.ctrl)
  val interrupt = Handle(logic.io.interrupt)

  val accessSource = Handle[BmbAccessCapabilities]
  val accessRequirements = Handle[BmbAccessParameter]
  interconnect.addSlave(
    accessSource = accessSource,
    accessCapabilities = accessSource.derivate(BmbGpio2.getBmbCapabilities),
    accessRequirements = accessRequirements,
    bus = ctrl,
    mapping = apbOffset.derivate(SizeMapping(_, 1 << Gpio.addressWidth))
  )
  if(decoder != null) interconnect.addConnection(decoder.bus, ctrl)

  val logic = Handle(BmbI2cCtrl(parameter, accessRequirements.toBmbParameter()))

  def connectInterrupt(ctrl : InterruptCtrlGeneratorI, id : Int): Unit = {
    ctrl.addInterrupt(interrupt, id)
  }
}


case class BmbMacEthGenerator(address : Handle[BigInt] = Unset)
                             (implicit interconnect: BmbInterconnectGenerator, decoder : BmbImplicitPeripheralDecoder = null) extends Area {
  val parameter = Handle[MacEthParameter]
  val rxCd, txCd = Handle[ClockDomain]
  val interrupt = Handle(logic.io.interrupt)
  val phy = Handle(logic.io.phy)
  val ctrl = Handle(logic.io.bus)

  val accessSource = Handle[BmbAccessCapabilities]
  val accessRequirements = Handle[BmbAccessParameter]
  val logic = Handle(BmbMacEth(
      p            = parameter,
      bmbParameter = accessRequirements.toBmbParameter(),
      txCd         = txCd,
      rxCd         = rxCd
  ))

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


  def withPhyMii() = new Area{
    val mii = Handle(master(Mii(
      MiiParameter(
        MiiTxParameter(
          dataWidth = 4,
          withEr    = false
        ),
        MiiRxParameter(
          dataWidth = 4
        )
      )
    )))

    txCd.loadAsync(ClockDomain(mii.TX.CLK))
    rxCd.loadAsync(ClockDomain(mii.RX.CLK))

    Handle{
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

  def withPhyRmii(ffIn : Bool => Bool = e => e, ffOut : Bool => Bool = e => e, withEr : Boolean = true) = new Area {
    val mii = Handle(master(Rmii(
      RmiiParameter(
        RmiiTxParameter(
          dataWidth = 2
        ),
        RmiiRxParameter(
          dataWidth = 2,
          withEr    = withEr
        )
      )
    )))

    Handle{
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
