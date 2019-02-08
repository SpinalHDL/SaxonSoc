package saxon.dma

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb.sim.Apb3Driver
import spinal.lib.bus.amba3.apb.{Apb3, Apb3SlaveFactory}
import spinal.lib.bus.simple.{PipelinedMemoryBus, PipelinedMemoryBusConfig}
import spinal.lib.eda.bench.{Bench, Rtl}
import spinal.lib.eda.icestorm.IcestormStdTargets

import scala.util.Random
object Dma{

  case class MappingParameter(val channelOffset : Int = 0x100,
                              val channelDelta : Int = 0x80,
                              val sourceOffset : Int = 0x40,
                              val destinationOffset : Int = 0x60,

                              val channelLengthOffset : Int = 0x0C,
                              val channelAddressOffset : Int = 0x08,
                              val channelConfigOffset : Int = 0x04,
                              val channelCtrlOffset : Int = 0x00,
                              val channelIncrementBit : Int = 0,
                              val channelReloadBit : Int = 4,
                              val channelMemoryBit : Int = 5,
                              val channelSizeBit : Int = 12,
                              val channelBurstBit : Int = 16,
                              val channelStartBit : Int = 0,
                              val channelStopBit : Int = 1,
                              val channelBusyBit : Int = 2,
                              val channelResetBit : Int = 0)

  case class Parameter( memConfig: PipelinedMemoryBusConfig,
                        inputs : Seq[InputParameter],
                        outputs : Seq[OutputParameter],
                        channels : Seq[ChannelParameter],
                        mapping : MappingParameter = new MappingParameter){
    val sizeWidth = log2Up(memConfig.dataWidth/8)
    val sizeType = HardType(UInt(sizeWidth bits))

  }

  case class InputParameter(dataWidth : Int)
  case class OutputParameter(dataWidth : Int)
  case class ChannelParameter(fifoDepth : Int,
                              sourceLengthWidth : Int,
                              destinationLengthWidth : Int){
    assert(isPow2(fifoDepth))
    val fifoPtrType = HardType(UInt(log2Up(fifoDepth)+1 bits))
    val fifoPtrTailType = HardType(UInt(log2Up(fifoDepth) bits))
    val burstType = HardType(UInt(log2Up(fifoDepth) bits))
  }

  case class InputPayload(p : InputParameter) extends Bundle{
    val data = Bits(p.dataWidth bits)
  }
  case class OutputPayload(p : OutputParameter) extends Bundle{
    val data = Bits(p.dataWidth bits)
  }



  case class Io(p : Parameter) extends Bundle{
    val config = slave(Apb3(12, 32))
    val memRead = master(PipelinedMemoryBus(p.memConfig))
    val memWrite = master(PipelinedMemoryBus(p.memConfig))
    val inputs = Vec(p.inputs.map(ip => slave(Stream(InputPayload(ip)))))
    val ouputs = Vec(p.outputs.map(op => master(Stream(OutputPayload(op)))))
  }

