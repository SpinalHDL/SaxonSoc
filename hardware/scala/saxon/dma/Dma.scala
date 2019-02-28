package saxon.dma

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb.sim.Apb3Driver
import spinal.lib.bus.amba3.apb.{Apb3, Apb3SlaveFactory}
import spinal.lib.bus.misc.SizeMapping
import spinal.lib.bus.simple.{PipelinedMemoryBus, PipelinedMemoryBusCmd, PipelinedMemoryBusConfig}
import spinal.lib.eda.bench.{Bench, Rtl}
import spinal.lib.eda.icestorm.IcestormStdTargets
import spinal.lib.sim.{FlowMonitor, ScoreboardInOrder, SimData, StreamMonitor}

import scala.collection.mutable
import scala.util.Random
object Dma{

  def apply(p : Parameter) = new Dma(p)

  case class MappingParameter(val sharedRamOffset : Int = 0x100,
                              val channelOffset : Int = 0x200,
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
                        memoryLengthWidth : Int,
                        singleMemoryPort : Boolean,
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
    val memRead = !p.singleMemoryPort generate master(PipelinedMemoryBus(p.memConfig))
    val memWrite = !p.singleMemoryPort generate master(PipelinedMemoryBus(p.memConfig))
    val mem = p.singleMemoryPort generate master(PipelinedMemoryBus(p.memConfig))
    val inputs = Vec(p.inputs.map(ip => slave(Stream(InputPayload(ip)))))
    val outputs = Vec(p.outputs.map(op => master(Stream(OutputPayload(op)))))
  }

