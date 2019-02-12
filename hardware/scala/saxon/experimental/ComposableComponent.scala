package saxon.experimental

import spinal.core._
import spinal.core.internals.classNameOf

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object Key{
  implicit def keyImplicit[T](key : Key[T])(implicit c : Composable) : T = key.get
}

class Key[T]{
  def apply(implicit c : Composable) : T = get(c)

  def get(implicit c : Composable) : T = {
    c.database(this).asInstanceOf[T]
  }

  def set(value : T)(implicit c : Composable) : T = {
    c.database(this) = value
    value
  }
}

class DefaultKey[T](default : => T) extends Key[T]{
  override def get(implicit c: Composable): T = c.database.getOrElseUpdate(this, default).asInstanceOf[T]
}

abstract class Plugin(val implicitCd : Key[ClockDomain] = null) extends Nameable {
  implicit var c : Composable = null
  var elaborated = false
  def dependancies : Seq[Any] = Nil
  def locks : Seq[Any] = Nil
  lazy val logic : Any = ???

  def dependanciesHidden : Seq[Any] = Nil
  def locksHidden : Seq[Any] = Nil

  def dependanciesAll : Seq[Any] = dependancies ++ dependanciesHidden ++ (if(implicitCd != null) List(implicitCd) else Nil)
  def locksAll : Seq[Any] = locks ++ locksHidden

  lazy val logicHidden : Any = {}
}

class Composable {
  val plugins = ArrayBuffer[Plugin]()
  val database = mutable.LinkedHashMap[Any, Any]()
  def add(that : Plugin) = plugins += that
  def build(): Unit = {
    implicit val c = this
    println(s"Build start")
    for(p <- plugins) {
      p.c = this
      val splitName = classNameOf(p).splitAt(1)
      if(p.isUnnamed) p.setWeakName(splitName._1.toLowerCase + splitName._2)
    }
    var step = 0
    while(plugins.exists(!_.elaborated)){
      println(s"Step $step")
      var progressed = false
      val produced = database.keys.toSet - plugins.filter(!_.elaborated).flatMap(_.locks).toSet
      for(p <- plugins if !p.elaborated && p.dependanciesAll.forall(d => produced.contains(d))){
        println(s"Build " + p.getName)
        if(p.implicitCd != null) p.implicitCd.push()
        p.logic match {
          case n : Nameable => {
            n.setCompositeName(p, true)
          }
          case _ =>
        }
        if(p.implicitCd != null) p.implicitCd.pop()
        p.elaborated = true
        progressed = true
      }
      if(!progressed){
        SpinalError(s"Composable hang, remaings are : ${plugins.filter(!_.elaborated).mkString(", ")}")
      }
      step += 1
    }
  }
}