  case class Dma(val p : Parameter) extends Component{
    val io = Io(p)

    val fifoDepth = p.channels.map(_.fifoDepth).sum
    val fifoRam = Mem(Bits(p.memConfig.dataWidth bits), fifoDepth)

    class SourceDestinationBase(cp : ChannelParameter) extends Bundle {
      val busy = Bool()
      val start = Bool()
      val stop = Bool()
      val dontStop = Bool()
      val increment, reload = Bool()
      val memory = Bool()
      val address = UInt(p.memConfig.addressWidth bits)
      val burst = cp.burstType()
      val length, counter = UInt(cp.sourceLengthWidth bits)
      val counterMatch = Bool()
      val size = p.sizeType()
      val fifoPtrIncrement = Bool()
      val fifoPtr = cp.fifoPtrType()

      def behaviour() : Unit = {
        List(busy, stop, increment, reload, memory, address, burst, length, counter, size, fifoPtr).foreach(_.setAsReg())
        busy.init(False)
        stop.init(False)
        start := False
        dontStop := False
        busy clearWhen(counterMatch || (stop && !dontStop)) setWhen(start)
        fifoPtr init(0)
        fifoPtrIncrement := False
        when(fifoPtrIncrement){
          fifoPtr := fifoPtr + 1
        }
        counterMatch := counter === length

      }
    }

    case class Channel(cp : ChannelParameter) extends Bundle {
      val reset = Bool()

      val source = new SourceDestinationBase(cp)
      val destination = new SourceDestinationBase(cp)

      val sourceFifoPtr, destinationFifoPtr = fifoRam.addressType()

      val fifoFullOrEmpty, fifoFull, fifoEmpty = Bool()

      def behaviour(): Unit= {
        source.behaviour()
        destination.behaviour()
        reset := False

        fifoFullOrEmpty := source.fifoPtr(log2Up(cp.fifoDepth)-1 downto 0) === destination.fifoPtr(log2Up(cp.fifoDepth)-1 downto 0)
        fifoFull := fifoFullOrEmpty && source.fifoPtr.msb =/= destination.fifoPtr.msb
        fifoEmpty := fifoFullOrEmpty && source.fifoPtr.msb === destination.fifoPtr.msb

        sourceFifoPtr := U(channelToFifoOffset(cp), log2Up(fifoRam.wordCount) bits) | source.fifoPtr(widthOf(source.fifoPtr)-2 downto 0).resized
        destinationFifoPtr := U(channelToFifoOffset(cp), log2Up(fifoRam.wordCount) bits) | destination.fifoPtr(widthOf(destination.fifoPtr)-2 downto 0).resized
      }
    }

    val channelsByFifoSize = p.channels.sortBy(_.fifoDepth).reverse
    val channelToFifoOffset = (channelsByFifoSize, channelsByFifoSize.scanLeft(0)(_ + _.fifoDepth)).zipped.toMap

    val channels = Vec(p.channels.map(Channel(_)))
    channels.foreach(_.behaviour())

    val beatType = HardType(UInt(p.channels.map(_.burstType.getBitsWidth).max bits))
    val memReadCmd = new Area {
      val proposal = new Area {
        val valid = Vec(channels.map(c =>
          c.source.busy && !c.source.stop && c.source.memory && !c.source.counterMatch && !(c.source.fifoPtr - c.destination.fifoPtr + c.source.burst).msb
        ))
        val oneHot = OHMasking.first(valid)
      }
      val busy = RegInit(False)
      val cmdBeat, rspBeat = Reg(beatType)
      val selected = new Area{
        val oh = Reg(proposal.oneHot)
        val channel = channels(OHToUInt(oh))
      }

      val cmdDone = cmdBeat === selected.channel.source.burst || selected.channel.source.counterMatch
      val rspDone = rspBeat === rspBeat && cmdDone
      when(!busy){
        busy := proposal.valid.orR
        selected.oh := proposal.oneHot
        cmdBeat := 0
        rspBeat := 0
      } otherwise {
        busy := !rspDone
        selected.channel.source.dontStop := True
      }

      io.memRead.cmd.valid := busy && !cmdDone
      io.memRead.cmd.address := selected.channel.source.address + (selected.channel.source.counter |<< selected.channel.source.size)
      io.memRead.cmd.write := False
      io.memRead.cmd.data.assignDontCare()
      io.memRead.cmd.mask.assignDontCare()

      when(io.memRead.cmd.fire){
        selected.channel.source.counter := selected.channel.source.counter + 1
        cmdBeat := cmdBeat + 1
      }

      when(io.memRead.rsp.fire){
        rspBeat := rspBeat + 1
      }
    }

    val fifoWrite = new Area{
      val valid = False
      val address = fifoRam.addressType().assignDontCare()
      val data = fifoRam.wordType().assignDontCare()
      fifoRam.write(address, data, valid)

      val selected = new Area{
//        val inputsValid = channels.map(c => c.source.busy && !c.source.stop && !c.source.memory && io.inputs(c.source.address.resized).valid)
      }

      when(io.memRead.rsp.valid){
        valid := True
        address := memReadCmd.selected.channel.sourceFifoPtr
        data := io.memRead.rsp.data  //TODO normalisation
        memReadCmd.selected.channel.source.fifoPtrIncrement := True
      } otherwise {
//        valid := selected.inputsValid.orR
      }


    }

    val fifoRead = new Area {
      val proposal = new Area {
        val valid = Vec(channels.map(c =>
          c.destination.busy && !c.source.stop && c.destination.memory && !c.destination.counterMatch && !c.fifoEmpty
        ))
        val oneHot = OHMasking.first(valid)
      }
      val busy = RegInit(False)
      val fifoBeat, memBeat = Reg(beatType)
      val selected = new Area{
        val oh = Reg(proposal.oneHot)
        val channel = channels(OHToUInt(oh))
      }

      val cmdDone = fifoBeat === selected.channel.destination.burst || selected.channel.destination.counterMatch || selected.channel.destination.fifoPtr === selected.channel.source.fifoPtr
      val rspDone = memBeat === fifoBeat && cmdDone

      when(!busy){
        busy := proposal.valid.orR
        selected.oh := proposal.oneHot
        fifoBeat := 0
        memBeat := 0
      } otherwise {
        busy := !rspDone
        selected.channel.destination.dontStop := True
      }

      val fifoReadCmd = Stream(fifoRam.addressType)
      fifoReadCmd.valid := busy && !rspDone
      fifoReadCmd.payload := memReadCmd.selected.channel.destinationFifoPtr
      selected.channel.destination.fifoPtrIncrement setWhen(fifoReadCmd.fire)

      val read = fifoRam.streamReadSync(fifoReadCmd)

      val bufferIn = Stream(Bits(p.memConfig.dataWidth bits))
      bufferIn.arbitrationFrom(read)
      bufferIn.payload := read.payload

      val bufferOut = bufferIn.s2mPipe()
      io.memWrite.cmd.valid := busy && bufferOut.valid
      io.memWrite.cmd.address := selected.channel.destination.address + (selected.channel.destination.counter |<< selected.channel.destination.size)
      io.memWrite.cmd.write := True
      io.memWrite.cmd.data := bufferOut.payload
      io.memWrite.cmd.mask := "1111" //TODO
      bufferOut.ready := io.memWrite.cmd.ready

      when(io.memWrite.cmd.fire){
        selected.channel.destination.counter := selected.channel.destination.counter + 1
      }
    }



    val bus = Apb3SlaveFactory(io.config)

    for((channel, idx) <- channels.zipWithIndex){
      def map(that : SourceDestinationBase, offset : Int): Unit = {
        bus.write(
          offset + p.mapping.channelCtrlOffset,
          p.mapping.channelStartBit -> that.start,
          p.mapping.channelStopBit -> that.stop
        )
        bus.read(
          offset + p.mapping.channelCtrlOffset,
          p.mapping.channelBusyBit -> that.busy
        )
        bus.write(
          offset + p.mapping.channelConfigOffset,
          p.mapping.channelIncrementBit -> that.increment,
          p.mapping.channelReloadBit -> that.reload,
          p.mapping.channelMemoryBit -> that.memory,
          p.mapping.channelSizeBit  -> that.size,
          p.mapping.channelBurstBit  -> that.burst
        )
        bus.write(offset + p.mapping.channelAddressOffset, 0 -> that.address)
        bus.write(offset + p.mapping.channelLengthOffset, 0 -> that.length)
        when(bus.isWriting(offset + p.mapping.channelCtrlOffset)){
          that.counter := 0
        }
      }

      val offset = p.mapping.channelOffset + idx * p.mapping.channelDelta
      bus.write(offset, p.mapping.channelResetBit -> channel.reset)
      map(channel.source, offset + p.mapping.sourceOffset)
      map(channel.destination, offset + p.mapping.destinationOffset)
    }
  }

}

