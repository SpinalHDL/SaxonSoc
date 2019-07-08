package saxon


import spinal.core._
import spinal.lib.generator._
import spinal.lib.bus.amba3.apb.{Apb3, Apb3SlaveFactory}
import spinal.lib.bus.misc.BusSlaveFactory
import spinal.lib.com.uart.{Uart, UartCtrlMemoryMappedConfig}
import spinal.lib._
import spinal.lib.bus.amba3.apb.sim.Apb3Driver
import spinal.lib.io.{Gpio, TriState, TriStateArray}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

abstract class IoMuxGenerator extends Generator {
  val mapper = createDependency[BusSlaveFactory]

  case class Connection(source: Handle[Bool], sink: Handle[Bool], enable : Handle[Bool])
  case class SinkModel(sink: Handle[Bool]) extends Generator {
//    val id = createDependency[Int]

    dependencies += sink

    val logic = add task new Area{
      val sources = connections.filter(_.sink == sink)
      sources.foreach(source => makeConnectable(sink, source.source))
      sources.length match {
        case 0 => ???
        case _ => sink := sources.map(s => s.source && s.enable).reduce(_ || _)
      }
    }
  }

  val sinks = mutable.LinkedHashMap[Handle[Bool], SinkModel]()
  @dontName val connections = ArrayBuffer[Connection]()

  def getSinkModel(sink : Handle[Bool]) = sinks.getOrElseUpdate(sink,new SinkModel(sink))
  def addConnection(source: Handle[Bool], sink: Handle[Bool], enable : Handle[Bool]) : Unit = {
    val sinkModel = getSinkModel(sink)
    sinkModel.dependencies += source
    sinkModel.dependencies += enable
    connections += Connection(source, sink, enable)
  }

  def addOutput(source: Handle[Bool], sink: IoGenerator, enable : Handle[Bool]) : Unit = {
//    source.setAsDirectionLess()
    addConnection(source, sink.write, enable)
    addConnection(Handle(True), sink.writeEnable, enable)
  }

  def addOutput(m: Handle[Bits], id : Int, s: IoGenerator, enable : Handle[Bool]) : Unit = {
    val pin = m.produce(m.setAsDirectionLess.apply(id))
    addOutput(pin, s, enable)
  }


  def addInput(source: Handle[Bool], sink: IoGenerator, enable : Handle[Bool]) : Unit = {
    source.produce{
      assert(source.component == sink.component)
      source.setAsDirectionLess()
      source := sink.read
    }
  }


//  def addConnection(m: Handle[TriState[Bool]], s: Handle[TriState[Bool]], enable : Handle[Bool]) : Unit = {
//    addConnection(m.produce(m.write), s.produce(s.write), enable)
//    addConnection(m.produce(m.writeEnable), s.produce(s.writeEnable), enable)
//    Dependable(m, s){ makeConnectable(s.read, m.read); m.read := s.read}
//  }


  def addConnection(m: Handle[TriStateArray], id : Int, s: IoGenerator, enable : Handle[Bool]) : Unit = {
    val pin = m.produce(m.setAsDirectionLess.apply(id))
    addConnection(pin.produce(pin.write), s.write, enable)
    addConnection(pin.produce(pin.writeEnable), s.writeEnable, enable)
    Dependable(pin, s){ makeConnectable(s.read, pin.read); pin.read := s.read}
  }

  def makeConnectable(to : Bool, from : Bool): Unit ={
  /*  if(to.isDirectionLess){
      assert(from.component == to.component)
    } else {
      if(from.isDirectionLess){
        assert(from.component == to.component)
      } else {
        if(from.component == to.component) from.setAsDirectionLess()
      }
    }*/
  }

  def createEnable(id : Int) : Handle[Bool] = mapper.produce{
    mapper.createReadAndWrite(Bool, id / mapper.busDataWidth, id % mapper.busDataWidth) init(False)
  }
}

case class Apb3IoMuxGenerator(apbOffset : BigInt)
                             (implicit decoder: Apb3DecoderGenerator)extends IoMuxGenerator{

  val apb = Handle(Apb3(12, 32))
  apb.produce(mapper.load(Apb3SlaveFactory(apb)))
  decoder.addSlave(apb, apbOffset)
}

case class IoGenerator() extends Generator{
  val pin = add task master(io.TriState(Bool))
  val read = produce(pin.read)
  val write = produce(pin.write)
  val writeEnable = produce(pin.writeEnable)

  def connect(that : Handle[Bool]) = that.produce {
    assert(that.component == Component.current)
    if(that.isInput){
      that.setAsDirectionLess()
      that := read
    } else if(that.isOutput){
      that.setAsDirectionLess()
      write := that
      writeEnable := True
    } else {
      ???
    }
  }
}


case class IoGeneratorTest() extends Generator{

  implicit val apbDecoder = Apb3DecoderGenerator()
  //Create an slave apb bus to controle the apbDecoder for tests purposes
  val apb = apbDecoder.produce{
    val bus = slave(cloneOf(apbDecoder.input.get))
    bus <> apbDecoder.input
    bus
  }

  val portA, portB = IoGenerator()
  val gpioA = Apb3GpioGenerator(0x0000)
  val uartA = Apb3UartGenerator(0x1000)
  
  uartA.parameter load UartCtrlMemoryMappedConfig(
    baudrate = 20000000,
    txFifoDepth = 1,
    rxFifoDepth = 1
  )

  gpioA.parameter load Gpio.Parameter(width = 2)

  val pinMux = Apb3IoMuxGenerator(apbOffset=0x2000)
  val gpio0Enable = pinMux.createEnable(id=0)
  val gpio1Enable = pinMux.createEnable(id=1)
  val uartEnable = pinMux.createEnable(id=2)

  pinMux.addConnection(gpioA.gpio, 0, portA, gpio0Enable)
  pinMux.addConnection(gpioA.gpio, 1, portB, gpio1Enable)
  pinMux.addOutput(uartA.txd, portA, uartEnable)
  pinMux.addInput(uartA.rxd, portB, uartEnable)
}

object IoGeneratorTester extends App{
  import spinal.core.sim._

  SimConfig.withConfig(SpinalConfig(defaultClockDomainFrequency = FixedFrequency(100 MHz))).withWave.compile(IoGeneratorTest().toComponent()).doSim {dut =>
    dut.clockDomain.forkStimulus(10)
    val driver = Apb3Driver(dut.apb, dut.clockDomain)

    def stim(): Unit ={
      for(i <- 0 until 4) {
        driver.write(0x0004, 0)
        driver.write(0x0004, 3)
      }

      driver.write(0x1000, 0xAA)
      while(((driver.read(0x1004) >> 16) & 0xFF) != 1){}
    }

    driver.write(0x0008, 3)

    driver.write(0x2000, 0)
    stim()

    driver.write(0x2000, 1)
    stim()

    driver.write(0x2000, 2)
    stim()

    driver.write(0x2000, 4)
    stim()
  }
}