  case class Dma(val p : Parameter) extends Component{
    val io = Io(p)

    def wordRange = log2Up(p.memConfig.dataWidth/8)-1 downto 0
    val sharedRam = new Area{
      val memoryContextWordCount = 1 << log2Up(p.channels.size*4)
      val memSize = 1 << log2Up(p.channels.map(_.fifoDepth).sum + memoryContextWordCount)
      val contextBase = memSize - memoryContextWordCount
      val contextRange = (log2Up(memoryContextWordCount)-1 downto 0)
      val nonContextRange = (log2Up(memSize)-1 downto contextRange.size)
      val mem = Mem(Bits(p.memConfig.dataWidth bits), memSize)
      def wordRange = log2Up(p.memConfig.dataWidth/8)-1 downto 0

      val readCmd = Vec(Stream(mem.addressType), 1 + (if(p.singleMemoryPort) 1 else 2))
      val readRsp = mem.streamReadSyncMultiPort(readCmd)

      val writeCmd = Vec(Stream(MemWriteCmd(mem)), 2 + (if(p.singleMemoryPort) 1 else 2))
      val writeCmdSelected = StreamArbiterFactory.noLock.lowerFirst.on(writeCmd)
      mem.writePort << writeCmdSelected.toFlow
    }


    class SourceDestinationBase(cp : ChannelParameter, val sdp : SDParameter, streamIdWidth : Int) extends Area {
      val busy, selfStop, userStop = RegInit(False)
      val increment, reload, memory = Reg(Bool()) //TODO harddrive memory if possible
      val start, dontStop = False

       //Stream capable
      val length, counter = sdp.stream generate Reg(UInt(sdp.lengthWidth bits))
      val streamId = sdp.stream generate Reg(UInt(streamIdWidth bits))
      val counterIncrement = sdp.stream generate False
      val counterMatch = sdp.stream generate counter === length

      //Memory capable
      val burst = sdp.memory generate Reg(sdp.burstType())
      val size = sdp.memory generate Reg(p.sizeType())
      val alignement = sdp.memory generate Reg(UInt(log2Up(p.memConfig.dataWidth/8) bits))
      val alignementIncrement = sdp.memory generate False

      val fifoPtr = Reg(cp.fifoPtrType()) init(0)
      val fifoPtrIncrement  = False

      val stop = userStop || selfStop
      val done = busy && !dontStop && (selfStop || userStop)
      val memoryStop = sdp.memory generate False
      if(sdp.memory) selfStop setWhen(memoryStop)
      if(sdp.stream) selfStop setWhen(counterIncrement && counterMatch)
      selfStop clearWhen(done)
      start setWhen(done && reload && !userStop)
      busy clearWhen(done) setWhen(start)
      fifoPtr := fifoPtr + U(fifoPtrIncrement)
      if(sdp.lengthWidth != 0 && sdp.stream) when(counterIncrement) {
        counter := counter + 1
      }
      if(sdp.memory) alignement := alignement + (alignementIncrement ? (0 until p.sizeCount).map(v => U(1 << v)).read(size) | U(0)).resized
      when(start){
        if(sdp.stream) counter := 0
      }
    }

    case class Channel(val cp : ChannelParameter) extends Area {
      val reset = False

      val source = new SourceDestinationBase(cp, cp.source, log2Up(p.inputs.length))
      val destination = new SourceDestinationBase(cp, cp.destination, log2Up(p.outputs.length))

      val sourceFifoPtr = U(channelToFifoOffset(cp), log2Up(sharedRam.mem.wordCount) bits) | source.fifoPtr(widthOf(source.fifoPtr)-2 downto 0).resized
      val destinationFifoPtr = U(channelToFifoOffset(cp), log2Up(sharedRam.mem.wordCount) bits) | destination.fifoPtr(widthOf(destination.fifoPtr)-2 downto 0).resized

      val fifoFullOrEmpty = source.fifoPtr(log2Up(cp.fifoDepth)-1 downto 0) === destination.fifoPtr(log2Up(cp.fifoDepth)-1 downto 0)
      val fifoFull = fifoFullOrEmpty && source.fifoPtr.msb =/= destination.fifoPtr.msb
      val fifoEmpty = fifoFullOrEmpty && source.fifoPtr.msb === destination.fifoPtr.msb
    }

//    def muxAddress(index : UInt, sdList : Seq[SourceDestinationBase], streamCount : Int) : UInt = {
//      val ret = UInt(p.memConfig.addressWidth bits).assignDontCare()
//      switch(index) {
//        for ((sd, id) <- sdList.zipWithIndex) {
//          is(id){
//            if(!sd.sdp.memory) {
//              ret(0, log2Up(streamCount) bits) := sd.address.resized
//            } else {
//              ret := sd.address
//            }
//          }
//        }
//      }
//      ret
//    }

    val channelsByFifoSize = p.channels.sortBy(_.fifoDepth).reverse
    val channelToFifoOffset = (channelsByFifoSize, channelsByFifoSize.scanLeft(0)(_ + _.fifoDepth)).zipped.toMap

    val channels = p.channels.map(Channel(_))

    case class MemoryContext() extends Area{
      val size = p.sizeType()
      val address = Reg(UInt(p.memConfig.addressWidth bits))
      val length, counter = Reg(UInt(p.memoryLengthWidth bits))
      val counterMatch = counter === length
      val counterIncrement = False
      val counterClear = False
      val addressPlusCounter = address + counter
      val addressPlusCounterReg = RegNextWhen(addressPlusCounter, counterIncrement)

      when(counterIncrement) {
        counter := counter + ((0 until p.sizeCount).map(s => U(1 << s)).read(size)).resized
      }
      when(counterClear){
        counter := 0
      }
    }
    case class MemoryContextCtrl(context : MemoryContext,
                                 portCount : Int,
                                 readCmd : Stream[UInt],
                                 readRsp : Stream[Bits],
                                 writeCmd : Stream[MemWriteCmd[Bits]]) extends Area{
      case class Port() extends Bundle{
        val valid = Bool()
        val channelId = UInt(log2Up(p.channels.length) bits)
        val size = p.sizeType()
        val fromDestination = Bool()
        val hit = Bool()
        val hitClear = Bool()
        val release = Bool()
      }
      val ports = Vec(Port(), portCount)

      val state = Reg(UInt(3 bits)) init(0)
      val hitEnable = Reg(Bool)
      val portId = UInt(log2Up(portCount) bits)
      val portIdLock = Reg(UInt(log2Up(portCount) bits))
      val channelId = Reg(UInt(log2Up(p.channels.length) bits))
      portId := portIdLock
      portIdLock := portId
      ports.foreach(_.hit := False)

      context.size := ports(portIdLock).size

      readCmd.valid := False
      readCmd.payload(sharedRam.nonContextRange).setAll()
      readCmd.payload(sharedRam.contextRange) := ports(portId).channelId @@ ports(portId).fromDestination @@ state.lsb
      readRsp.ready := True

      writeCmd.valid := False
      writeCmd.address(sharedRam.nonContextRange).setAll()
      writeCmd.address(sharedRam.contextRange) := channelId @@ ports(portId).fromDestination @@ True
      writeCmd.data.assignDontCare()

      switch(state){
        is(0){
          portId := OHToUInt(OHMasking.first(ports.map(_.valid)))
          hitEnable := True
          channelId := ports(portId).channelId
          when(ports.map(_.valid).orR){
            readCmd.valid := True
            when(readCmd.ready){
              state := 1
            }
          }
        }
        is(1){
          when(readRsp.valid){ context.address(context.address.range) := readRsp.payload.asUInt(context.address.range) }
          readCmd.valid := True
          when(readCmd.ready){
            state := 2
          }
        }
        is(2){
          when(readRsp.valid){
            context.length := readRsp.payload(context.length.range).asUInt
            context.counter := (readRsp.payload >> p.memConfig.dataWidth/2)(context.length.range).asUInt
          }
          state := 3
        }
        is(3){
          ports(portIdLock).hit := hitEnable
          hitEnable.clearWhen(ports(portIdLock).hitClear)
          when(ports(portIdLock).release){
            state := 4
          }
        }
        is(4){
          writeCmd.valid := True
          writeCmd.data(context.length.range) := context.length.asBits
          writeCmd.data(p.memConfig.dataWidth/2, context.counter.getWidth bits) := context.counter.asBits
          when(writeCmd.ready){
            state := 0
          }
        }
      }
    }

    val sharedMemoryContext = p.singleMemoryPort generate new Area{
      val context = MemoryContext()
      val contextCtrl = MemoryContextCtrl(context, 2, sharedRam.readCmd(1), sharedRam.readRsp(1), sharedRam.writeCmd(1))
    }


    val memHub = new Area{
      val read = PipelinedMemoryBus(p.memConfig)
      val write = Stream(PipelinedMemoryBusCmd(p.memConfig))
      if(p.singleMemoryPort){
        io.mem.cmd.valid := read.cmd.valid | write.valid
        io.mem.cmd.address := sharedMemoryContext.context.addressPlusCounterReg
        io.mem.cmd.address(wordRange) := 0
        io.mem.cmd.write := write.valid
        io.mem.cmd.data := write.data
        io.mem.cmd.mask := write.mask
        io.mem.rsp <> read.rsp
        io.mem.cmd.ready <> read.cmd.ready
        io.mem.cmd.ready <> write.ready
      } else {
        io.memRead <> read
        io.memWrite.cmd <> write
      }
    }


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
        val allIndex = Reg(UInt(log2Up(channels.length) bits))
        val memoryIndex = Reg(UInt(log2Up(memoryChannels.length) bits))
        def memoryChannel[T <: Data](f : Channel => T) = Vec(memoryChannels.map(f))(memoryIndex)
        val burst = memoryChannel(_.source.burst)
        val size = memoryChannel(_.source.size)
        val alignement = memoryChannel(_.source.alignement)
        val alignementIncrement = memoryChannel(_.source.alignementIncrement)
        val dontStop = memoryChannel(_.source.dontStop)
        val sourceFifoPtr = memoryChannel(_.sourceFifoPtr)
        val fifoPtrIncrement = memoryChannel(_.source.fifoPtrIncrement)
        val memoryStop = memoryChannel(_.source.memoryStop)
        val selfStop = memoryChannel(_.source.selfStop)
        val context = if(p.singleMemoryPort) sharedMemoryContext.context else MemoryContext()
        val contextCtrl = !p.singleMemoryPort generate MemoryContextCtrl(context, 1, sharedRam.readCmd(1), sharedRam.readRsp(1), sharedRam.writeCmd(1))
        val contextPort = if(p.singleMemoryPort) sharedMemoryContext.contextCtrl.ports(0) else contextCtrl.ports(0)
      }