object DmaBench extends App{

  def reduceIo[T <: Component](c : T): T ={
    c.rework {
      c.getOrdredNodeIo.foreach(_.allowDirectionLessIo)
      val inputs = c.getOrdredNodeIo.filter(_.isInput).map(_.setAsDirectionLess())
      val outputs = c.getOrdredNodeIo.filter(_.isOutput).map(_.setAsDirectionLess())
      val o = outputs.map(_.setAsDirectionLess()).asBits()
      val input = in(Bool())
      val output = out(Bool())
      val inputHistory = History(input, 1 to inputs.map(widthOf(_)).sum)
      val inputVec = inputs.map{
        case b : Bool => List(b)
        case b : BitVector => b.asBools
      }.flatten
      (inputVec, inputHistory.asBits.asBools).zipped.foreach(_ := _)
      println("HistoryLength=" + inputHistory.length)
      output := RegNext(o.asBools.xorR)
    }

    c
  }

  val p = Dma.Parameter(
    memConfig = PipelinedMemoryBusConfig(32,32),
    inputs = Nil,
    outputs = Nil,
    channels = List(
      Dma.ChannelParameter(
        fifoDepth = 32,
        sourceLengthWidth = 12,
        destinationLengthWidth = 12
      ),
      Dma.ChannelParameter(
        fifoDepth = 32,
        sourceLengthWidth = 12,
        destinationLengthWidth = 12
      ),
      Dma.ChannelParameter(
        fifoDepth = 32,
        sourceLengthWidth = 12,
        destinationLengthWidth = 12
      )
    )
  )
  val dma = new Rtl {
    override def getName(): String = "Dma"
    override def getRtlPath(): String = "Dma.v"
    SpinalVerilog(reduceIo(Dma.Dma(p)))
  }


