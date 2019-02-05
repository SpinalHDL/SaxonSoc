package saxon.dma

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb.sim.Apb3Driver
import spinal.lib.bus.amba3.apb.{Apb3, Apb3SlaveFactory}
import spinal.lib.bus.simple.{PipelinedMemoryBus, PipelinedMemoryBusConfig}

import scala.util.Random
object Dma{

  case class MappingParameter( val channelOffset : Int = 0x100,
                               val channelDelta : Int = 0x20,
                               val channelSourceLengthOffset : Int = 0x0C,
                               val channelSourceAddressOffset : Int = 0x08,
                               val channelSourceConfigOffset : Int = 0x04,
                               val channelSourceCtrlOffset : Int = 0x00,

                               val channelSourceIncrementBit : Int = 0,
                               val channelSourceReloadBit : Int = 4,
                               val channelSourceFromInputBit : Int = 5,
                               val channelSourceSizeBit : Int = 12,
                               val channelSourceBurstBit : Int = 16)

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
    var fifoAllocator = 0

//    val channelsRegs = for(cp <- p.channels) yield Reg(new Bundle{
//      val fifoPtrType = HardType(UInt(log2Up(cp.fifoDepth)+1 bits))
//      val source = new Bundle{
//        val enable = Bool()
//        val increment, reload = Bool()
//        val fromInput = Bool()
//        def fromMem = !fromInput
//        val address = UInt(p.memConfig.addressWidth bits)
//        val length, counter = UInt(cp.sourceLengthWidth bits)
//        val size = p.sizeType()
//        val fifoPtr = fifoPtrType()
//      }
//      val destination = new Bundle{
//        val enable = Bool()
//        val increment, reload = Bool()
//        val toOutput = Bool()
//        def toMem = !toOutput
//        val address = UInt(p.memConfig.addressWidth bits)
//        val length, counter = UInt(cp.destinationLengthWidth bits)
//        val size = p.sizeType()
//        val fifoPtr = fifoPtrType()
//      }
//    })
//
//    val channelsLogic = for((cp, regs) <- (p.channels, channelsRegs).zipped) yield Reg(new Bundle{
//      val fifoPtrType = HardType(UInt(log2Up(cp.fifoDepth)+1 bits))
//      val source = new Bundle{
//        val enable = Bool()
//        val increment, reload = Bool()
//        val fromInput = Bool()
//        def fromMem = !fromInput
//        val address = UInt(p.memConfig.addressWidth bits)
//        val length, counter = UInt(cp.sourceLengthWidth bits)
//        val size = p.sizeType()
//        val fifoPtr = fifoPtrType()
//      }
//      val destination = new Bundle{
//        val enable = Bool()
//        val increment, reload = Bool()
//        val toOutput = Bool()
//        def toMem = !toOutput
//        val address = UInt(p.memConfig.addressWidth bits)
//        val length, counter = UInt(cp.destinationLengthWidth bits)
//        val size = p.sizeType()
//        val fifoPtr = fifoPtrType()
//      }
//    })

//    val channels = for(cp <- p.channels) yield new Bundle{
//      val fifoPtrType = HardType(UInt(log2Up(cp.fifoDepth)+1 bits))
//      val fifoHead = U(fifoAllocator, fifoPtrType.getBitsWidth bits)
//      fifoAllocator += cp.fifoDepth
//      val source = new Bundle{
//        val enable = RegInit(False)
//        val increment, reload = Reg(Bool())
//        val fromInput = Reg(Bool())
//        def fromMem = !fromInput
//        val address = Reg(UInt(p.memConfig.addressWidth bits))
//        val burst = Reg(UInt(p.burstWidth bits))
//        val length, counter = Reg(UInt(cp.sourceLengthWidth bits))
//        val size = Reg(p.sizeType)
//        val fifoPtr = Reg(fifoPtrType)
//      }
//      val destination = new Bundle{
//        val enable = RegInit(False)
//        val increment, reload = Reg(Bool())
//        val toOutput = Reg(Bool())
//        def toMem = !toOutput
//        val address = Reg(UInt(p.memConfig.addressWidth bits))
//        val burst = Reg(UInt(p.burstWidth bits))
//        val length, counter = Reg(UInt(cp.destinationLengthWidth bits))
//        val size = Reg(p.sizeType)
//        val fifoPtr = Reg(fifoPtrType)
//      }
//
//      val fifoFullOrEmpty = source.fifoPtr(log2Up(cp.fifoDepth)-1 downto 0) === destination.fifoPtr(log2Up(cp.fifoDepth)-1 downto 0)
//      val fifoFull = fifoFullOrEmpty && source.fifoPtr.msb =/= destination.fifoPtr.msb
//      val fifoEmpty = fifoFullOrEmpty && source.fifoPtr.msb === destination.fifoPtr.msb
//    }

