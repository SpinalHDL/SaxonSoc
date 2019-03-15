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
    val p = new Generator()
    p.dependencies ++= d
    p.add task(body)
    p
  }
}


trait Dependable{
  def isDone : Boolean
}

object Handle{
  def apply[T](value : =>  T) : Handle[T] = {
    val h = Handle[T]
    h.lazyDefaultGen = () => value
    h
  }
  def apply[T]() = new Handle[T]
  implicit def keyImplicit[T](key : Handle[T])(implicit c : Composable) : T = key.get
  implicit def initImplicit[T](value : T) : Handle[T] = Handle(value)
  implicit def initImplicit[T](value : Unset) : Handle[T] = Handle[T]
}

trait HandleCoreSubscriber[T]{
  def changeCore(core : HandleCore[T]) : Unit
  def lazyDefault (): T
  def lazyDefaultAvailable : Boolean
}

class HandleCore[T]{
  private var loaded = false
  private var value = null.asInstanceOf[T]

  val subscribers = mutable.HashSet[HandleCoreSubscriber[T]]()

  def get : T = {
    if(!loaded){
      subscribers.count(_.lazyDefaultAvailable) match {
        case 0 =>
        case 1 => load(subscribers.find(_.lazyDefaultAvailable).get.lazyDefault())
        case _ => SpinalError("Multiple handle default values")
      }
    }
    value
  }
  def load(value : T): T = {
    this.value = value
    loaded = true
    value
  }

  def merge(that : HandleCore[T]): Unit ={
    (this.loaded, that.loaded) match {
      case (false, _) => this.subscribers.foreach(_.changeCore(that))
      case (true, false) => that.subscribers.foreach(_.changeCore(this))
    }
  }

  def isLoaded = loaded || subscribers.exists(_.lazyDefaultAvailable)
}

class Handle[T] extends Nameable with Dependable with HandleCoreSubscriber[T]{
  private var core = new HandleCore[T]
  core.subscribers += this

  override def changeCore(core: HandleCore[T]): Unit = this.core = core

  def merge(that : Handle[T]): Unit = this.core.merge(that.core)

  def apply : T = get
  def get: T = core.get
  def load(value : T): T = core.load(value)

  def isLoaded = core.isLoaded

  override def isDone: Boolean = isLoaded

  var lazyDefaultGen : () => T = null
  override def lazyDefault() : T = lazyDefaultGen()
  override def lazyDefaultAvailable: Boolean = lazyDefaultGen != null
}

//object HandleInit{
//  def apply[T](init : => T)  = new HandleInit[T](init)
//}
//
//class HandleInit[T](initValue : => T) extends Handle[T]{
//  override def init : Unit = {
//    load(initValue)
//  }
//}

object Task{
  implicit def generatorToValue[T](generator : Task[T]) : T = generator.value
}

class Task[T](var gen :() => T) extends Dependable {
  var value : T = null.asInstanceOf[T]
  var isDone = false
  var enabled = true

  def build() : Unit = {
    if(enabled) value = gen()
    isDone = true
  }

  def disable(): Unit ={
    enabled = false
  }

  def patchWith(patch : => T): Unit ={
    gen = () => patch
  }
}

object Generator{
  def stack = GlobalData.get.userDatabase.getOrElseUpdate(Generator, new Stack[Generator]).asInstanceOf[Stack[Generator]]
}

class Generator(@dontName constructionCd : Handle[ClockDomain] = null) extends Nameable  with Dependable with DelayedInit {
  if(Generator.stack.nonEmpty && Generator.stack.head != null){
    Generator.stack.head.generators += this
  }

  Generator.stack.push(this)
  var elaborated = false
  @dontName implicit var c : Composable = null
//  @dontName implicit val p : Plugin = this
  @dontName val dependencies = ArrayBuffer[Dependable]()
  @dontName val locks = ArrayBuffer[Dependable]()
  @dontName val tasks = ArrayBuffer[Task[_]]()
  @dontName val generators = ArrayBuffer[Generator]()

  var implicitCd : Handle[ClockDomain] = null
  if(constructionCd != null) on(constructionCd)

  def on(clockDomain : Handle[ClockDomain]): this.type ={
    implicitCd = clockDomain
    dependencies += clockDomain
    this
  }

  def apply[T](body : => T): T = {
    Generator.stack.push(this)
    val b = body
    Generator.stack.pop()
    b
  }
//  {
//    val stack = Composable.stack
//    if(stack.nonEmpty) stack.head.generators += this
//  }

  //User API
//  implicit def lambdaToGenerator[T](lambda : => T) = new Task(() => lambda)
  def add = new {
    def task[T](gen : => T) : Task[T] = {
      val task = new Task(() => gen)
      tasks += task
      task
    }
  }
  def add[T <: Generator](generator : => T) : T = {
//    generators += generator
    apply(generator)
  }

  override def isDone: Boolean = elaborated


  override def delayedInit(body: => Unit) = {
    body
    if ((body _).getClass.getDeclaringClass == this.getClass) {
      Generator.stack.pop()
    }
  }
}
//object Composable{
//  def stack = GlobalData.get.userDatabase.getOrElseUpdate(Composable, new Stack[Composable]).asInstanceOf[Stack[Composable]]
//}
class Composable {
//  Composable.stack.push(this)
  val generators = ArrayBuffer[Generator]()
  val database = mutable.LinkedHashMap[Any, Any]()
  def add(that : Generator) = generators += that
  def build(): Unit = {
    implicit val c = this
    println(s"Build start")
    val generatorsAll = ArrayBuffer[Generator]()
    def addPlugin(generator: Generator, clockDomain : Handle[ClockDomain]): Unit = {
      if(generator.implicitCd == null && clockDomain != null) generator.on(clockDomain)
      generatorsAll += generator
      for(child <- generator.generators) addPlugin(child, generator.implicitCd)
    }
    for(generator <- generators) addPlugin(generator, null)
    for(p <- generatorsAll) {
      p.reflectNames()
      p.c = this
      val splitName = classNameOf(p).splitAt(1)
      if(p.isUnnamed) p.setWeakName(splitName._1.toLowerCase + splitName._2)
    }
//    generatorsAll.flatMap(_.dependencies).distinct.foreach{
//      case h : Handle[_] => h.init
//      case _ =>
//    }
    var step = 0
    while(generatorsAll.exists(!_.elaborated)){
      println(s"Step $step")
      var progressed = false
      val locks = generatorsAll.filter(!_.elaborated).flatMap(_.locks).toSet
      val produced = generatorsAll.flatMap(_.dependencies).filter(_.isDone) -- locks
      for(p <- generatorsAll if !p.elaborated && p.dependencies.forall(d => produced.contains(d)) && !locks.contains(p)){
        println(s"Build " + p.getName)
        if(p.implicitCd != null) p.implicitCd.push()
        for(generator <- p.tasks){
          generator.build()
          generator.value match {
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
//      val p = generatorsAll.find(p => !p.elaborated && p.dependencies.forall(d => produced.contains(d)) && !locks.contains(p))
//      p match {
//        case Some(p) => {
//          println(s"Build " + p.getName)
//          if (p.implicitCd != null) p.implicitCd.push()
//          for (generator <- p.generators) {
//            generator.build()
//            generator.value match {
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
        SpinalError(s"Composable hang, remaings are :\n${generatorsAll.filter(!_.elaborated).map(p => s"- ${p} depend on ${p.dependencies.filter(d => !produced.contains(d)).mkString(", ")}").mkString("\n")}")
      }
      step += 1
    }
//    Composable.stack.pop()
  }
}