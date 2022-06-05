package saxon.board.blackice

import spinal.core._
import spinal.lib.master
import spinal.lib.io._

import scala.collection.mutable

case class SB_IO(pinType : String) extends BlackBox{
  addGeneric("PIN_TYPE", B(pinType))
  val PACKAGE_PIN = inout(Analog(Bool))
  val OUTPUT_ENABLE = in(Bool)
  val D_OUT_0 = in(Bool)
  val D_IN_0 = out(Bool)
  setDefinitionName("SB_IO")
}

object IceStormInOutWrapper {
  def apply[T <: Component](c : T) : T = {
    val dataParents = mutable.LinkedHashMap[Data,Int]()
    def add(that : Data): Unit ={
      if(that.parent != null){
        dataParents(that.parent) = dataParents.getOrElseUpdate(that.parent,0) + 1
        add(that.parent)
      }
    }
    for(io <- c.getAllIo){
      add(io)
    }

    c.rework {
      for ((dataParent, count) <- dataParents) {
        dataParent match {
          case bundle: TriState[_]   if bundle.writeEnable.isOutput  => {
            if (bundle.read.isInstanceOf[Bits]) {
              val bitsBundle = bundle.asInstanceOf[TriState[Bits]]
              for(i <- 0 until bitsBundle.read.getBitsWidth) {
                val newIo = inout(Analog(Bool)).setWeakName(bitsBundle.getName() + "_" + i)
                val sbio = SB_IO("101001")
                sbio.PACKAGE_PIN := newIo
                sbio.OUTPUT_ENABLE := bitsBundle.writeEnable
                sbio.D_OUT_0 := bitsBundle.write.asBits(i)
                bitsBundle.read(i) := sbio.D_IN_0
                println("set_io " + bitsBundle.getName() + "_" + i)
              }
            } else if (bundle.read.isInstanceOf[Bool]) {
              val boolBundle = bundle.asInstanceOf[TriState[Bool]]
              val newIo = inout(Analog(Bool)).setWeakName(boolBundle.getName())
              val sbio = SB_IO("101001")
              sbio.PACKAGE_PIN := newIo
              sbio.OUTPUT_ENABLE := boolBundle.writeEnable
              sbio.D_OUT_0 := boolBundle.write
              boolBundle.read := sbio.D_IN_0
              println("set_io " + boolBundle.getName())
            }
            bundle.setAsDirectionLess.unsetName().allowDirectionLessIo
          }          

          case bundle : TriStateOutput[_] if bundle.isOutput => {
            val newIo = inout(Analog(bundle.dataType)).setWeakName(bundle.getName())
            bundle.setAsDirectionLess.unsetName().allowDirectionLessIo
            when(bundle.writeEnable){
              newIo := bundle.write
            }
          }
          case bundle: ReadableOpenDrain[Bool] => {
            val newIo = inout(Analog(Bool)).setWeakName(bundle.getName())
            val sbio = SB_IO("101001")
            sbio.PACKAGE_PIN := newIo
            sbio.OUTPUT_ENABLE := !bundle.write
            sbio.D_OUT_0 := bundle.write
            bundle.read := sbio.D_IN_0
            println("set_io " + bundle.getName())
            bundle.setAsDirectionLess.unsetName().allowDirectionLessIo
          }
          case bundle: TriStateArray if bundle.writeEnable.isOutput => {
            for(i <- 0 until bundle.width) {
              println("set_io " + bundle.getName() + "_" + i)
              val newIo = inout(Analog(Bool)).setWeakName(bundle.getName() + "_" + i)
              val sbio = SB_IO("101001")
              sbio.PACKAGE_PIN := newIo
              sbio.OUTPUT_ENABLE := bundle.writeEnable(i)
              sbio.D_OUT_0 := bundle.write(i)
              bundle.read(i) := sbio.D_IN_0
            }
            bundle.setAsDirectionLess.unsetName().allowDirectionLessIo
          }
          case bundle: Bundle => {
            for (data <- bundle.elements) {
              println("set_io " + data._2.getName())
            }
          }
          case _ =>
        }
      }
    }
    c
  }
}


