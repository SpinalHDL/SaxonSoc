package saxon

import spinal.core._
import spinal.lib.bus.amba3.apb.{Apb3, Apb3Config, Apb3SlaveFactory}
import spinal.lib.bus.misc.BusSlaveFactory
import spinal.lib.com.spi.SpiHalfDuplexMaster
import spinal.lib.com.spi.ddr.{Apb3SpiXdrMasterCtrl, SpiXdrMasterCtrl}
import spinal.lib.com.uart.{Apb3UartCtrl, UartCtrlGenerics, UartCtrlInitConfig, UartCtrlMemoryMappedConfig, UartParityType, UartStopType}
import spinal.lib.generator.{Dependable, Generator, Handle}
import spinal.lib.io.Apb3Gpio2
import spinal.lib.{generator, master}
import spinal.lib.misc.plic.{PlicGateway, PlicGatewayActiveHigh, PlicMapper, PlicMapping, PlicTarget}

import scala.collection.mutable.ArrayBuffer


object Apb3DecoderStdGenerators {

  def apbUart(apbOffset : BigInt,
              p : UartCtrlMemoryMappedConfig)
             (implicit decoder: Apb3DecoderGenerator) = wrap(new Generator {
    val apb = produce(logic.io.apb)
    val interrupt = produce(logic.io.interrupt)
    val uart = produceIo(logic.io.uart)
    val logic = add task Apb3UartCtrl(p)

    decoder.addSlave(apb, apbOffset)
  })

  def addBasicUart(apbOffset : BigInt,
                   baudrate : Int,
                   txFifoDepth : Int,
                   rxFifoDepth : Int)
                  (implicit decoder: Apb3DecoderGenerator) = apbUart(
    apbOffset = apbOffset,
    UartCtrlMemoryMappedConfig(
      uartCtrlConfig = UartCtrlGenerics(
        dataWidthMax      = 8,
        clockDividerWidth = 12,
        preSamplingSize   = 1,
        samplingSize      = 3,
        postSamplingSize  = 1
      ),
      initConfig = UartCtrlInitConfig(
        baudrate = baudrate,
        dataLength = 7,  //7 => 8 bits
        parity = UartParityType.NONE,
        stop = UartStopType.ONE
      ),
      busCanWriteClockDividerConfig = false,
      busCanWriteFrameConfig = false,
      txFifoDepth = txFifoDepth,
      rxFifoDepth = rxFifoDepth
    )
  )

  def addGpio(apbOffset : BigInt,
              p : spinal.lib.io.Gpio.Parameter)
             (implicit decoder: Apb3DecoderGenerator) = wrap(new Generator{

    val gpio = produceIo(logic.io.gpio)
    val apb = produce(logic.io.bus)
    val logic = add task Apb3Gpio2(p)

    decoder.addSlave(apb, apbOffset)
  })

  def addPlic(apbOffset : BigInt) (implicit decoder: Apb3DecoderGenerator) = wrap(new Generator {
    val gateways = ArrayBuffer[Handle[PlicGateway]]()
    val interrupt = produce(logic.targets(0).iep)
    val apb = produce(logic.apb)

    val priorityWidth = 1

    def addInterrupt[T <: Generator](source : Handle[Bool], id : Int) = {
      this.dependencies += wrap(new Generator {
        dependencies += source
        add task new Area {
          gateways += PlicGatewayActiveHigh(
            source = source,
            id = id,
            priorityWidth = priorityWidth
          )
        }
      })
    }

    val logic = add task new Area{
      val apb = Apb3(addressWidth = 16, dataWidth = 32)
      val bus = Apb3SlaveFactory(apb)

      val targets = Seq(
        PlicTarget(
          gateways = gateways.map(_.get),
          priorityWidth = priorityWidth
        )
      )

      val plicMapping = PlicMapping.light
      gateways.foreach(_.priority := 1)
      targets.foreach(_.threshold := 0)
      targets.foreach(_.ie.foreach(_ := True))
      val mapping = PlicMapper(bus, plicMapping)(
        gateways = gateways.map(_.get),
        targets = targets
      )
    }


    decoder.addSlave(apb, apbOffset)
  })

  def addMachineTimer(apbOffset : BigInt) (implicit decoder: Apb3DecoderGenerator) = wrap(new Generator{
    val interrupt = produce(logic.io.mTimeInterrupt) //TODO fix error report when this one isn't drived
    val apb = produce(logic.io.bus)
    val logic = add task MachineTimer()

    decoder.addSlave(apb, apbOffset)
  })
}







