package saxon.dma

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb.sim.Apb3Driver
import spinal.lib.bus.amba3.apb.{Apb3, Apb3SlaveFactory}
import spinal.lib.bus.simple.{PipelinedMemoryBus, PipelinedMemoryBusCmd, PipelinedMemoryBusConfig}
import spinal.lib.eda.bench.{Bench, Rtl}
import spinal.lib.eda.icestorm.IcestormStdTargets
import spinal.lib.sim.{FlowMonitor, ScoreboardInOrder, SimData, StreamMonitor}

import scala.collection.mutable
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
    val sizeCount = log2Up(memConfig.dataWidth/8) + 1
    val sizeWidth = log2Up(sizeCount)
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
    assert(1 << source.burstWidth <= fifoDepth)
    assert(1 << destination.burstWidth <= fifoDepth)
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
    val outputs = Vec(p.outputs.map(op => master(Stream(OutputPayload(op)))))
  }

  case class Dma(val p : Parameter) extends Component{
    val io = Io(p)

    val fifoDepth = p.channels.map(_.fifoDepth).sum
    val fifoRam = Mem(Bits(p.memConfig.dataWidth bits), fifoDepth)
    def wordRange = log2Up(p.memConfig.dataWidth/8)-1 downto 0

    class SourceDestinationBase(cp : ChannelParameter, sdp : SDParameter, addressWidth : Int) extends Area {
      val busy, stop = RegInit(False)
      val increment, reload, memory = Reg(Bool())
      val start, dontStop = False

      val address = Reg(UInt(addressWidth bits))
      val burst = Reg(sdp.burstType())
      val size = Reg(p.sizeType())

      val length, counter = Reg(UInt(sdp.lengthWidth bits))
      val alignement = Reg(UInt(log2Up(p.memConfig.dataWidth/8) bits))
      val fifoPtr = Reg(cp.fifoPtrType()) init(0)
      val counterIncrement, fifoPtrIncrement, alignementIncrement = False
      val counterMatch = counter === length

      //TODO can't stop a reload
      val done = busy && !dontStop && stop
      stop setWhen(counterIncrement && counterMatch) clearWhen(done)
      start setWhen(done && reload)
      busy clearWhen(done) setWhen(start)
      fifoPtr := fifoPtr + U(fifoPtrIncrement)
      if(sdp.lengthWidth != 0) counter := counter + U(counterIncrement)
      alignement := alignement + (alignementIncrement ? (0 until p.sizeCount).map(v => U(1 << v)).read(size) | U(0)).resized
      when(start){
        counter := 0
        alignement := address.resized
      }
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
          c.source.busy && !c.source.stop && c.source.memory && !(c.source.fifoPtr - c.destination.fifoPtr + c.source.burst).msb
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
        val alignement = channel(_.source.alignement)
        val alignementIncrement = channel(_.source.alignementIncrement)
        val counterMatch = channel(_.source.counterMatch)
        val counterIncrement = channel(_.source.counterIncrement)
        val dontStop = channel(_.source.dontStop)
        val sourceFifoPtr = channel(_.sourceFifoPtr)
        val fifoPtrIncrement = channel(_.source.fifoPtrIncrement)
      }


      val cmdDone = Reg(Bool)
      val rspDone = rspBeat === cmdBeat && cmdDone
      when(!busy){
        busy := proposal.valid.orR
        selected.index := OHToUInt(proposal.oneHot)
        cmdBeat := 0
        rspBeat := 0
        cmdDone := False
      } otherwise {
        busy := !rspDone
        selected.dontStop := True
        cmdDone setWhen(io.memRead.cmd.ready && (selected.counterMatch || cmdBeat === selected.burst))
      }

      io.memRead.cmd.valid := busy && !cmdDone
      io.memRead.cmd.address := selected.address + (selected.counter << selected.size)
      io.memRead.cmd.address(wordRange) := 0
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
        val inputsValid = streamChannels.map(c => c.source.busy && !c.source.stop && !c.source.memory && !c.fifoFull && io.inputs(c.source.address.resized).valid)
        val index = OHToUInt(OHMasking.first(inputsValid))
        def channel[T <: Data](f : Channel => T) = Vec(streamChannels.map(f))(index)
        val sourceFifoPtr = channel(_.sourceFifoPtr)
        val fifoPtrIncrement = channel(_.source.fifoPtrIncrement)
        val counterIncrement = channel(_.source.counterIncrement)
        val address = channel(_.source.address) //TODO optimise mux
      }

      val memReadRspShifted = Bits(p.memConfig.dataWidth bits).assignDontCare()
      switch(memReadCmd.selected.alignement){
        for(i <- 0 to memReadCmd.selected.alignement.maxValue.toInt){
          is(i) {
            val byteCount = (0 until p.sizeCount).map(1 << _).filter(i % _ == 0).max
            memReadRspShifted(8 * byteCount - 1 downto 0) := io.memRead.rsp.data(i * 8, byteCount * 8 bits)
          }
        }
      }

      io.inputs.foreach(_.ready := False)
      when(io.memRead.rsp.valid){
        valid := True
        address := memReadCmd.selected.sourceFifoPtr
        data := memReadRspShifted
        memReadCmd.selected.fifoPtrIncrement := True
        memReadCmd.selected.alignementIncrement := True
      } otherwise {
        if(p.inputs.nonEmpty) {
          address := stream.sourceFifoPtr
          data := io.inputs.map(_.data.resized).read(stream.address.resized)
          streamChannels.nonEmpty generate when(stream.inputsValid.orR) {
            valid := True
            io.inputs(stream.address.resized).ready := True
            stream.fifoPtrIncrement := True
            stream.counterIncrement := True
          }
        }
      }
    }

    val outputs = for(output <- io.outputs) yield new Area{
      val bufferIn = Flow(output.payloadType)
      val bufferStage = bufferIn.toStream.stage
      val bufferOut = bufferStage.stage
      output << bufferOut

      val fillMe = !bufferStage.valid && ! bufferIn.valid
    }

    val fifoRead = new Area {
      val memoryChannels = channels.filter(_.cp.destination.memory)
      val streamChannels = if(p.outputs.nonEmpty) channels.filter(_.cp.destination.stream) else Nil
      val beatType = HardType(UInt(memoryChannels.map(_.cp.destination.burstWidth).max bits))

      val cmd = new Area{
        val proposal = Vec(channels.map(c =>
          c.destination.busy && !c.destination.stop && (c.destination.memory || (if(outputs.nonEmpty) outputs.map(_.fillMe).read(c.destination.address.resized) else False)) && !c.fifoEmpty
        ))
        val proposalValid = proposal.orR
        val oneHot = OHMasking.first(proposal)
        val index = OHToUInt(oneHot)
        val lock = RegInit(False)
        val unlock = False
        val indexLock = RegNextWhen(index, !lock)
        when(lock) { index := indexLock }
        val valid = proposalValid || lock
        def channel[T <: Data](f : Channel => T) = Vec(memoryChannels.map(f))(index)
        val memory = channel(_.destination.memory)
        val burst = channel(_.destination.burst)
        val counter = channel(_.destination.counter)
        val counterMatch = channel(_.destination.counterMatch)
        val counterIncrement = channel(_.destination.counterIncrement)
        val fifoEmpty = channel(_.fifoEmpty)
        val destinationFifoPtr = channel(_.destinationFifoPtr)
        val fifoPtrIncrement = channel(_.destination.fifoPtrIncrement)
        val dontStop = channel(_.destination.dontStop)


        dontStop.setWhen(valid)

        val beat = Reg(beatType) init(0)
        unlock setWhen(fifoEmpty)

        val fifoReadCmd = Stream(fifoRam.addressType)
        fifoReadCmd.valid := valid && !fifoEmpty
        fifoReadCmd.payload := destinationFifoPtr
        fifoPtrIncrement setWhen(fifoReadCmd.fire)


        when(valid && fifoReadCmd.fire){
          counterIncrement := True
          if(widthOf(beat) != 0) beat := beat + U(memory)
          when(counterMatch || beat === burst){
            unlock := True
          }
        }

        when(proposalValid && memory){
          lock := True
        }

        when(lock && unlock){
          lock := False
          beat := 0
        }
      }

      val rsp = new Area{
        val input = fifoRam.streamReadSync(cmd.fifoReadCmd)
        val index = RegNextWhen(cmd.index, cmd.fifoReadCmd.ready)
        val counter = RegNextWhen(cmd.counter, cmd.fifoReadCmd.ready)
        def channel[T <: Data](f : Channel => T) = Vec(memoryChannels.map(f))(index)
        val memory = channel(_.destination.memory)
        val address = channel(_.destination.address) //TODO redduce mux usage for channels without memory capabilities
        val size = channel(_.destination.size)
        val dontStop = channel(_.destination.dontStop)
        val alignement = channel(_.destination.alignement)
        val alignementIncrement = channel(_.destination.alignementIncrement)
        dontStop.setWhen(input.valid)



        io.memWrite.cmd.valid := input.valid && memory
        io.memWrite.cmd.address := address + (counter << size)
        io.memWrite.cmd.address(wordRange) := 0
        io.memWrite.cmd.write := True
        io.memWrite.cmd.data.assignDontCare()
        io.memWrite.cmd.mask := (0 until p.sizeCount).map(v => B((1 << (1 << v))-1)).read(size) |<< alignement
        alignementIncrement setWhen(input.fire)
        switch(alignement){
          for(i <- 0 to alignement.maxValue.toInt){
            is(i) {
              val byteCount = (0 until p.sizeCount).map(1 << _).filter(i % _ == 0).max
              io.memWrite.cmd.data(i * 8, byteCount * 8 bits) := input.payload(8 * byteCount - 1 downto 0)
            }
          }
        }

        for((output, id) <- outputs.zipWithIndex){
          output.bufferIn.valid := input.valid && !memory && address(log2Up(outputs.size) - 1 downto 0) === id
          output.bufferIn.data := input.payload.resized
        }

        input.ready := memory ? io.memWrite.cmd.ready | True


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
    channels = List.fill(1)(
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
    outputs = List(
      Dma.OutputParameter(dataWidth = 32),
      Dma.OutputParameter(dataWidth = 32)
    ),
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
      burst = 8-1,
      address = 0x100,
      length = 0x40-1
    )
    writeXConfig(destinationOffset(0))(
      increment = true,
      reload = false,
      memory = true,
      size = 2,
      burst = 8-1,
      address = 0x200,
      length = 0x40-1
    )
    writeXConfig(sourceOffset(1))(
      increment = true,
      reload = false,
      memory = true,
      size = 2,
      burst = 5-1,
      address = 0x300,
      length = 0x20-1
    )
    writeXConfig(destinationOffset(1))(
      increment = true,
      reload = false,
      memory = true,
      size = 2,
      burst = 5-1,
      address = 0x400,
      length = 0x20-1
    )
    doStart(sourceOffset(1))
    doStart(destinationOffset(1))
    doStart(sourceOffset(0))
    doStart(destinationOffset(0))
    dut.clockDomain.waitSampling(300)
  }
}