    case class Channel(cp : ChannelParameter) extends Bundle {
      val fifoHead = fifoRam.addressType()
      val source = new Bundle{
        val enable = Bool()
        val increment, reload = Bool()
        val fromInput = Bool()
        val address = UInt(p.memConfig.addressWidth bits)
        val burst = cp.burstType()
        val length, counter = UInt(cp.sourceLengthWidth bits)
        val size = p.sizeType()
        val fifoPtr = cp.fifoPtrType()
        def fromMem = !fromInput
        def regify() : Unit = {
          List(enable, enable, increment, reload, fromInput, address, burst, length, counter, size, fifoPtr).foreach(_.setAsReg())
          enable.init(False)
          fifoPtr init(0)
          fifoPtrIncrement := False
          when(fifoPtrIncrement){
            fifoPtr := fifoPtr + 1
          }
        }

        val fifoPtrIncrement = Bool()
      }

      val destination = new Bundle{
        val enable = Bool()
        val increment, reload = Bool()
        val toOutput = Bool()
        def toMem = !toOutput
        val address = UInt(p.memConfig.addressWidth bits)
        val burst = cp.burstType()
        val length, counter = UInt(cp.destinationLengthWidth bits)
        val size = p.sizeType()
        val fifoPtr = cp.fifoPtrType()
        def regify() : Unit = {
          List(enable, enable, increment, reload, toOutput, address, burst, length, counter, size, fifoPtr).foreach(_.setAsReg())
          enable.init(False)
          fifoPtr init(0)
        }
      }

      def regify(): Unit= {
        source.regify()
        destination.regify()
      }
    }


    val channels = Vec(p.channels.map { cp =>
      val c = Channel(cp)
      c.regify()
      c.fifoHead := fifoAllocator
      fifoAllocator += cp.fifoDepth
      c
    })




//    val channels = for(cp <- p.channels) yield new Bundle{
//      val fifoPtrType = HardType(UInt(log2Up(cp.fifoDepth)+1 bits))
//      val fifoHead = U(fifoAllocator, fifoPtrType.getBitsWidth bits)
//      fifoAllocator += cp.fifoDepth
//      val source = new Bundle{
//        val enable = RegInit(False)
//        val increment, reload = Reg(Bool())
//        val fromInput = Reg(Bool())
//        def fromMem = !fromInput
//        val address = Reg(UInt(p.memConfig.addressWidth bits))
//        val burst = Reg(UInt(p.burstWidth bits))
//        val length, counter = Reg(UInt(cp.sourceLengthWidth bits))
//        val size = Reg(p.sizeType)
//        val fifoPtr = Reg(fifoPtrType)
//      }
//      val destination = new Bundle{
//        val enable = RegInit(False)
//        val increment, reload = Reg(Bool())
//        val toOutput = Reg(Bool())
//        def toMem = !toOutput
//        val address = Reg(UInt(p.memConfig.addressWidth bits))
//        val burst = Reg(UInt(p.burstWidth bits))
//        val length, counter = Reg(UInt(cp.destinationLengthWidth bits))
//        val size = Reg(p.sizeType)
//        val fifoPtr = Reg(fifoPtrType)
//      }
//
//      val fifoFullOrEmpty = source.fifoPtr(log2Up(cp.fifoDepth)-1 downto 0) === destination.fifoPtr(log2Up(cp.fifoDepth)-1 downto 0)
//      val fifoFull = fifoFullOrEmpty && source.fifoPtr.msb =/= destination.fifoPtr.msb
//      val fifoEmpty = fifoFullOrEmpty && source.fifoPtr.msb === destination.fifoPtr.msb
//    }