case class Apb3UartGenerator(apbOffset : BigInt)
                            (implicit decoder: Apb3DecoderGenerator) extends Generator {
  val parameter = createDependency[UartCtrlMemoryMappedConfig]
  val interrupt = produce(logic.io.interrupt)
  val uart = produceIo(logic.io.uart)
  val apb = produce(logic.io.apb)
  val logic = add task Apb3UartCtrl(parameter)

  decoder.addSlave(apb, apbOffset)

  add task dts(apb,
    s"""${this.name}: tty@${apbOffset.toString(16)} {
       |  compatible = "spinal-lib,uart-1.0";
       |  reg = <0x${apbOffset.toString(16)} 0x1000>;
       |}""".stripMargin
  )
  export(parameter)

}


case class Apb3SpiGenerator(apbOffset : BigInt)
                            (implicit decoder: Apb3DecoderGenerator) extends Generator {
  val parameter = createDependency[SpiXdrMasterCtrl.MemoryMappingParameters]
  val interrupt = produce(logic.io.interrupt)
  val phy = produce(logic.io.spi)
  val spi = Handle[SpiHalfDuplexMaster]
  val apb = produce(logic.io.apb)
  val logic = add task Apb3SpiXdrMasterCtrl(parameter)

  decoder.addSlave(apb, apbOffset)

  add task dts(
    apb,
    s"""${this.name}: spi@${apbOffset.toString(16)} {
       |  compatible = "spinal-lib,spi-1.0";
       |  #address-cells = <1>;
       |  #size-cells = <0>;
       |  reg = <0x${apbOffset.toString(16)} 0x1000>;
       |}""".stripMargin
  )

  def inferSpiSdrIo() = Dependable(phy)(spi.load(master(phy.toSpi().setPartialName(spi, "")))) //TODO automated naming
}





case class  Apb3GpioGenerator(apbOffset : BigInt)
                             (implicit decoder: Apb3DecoderGenerator) extends Generator{
  val parameter = createDependency[spinal.lib.io.Gpio.Parameter]
  val gpio = produceIo(logic.io.gpio)
  val apb = produce(logic.io.bus)
  val logic = add task Apb3Gpio2(parameter)

  decoder.addSlave(apb, apbOffset)

  add task dts(
    apb,
    s"""${this.name}: gpio@${apbOffset.toString(16)} {
       |  compatible = "spinal-lib,gpio-1.0";
       |  reg = <0x${apbOffset.toString(16)} 0x1000>;
       |}""".stripMargin
  )
}

//case class Apb3IoMuxGenerator(apbOffset : BigInt)
//                             (implicit decoder: Apb3DecoderGenerator) extends IoMuxGenerator{
//  val apb = produce(Apb3(12, 32))
//  override val mapper : Handle[BusSlaveFactory] = produce(Apb3SlaveFactory(apb))
//}


case class Apb3PlicGenerator(apbOffset : BigInt) (implicit decoder: Apb3DecoderGenerator) extends Generator {
  val gateways = ArrayBuffer[Handle[PlicGateway]]()
  val apb = produce(logic.apb)
  val apbConfig = Apb3Config(22, 32)

  val priorityWidth = createDependency[Int]
  val mapping = createDependency[PlicMapping]

  val targetsModel = ArrayBuffer[Handle[Bool]]()
  def addTarget(target : Handle[Bool]) = {
    targetsModel += target
    dependencies += target
  }

  def addInterrupt[T <: Generator](source : Handle[Bool], id : Int) = {
    this.dependencies += wrap(new Generator {
      dependencies += source
      add task new Area {
        gateways += PlicGatewayActiveHigh(
          source = source,
          id = id,
          priorityWidth = priorityWidth
        )
      }
    })
  }

  val logic = add task new Area{
    val apb = Apb3(apbConfig)
    val bus = Apb3SlaveFactory(apb)

    val targets = targetsModel.map(flag =>
      PlicTarget(
        gateways = gateways.map(_.get),
        priorityWidth = priorityWidth
      )
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
}

case class Apb3MachineTimerGenerator(apbOffset : BigInt) (implicit decoder: Apb3DecoderGenerator) extends Generator{
  val interrupt = produce(logic.io.mTimeInterrupt) //TODO fix error report when this one isn't drived
  val apb = produce(logic.io.bus)
  val logic = add task MachineTimer()

  decoder.addSlave(apb, apbOffset)

  val hz = export(produce(ClockDomain.current.frequency))
}