      val cmdDone = Reg(Bool)
      val rspDone = rspBeat === cmdBeat && cmdDone
      selected.contextPort.valid := False
      selected.contextPort.channelId := selected.allIndex
      selected.contextPort.fromDestination := False
      selected.contextPort.release := False
      selected.contextPort.hitClear := False
      selected.contextPort.size := selected.size




      val memReadCmdEvent = Event
      memReadCmdEvent.valid := busy && !cmdDone && selected.contextPort.hit
      val memReadCmdEventStage = memReadCmdEvent.stage()
      memHub.read.cmd.valid := memReadCmdEventStage.valid
      memReadCmdEventStage.ready := memHub.read.cmd.ready
      memHub.read.cmd.address := selected.context.addressPlusCounterReg
      memHub.read.cmd.address(wordRange) := 0
      memHub.read.cmd.write := False
      memHub.read.cmd.data.assignDontCare()
      memHub.read.cmd.mask.assignDontCare()


      when(!busy){
        when(proposal.valid.orR) {
          busy := True
        }
        selected.memoryIndex := OHToUInt(proposal.oneHot)
        var proposalIterator = proposal.oneHot.iterator
        selected.allIndex := OHToUInt((0 until channels.length).map(i => if(p.channels(i).source.memory) proposalIterator.next() else False))
        cmdBeat := 0
        rspBeat := 0
        cmdDone := False
      } otherwise {
        selected.contextPort.valid := True
        selected.dontStop := True

        cmdDone setWhen(selected.contextPort.hit && memReadCmdEvent.ready && (selected.context.counterMatch || cmdBeat === selected.burst))
        selected.memoryStop setWhen(selected.contextPort.hit && memReadCmdEvent.fire && selected.context.counterMatch)
        when(rspDone) {
          busy := False
          selected.contextPort.release := True
          when(selected.selfStop){
            selected.context.counterClear := True
            selected.alignement := selected.context.address.resized
          }
        }
      }