  val rtls = List(dma)

  val targets = IcestormStdTargets().take(1)

  Bench(rtls, targets, "/eda/tmp/")

}

object DmaDebug extends App{
  import spinal.core.sim._

  val p = Dma.Parameter(
    memConfig = PipelinedMemoryBusConfig(30,32),
    inputs = Nil,
    outputs = Nil,
    channels = List(
      Dma.ChannelParameter(
        fifoDepth = 32,
        sourceLengthWidth = 12,
        destinationLengthWidth = 12
      ),
      Dma.ChannelParameter(
        fifoDepth = 16,
        sourceLengthWidth = 12,
        destinationLengthWidth = 12
      )
    )
  )
  SimConfig.withWave.compile(new Dma.Dma(p)).doSim("test", 42){dut =>
    val config = Apb3Driver(dut.io.config, dut.clockDomain)

    def writeXConfig(offset : Int)(
      increment : Boolean,
      reload : Boolean,
      memory : Boolean,
      size : Int,
      burst : Int,
      address : Int,
      length : Int): Unit ={
      config.write(offset + p.mapping.channelLengthOffset, length)
      config.write(offset + p.mapping.channelAddressOffset, address)
      config.write(offset + p.mapping.channelConfigOffset,
        (if (increment) 1 << p.mapping.channelIncrementBit else 0) |
          (if (reload) 1 << p.mapping.channelReloadBit else 0) |
          (if (memory) 1 << p.mapping.channelMemoryBit else 0) |
          (size << p.mapping.channelSizeBit) |
          (burst << p.mapping.channelBurstBit)
      )
    }

    def sourceOffset(channelId : Int) = p.mapping.channelOffset + channelId * p.mapping.channelDelta + p.mapping.sourceOffset
    def destinationOffset(channelId : Int) = p.mapping.channelOffset + channelId * p.mapping.channelDelta + p.mapping.destinationOffset

    def doStart(offset : Int): Unit = {
      config.write(offset + p.mapping.channelCtrlOffset, 1 << p.mapping.channelStartBit)
    }





    val memory = new Array[Byte](0x10000)
    for(i <- 0 until memory.length) memory(i) = i.toByte
//    Random.nextBytes(memory)


    def newMemoryAgent(bus : PipelinedMemoryBus): Unit = {
      bus.cmd.ready #= true
      bus.rsp.valid #= false

      dut.clockDomain.onSamplings{
        bus.cmd.ready #= true
        bus.rsp.valid #= false
        bus.rsp.data.randomize()

        if(bus.cmd.valid.toBoolean){
          val address = bus.cmd.address.toInt
          assert((address.toInt & 0x3) == 0)
          if(bus.cmd.write.toBoolean){
            val data = bus.cmd.data.toLong
            val mask = bus.cmd.mask.toInt
            for(i <- 0 to 3){
              if((mask & (1 << i)) != 0){
                memory(address + i) = (data >> (i*8)).toByte
              }
            }
          } else {
            bus.rsp.valid #= true
            var buffer = 0l
            for(i <- 0 to 3){
              buffer |= (memory(address + i).toLong & 0xFF) << (i*8)
            }
            bus.rsp.data #= buffer
          }
        }
      }
    }

    newMemoryAgent(dut.io.memRead)
    newMemoryAgent(dut.io.memWrite)


    dut.clockDomain.forkStimulus(10)
    dut.clockDomain.waitSampling(10)

    writeXConfig(sourceOffset(0))(
      increment = true,
      reload = false,
      memory = true,
      size = 2,
      burst = 8,
      address = 0x100,
      length = 0x40
    )
    writeXConfig(destinationOffset(0))(
      increment = true,
      reload = false,
      memory = true,
      size = 2,
      burst = 8,
      address = 0x200,
      length = 0x40
    )
    writeXConfig(sourceOffset(1))(
      increment = true,
      reload = false,
      memory = true,
      size = 2,
      burst = 5,
      address = 0x300,
      length = 0x20
    )
    writeXConfig(destinationOffset(1))(
      increment = true,
      reload = false,
      memory = true,
      size = 2,
      burst = 5,
      address = 0x400,
      length = 0x20
    )
    doStart(sourceOffset(1))
    doStart(destinationOffset(1))
    doStart(sourceOffset(0))
    doStart(destinationOffset(0))
    dut.clockDomain.waitSampling(300)
  }
}
