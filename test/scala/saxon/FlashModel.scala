package saxon

import java.nio.file.{Files, Paths}

import spinal.core.ClockDomain
import spinal.sim._
import spinal.core.sim._
import spinal.lib.com.spi.ddr.SpiXdrMaster

case class FlashModel(spi : SpiXdrMaster, cd : ClockDomain) {
  var sclkLast = false
  val spiDataWithIndex = spi.data.zipWithIndex
  var spiDataRead = Array.fill(spi.p.dataWidth)(0)

  var edgeHandler : (Boolean, Int) => Unit = idle
  var counter, buffer, address = 0
  var miso = 0

  val content = Array.fill[Byte](16 * 1024 * 1024)(0xAA.toByte)
  def loadBinary(path : String, offset : Int): Unit ={
    val bin = Files.readAllBytes(Paths.get(path))
    for((v,i) <- bin.zipWithIndex) content(i + offset) = v
  }

  def goto(that : (Boolean, Int) => Unit): Unit = {
    edgeHandler = that
  }


  cd.onSampling{
    if(spi.ss.toInt == 1){
      sclkLast = false
      counter = 8
      buffer = 0
      goto(command)
    } else {
      val sclkWrite = spi.sclk.write.toInt
      for(pin <- 0 until spi.p.dataWidth) {
        spi.data(pin).read #= spiDataRead(pin)
        spiDataRead(pin) = 0
      }
      for(phase <- 0 until spi.p.ioRate){
        val sclk = ((sclkWrite >> phase) & 1) != 0
        if(sclkLast != sclk) {
          var mosi = 0
          for((v,pin) <- spiDataWithIndex) mosi |= ((v.write.toInt >> phase) & 1) << pin
          edgeHandler(sclk, mosi)
        }
        for(pin <- 0 until spi.p.dataWidth) spiDataRead(pin) |= ((miso >> pin) & 1) << phase
        sclkLast = sclk
      }
    }
  }

  def idle(rising : Boolean, mosi : Int): Unit = 0
  def command(rising : Boolean, mosi : Int): Unit = {
    if (rising){
      counter -= 1
      buffer |= (mosi & 1) << counter
      if (counter == 0) {
        println(s"CMD $buffer")
        buffer match {
          case 0x0B => counter = 24; address = 0; goto(readAddress)
          case _ =>
        }
      }
    }
  }
  def readAddress(rising : Boolean, mosi : Int): Unit = {
    if (rising){
      counter -= 1
      address |= (mosi & 1) << counter
      if (counter == 0) {
        println(s"ADDRESS $address")
        counter = 8; goto(readDummy)
      }
    }
  }
  def readDummy(rising : Boolean, mosi : Int): Unit = {
    if (rising){
      counter -= 1
      if (counter == 0) {
        counter = 8; goto(readPayload)
      }
    }
  }
  def readPayload(rising : Boolean, mosi : Int): Unit = {
    if (!rising){
      counter -= 1
      miso = ((content(address) >> counter) & 1) << 1
      if (counter == 0) {
        address += 1
        counter = 8
      }
    }
  }
}