object DmaTester extends App {

  import spinal.core.sim._
  Random.setSeed(42)
  val p = Dma.Parameter(
    memConfig = PipelinedMemoryBusConfig(30, 32),
    inputs = List.fill(10)(Dma.InputParameter(dataWidth = 8*(1 << Random.nextInt(3)))),
    outputs = List.fill(10)(Dma.OutputParameter(dataWidth = 8*(1 << Random.nextInt(3)))),
    channels = Dma.ChannelParameter(
      fifoDepth = 32,
      source = Dma.SDParameter(
        lengthWidth = 7,
        burstWidth = 4,
        memory = true,
        stream = true
      ),
      destination = Dma.SDParameter(
        lengthWidth = 7,
        burstWidth = 4,
        memory = true,
        stream = true
      )
    ) :: List.tabulate(9){i =>
      val fifoDepth = Math.max(1, 1 << (Random.nextInt(3) + Random.nextInt(3)))
      Dma.ChannelParameter(
        fifoDepth = fifoDepth,
        source = Dma.SDParameter(
          lengthWidth = i,
          burstWidth = Math.min(log2Up(fifoDepth), Random.nextInt(3) + Random.nextInt(2)),
          memory = true,
          stream = true
        ),
        destination = Dma.SDParameter(
          lengthWidth = i,
          burstWidth = Math.min(log2Up(fifoDepth), Random.nextInt(3) + Random.nextInt(2)),
          memory = true,
          stream = true
        )
      )
    }.sortBy(_.hashCode())
  )


