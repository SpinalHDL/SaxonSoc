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
//  def update(value : T)(implicit c : Composable) : Unit = set(value)

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

object Keys{

  val A = new Key[Int]
  val B = new Key[Int]
}


class KeyAPlugin(value : Int) extends Plugin {
  override def products = Seq(Keys.A)

  override lazy val logic = {
    println(s"set Key A" + value)
    Keys.A.set(value)
  }
}

class AdderPlugin(width : Int) extends Plugin{
  override def dependancies = Seq(Keys.A)
  override def products = Seq(Keys.B)


  override lazy val logic = new Area{
    println(s"Build " + width)
    val a, b = in UInt (width bits)
    val result = out UInt (width bits)
    result := a + b + Keys.A.get
    Keys.B.set(42)
  }
}

class KeyBPlugin extends Plugin {
  override def dependancies = Seq(Keys.B)

  override lazy val logic = {
    println(s"Key B=" + Keys.B.get)

  }
}


abstract class Plugin(val implicitCd : Key[ClockDomain] = null) extends Nameable {
  implicit var c : Composable = null
  var elaborated = false
  def dependancies : Seq[Any] = Nil
  def products : Seq[Any] = Nil
  lazy val logic : Any = ???

  def dependanciesHidden : Seq[Any] = Nil
  def productsHidden : Seq[Any] = Nil

  def dependanciesAll : Seq[Any] = dependancies ++ dependanciesHidden ++ (if(implicitCd != null) List(implicitCd) else Nil)
  def productsAll : Seq[Any] = products ++ productsHidden

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
    while(plugins.exists(!_.elaborated)){
      var progressed = false
      val produced = database.keys.toSet - plugins.filter(!_.elaborated).flatMap(_.products).toSet
//      val produced = plugins.filter(_.elaborated).flatMap(_.productsAll).toSet - plugins.filter(!_.elaborated).flatMap(_.products).toSet
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
    }
  }
}




class ComposablePlay(plugins : Seq[Plugin]) extends Component{
  val c = new Composable()
  c.plugins ++= plugins
  c.build()
}

object ComposablePlay{
  def main(args: Array[String]): Unit = {
    SpinalVerilog(new ComposablePlay(
      List(
        new KeyBPlugin,
        new AdderPlugin(16).setName("miaou"),
        new KeyAPlugin(8),
        new AdderPlugin(8).setName("toto")
      )
    ))
  }
}