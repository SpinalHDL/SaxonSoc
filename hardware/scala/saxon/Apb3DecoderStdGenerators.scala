package saxon

import spinal.core._
import spinal.lib.bus.amba3.apb.{Apb3, Apb3Config, Apb3Dummy, Apb3SlaveFactory}
import spinal.lib.bus.bmb.{Bmb, BmbParameter}
import spinal.lib.bus.misc.{AddressMapping, BusSlaveFactory, SizeMapping}
import spinal.lib.com.spi.SpiHalfDuplexMaster
import spinal.lib.com.spi.ddr.{Apb3SpiXdrMasterCtrl, SpiXdrMasterCtrl}
import spinal.lib.com.uart.{Apb3UartCtrl, UartCtrlGenerics, UartCtrlInitConfig, UartCtrlMemoryMappedConfig, UartParityType, UartStopType}
import spinal.lib.generator._
import spinal.lib.io.Apb3Gpio2
import spinal.lib._
import spinal.lib.com.spi.ddr.SpiXdrMasterCtrl.XipBusParameters
import spinal.lib.misc.MachineTimer
import spinal.lib.misc.plic.{PlicGateway, PlicGatewayActiveHigh, PlicMapper, PlicMapping, PlicTarget}

import scala.collection.mutable.ArrayBuffer


trait InterruptCtrl{
  def addInterrupt(interrupt : Handle[Bool], id : Int)
  def getBus : Handle[Nameable]
}

case class Apb3UartGenerator(apbOffset : Handle[BigInt] = Unset)
                            (implicit decoder: Apb3DecoderGenerator) extends Generator {
  val parameter = createDependency[UartCtrlMemoryMappedConfig]
  val interrupt = produce(logic.io.interrupt)
  val uart = produceIo(logic.io.uart)
  val apb = produce(logic.io.apb)
  val logic = add task Apb3UartCtrl(parameter)

  val txd = uart.produce(uart.txd)
  val rxd = uart.produce(uart.rxd)

  @dontName var interruptCtrl : InterruptCtrl = null
  var interruptId = 0
  def connectInterrupt(ctrl : InterruptCtrl, id : Int): Unit = {
    ctrl.addInterrupt(interrupt, id)
    interruptCtrl = ctrl
    interruptId = id
  }


  decoder.addSlave(apb, apbOffset)

  dts(apb) {
    s"""${apb.getName()}: tty@${apbOffset.toString(16)} {
       |  compatible = "spinal-lib,uart-1.0";
       |  reg = <0x${apbOffset.toString(16)} 0x1000>;
       |${if(interruptCtrl != null) {
    s"""  interrupt-parent = <&${interruptCtrl.getBus.getName()}>
       |  interrupts = <$interruptId>;""".stripMargin} else ""}
       |}""".stripMargin
  }
  export(parameter)
}

object Apb3SpiGenerator{
  def apply(apbOffset : BigInt, xipOffset : BigInt = 0)
           (implicit decoder: Apb3DecoderGenerator, interconnect: BmbInterconnectGenerator = null): Apb3SpiGenerator ={
    new Apb3SpiGenerator(apbOffset,xipOffset)
  }
}
class Apb3SpiGenerator(apbOffset : Handle[BigInt] = Unset, xipOffset : Handle[BigInt] = 0)
                            (implicit decoder: Apb3DecoderGenerator, interconnect: BmbInterconnectGenerator = null) extends Generator {
  val parameter = createDependency[SpiXdrMasterCtrl.MemoryMappingParameters]
  val withXip = Handle(false)
  val interrupt = produce(logic.io.interrupt)
  val phy = produce(logic.io.spi)
  val spi = Handle[Nameable]
  val apb = produce(logic.io.apb)
  val logic = add task Apb3SpiXdrMasterCtrl(parameter.copy(xip = if(!withXip) null else XipBusParameters(24, bmbRequirements.lengthWidth)))

  val bmbRequirements = Handle[BmbParameter]
  val bmb = product[Bmb]

  dependencies += withXip.produce{
    if(withXip) {
      dependencies += bmbRequirements
      interconnect.addSlaveAt(
        capabilities = Handle(SpiXdrMasterCtrl.getXipBmbCapabilities()),
        requirements = bmbRequirements,
        bus = bmb,
        address = xipOffset
      )
      Dependable(Apb3SpiGenerator.this, bmbRequirements){
        bmb.load(logic.io.xip.fromBmb(bmbRequirements))
      }
    }
  }


  dependencies += withXip

  decoder.addSlave(apb, apbOffset)

  @dontName var interruptCtrl : InterruptCtrl = null
  var interruptId = 0
  def connectInterrupt(ctrl : InterruptCtrl, id : Int): Unit = {
    ctrl.addInterrupt(interrupt, id)
    interruptCtrl = ctrl
    interruptId = id
  }

  def inferSpiSdrIo() = this(Dependable(phy)(spi.load(master(phy.toSpi().setPartialName(spi, ""))))) //TODO automated naming
  def inferSpiIce40() = this(Dependable(phy)(spi.load{
    phy.toSpiIce40().asInOut().setPartialName(spi, "")
  }))
  def phyAsIo() = produceIo(phy.get)

  dts(apb) {
    s"""${apb.getName()}: spi@${apbOffset.toString(16)} {
       |  compatible = "spinal-lib,spi-1.0";
       |  #address-cells = <1>;
       |  #size-cells = <0>;
       |  reg = <0x${apbOffset.toString(16)} 0x1000>;
       |}""".stripMargin
  }
}





