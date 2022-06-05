package saxon.common


import spinal.core.sim._
import spinal.core.{ClockDomain, sim}
import spinal.lib.com.i2c.sim.{I2cSoftMaster, OpenDrainSoftConnection}

import scala.collection.mutable

class I2cModel(scl : OpenDrainSoftConnection, sda : OpenDrainSoftConnection, baudPeriod : Int) extends I2cSoftMaster(scl, sda, baudPeriod){
  var state : Event => Unit = start
  var inFrame = false

  def goto(s : Event => Unit) : Unit = state = s

  def start(e: Event) : Unit = e match {
    case START => goto(address)
    case STOP =>
  }

  //  def address(e : Event) : Unit = e match {
  //    case DATA(0x42) => driveFeed.push(false); goto(write42_addressAck)
  //  }

  def doStart(): Unit ={
    waitUntil(!inFrame)
    sleep(baudPeriod/2)
    sda.write(false)
    sleep(baudPeriod/2)
    waitUntil(!sda.read())
    scl.write(false)
    waitUntil(!scl.read())
    sleep(baudPeriod/2)
  }

  def doScl(): Unit ={
    sleep(baudPeriod/2)
    scl.write(true)
    waitUntil(scl.read())
    sleep(baudPeriod/2)
    scl.write(false)
    waitUntil(!scl.read())
  }

  def doScl(count : Int)  : Unit = (0 until count).foreach(_ => doScl())

  def doStop(): Unit ={
    sleep(baudPeriod/2)
    sda.write(false)
    sleep(baudPeriod/2)
    scl.write(true)
    waitUntil(scl.read())
    sleep(baudPeriod/2)
    sda.write(true)
    waitUntil(sda.read())
    sleep(baudPeriod/2)
  }

  def doFrame(bytes : Int) = {
    doStart()
    doScl(bytes*9)
    doStop()
  }


  def address(e : Event) : Unit = e match {
    case     DATA(0x42) => { ack();   expect(ACK){}
      expect(DATA(0x95))   { ack()};  expect(ACK){}
      expect(DATA(0x64))   { nack()}; expect(NACK){goto(stop)}
    }
    case     DATA(0x86) => {  ack();   expect(ACK){ write(0xA8)}
      expect(DATA(0xA8))   { nack()};  expect(ACK) {write(0xE4)}
      expect(DATA(0xE4))   { nack()};  expect(NACK){goto(stop)}
    }

    case     DATA(0xEE) =>  ack(); nack(); expect(ACK) {goto(stop)}; fork{
      write(0x60)
      doFrame(3)
      write(0x61)
      doFrame(3)
    }
    case     DATA(0x60) => { nack();  expect(ACK){ write(0x33)}
      expect(DATA(0x33))   { nack()}; expect(ACK) {write(0x48)}
      expect(DATA(0x48))   { nack()}; expect(NACK){goto(stop)}
    }
    case     DATA(0x61) => { nack();  expect(ACK){ write(0xFF)}
      expect(DATA(0x9A))   {  ack()}; expect(ACK) {write(0xFF)}
      expect(DATA(0x7E))   { nack()}; expect(NACK){goto(stop)}
    }

  }


  def ack(): Unit = driveFeed.enqueue(false)
  def nack(): Unit = driveFeed.enqueue(true)

  def write(value : Int): Unit = for(i <- (0 to 7).reverse) driveFeed.enqueue(((value >> i) & 1) != 0)

  def expect(e : Event)(action : => Unit): Unit = {
    expectations.enqueue(e -> {() => action})
    goto(expectationState)
  }

  //  def address(e : Event) : Unit = e match {
  //    case DATA(0x42) => {
  //      driveFeed.push(false); goto(write42_addressAck)
  //      expectations.pushAll(List(
  //        ACK -> null,
  //        DATA(0x95) -> {() => driveFeed.push(false)},
  //        ACK -> {() => },
  //        DATA(0x67) -> {() => driveFeed.push(true)},
  //        NACK -> null,
  //        STOP -> null
  //      ))
  //    }
  //  }
  //
  val expectations = mutable.Queue[(Event, () => Unit)]()
  def expectationState(e : Event): Unit ={
    val top = expectations.dequeue()
    assert(top._1 == e)
    top._2()
  }

  //  def write42_addressAck(e : Event) : Unit = e match {
  //    case ACK => goto(write42_aData)
  //  }
  //
  //  def write42_aData(e : Event) : Unit = e match {
  //    case DATA(0x95) => driveFeed.push(false); goto(write42_aAck)
  //  }
  //  def write42_aAck(e : Event) : Unit = e match {
  //    case ACK => goto(write42_bData)
  //  }
  //  def write42_bData(e : Event) : Unit = e match {
  //    case DATA(0x67) => driveFeed.push(true); goto(write42_bNack)
  //  }
  //  def write42_bNack(e : Event) : Unit = e match {
  //    case NACK => goto(stop)
  //  }
  def stop(e: Event) : Unit = e match {
    case STOP => goto(start)
  }

  override def event(e: Event): Unit = {
    if(sim.simTime() < 10) return
    e match {
      case START => println("I2C START")
      case DATA(value) => println(s" I2C DATA ${Integer.toHexString(value)}")
      case ACK => println(" I2C ACK")
      case NACK => println(" I2C NACK")
      case STOP => println("I2C STOP")
    }
    e match {
      case START => inFrame = true
      case STOP =>  inFrame = false
      case _ =>
    }
    state(e)

  }
}