  SimConfig.withWave.compile(new Dma.Dma(p)).doSim("test", 42) { dut =>



    val memory = new Array[Byte](0x10000)
    Random.nextBytes(memory)

    def newMemoryAgent(bus : PipelinedMemoryBus): Unit = {
      bus.cmd.ready #= true
      bus.rsp.valid #= false

      var pendingRsp = 0
      dut.clockDomain.onSamplings{
        bus.cmd.ready #= true
        bus.rsp.valid #= false
        bus.rsp.data.randomize()

        if(bus.cmd.valid.toBoolean){
          val address = bus.cmd.address.toInt
          assert((address.toInt & 0x3) == 0)
          if(!bus.cmd.write.toBoolean){
            pendingRsp += 1
          }
        }

        if(pendingRsp != 0 && Random.nextBoolean()){
          bus.rsp.valid #= true
          pendingRsp -= 1
        }
      }
    }

    newMemoryAgent(dut.io.memRead)
    newMemoryAgent(dut.io.memWrite)


    val inputsAgents = for((p,i) <- dut.p.inputs.zipWithIndex; input = dut.io.inputs(i)) yield new {
      val transactions = mutable.Stack[BigInt]()
      dut.clockDomain.onSamplings{
        if(input.valid.toBoolean && input.ready.toBoolean){
          transactions.push(input.data.toBigInt)
        }
        input.valid #= Random.nextFloat() < 0.25
        input.data.randomize()
      }
    }

    val outputAgents = for((p,i) <- dut.p.outputs.zipWithIndex; output = dut.io.outputs(i)) yield new {
      val transactions = mutable.Stack[BigInt]()
      dut.clockDomain.onSamplings{
        if(output.valid.toBoolean && output.ready.toBoolean){
          transactions.push(output.data.toBigInt)
        }
        output.ready #= Random.nextFloat() < 0.25
      }
    }

    class MemCmdScoreboard extends ScoreboardInOrder[SimData](){
      override def compare(ref: SimData, dut: SimData): Boolean = {
        if(ref.address != dut.address) return false
        if(ref("write") != dut("write")) return false
        ref("write") match {
          case 1 =>
            val mask = ref.mask.asInstanceOf[BigInt]
            val dataMask = (0 until mask.bitLength).filter(mask.testBit(_)).map(BigInt(0xFF) << _*8).sum
            if((ref.data.asInstanceOf[BigInt] & dataMask) != (dut.data.asInstanceOf[BigInt] & dataMask) || ref.mask != dut.mask) return false
          case _ =>
        }
        return true
      }
    }
    val channelAddressRange = 0x01000000
    def addressToChannelId(address : Int) = address / channelAddressRange

    val channels = for(i <- 0 until p.channels.length) yield new {
      val memWriteCmdScoreboard, memReadCmdScoreboard = new MemCmdScoreboard

      val readCallbacks =  mutable.Queue[BigInt => Unit]()
    }

    val inputs = for(i <- 0 until p.inputs.length) yield new {
      val callbacks =  mutable.Queue[BigInt => Unit]()
      val monitor = StreamMonitor(dut.io.inputs(i), dut.clockDomain)(payload => callbacks.dequeue()(payload.data.toBigInt))
    }
    val outputs = for(i <- 0 until p.outputs.length) yield new {
      val scoreboard = new ScoreboardInOrder[SimData]{
        override def compare(ref: SimData, dut: SimData): Boolean = {
          val mask = ref.mask.asInstanceOf[BigInt]
          (ref.data.asInstanceOf[BigInt] & mask) == (dut.data.asInstanceOf[BigInt] & mask)
        }
      }
      val monitor = StreamMonitor(dut.io.outputs(i), dut.clockDomain)(scoreboard.pushDut(_))
    }

    val memReadRspCallback =  mutable.Queue[BigInt => Unit]()

    val meReadCmdMonitor = StreamMonitor(dut.io.memRead.cmd, dut.clockDomain){ payload =>
      val cmd = SimData.copy(payload)
      val channelId = addressToChannelId(payload.address.toInt)
      channels(channelId).memReadCmdScoreboard.pushDut(cmd)

      memReadRspCallback += (channels(channelId).readCallbacks.dequeue())
    }

    val memWriteCmdMonitor = StreamMonitor(dut.io.memWrite.cmd, dut.clockDomain){ payload =>
      val cmd = SimData.copy(payload)
      val channelId = addressToChannelId(payload.address.toInt)
      channels(channelId).memWriteCmdScoreboard.pushDut(cmd)
    }

    FlowMonitor(dut.io.memRead.rsp, dut.clockDomain) { payload =>
      memReadRspCallback.dequeue()(payload.data.toBigInt)
    }



    val config = Apb3Driver(dut.io.config, dut.clockDomain)
    dut.clockDomain.forkStimulus(10)
    dut.clockDomain.waitSampling(10)
    dut.clockDomain.forkSimSpeedPrinter()
    SimTimeout(10*1000000)


    var configTarget, configHit = 0
    def configLock(): Unit ={
      val target = configTarget
      configTarget += 1
      waitUntil(configHit == target)
    }
    def configUnlock(): Unit ={
      configHit += 1
    }
    var taskCount = 0
    //TODO
    val channelsFree = mutable.ArrayBuffer[Int]() ++ (0 until p.channels.length)
    val inputsFree = mutable.ArrayBuffer[Int]()   ++ (0 until p.inputs.length)
    val outputsFree = mutable.ArrayBuffer[Int]()  ++ (0 until p.outputs.length)
    def alloc(array : mutable.ArrayBuffer[Int]) : Int = {
      val i = Random.nextInt(array.size)
      val value = array(i)
      array.remove(i)
      value
    }
    case class SdData(){
      var increment = true
      var reload = false
      var memory = true
      var size = 4
      var burst = 0
      var address = 0
      var length = 0
      def write(offset : Int, apb3Driver: Apb3Driver): Unit ={
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
    }

    def sourceOffset(channelId : Int) = p.mapping.channelOffset + channelId * p.mapping.channelDelta + p.mapping.sourceOffset
    def destinationOffset(channelId : Int) = p.mapping.channelOffset + channelId * p.mapping.channelDelta + p.mapping.destinationOffset
    def doStart(offset : Int): Unit = {
      config.write(offset + p.mapping.channelCtrlOffset, 1 << p.mapping.channelStartBit)
    }
    def configLocked[T](body : => T): T ={
      configLock()
      val ret = body
      configUnlock()
      ret
    }

    def createTask() = {
      val channelId = alloc(channelsFree)
      val cp = p.channels(channelId)
      val src = new SdData()
      val sourceAddress = sourceOffset(channelId)
      val destinationAddress = destinationOffset(channelId)
      val length =  Random.nextInt(1 << Math.min(cp.source.lengthWidth,cp.destination.lengthWidth))
      val size = Random.nextInt(p.sizeCount)

      src.increment = true
      src.reload = false
      src.memory = (Random.nextBoolean() && cp.source.memory) || !cp.source.stream
      val inputId = !src.memory generate alloc(inputsFree)
      src.size = size
      src.burst = Math.min(cp.fifoDepth-1, Random.nextInt(1 << cp.source.burstWidth))
      src.address = if(src.memory) (channelId * channelAddressRange + Random.nextInt(channelAddressRange/2)) & ~ 0x3 else inputId
      src.length = length


      val dst = new SdData()
      dst.increment = true
      dst.reload = false
      dst.memory = (Random.nextBoolean() && cp.destination.memory) || !cp.destination.stream
      val outputId = !dst.memory generate alloc(outputsFree)
      dst.size = size
      dst.burst = Math.min(cp.fifoDepth-1,  Random.nextInt(1 << cp.destination.burstWidth))
      dst.address = if(dst.memory) (channelId * channelAddressRange + Random.nextInt(channelAddressRange/2)) & ~ 0x3 else outputId
      dst.length = length

      val addressMask = ~(p.memConfig.dataWidth/8-1)



      for(beat <- 0 to src.length) {
        if(src.memory) {
          val address = src.address + (beat << src.size)
          val cmd = SimData()
          cmd("write") = 0
          cmd.address = address & addressMask
          channels(channelId).memReadCmdScoreboard.pushRef(cmd)
        } else {
          inputs(inputId).callbacks += (data => channels(channelId).readCallbacks.dequeue()(data))
        }
      }

      for(beat <- 0 to dst.length){
        channels(channelId).readCallbacks += { data =>
          val dataSrcShift = if(src.memory) data >> ((src.address + (beat << src.size)) & ~addressMask)*8 else data
          if(dst.memory) {
            val address =  dst.address + (beat << dst.size)
            val cmd = SimData()
            cmd("write") = 1
            cmd.address = address & addressMask
            cmd.data = dataSrcShift << ((address & ~addressMask)*8)
            cmd.mask = ((1 << (1 << dst.size))-1) << (address & ~addressMask)
            channels(channelId).memWriteCmdScoreboard.pushRef(cmd)
          } else {
            val readDataMask = if(src.memory) (BigInt(1) << (1 << src.size) * 8) - 1 else (BigInt(1) << p.inputs(inputId).dataWidth) - 1
            val writeDataMask = if(dst.memory) (BigInt(1) << (1 << dst.size) * 8) - 1 else (BigInt(1) << p.outputs(outputId).dataWidth) - 1

            val cmd = SimData()
            cmd.data = dataSrcShift
            cmd.mask = readDataMask & writeDataMask
            outputs(outputId).scoreboard.pushRef(cmd)
          }
        }
      }


      configLocked(src.write(sourceAddress, config))
      configLocked(dst.write(destinationAddress, config))
      configLocked(doStart(sourceAddress))
      configLocked(doStart(destinationAddress))

      while((configLocked(config.read(destinationAddress + p.mapping.channelCtrlOffset)) & (1 << p.mapping.channelBusyBit)) != 0){
        dut.clockDomain.waitSampling(Random.nextInt(10))
      }
      dut.clockDomain.waitSampling(Random.nextInt(100))
      channelsFree += channelId
      if(!src.memory) inputsFree += inputId
      if(!dst.memory) outputsFree += outputId
    }

    val agents = for(agent <- 0 until 5) yield fork {
      for (i <- 0 until 100) createTask()
    }

    agents.foreach(_.join())
//    createTask()

    dut.clockDomain.waitSampling(20000)

    for(c <- channels) {
      println(s"${c.memReadCmdScoreboard.matches} ${c.memWriteCmdScoreboard.matches}")
      c.memWriteCmdScoreboard.checkEmptyness()
      c.memReadCmdScoreboard.checkEmptyness()
      assert(c.readCallbacks.isEmpty)
    }


    for(o <- outputs){
      o.scoreboard.checkEmptyness()
    }

    for(i <- inputs){
      assert(i.callbacks.isEmpty)
    }
  }
}