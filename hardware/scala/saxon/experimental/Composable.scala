package saxon.experimental

import java.util

import spinal.core._
import spinal.core.internals.classNameOf

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, Stack}
object Handle{
  def apply[T] = new Handle[T]
  implicit def keyImplicit[T](key : Handle[T])(implicit c : Composable) : T = key.get
}

class Handle[T]{
  def default : T = ???
  def apply(implicit c : Composable) : T = get(c)
  def get(implicit c : Composable) : T = {
    c.database.getOrElseUpdate(this, default).asInstanceOf[T]
  }
  def set(value : T)(implicit c : Composable) : T = {
    c.database(this) = value
    value
  }
}

object HandleInit{
  def apply[T](init : => T)  = new HandleInit[T](init)
}

class HandleInit[T](init : => T) extends Handle[T]{
  override def default : T = init
}

object Task{
  implicit def taskToValue[T](task : Task[T]) : T = task.value
}

class Task[T](gen : => T){
  var value : T = null.asInstanceOf[T]
  def build() : Unit = value = gen
}

abstract class Plugin(@dontName constructionCd : Handle[ClockDomain] = null) extends Nameable {
  var elaborated = false
  @dontName implicit var c : Composable = null
  @dontName val dependencies = ArrayBuffer[Any]()
  @dontName val locks = ArrayBuffer[Any]()
  @dontName val tasks = ArrayBuffer[Task[_]]()
  @dontName val plugins = ArrayBuffer[Plugin]()

  var implicitCd : Handle[ClockDomain] = null
  if(constructionCd != null) on(constructionCd)


  def on(clockDomain : Handle[ClockDomain]): this.type ={
    implicitCd = clockDomain
    dependencies += clockDomain
    this
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
  def add[T <: Plugin](plugin : T) : T = {
    plugins += plugin
    plugin
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
    var step = 0
    while(pluginsAll.exists(!_.elaborated)){
      println(s"Step $step")
      var progressed = false
      val produced = database.keys.toSet - pluginsAll.filter(!_.elaborated).flatMap(_.locks).toSet
      for(p <- pluginsAll if !p.elaborated && p.dependencies.forall(d => produced.contains(d))){
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
      if(!progressed){
        SpinalError(s"Composable hang, remaings are : ${plugins.filter(!_.elaborated).mkString(", ")}")
      }
      step += 1
    }
//    Composable.stack.pop()
  }
}