      when(memReadCmdEvent.fire){
        selected.context.counterIncrement := True
        cmdBeat := cmdBeat + 1
      }

      when(memHub.read.rsp.fire){
        rspBeat := rspBeat + 1
      }
    }

    val fifoWrite = new Area{
      val streamChannels = if(p.inputs.nonEmpty) channels.filter(_.cp.source.stream) else Nil

      def sharedRamWrite = sharedRam.writeCmd(0)
      sharedRamWrite.valid := False
      sharedRamWrite.address.assignDontCare()
      sharedRamWrite.data.assignDontCare()

      val stream = streamChannels.nonEmpty generate new Area{
        val inputsValid = streamChannels.map(c => c.source.busy && !c.source.stop && !c.source.memory && !c.fifoFull && io.inputs(c.source.streamId).valid)
        val index = OHToUInt(OHMasking.first(inputsValid))
        def channel[T <: Data](f : Channel => T) = Vec(streamChannels.map(f))(index)
        val sourceFifoPtr = channel(_.sourceFifoPtr)
        val fifoPtrIncrement = channel(_.source.fifoPtrIncrement)
        val counterIncrement = channel(_.source.counterIncrement)
        val streamId = channel(_.source.streamId)
      }

      val memReadRspShifted = Bits(p.memConfig.dataWidth bits).assignDontCare()
      switch(memReadCmd.selected.alignement){
        for(i <- 0 to memReadCmd.selected.alignement.maxValue.toInt){
          is(i) {
            val byteCount = (0 until p.sizeCount).map(1 << _).filter(i % _ == 0).max
            memReadRspShifted(8 * byteCount - 1 downto 0) := memHub.read.rsp.data(i * 8, byteCount * 8 bits)
          }
        }
      }

      io.inputs.foreach(_.ready := False)
      when(memHub.read.rsp.valid){
        sharedRamWrite.valid := True
        sharedRamWrite.address := memReadCmd.selected.sourceFifoPtr
        sharedRamWrite.data := memReadRspShifted
        memReadCmd.selected.fifoPtrIncrement := True
        memReadCmd.selected.alignementIncrement := True
      } otherwise {
        if(p.inputs.nonEmpty) {
          sharedRamWrite.address := stream.sourceFifoPtr
          //TODO optimize mux
          sharedRamWrite.data(0, p.inputs.map(_.dataWidth).max bits) := io.inputs.map(_.data.resized).read(stream.streamId.resized)
          streamChannels.nonEmpty generate when(stream.inputsValid.orR) {
            sharedRamWrite.valid := True
            io.inputs(stream.streamId.resized).ready := True
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
          c.destination.busy && !c.destination.stop && (c.destination.memory || (if(outputs.nonEmpty && c.cp.destination.stream) outputs.map(_.fillMe).read(c.destination.streamId.resized) else False)) && !c.fifoEmpty
        ))
        val proposalValid = proposal.orR
        val oneHot = OHMasking.first(proposal)
        val memoryOneHot = oneHot.zipWithIndex.filter(e => p.channels(e._2).destination.memory).map(_._1)
        val streamOneHot = oneHot.zipWithIndex.filter(e => p.channels(e._2).destination.stream).map(_._1)
        val index = OHToUInt(oneHot)
        val memoryIndex = OHToUInt(memoryOneHot)
        val streamIndex = OHToUInt(streamOneHot)
        val lock = RegInit(False)
        val unlock = False
        val oneHotLock = RegNextWhen(oneHot, !lock)
        when(lock) { oneHot := oneHotLock }
        val valid = proposalValid || lock
        def allChannel[T <: Data](f : Channel => T) = Vec(channels.map(f))(index)
        def memoryChannel[T <: Data](f : Channel => T) = Vec(memoryChannels.map(f))(memoryIndex)
        def streamChannel[T <: Data](f : Channel => T) : T = p.outputs.nonEmpty generate Vec(streamChannels.map(f))(streamIndex)
        val memory = allChannel(_.destination.memory)
        val burst = memoryChannel(_.destination.burst)
        val fifoEmpty = allChannel(_.fifoEmpty)
        val destinationFifoPtr = allChannel(_.destinationFifoPtr)
        val memoryStop = memoryChannel(_.destination.memoryStop)
        val fifoPtrIncrement = allChannel(_.destination.fifoPtrIncrement)
        val dontStop = allChannel(_.destination.dontStop)
        val context = if(p.singleMemoryPort) sharedMemoryContext.context else MemoryContext()
        val contextCtrl = !p.singleMemoryPort generate MemoryContextCtrl(context, 1, sharedRam.readCmd(2), sharedRam.readRsp(2), sharedRam.writeCmd(2))
        val contextPort = if(p.singleMemoryPort) sharedMemoryContext.contextCtrl.ports(1) else contextCtrl.ports(0)
        val counterIncrement = streamChannel(_.destination.counterIncrement)
        val size = memoryChannel(_.destination.size)
        val alignement = memoryChannel(_.destination.alignement)



        contextPort.valid := proposalValid && memory
        contextPort.channelId := index
        contextPort.fromDestination := True
        contextPort.hitClear := unlock
        contextPort.size := size


        dontStop.setWhen(valid)

        val beat = Reg(beatType) init(0)
        unlock setWhen(fifoEmpty)

        def sharedRamReadCmd = sharedRam.readCmd(0)
        sharedRamReadCmd.valid := valid && !fifoEmpty && !(memory && !contextPort.hit)
        sharedRamReadCmd.payload := destinationFifoPtr
        fifoPtrIncrement setWhen(sharedRamReadCmd.fire)


        when(valid && sharedRamReadCmd.fire){
          if(p.outputs.nonEmpty) counterIncrement := !memory
          context.counterIncrement setWhen(memory)
          if(widthOf(beat) != 0) beat := beat + U(memory)
          memoryStop := memory && context.counterMatch
          unlock := memory && (context.counterMatch || beat === burst)
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
        val input = sharedRam.readRsp(0)
        val index = RegNextWhen(cmd.index, cmd.sharedRamReadCmd.ready)
        val unlock = RegNextWhen(cmd.unlock, cmd.sharedRamReadCmd.ready)
        val memoryIndex = RegNextWhen(cmd.memoryIndex, cmd.sharedRamReadCmd.ready)
        val streamIndex = RegNextWhen(cmd.streamIndex, cmd.sharedRamReadCmd.ready)
        def allChannel[T <: Data](f : Channel => T) = Vec(channels.map(f))(index)
        def memoryChannel[T <: Data](f : Channel => T) = Vec(memoryChannels.map(f))(memoryIndex)
        def streamChannel[T <: Data](f : Channel => T) = p.outputs.nonEmpty generate Vec(streamChannels.map(f))(streamIndex)
        val memory = allChannel(_.destination.memory)
        val dontStop = allChannel(_.destination.dontStop)
        val alignement = memoryChannel(_.destination.alignement)
        val alignementIncrement = memoryChannel(_.destination.alignementIncrement)
        val size = memoryChannel(_.destination.size)
        val streamId = streamChannel(_.destination.streamId)
        val selfStop = allChannel(_.destination.selfStop)
        dontStop.setWhen(input.valid)


        memHub.write.valid := input.valid && memory
        memHub.write.address := cmd.context.addressPlusCounterReg
        memHub.write.address(wordRange) := 0
        memHub.write.write := True
        memHub.write.data.assignDontCare()
        memHub.write.mask := (0 until p.sizeCount).map(v => B((1 << (1 << v))-1)).read(size) |<< alignement
        alignementIncrement setWhen(memory && input.fire)
        switch(alignement){
          for(i <- 0 to alignement.maxValue.toInt){
            is(i) {
              val byteCount = (0 until p.sizeCount).map(1 << _).filter(i % _ == 0).max
              memHub.write.data(i * 8, byteCount * 8 bits) := input.payload(8 * byteCount - 1 downto 0)
            }
          }
        }

        for((output, id) <- outputs.zipWithIndex){
          output.bufferIn.valid := input.valid && !memory && streamId(log2Up(outputs.size) - 1 downto 0) === id
          output.bufferIn.data := input.payload.resized
        }

        input.ready := memory ? memHub.write.ready | True
        cmd.contextPort.release := input.fire && memory && (!cmd.contextPort.hit || cmd.fifoEmpty)

        when(cmd.contextPort.release && selfStop){
          cmd.context.counterClear := True
          alignement := cmd.context.address.resized
        }
      }
    }



    val bus = Apb3SlaveFactory(io.config)

    for((channel, idx) <- channels.zipWithIndex){
      def map(that : SourceDestinationBase, offset : Int, isDestination : Boolean): Unit = {
        bus.write(
          offset + p.mapping.channelCtrlOffset,
          p.mapping.channelStartBit -> that.start,
          p.mapping.channelStopBit -> that.userStop
        )
        bus.read(
          offset + p.mapping.channelCtrlOffset,
          p.mapping.channelBusyBit -> that.busy
        )
        bus.write(
          offset + p.mapping.channelConfigOffset,
          p.mapping.channelIncrementBit -> that.increment,
          p.mapping.channelReloadBit -> that.reload,
          p.mapping.channelMemoryBit -> that.memory
        )

        that.sdp.memory generate {
          bus.write(
            offset + p.mapping.channelConfigOffset,
            p.mapping.channelSizeBit  -> that.size,
            p.mapping.channelBurstBit  -> that.burst
          )
          bus.write(p.mapping.sharedRamOffset + idx*16 + (if(isDestination) 8 else 0), 0 -> that.alignement) //TODO can be optimized ?
        }

        that.sdp.stream generate {
          bus.write(offset + p.mapping.channelAddressOffset, 0 -> that.streamId)
          bus.write(offset + p.mapping.channelLengthOffset, 0 -> that.length)
          when(bus.isWriting(offset + p.mapping.channelCtrlOffset)) {
            that.counter := 0
          }
        }
      }

      val offset = p.mapping.channelOffset + idx * p.mapping.channelDelta
      bus.write(offset, p.mapping.channelResetBit -> channel.reset)
      map(channel.source, offset + p.mapping.sourceOffset, false)
      map(channel.destination, offset + p.mapping.destinationOffset, true)
    }

    {
      def sharedMemWritePort = sharedRam.writeCmd.last
      sharedMemWritePort.valid := False
      sharedMemWritePort.address := U(sharedRam.contextBase, log2Up(sharedRam.memSize) bits) | (io.config.PADDR >> log2Up(p.memConfig.dataWidth/8))(sharedRam.contextRange).resized
      sharedMemWritePort.data := io.config.PWDATA
      when(io.config.PSEL.lsb && io.config.PENABLE && SizeMapping(p.mapping.sharedRamOffset, sharedRam.memoryContextWordCount*p.memConfig.dataWidth/8).hit(io.config.PADDR)){
        when(io.config.PWRITE){
          sharedMemWritePort.valid := True
          when(!sharedMemWritePort.ready) {
            bus.writeHalt()
          }
        }
      }
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

//  val channels = List(
//    Dma.ChannelParameter(
//      fifoDepth = 32,
//      source = Dma.SDParameter(
//        lengthWidth = 12,
//        burstWidth = 4,
//        memory = true,
//        stream = true
//      ),
//      destination = Dma.SDParameter(
//        lengthWidth = 12,
//        burstWidth = 4,
//        memory = true,
//        stream = true
//      )
//    ),
//    Dma.ChannelParameter(
//      fifoDepth = 32,
//      source = Dma.SDParameter(
//        lengthWidth = 12,
//        burstWidth = 4,
//        memory = true,
//        stream = false
//      ),
//      destination = Dma.SDParameter(
//        lengthWidth = 12,
//        burstWidth = 4,
//        memory = false,
//        stream = true
//      )
//    ),
//    Dma.ChannelParameter(
//      fifoDepth = 32,
//      source = Dma.SDParameter(
//        lengthWidth = 12,
//        burstWidth = 4,
//        memory = false,
//        stream = true
//      ),
//      destination = Dma.SDParameter(
//        lengthWidth = 12,
//        burstWidth = 4,
//        memory = true,
//        stream = false
//      )
//    )
//  )

  val channels = List.tabulate(4)( i =>
    Dma.ChannelParameter(
      fifoDepth = 32,
      source = Dma.SDParameter(
        lengthWidth = 12,
        burstWidth = 4,
        memory = true,
        stream = i == 0
      ),
      destination = Dma.SDParameter(
        lengthWidth = if(i == 0) 12 else 4,
        burstWidth = 4,
        memory = i == 0,
        stream = true
      )
    )
  )
//
//  val channels = List.tabulate(4)( i =>
//    Dma.ChannelParameter(
//      fifoDepth = 32,
//      source = Dma.SDParameter(
//        lengthWidth = -10,
//        burstWidth = 4,
//        memory = true,
//        stream = false
//      ),
//      destination = Dma.SDParameter(
//        lengthWidth = -10,
//        burstWidth = 4,
//        memory = true,
//        stream = false
//      )
//    )
//  )

  case class DmaRtl(channelCount : Int) extends  Rtl {
    override def getName(): String = "Dma" + channelCount
    override def getRtlPath(): String = "Dma" + channelCount + ".v"
    var regCount = 0
    SpinalVerilog{
      val c = (Dma.Dma(Dma.Parameter(
      memConfig = PipelinedMemoryBusConfig(32,32),
      memoryLengthWidth = 12,
      singleMemoryPort = true,
      inputs = List(
        Dma.InputParameter(dataWidth = 8),
        Dma.InputParameter(dataWidth = 8)
      ),
      outputs = List(
        Dma.OutputParameter(dataWidth = 8),
        Dma.OutputParameter(dataWidth = 8)
      ),
//      inputs = Nil,
//      outputs = Nil,
      channels = channels.take(channelCount)
    ))).setDefinitionName("Dma" + channelCount)
//        c.io.memWrite.cmd.address.setAsDirectionLess().allowDirectionLessIo
//        c.io.memRead.cmd.address.setAsDirectionLess().allowDirectionLessIo
        c
    }.toplevel.dslBody.walkDeclarations{
      case bt : BaseType if bt.isReg => println(bt); regCount += widthOf(bt)
      case _ =>
    }
    println("Total=" + regCount)
  }


  val rtls = List(DmaRtl(1), DmaRtl(2), DmaRtl(3), DmaRtl(4))

  val targets = IcestormStdTargets().take(1)

  Bench(rtls, targets, "/eda/tmp/")

}

object DmaDebug extends App{
  import spinal.core.sim._

  val p = Dma.Parameter(
    memConfig = PipelinedMemoryBusConfig(30,32),
    memoryLengthWidth = 12,
    singleMemoryPort = false,
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
//  SimConfig.withWave.compile(new Dma.Dma(p)).doSim("test", 42){dut =>
  SimConfig.allOptimisation.compile(new Dma.Dma(p)).doSim("test", 42){dut =>
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
    memoryLengthWidth = 12,
    singleMemoryPort = true,
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
    ) :: List.tabulate(9){i => //TODO
      val fifoDepth = Math.max(1, 1 << (Random.nextInt(3) + Random.nextInt(3)))
      val srcMemory = Random.nextBoolean() //TODO
      val dstMemory = Random.nextBoolean() //TODO
      Dma.ChannelParameter(
        fifoDepth = fifoDepth,
        source = Dma.SDParameter(
          lengthWidth = Math.max(4, i),
          burstWidth = Math.min(log2Up(fifoDepth), Random.nextInt(3) + Random.nextInt(2)),
          memory = srcMemory,
          stream = !srcMemory || Random.nextBoolean()
        ),
        destination = Dma.SDParameter(
          lengthWidth = Math.max(4, i),
          burstWidth = Math.min(log2Up(fifoDepth), Random.nextInt(3) + Random.nextInt(2)),
          memory = dstMemory,
          stream = !dstMemory || Random.nextBoolean()
        )
      )
    }.sortBy(_.hashCode())
  )


//  SimConfig.withWave.compile(new Dma.Dma(p)).doSim("test", 42) { dut =>
  SimConfig.allOptimisation.compile(new Dma.Dma(p)).doSim("test", 42) { dut =>

    def newMemoryAgent(bus : PipelinedMemoryBus, randomizeReady : Boolean): Unit = {
      bus.cmd.ready #= true
      bus.rsp.valid #= false

      var pendingRsp = 0
      dut.clockDomain.onSamplings{
        if(randomizeReady) bus.cmd.ready.randomize()
        bus.rsp.valid #= false
        bus.rsp.data.randomize()

        if(bus.cmd.valid.toBoolean && bus.cmd.ready.toBoolean){
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

    if(p.singleMemoryPort){
      newMemoryAgent(dut.io.mem, true)
    } else {
      newMemoryAgent(dut.io.memRead, true)
      newMemoryAgent(dut.io.memWrite, true)
    }


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

    def onMemRead(payload : PipelinedMemoryBusCmd): Unit ={
      val cmd = SimData.copy(payload)
      val channelId = addressToChannelId(payload.address.toInt)
      channels(channelId).memReadCmdScoreboard.pushDut(cmd)

      memReadRspCallback += (channels(channelId).readCallbacks.dequeue())
    }

    def onMemWrite(payload : PipelinedMemoryBusCmd): Unit ={
      val cmd = SimData.copy(payload)
      val channelId = addressToChannelId(payload.address.toInt)
      channels(channelId).memWriteCmdScoreboard.pushDut(cmd)
    }

    val memMonitors = if(p.singleMemoryPort){
      val memReadCmdMonitor = StreamMonitor(dut.io.mem.cmd, dut.clockDomain){ payload =>
        if(payload.write.toBoolean) onMemWrite(payload) else onMemRead(payload)
      }
    } else {
      val memReadCmdMonitor = StreamMonitor(dut.io.memRead.cmd, dut.clockDomain)(onMemRead)
      val memWriteCmdMonitor = StreamMonitor(dut.io.memWrite.cmd, dut.clockDomain)(onMemWrite)
    }

    FlowMonitor(if(p.singleMemoryPort) dut.io.mem.rsp else dut.io.memRead.rsp, dut.clockDomain) { payload =>
      memReadRspCallback.dequeue()(payload.data.toBigInt)
    }

    val config = Apb3Driver(dut.io.config, dut.clockDomain)
    dut.clockDomain.forkStimulus(10)
    dut.clockDomain.waitSampling(10)
    dut.clockDomain.forkSimSpeedPrinter()
    SimTimeout(10*10000000)


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
    val channelsFree = mutable.ArrayBuffer[Int]() ++ (0 until p.channels.length)
    val inputsFree = mutable.ArrayBuffer[Int]()   ++ (0 until p.inputs.length)
    val outputsFree = mutable.ArrayBuffer[Int]()  ++ (0 until p.outputs.length)
    def alloc(array : mutable.ArrayBuffer[Int]) : Int = {
      val i = Random.nextInt(array.size)
      val value = array(i)
      array.remove(i)
      value
    }
    case class SdData(channelId : Int, isSource : Boolean){
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
        config.write(p.mapping.sharedRamOffset + 16*channelId + 0x00 + (if(isSource) 0 else 8), address)
        config.write(p.mapping.sharedRamOffset + 16*channelId + 0x04 + (if(isSource) 0 else 8), length)
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
      val src = new SdData(channelId, true)
      val dst = new SdData(channelId, false)
      val sourceAddress = sourceOffset(channelId)
      val destinationAddress = destinationOffset(channelId)
      val size = Random.nextInt(p.sizeCount)
      src.memory = (Random.nextBoolean() && cp.source.memory) || !cp.source.stream
      dst.memory =  (Random.nextBoolean() && cp.destination.memory) || !cp.destination.stream

      val length =  Random.nextInt(1 << Math.min(if(src.memory) p.memoryLengthWidth >> size else cp.source.lengthWidth, if(dst.memory) p.memoryLengthWidth >> size else cp.destination.lengthWidth))

      src.increment = true
      src.reload = false
      val inputId = !src.memory generate alloc(inputsFree)
      src.size = if(src.memory) size else 0
      src.burst = Math.min(cp.fifoDepth-1, Random.nextInt(1 << cp.source.burstWidth))
      src.address = if(src.memory) (channelId * channelAddressRange + Random.nextInt(channelAddressRange/2)) & ~ 0x3 else inputId
      src.length = if(src.memory) length << src.size else length

      dst.increment = true
      dst.reload = false
      val outputId = !dst.memory generate alloc(outputsFree)
      dst.size = if(dst.memory) size else 0
      dst.burst = Math.min(cp.fifoDepth-1,  Random.nextInt(1 << cp.destination.burstWidth))
      dst.address = if(dst.memory) (channelId * channelAddressRange + Random.nextInt(channelAddressRange/2)) & ~ 0x3 else outputId
      dst.length = if(dst.memory) length << dst.size else length

      val addressMask = ~(p.memConfig.dataWidth/8-1)



      for(beat <- 0 to length) {
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

      for(beat <- 0 to length){
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
        dut.clockDomain.waitSampling(Random.nextInt(20))
      }
      dut.clockDomain.waitSampling(Random.nextInt(100))
      channelsFree += channelId
      if(!src.memory) inputsFree += inputId
      if(!dst.memory) outputsFree += outputId
    }

    //TODO
    val agents = for(agent <- 0 until Math.max(1, p.channels.length/2)) yield fork {
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