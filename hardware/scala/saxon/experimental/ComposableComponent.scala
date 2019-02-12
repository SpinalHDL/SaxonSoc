package saxon.experimental

import spinal.core._
import spinal.core.internals.classNameOf

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

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

abstract class Plugin(val implicitCd : Handle[ClockDomain] = null) extends Nameable {
  implicit var c : Composable = null
  var elaborated = false
  val tasks = ArrayBuffer[Task[_]]()
  val dependencies = ArrayBuffer[Any]()
  val locks = ArrayBuffer[Any]()
  if(implicitCd != null) dependencies += implicitCd

  //User API
  implicit def lambdaToTask[T](lambda : => T) = new Task(lambda)
  def add = new {
    def task[T](gen : => T) : Task[T] = {
      val task = new Task(gen)
      tasks += task
      task
    }
  }
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
      for(p <- plugins if !p.elaborated && p.dependencies.forall(d => produced.contains(d))){
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
  }
}