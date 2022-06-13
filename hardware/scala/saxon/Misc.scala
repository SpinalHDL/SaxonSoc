package saxon

import org.apache.commons.io.FileUtils
import spinal.core._
import spinal.core.fiber._
import spinal.lib._
import spinal.core.internals.Misc
import spinal.lib.generator.{Dependable, Dts, Export, Generator, MemoryConnection, SimpleBus}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import java.io._
import spinal.core.ClockDomain.FixedFrequency
import spinal.lib.bus.bmb.BmbInterconnectGenerator
import spinal.lib.bus.misc.{AddressMapping, SizeMapping}
import spinal.lib.bus.wishbone.{Wishbone, WishboneConfig}
import spinal.lib.com.spi.SpiHalfDuplexMaster
import spinal.lib.com.spi.ddr.{SpiXdrMaster, SpiXdrParameter}
import spinal.lib.system.debugger.{JtagBridge, JtagBridgeNoTap, SystemDebugger, SystemDebuggerConfig}

object BspGenerator {
  def apply[T <: Nameable](name : String, root: Component, memoryView : Handle[T]) {

    val allTags = ArrayBuffer[SpinalTag]()
//    root.walkComponents(tags ++= _.getTags())

    val bsp = new File("bsp")
    bsp.mkdir()

    val target = new File(bsp, name)
    target.mkdir()

    val include = new File(target, "include")
    include.mkdir()

    val linker = new File(target, "linker")
    linker.mkdir()

    val socFile = new File(include, "soc.h")
    val dtsFile = new File(include, "soc.dts")

    val headerWriter = new PrintWriter(socFile)
    val dtsWriter = new PrintWriter(dtsFile)

    headerWriter.println("#ifndef SOC_H")
    headerWriter.println("#define SOC_H")


    def camelToUpperCase(str : String) = str.split("(?=\\p{Upper})").map(_.toUpperCase).mkString("_")

    val connections = ArrayBuffer[MemoryConnection[_ <: Nameable, _ <: Nameable]]()
    val dtss = mutable.LinkedHashMap[Handle[_ <: Nameable], Dts[_]]()
    root.walkComponents{c =>
//      val gName = camelToUpperCase(g.getName())

      def rec(prefix : String, value : Any): Unit = value match {
        case value : Int => headerWriter.println(s"#define ${prefix} $value")
        case value : FixedFrequency => headerWriter.println(s"#define ${prefix} ${value.getValue.toBigDecimal.toBigInt.toString(10)}")
        case value : Boolean => headerWriter.println(s"#define ${prefix} ${if(value) 1 else 0}")
        case value : SpinalEnumElement[_] =>  headerWriter.println(s"#define ${prefix} $value")
        case value : Object => Misc.reflect(value, (name, obj) => {
          rec(prefix + "_" + camelToUpperCase(name), obj)
        })
        case _ =>
      }
      c.foreachTag{ t =>
        t match {
          case t: Export => rec(camelToUpperCase(t.name), t.value)
          case t: Dts[_] => dtss(t.node) = t
          case t: MemoryConnection[_, _] => connections += t
          case _ =>
        }
        allTags += t
      }
    }




    def connectionExplorer[T <: Nameable](view : Handle[T], address : BigInt, mapping : AddressMapping, addressLast : BigInt, tab : String): Unit ={
      headerWriter.println(s"#define ${camelToUpperCase(view.getName)} 0x${address.toString(16)}")
      mapping match{
        case mapping : SizeMapping =>
          headerWriter.println(s"#define ${camelToUpperCase(view.getName + "Size")} 0x${mapping.size.toString(16)}")
        case _ =>
      }

      val viewConnections = connections.filter(_.input == view)
      if(viewConnections.nonEmpty){
        val simpleBusOption = allTags.find{
          case t : SimpleBus[_] => t.node == view
          case _ => false
        }
        var innerTab = tab
        var addressLastRec = addressLast
        if(simpleBusOption.isDefined) {
          val simpleBus = simpleBusOption.get.asInstanceOf[SimpleBus[_]]
          val busAddress = address - addressLastRec
          dtsWriter.println(
            s"""$tab${view.getName}@${busAddress.toString(16)} {
               |$tab  compatible = "simple-bus";
               |$tab  #address-cells = <0x1>;
               |$tab  #size-cells = <0x1>;
               |$tab  ranges = <0x0 0x${busAddress.toString(16)} 0x${simpleBus.size.toString(16)}>;
                """.stripMargin
          )
          innerTab += "  "
          addressLastRec = address
        }

        for(c <- viewConnections){
          val connectionAddress = address + c.address
          connectionExplorer(c.output, connectionAddress, if(c.mapping != null) c.mapping else null, addressLastRec, innerTab)
        }
        if(simpleBusOption.isDefined) dtsWriter.println(s"\n$tab};")
      } else {
        dtss.get(view) match {
          case Some(dts) => dtsWriter.println(dts.value.split("\n").map(e => tab + e).mkString("\n"));
          case None =>
        }
      }
    }

    connectionExplorer(memoryView, 0, null, 0, "")


    headerWriter.println("#endif")
    headerWriter.close
    dtsWriter.close


  }
}