case class  Apb3GpioGenerator(apbOffset : Handle[BigInt] = Unset)
                             (implicit decoder: Apb3DecoderGenerator) extends Generator{
  val parameter = createDependency[spinal.lib.io.Gpio.Parameter]
  val gpio = produceIo(logic.io.gpio)
  val apb = produce(logic.io.bus)
  //TODO not having to setCompositeName
  val interrupts : Handle[List[Handle[Bool]]] = parameter.produce(List.tabulate(parameter.width)(i => this.produce(logic.io.interrupt(i)).setCompositeName(interrupts, i.toString)))
  val logic = add task Apb3Gpio2(parameter)

  @dontName var interruptCtrl : InterruptCtrl = null
  var interruptOffsetId = 0
  def connectInterrupts(ctrl : InterruptCtrl, offsetId : Int): Unit = interrupts.produce{
    for(pinId <- parameter.interrupt) ctrl.addInterrupt(interrupts.get(pinId), offsetId + pinId)
    interruptCtrl = ctrl
    interruptOffsetId = offsetId
  }
  def connectInterrupt(ctrl : InterruptCtrl, pinId : Int, interruptId : Int): Unit = interrupts.produce{
    ctrl.addInterrupt(interrupts.get(pinId), interruptId)
  }
  def pin(id : Int) = gpio.produce(gpio.get.setAsDirectionLess.apply(id))

  decoder.addSlave(apb, apbOffset)

  dts(apb) {
    s"""${apb.getName()}: gpio@${apbOffset.toString(16)} {
       |  compatible = "spinal-lib,gpio-1.0";
       |  reg = <0x${apbOffset.toString(16)} 0x1000>;
       |${if(interruptCtrl != null) {
    s"""  interrupt-parent = <&${interruptCtrl.getBus.getName()}>
       |  interrupts = <${parameter.interrupt.map(_ + interruptOffsetId).mkString(" ")}>;""".stripMargin} else ""}
       |}""".stripMargin
  }
}

//case class Apb3IoMuxGenerator(apbOffset : BigInt)
//                             (implicit decoder: Apb3DecoderGenerator) extends IoMuxGenerator{
//  val apb = produce(Apb3(12, 32))
//  override val mapper : Handle[BusSlaveFactory] = produce(Apb3SlaveFactory(apb))
//}


case class Apb3PlicGenerator(apbOffset : Handle[BigInt] = Unset) (implicit decoder: Apb3DecoderGenerator) extends Generator with InterruptCtrl{
  @dontName val gateways = ArrayBuffer[Handle[PlicGateway]]()
  val apb = produce(logic.apb)
  val apbConfig = Apb3Config(22, 32)

  val priorityWidth = createDependency[Int]
  val mapping = createDependency[PlicMapping]

  val targetsModel = ArrayBuffer[Handle[Bool]]()
  def addTarget(target : Handle[Bool]) = {
    val id = targetsModel.size

    targetsModel += target
    dependencies += target

    //TODO remove the need of delaying stuff for name capture
    add task(tags += new Export(Apb3PlicGenerator.this.getName() + "_" + target.getName, id))
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

        tags += new Export(Apb3PlicGenerator.this.getName() + "_" + source.getName, id)
      }
    }
  }

  override def getBus(): Handle[Nameable] = apb

  val logic = add task new Area{
    val apb = Apb3(apbConfig)
    val bus = Apb3SlaveFactory(apb)

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


  decoder.addSlave(apb, apbConfig, apbOffset)


  dts(apb) {
    s"""${apb.getName()}: interrupt-controller@${apbOffset.toString(16)} {
       |  compatible = "sifive,plic-1.0.0", "sifive,fu540-c000-plic";
       |  #interrupt-cells = <1>;
       |  interrupt-controller;
       |  interrupts-extended = <&L1 11 &L1 9>;
       |  reg = <0x${apbOffset.toString(16)} 0x400000>;
       |  riscv,ndev = <${(gateways.map(_.id) ++ Seq(0)).max}>;
       |}""".stripMargin
  }
}

case class Apb3DummyGenerator(apbConfig : Apb3Config, apbOffset : Handle[BigInt] = Unset) (implicit decoder: Apb3DecoderGenerator) extends Generator{
  val apb = produce(logic.io.apb)
  val logic = add task Apb3Dummy(apbConfig)
  decoder.addSlave(apb, apbOffset)
}

case class Apb3MachineTimerGenerator(apbOffset : Handle[BigInt] = Unset) (implicit decoder: Apb3DecoderGenerator) extends Generator{
  val interrupt = produce(logic.io.mTimeInterrupt)
  val apb = produce(logic.io.bus)
  val logic = add task MachineTimer()

  decoder.addSlave(apb, apbOffset)

  val hz = export(produce(ClockDomain.current.frequency))
}



case class Apb3MasterGenerator(apbOffset : Handle[BigInt] = Unset)
                              (implicit decoder: Apb3DecoderGenerator) extends Generator {
  val parameter = createDependency[Apb3Config]
  val apb = produce(master(Apb3(parameter)))
  decoder.addSlave(apb, apbOffset)
}
