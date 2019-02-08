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

  case class SDParameter(lengthWidth : Int,
                         burstWidth : Int,
                         memory : Boolean = true,
                         stream : Boolean = true){
    assert(memory || stream)
    val burstType = HardType(UInt(burstWidth bits))
  }
  case class ChannelParameter(fifoDepth : Int,
                              source : SDParameter,
                              destination : SDParameter){
    assert(isPow2(fifoDepth))
    val fifoPtrType = HardType(UInt(log2Up(fifoDepth)+1 bits))
    val fifoPtrTailType = HardType(UInt(log2Up(fifoDepth) bits))
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

    class SourceDestinationBase(cp : ChannelParameter, sdp : SDParameter, addressWidth : Int) extends Area {
      val busy, stop = RegInit(False)
      val increment, reload, memory = Reg(Bool())
      val start, dontStop = False

      val address = Reg(UInt(addressWidth bits))
      val burst = Reg(sdp.burstType())
      val size = Reg(p.sizeType())

      val length, counter = Reg(UInt(sdp.lengthWidth bits))
      val fifoPtr = Reg(cp.fifoPtrType()) init(0)
      val counterIncrement, fifoPtrIncrement = False
      val counterMatch = counter === length

      busy clearWhen(counterMatch || (stop && !dontStop)) setWhen(start)
      fifoPtr := fifoPtr + U(fifoPtrIncrement)
      counter := counter + U(counterIncrement)
    }

    case class Channel(val cp : ChannelParameter) extends Area {
      val reset = False

      val source = new SourceDestinationBase(cp, cp.source, if(cp.source.memory) p.memConfig.addressWidth else log2Up(p.inputs.length))
      val destination = new SourceDestinationBase(cp, cp.destination, if(cp.destination.memory) p.memConfig.addressWidth else log2Up(p.outputs.length))

      val sourceFifoPtr = U(channelToFifoOffset(cp), log2Up(fifoRam.wordCount) bits) | source.fifoPtr(widthOf(source.fifoPtr)-2 downto 0).resized
      val destinationFifoPtr = U(channelToFifoOffset(cp), log2Up(fifoRam.wordCount) bits) | destination.fifoPtr(widthOf(destination.fifoPtr)-2 downto 0).resized

      val fifoFullOrEmpty = source.fifoPtr(log2Up(cp.fifoDepth)-1 downto 0) === destination.fifoPtr(log2Up(cp.fifoDepth)-1 downto 0)
      val fifoFull = fifoFullOrEmpty && source.fifoPtr.msb =/= destination.fifoPtr.msb
      val fifoEmpty = fifoFullOrEmpty && source.fifoPtr.msb === destination.fifoPtr.msb
    }

    val channelsByFifoSize = p.channels.sortBy(_.fifoDepth).reverse
    val channelToFifoOffset = (channelsByFifoSize, channelsByFifoSize.scanLeft(0)(_ + _.fifoDepth)).zipped.toMap

    val channels = p.channels.map(Channel(_))

    val memReadCmd = new Area {
      val memoryChannels = channels.filter(_.cp.source.memory)
      val beatType = HardType(UInt(memoryChannels.map(_.cp.source.burstWidth).max bits))
      val proposal = new Area {
        val valid = Vec(memoryChannels.map(c =>
          c.source.busy && !c.source.stop && c.source.memory && !c.source.counterMatch && !(c.source.fifoPtr - c.destination.fifoPtr + c.source.burst).msb
        ))
        val oneHot = OHMasking.first(valid)
      }
      val busy = RegInit(False)
      val cmdBeat, rspBeat = Reg(beatType)
      val selected = new Area{
        val index = Reg(UInt(log2Up(memoryChannels.length) bits))
        def channel[T <: Data](f : Channel => T) = Vec(memoryChannels.map(f))(index)
        val burst = channel(_.source.burst)
        val address = channel(_.source.address)
        val size = channel(_.source.size)
        val counter = channel(_.source.counter)
        val counterMatch = channel(_.source.counterMatch)
        val counterIncrement = channel(_.source.counterIncrement)
        val dontStop = channel(_.source.dontStop)
        val sourceFifoPtr = channel(_.sourceFifoPtr)
        val fifoPtrIncrement = channel(_.source.fifoPtrIncrement)
      }

      val cmdDone = cmdBeat === selected.burst || selected.counterMatch
      val rspDone = rspBeat === rspBeat && cmdDone
      when(!busy){
        busy := proposal.valid.orR
        selected.index := OHToUInt(proposal.oneHot)
        cmdBeat := 0
        rspBeat := 0
      } otherwise {
        busy := !rspDone
        selected.dontStop := True
      }

      io.memRead.cmd.valid := busy && !cmdDone
      io.memRead.cmd.address := selected.address + (selected.counter |<< selected.size)
      io.memRead.cmd.write := False
      io.memRead.cmd.data.assignDontCare()
      io.memRead.cmd.mask.assignDontCare()

      when(io.memRead.cmd.fire){
        selected.counterIncrement := True
        cmdBeat := cmdBeat + 1
      }

      when(io.memRead.rsp.fire){
        rspBeat := rspBeat + 1
      }
    }

    val fifoWrite = new Area{
      val memoryChannels = channels.filter(_.cp.source.memory)
      val streamChannels = if(p.inputs.nonEmpty) channels.filter(_.cp.source.stream) else Nil

      val valid = False
      val address = fifoRam.addressType().assignDontCare()
      val data = fifoRam.wordType().assignDontCare()
      fifoRam.write(address, data, valid)

      val stream = streamChannels.nonEmpty generate new Area{
        val inputsValid = streamChannels.map(c => c.source.busy && !c.source.stop && !c.source.counterMatch && !c.source.memory && !c.fifoFull && io.inputs(c.source.address.resized).valid)
        val index = OHToUInt(OHMasking.first(inputsValid))
        def channel[T <: Data](f : Channel => T) = Vec(streamChannels.map(f))(index)
        val sourceFifoPtr = channel(_.sourceFifoPtr)
        val fifoPtrIncrement = channel(_.source.fifoPtrIncrement)
      }

      io.inputs.foreach(_.ready := False)
      when(io.memRead.rsp.valid){
        valid := True
        address := memReadCmd.selected.sourceFifoPtr
        data := io.memRead.rsp.data  //TODO normalisation
        memReadCmd.selected.fifoPtrIncrement := True
      } otherwise {
        streamChannels.nonEmpty generate when(stream.inputsValid.orR) {
          valid := True
          address := stream.sourceFifoPtr
          data := io.inputs(stream.index).data
          io.inputs(stream.index).ready := True
          stream.fifoPtrIncrement := True
        }
      }
    }

    val fifoRead = new Area {
      val memoryChannels = channels.filter(_.cp.destination.memory)
      val beatType = HardType(UInt(memoryChannels.map(_.cp.source.burstWidth).max bits))
      
      val proposal = new Area {
        val valid = Vec(channels.map(c =>
          c.destination.busy && !c.source.stop && c.destination.memory && !c.destination.counterMatch && !c.fifoEmpty
        ))
        val oneHot = OHMasking.first(valid)
      }
      val busy = RegInit(False)
      val fifoBeat, memBeat = Reg(beatType)
      val selected = new Area{
        val index = Reg(UInt(log2Up(memoryChannels.length) bits))
        def channel[T <: Data](f : Channel => T) = Vec(memoryChannels.map(f))(index)
        val burst = channel(_.destination.burst)
        val address = channel(_.destination.address)
        val size = channel(_.destination.size)
        val counter = channel(_.destination.counter)
        val counterIncrement = channel(_.destination.counterIncrement)
        val counterMatch = channel(_.destination.counterMatch)
        val dontStop = channel(_.destination.dontStop)
        val destinationFifoPtr = channel(_.destinationFifoPtr)
        val fifoPtrIncrement = channel(_.destination.fifoPtrIncrement)
        val fifoEmpty = channel(_.fifoEmpty)
      }

      val cmdDone = fifoBeat === selected.burst || selected.counterMatch || selected.fifoEmpty
      val rspDone = memBeat === fifoBeat && cmdDone

      when(!busy){
        busy := proposal.valid.orR
        selected.index := OHToUInt(proposal.oneHot)
        fifoBeat := 0
        memBeat := 0
      } otherwise {
        busy := !rspDone
        selected.dontStop := True
      }

      val fifoReadCmd = Stream(fifoRam.addressType)
      fifoReadCmd.valid := busy && !rspDone
      fifoReadCmd.payload := selected.destinationFifoPtr
      selected.fifoPtrIncrement setWhen(fifoReadCmd.fire)

      val read = fifoRam.streamReadSync(fifoReadCmd)

      val bufferIn = Stream(Bits(p.memConfig.dataWidth bits))
      bufferIn.arbitrationFrom(read)
      bufferIn.payload := read.payload

      val bufferOut = bufferIn.s2mPipe()
      io.memWrite.cmd.valid := busy && bufferOut.valid
      io.memWrite.cmd.address := selected.address + (selected.counter |<< selected.size)
      io.memWrite.cmd.write := True
      io.memWrite.cmd.data := bufferOut.payload
      io.memWrite.cmd.mask := "1111" //TODO
      bufferOut.ready := io.memWrite.cmd.ready

      when(io.memWrite.cmd.fire){
        selected.counterIncrement := True
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
    channels = List.fill(3)(
      Dma.ChannelParameter(
        fifoDepth = 32,
        source = Dma.SDParameter(
          lengthWidth = 12,
          burstWidth = 4,
          memory = true,
          stream = true
        ),
        destination = Dma.SDParameter(
          lengthWidth = 12,
          burstWidth = 4,
          memory = true,
          stream = true
        )
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
    inputs = List(
      Dma.InputParameter(dataWidth = 32),
      Dma.InputParameter(dataWidth = 32)
    ),
    outputs = Nil,
    channels = List(
      Dma.ChannelParameter(
        fifoDepth = 32,
        source = Dma.SDParameter(
          lengthWidth = 12,
          burstWidth = 4,
          memory = true,
          stream = true
        ),
        destination = Dma.SDParameter(
          lengthWidth = 12,
          burstWidth = 4,
          memory = true,
          stream = true
        )
      ),
      Dma.ChannelParameter(
        fifoDepth = 16,
        source = Dma.SDParameter(
          lengthWidth = 14,
          burstWidth = 4,
          memory = true,
          stream = true
        ),
        destination = Dma.SDParameter(
          lengthWidth = 10,
          burstWidth = 4,
          memory = true,
          stream = true
        )
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