object SpiPhyDecoderGenerator{
  def apply(phy : Handle[SpiXdrMaster]) : SpiPhyDecoderGenerator = {
    val g = SpiPhyDecoderGenerator()
    g.phy.load(phy)
    g
  }
}
case class SpiPhyDecoderGenerator() extends Area{
  val phy = Handle[SpiXdrMaster]

  case class Spec(dataWidth : Int,
                  ssGen : Boolean,
                  ssMask : Int,
                  ssValue : Int,
                  spi : Handle[SpiXdrMaster])

  val specs = ArrayBuffer[Spec]()

  private def ssFullMask = (1 << phy.p.ssWidth)-1

  def whenRaw(ssMask : Int, ssValue : Int, ssGen : Boolean, dataWidth : Int = -1) : Handle[SpiXdrMaster] = {
    val spec = Spec(
      dataWidth = dataWidth,
      ssGen = ssGen,
      ssMask = ssMask,
      ssValue = ssValue,
      spi = Handle[SpiXdrMaster]
    )
    specs += spec
    logic.soon(spec.spi)
    spec.spi
  }

  def phyId(ssId : Int, dataWidth : Int = -1) : Handle[SpiXdrMaster] = whenRaw(1 << ssId, 0, true)
  def phyNone(dataWidth : Int = -1) : Handle[SpiXdrMaster] = whenRaw(-1,-1, false)

  def spiMasterId(ssId : Int, dataWidth : Int = -1) = phyId(ssId, dataWidth).derivate(phy => master(phy.toSpi()))
  def spiMasterEcp5Id(ssId : Int, dataWidth : Int = -1) = phyId(ssId, dataWidth).derivate(phy => master(phy.toSpiEcp5()))
  def spiMasterEcp5FlashId(ssId : Int, dataWidth : Int = -1) = phyId(ssId, dataWidth).derivate(phy => master(phy.toSpiEcp5Flash()))
  def spiMasterNone(dataWidth : Int = -1) = phyNone(dataWidth).derivate(phy => master(phy.toSpi()))
  def mdioMasterId(ssId : Int, dataWidth : Int = -1) = phyId(ssId, dataWidth).derivate(phy => master(phy.toMdio()))

  val logic = Handle(new Area{
    phy.data.foreach(_.read.assignDontCare())
    val ports = for(spec <- specs) yield new Area{
      spec.spi.load(SpiXdrMaster(SpiXdrParameter(
        dataWidth  = if(spec.dataWidth == -1) phy.p.dataWidth else spec.dataWidth,
        ioRate     = phy.p.ioRate,
        ssWidth    = if(spec.ssGen) 1 else 0
      )))
      val ssMask = if(spec.ssMask == -1) ssFullMask else spec.ssMask
      val ssValue = if(spec.ssValue == -1) ssFullMask else spec.ssValue
      val selected = (phy.ss & ssMask) === ssValue
      spec.spi.sclk := phy.sclk
      if(spec.spi.p.ssWidth != 0) spec.spi.ss(0) := !selected
      for((m,s) <- (phy.data, spec.spi.data).zipped){
        s.write := m.write
        s.writeEnable := m.writeEnable
        when(selected){
          m.read := s.read
        }
      }
    }
  })
}


