package saxon.experimental

import java.util

import spinal.core._
import spinal.core.internals.classNameOf

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, Stack}


class Unset
object Unset extends  Unset{

}

object Dependable{
  def apply(d : Dependable*)(body : => Unit) = {
    val p = new Plugin()
    p.dependencies ++= d
    p.add task(body)
    p
  }
}


trait Dependable{
  def isDone : Boolean
}

object Handle{
  def apply[T](value : T) : Handle[T] = {
    val h = Handle[T]
    h.set(value)
    h
  }
  def apply[T]() = new Handle[T]
  implicit def keyImplicit[T](key : Handle[T])(implicit c : Composable) : T = key.get
  implicit def initImplicit[T](value : T) : Handle[T] = Handle(value)
  implicit def initImplicit[T](value : Unset) : Handle[T] = Handle[T]
}

class Handle[T] extends Nameable with Dependable{
  var setOnce = false
  var value = null.asInstanceOf[T]
  def apply : T = get
  def get: T = {
    value
  }
  def set(value : T): T = {
    this.value = value
    setOnce = true
    for(other <- propagations) other.set(value)
    listeners.foreach(_())
    value
  }

  def init = {}
  def isSet = setOnce

  val propagations = ArrayBuffer[Handle[T]]()
  def propagateTo(that : Handle[T]) = {
    propagations += that
    if(isSet) that.set(this.get)
  }
  def setFrom(that : Handle[T]) = that.propagateTo(this)
  def hasDependents(implicit c : Composable) : Boolean = {
    def hit(p : Plugin) : Boolean = p.dependencies.contains(this) || p.plugins.exists(hit)
    c.plugins.exists(hit) || propagations.exists(_.hasDependents)
  }

  val listeners = ArrayBuffer[() => Unit]()
  def onSet(body : => Unit) = {
    listeners += (() => body)
    if(isSet) body
  }

  override def isDone: Boolean = isSet
}

object HandleInit{
  def apply[T](init : => T)  = new HandleInit[T](init)
}

class HandleInit[T](initValue : => T) extends Handle[T]{
  override def init : Unit = {
    set(initValue)
  }
}

object Task{
  implicit def taskToValue[T](task : Task[T]) : T = task.value
}

class Task[T](gen : => T){
  var value : T = null.asInstanceOf[T]
  def build() : Unit = value = gen
}

object Plugin{
  def stack = GlobalData.get.userDatabase.getOrElseUpdate(Plugin, new Stack[Plugin]).asInstanceOf[Stack[Plugin]]
}

class Plugin(@dontName constructionCd : Handle[ClockDomain] = null) extends Nameable  with Dependable with DelayedInit {
  if(Plugin.stack.nonEmpty && Plugin.stack.head != null){
    Plugin.stack.head.plugins += this
  }

  Plugin.stack.push(this)
  var elaborated = false
  @dontName implicit var c : Composable = null
//  @dontName implicit val p : Plugin = this
  @dontName val dependencies = ArrayBuffer[Dependable]()
  @dontName val locks = ArrayBuffer[Dependable]()
  @dontName val tasks = ArrayBuffer[Task[_]]()
  @dontName val plugins = ArrayBuffer[Plugin]()

  var implicitCd : Handle[ClockDomain] = null
  if(constructionCd != null) on(constructionCd)

  def on(clockDomain : Handle[ClockDomain]): this.type ={
    implicitCd = clockDomain
    dependencies += clockDomain
    this
  }

  def apply[T](body : => T): T = {
    Plugin.stack.push(this)
    val b = body
    Plugin.stack.pop()
    b
  }
//  {
//    val stack = Composable.stack
//    if(stack.nonEmpty) stack.head.plugins += this
//  }

  //User API
  implicit def lambdaToTask[T](lambda : => T) = new Task(lambda)
  def add = new {
    def task[T](gen : => T) : Task[T] = {
      val task = new Task(gen)
      tasks += task
      task
    }
  }
  def add[T <: Plugin](plugin : => T) : T = {
//    plugins += plugin
    apply(plugin)
  }

  override def isDone: Boolean = elaborated


  override def delayedInit(body: => Unit) = {
    body
    if ((body _).getClass.getDeclaringClass == this.getClass) {
      Plugin.stack.pop()
    }
  }
}
//object Composable{
//  def stack = GlobalData.get.userDatabase.getOrElseUpdate(Composable, new Stack[Composable]).asInstanceOf[Stack[Composable]]
//}
class Composable {
//  Composable.stack.push(this)
  val plugins = ArrayBuffer[Plugin]()
  val database = mutable.LinkedHashMap[Any, Any]()
  def add(that : Plugin) = plugins += that
  def build(): Unit = {
    implicit val c = this
    println(s"Build start")
    val pluginsAll = ArrayBuffer[Plugin]()
    def addPlugin(plugin: Plugin, clockDomain : Handle[ClockDomain]): Unit = {
      if(plugin.implicitCd == null && clockDomain != null) plugin.on(clockDomain)
      pluginsAll += plugin
      for(child <- plugin.plugins) addPlugin(child, plugin.implicitCd)
    }
    for(plugin <- plugins) addPlugin(plugin, null)
    for(p <- pluginsAll) {
      p.reflectNames()
      p.c = this
      val splitName = classNameOf(p).splitAt(1)
      if(p.isUnnamed) p.setWeakName(splitName._1.toLowerCase + splitName._2)
    }
    pluginsAll.flatMap(_.dependencies).distinct.foreach{
      case h : Handle[_] => h.init
      case _ =>
    }
    var step = 0
    while(pluginsAll.exists(!_.elaborated)){
      println(s"Step $step")
      var progressed = false
      val locks = pluginsAll.filter(!_.elaborated).flatMap(_.locks).toSet
      val produced = pluginsAll.flatMap(_.dependencies).filter(_.isDone) -- locks
      for(p <- pluginsAll if !p.elaborated && p.dependencies.forall(d => produced.contains(d)) && !locks.contains(p)){
        println(s"Build " + p.getName)
        if(p.implicitCd != null) p.implicitCd.push()
        for(task <- p.tasks){
          task.build()
          task.value match {
            case n : Nameable => {
              n.setCompositeName(p, true)
            }
            case _ =>
          }
        }
        if(p.implicitCd != null) p.implicitCd.pop()
        p.elaborated = true
        progressed = true
      }
//      val p = pluginsAll.find(p => !p.elaborated && p.dependencies.forall(d => produced.contains(d)) && !locks.contains(p))
//      p match {
//        case Some(p) => {
//          println(s"Build " + p.getName)
//          if (p.implicitCd != null) p.implicitCd.push()
//          for (task <- p.tasks) {
//            task.build()
//            task.value match {
//              case n: Nameable => {
//                n.setCompositeName(p, true)
//              }
//              case _ =>
//            }
//          }
//          if (p.implicitCd != null) p.implicitCd.pop()
//          p.elaborated = true
//          progressed = true
//        }
//        case _ =>
//      }
      if(!progressed){
        SpinalError(s"Composable hang, remaings are :\n${pluginsAll.filter(!_.elaborated).map(p => s"- ${p} depend on ${p.dependencies.mkString(", ")}").mkString("\n")}")
      }
      step += 1
    }
//    Composable.stack.pop()
  }
}