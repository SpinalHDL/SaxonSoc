package saxon

import org.apache.commons.io.FileUtils
import spinal.core.{Nameable, SpinalEnumElement}
import spinal.core.internals.Misc
import spinal.lib.generator.{Dts, Export, Generator, Handle, MemoryConnection, SimpleBus, Tag}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import java.io._

import spinal.core.ClockDomain.FixedFrequency

object BspGenerator {
  def apply[T <: Nameable](name : String, root: Generator, memoryView : Handle[T]) {

    val generators = ArrayBuffer[Generator]()
    generators += root
    root.foreachGeneratorRec(generators += _)


    val bsp = new File("bsp")
    bsp.mkdir()

    val target = new File(bsp, name)
    FileUtils.deleteDirectory(target)
    target.mkdir()

    val include = new File(target, "include")
    include.mkdir()

    val headerFile = new PrintWriter(new File(include, "soc.h"))
    val dtsFile = new PrintWriter(new File("soc.dts"))

    headerFile.println("#ifndef SOC_H")
    headerFile.println("#define SOC_H")


    def camelToUpperCase(str : String) = str.split("(?=\\p{Upper})").map(_.toUpperCase).mkString("_")

    val allTags = ArrayBuffer[Tag]()
    val connections = ArrayBuffer[MemoryConnection[_ <: Nameable, _ <: Nameable]]()
    val dtss = mutable.LinkedHashMap[Handle[_ <: Nameable], Dts[_]]()
    for (g <- generators) {
      val gName = camelToUpperCase(g.getName())

      def rec(prefix : String, value : Any): Unit = value match {
        case value : Int => headerFile.println(s"#define ${prefix} $value")
        case value : FixedFrequency => headerFile.println(s"#define ${prefix} ${value.getValue.toBigDecimal.toBigInt.toString(10)}")
        case value : Boolean => headerFile.println(s"#define ${prefix} ${if(value) 1 else 0}")
        case value : SpinalEnumElement[_] =>  headerFile.println(s"#define ${prefix} $value")
        case value : Object => Misc.reflect(value, (name, obj) => {
          rec(prefix + "_" + camelToUpperCase(name), obj)
        })
        case _ =>
      }
      g.tags.foreach {
        case t : Export => rec(camelToUpperCase(t.name), t.value)
        case t : Dts[Nameable] => dtss(t.node) = t
        case t : MemoryConnection[_,_] => connections += t
        case _ =>
      }
      allTags ++= g.tags
    }




    def connectionExplorer[T <: Nameable](view : Handle[T], address : BigInt, addressLast : BigInt, tab : String): Unit ={
      headerFile.println(s"#define ${camelToUpperCase(view.getName)} 0x${address.toString(16)}")

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
          dtsFile.println(
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
          connectionExplorer(c.output, connectionAddress, addressLastRec, innerTab)
        }
        if(simpleBusOption.isDefined) dtsFile.println(s"\n$tab};")
      } else {
        dtss.get(view) match {
          case Some(dts) => dtsFile.println(dts.value.split("\n").map(e => tab + e).mkString("\n"));
          case None =>
        }
      }
    }

    connectionExplorer(memoryView, 0, 0, "")


    headerFile.println("#endif")
    headerFile.close
    dtsFile.close
  }
}