    val beatType = HardType(UInt(p.channels.map(_.burstType.getBitsWidth).max bits))
    val memReadCmd = new Area {
      val proposal = new Area {
        val valid = Vec(channels.map(c =>
          c.source.enable && c.source.fromMem && c.source.counter =/= c.source.length && !(c.source.fifoPtr - c.destination.fifoPtr + c.source.burst).msb
        ))
        val oneHot = OHMasking.first(valid)
      }
      val busy = RegInit(False)
      val cmdBeat, rspBeat = Reg(beatType)
      val selected = new Area{
        val oh = Reg(proposal.oneHot)
        val channel = channels(OHToUInt(oh))
      }

      val cmdDone = cmdBeat === selected.channel.source.burst || selected.channel.source.counter === selected.channel.source.length
      val rspDone = rspBeat === rspBeat && cmdDone
      when(!busy){
        busy := proposal.valid.orR
        selected.oh := proposal.oneHot
        cmdBeat := 0
        rspBeat := 0
      } otherwise {
        busy := !rspDone
      }

      io.memRead.cmd.valid := busy && !cmdDone
      io.memRead.cmd.payload.assignDontCare()
      io.memRead.cmd.address := selected.channel.source.address + (selected.channel.source.counter |<< selected.channel.source.size)
      io.memRead.cmd.write := False
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

      when(io.memRead.rsp.valid){
        valid := True
        address := (memReadCmd.selected.channel.fifoHead + memReadCmd.selected.channel.source.fifoPtr).resized
        data := io.memRead.rsp.data  //TODO normalisation
        memReadCmd.selected.channel.source.fifoPtrIncrement := True
      }
    }

//    val fifoRead = new Area{
//      val proposal = new Area {
//        val valid = Vec(channels.map(c =>
//          c.destination.enable && c.destination.toMem && c.destination.counter =/= c.destination.length && !c.fifoEmpty
//        ))
//        val oneHot = OHMasking.first(valid)
//      }
//      val busy = RegInit(False)
//      val cmdBeat, rspBeat = Reg(UInt(p.burstWidth bits))
//      val selected = new Area{
//        val oh = Reg(proposal.oneHot)
//        val channel = MuxOH(oh, channels)
//      }
//
//      val cmdDone = cmdBeat === selected.channel.source.burst || selected.channel.source.counter =/= selected.channel.source.length
//      val rspDone = rspBeat === rspBeat && cmdDone
//      when(!busy){
//        busy := proposal.valid.orR
//        selected.oh := proposal.oneHot
//        cmdBeat := 0
//        rspBeat := 0
//      } otherwise {
//        busy := rspDone
//      }
//    }


    io.memWrite.flatten.filter(_.isOutput).foreach(_.assignDontCare())

    val bus = Apb3SlaveFactory(io.config)

    for((channel, idx) <- channels.zipWithIndex){
      val offset = p.mapping.channelOffset + idx * p.mapping.channelDelta
      bus.write(offset + p.mapping.channelSourceCtrlOffset, 0 -> channel.source.enable)
      bus.write(
        offset + p.mapping.channelSourceConfigOffset,
        p.mapping.channelSourceIncrementBit -> channel.source.increment,
        p.mapping.channelSourceReloadBit -> channel.source.reload,
        p.mapping.channelSourceFromInputBit -> channel.source.fromInput,
        p.mapping.channelSourceSizeBit  -> channel.source.size,
        p.mapping.channelSourceBurstBit  -> channel.source.burst
      )
      bus.write(offset + p.mapping.channelSourceAddressOffset, 0 -> channel.source.address)
      bus.write(offset + p.mapping.channelSourceLengthOffset, 0 -> channel.source.length)
      when(bus.isWriting(offset + p.mapping.channelSourceCtrlOffset)){
        channel.source.counter := 0
      }
    }
  }

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

    def writeSourceConfig(channelId : Int)(
                          increment : Boolean,
                          reload : Boolean,
                          fromInput : Boolean,
                          size : Int,
                          burst : Int,
                          address : Int,
                          length : Int): Unit ={
      val offset = p.mapping.channelOffset + channelId * p.mapping.channelDelta
      config.write(offset + p.mapping.channelSourceLengthOffset, length)
      config.write(offset + p.mapping.channelSourceAddressOffset, address)
      config.write(offset + p.mapping.channelSourceConfigOffset,
        (if (increment) 1 << p.mapping.channelSourceIncrementBit else 0) |
        (if (reload) 1 << p.mapping.channelSourceReloadBit else 0) |
        (if (fromInput) 1 << p.mapping.channelSourceFromInputBit else 0) |
        (size << p.mapping.channelSourceSizeBit) |
        (burst << p.mapping.channelSourceBurstBit)
      )
    }

    def sourceStart(channelId : Int): Unit = {
      val offset = p.mapping.channelOffset + channelId * p.mapping.channelDelta
      config.write(offset + p.mapping.channelSourceCtrlOffset, 1)
    }



    val memory = new Array[Byte](0x10000)
    Random.nextBytes(memory)


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
            val data = bus.cmd.data.toInt
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
//    newMemoryAgent(dut.io.memWrite)


    dut.clockDomain.forkStimulus(10)
    dut.clockDomain.waitSampling(10)

    writeSourceConfig(0)(
      increment = true,
      reload = false,
      fromInput = false,
      size = 2,
      burst = 8,
      address = 0x100,
      length = 0x40
    )
    writeSourceConfig(1)(
      increment = true,
      reload = false,
      fromInput = false,
      size = 2,
      burst = 5,
      address = 0x200,
      length = 0x20
    )
    sourceStart(1)
    sourceStart(0)
    dut.clockDomain.waitSampling(100)
  }
}
