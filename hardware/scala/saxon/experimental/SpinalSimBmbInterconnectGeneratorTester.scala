package saxon.experimental

import org.scalatest.FunSuite
import saxon.GeneratorComponent
import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.bus.bmb.{Bmb, BmbParameter}
import spinal.lib.sim.{Phase, StreamDriver, StreamMonitor, StreamReadyRandomizer}
import saxon.experimental._

import scala.collection.mutable
import scala.util.Random
import saxon.wrap
import spinal.lib.bus.misc.SizeMapping


class BmbMemorySim(val memorySize : Long) {
  val memory = new Array[Byte](memorySize.toInt)
  Random.nextBytes(memory)

  def getByteAsInt(address : Long) = memory(address.toInt).toInt & 0xFF
  def getByte(address : Long) = memory(address.toInt)
  def setByte(address : Long, value : Byte) = memory(address.toInt) = value

  def addPort(bus : Bmb, busAddress : Long, clockDomain : ClockDomain) = {
    var cmdBeat = 0

    StreamReadyRandomizer(bus.cmd, clockDomain)

    val rspQueue =  Array.fill(1 << bus.p.sourceWidth)(mutable.Queue[() => Unit]())
    val rspQueueNonEmpty = mutable.ArrayBuffer[mutable.Queue[() => Unit]]()
    def addRsp(source : Int)(body : => Unit) = {
      if(rspQueue(source).isEmpty) rspQueueNonEmpty += rspQueue(source)
      rspQueue(source) += (() => body)
    }
    StreamDriver(bus.rsp, clockDomain){ _ =>
      if(rspQueueNonEmpty.nonEmpty){
        val queue = rspQueueNonEmpty(Random.nextInt(rspQueueNonEmpty.length))
        queue.dequeue()()
        if(queue.isEmpty) rspQueueNonEmpty -= queue
        true
      } else {
        false
      }
    }

    StreamMonitor(bus.cmd, clockDomain) { payload =>
      delayed(0) { //To be sure the of timing relation ship between CMD and RSP
        val opcode = bus.cmd.opcode.toInt
        val last = bus.cmd.last.toBoolean
        val source = bus.cmd.source.toInt
        val context = bus.cmd.context.toInt
        opcode match {
          case Bmb.Cmd.Opcode.READ => {
            assert(bus.p.canRead)
            val length = bus.cmd.length.toLong
            val address = bus.cmd.address.toLong + busAddress
            val startByte = (address & (bus.p.byteCount - 1))
            val endByte = startByte + length + 1
            val rspBeatCount = ((endByte + bus.p.byteCount - 1) / bus.p.byteCount).toInt
            for (rspBeat <- 0 until rspBeatCount) {
              addRsp(source) {
                val beatAddress = (address & ~(bus.p.byteCount - 1)) + rspBeat * bus.p.byteCount
                bus.rsp.last #= rspBeat == rspBeatCount - 1
                bus.rsp.opcode #= Bmb.Rsp.Opcode.SUCCESS
                bus.rsp.source  #= source
                bus.rsp.context #= context
                var data = BigInt(0)
                for (byteId <- 0 until bus.p.byteCount) {
                  val byteAddress = beatAddress + byteId
                  val byte = getByteAsInt(byteAddress)
                  data |= (BigInt(byte) << byteId * 8)
                }
                bus.rsp.data #= data
              }
            }
          }
          case Bmb.Cmd.Opcode.WRITE => {
            assert(bus.p.canWrite)
            val mask = bus.cmd.mask.toInt
            val address = bus.cmd.address.toLong + busAddress
            val data = bus.cmd.data.toBigInt
            val beatAddress = (address & ~(bus.p.byteCount - 1)) + cmdBeat * bus.p.byteCount
            for (byteId <- 0 until bus.p.byteCount) if ((mask & (1 << byteId)) != 0) {
              setByte(beatAddress + byteId, (data >> byteId * 8).toByte)
            }
            if (last) {
              addRsp(source) {
                bus.rsp.last #= true
                bus.rsp.opcode #= Bmb.Rsp.Opcode.SUCCESS
                bus.rsp.source  #= source
                bus.rsp.context #= context
              }
            }
          }
          case _ => simFailure("Bad opcode")
        }

        cmdBeat += 1
        if (last) {
          cmdBeat = 0
        }
      }
    }
  }
}

abstract class BmbMasterAgent(bus : Bmb, clockDomain: ClockDomain){
  val cmdQueue = mutable.Queue[() => Unit]()
  val rspQueue = Array.fill(1 << bus.p.sourceWidth)(mutable.Queue[() => Unit]())

  StreamReadyRandomizer(bus.rsp, clockDomain)

  def regionAllocate(sizeMax : Int) : SizeMapping
  def regionFree(region : SizeMapping) : Unit
  def regionIsMapped(region : SizeMapping, opcode : Int) : Boolean

  def onRspRead(address : BigInt, data : Seq[Byte]) : Unit = {}
  def onCmdWrite(address : BigInt, data : Byte) : Unit = {}

  def getCmd(): () => Unit = {
    //Generate a new CMD if none is pending
    if(cmdQueue.isEmpty) {
      val region = regionAllocate(1 << bus.p.lengthWidth)
      val length = region.size.toInt-1
      val context = bus.cmd.context.randomizedInt
      val source = bus.cmd.source.randomizedInt
      val address = region.base
      val opcode = List(Bmb.Cmd.Opcode.READ, Bmb.Cmd.Opcode.WRITE)(Random.nextInt(2))
      val startAddress = address
      val endAddress = address + length + 1
      val beatCount = ((((endAddress + bus.p.wordMask) & ~bus.p.wordMask) - (startAddress & ~bus.p.wordMask)) / bus.p.byteCount).toInt
      val mapped = regionIsMapped(region, opcode)

      opcode match {
        case Bmb.Cmd.Opcode.READ => {
          //READ CMD
          cmdQueue.enqueue { () =>
            bus.cmd.address #= address
            bus.cmd.opcode #= Bmb.Cmd.Opcode.READ
            bus.cmd.context #= context
            bus.cmd.source #= source
            bus.cmd.length #= length
            bus.cmd.last #= true
          }

          //READ RSP
          val rspReadData = new Array[Byte](length + 1)
          for(beat <- 0 until beatCount) rspQueue(source).enqueue{ () =>
            val beatAddress = (startAddress & ~(bus.p.byteCount-1)) + beat*bus.p.byteCount
            assert(bus.rsp.context.toInt == context)
            assert(bus.rsp.opcode.toInt == (if(mapped) Bmb.Rsp.Opcode.SUCCESS else Bmb.Rsp.Opcode.ERROR))
            val data = bus.rsp.data.toBigInt
            for(byteId <- 0 until bus.p.byteCount; byteAddress = beatAddress + byteId) if(byteAddress >= startAddress && byteAddress < endAddress){
              rspReadData((byteAddress-startAddress).toInt) = (data >> byteId*8).toByte
            }

            if(beat == beatCount-1){
              assert(bus.rsp.last.toBoolean)
              if(mapped) onRspRead(address, rspReadData)
              regionFree(region)
            } else {
              assert(!bus.rsp.last.toBoolean)
            }
          }
        }
        case Bmb.Cmd.Opcode.WRITE => {
          //WRITE CMD
          for (beat <- 0 until beatCount) cmdQueue.enqueue { () =>
            val beatAddress = (startAddress & ~(bus.p.byteCount - 1)) + beat * bus.p.byteCount
            val data = bus.cmd.data.randomizedBigInt()
            bus.cmd.address #= address
            bus.cmd.opcode #= Bmb.Cmd.Opcode.WRITE
            bus.cmd.data #= data
            bus.cmd.context #= context
            bus.cmd.source #= source
            bus.cmd.length #= length
            bus.cmd.last #= beat == beatCount - 1
            var mask = 0
            for (byteId <- 0 until bus.p.byteCount; byteAddress = beatAddress + byteId) if (byteAddress >= startAddress && byteAddress < endAddress) {
              if (Random.nextBoolean()) {
                mask |= 1 << byteId
                if (mapped) onCmdWrite(byteAddress, (data >> byteId * 8).toByte)
              }

            }
            bus.cmd.mask #= mask
          }

          //WRITE RSP
          rspQueue(source).enqueue { () =>
            assert(bus.rsp.context.toInt == context)
            assert(bus.rsp.opcode.toInt == (if (mapped) Bmb.Rsp.Opcode.SUCCESS else Bmb.Rsp.Opcode.ERROR))
            regionFree(region)
          }
        }
      }
    }
    if(cmdQueue.nonEmpty) cmdQueue.dequeue() else null
  }

  StreamDriver(bus.cmd, clockDomain){ _ =>
    val cmd = getCmd()
    if(cmd != null) cmd()
    cmd != null
  }

  val rspMonitor = StreamMonitor(bus.rsp, clockDomain){_ =>
    rspQueue(bus.rsp.source.toInt).dequeue()()
  }
}

//TODO handle generation when a master has no slave
class SpinalSimBmbInterconnectGeneratorTester  extends FunSuite{

  test("test1"){
    SimConfig.allOptimisation.compile(new GeneratorComponent(new Generator {
      val interconnect = BmbInterconnectGenerator()


      def addMaster(requirements : BmbParameter) = wrap(new Generator {
        val busHandle = Handle[Bmb]
        interconnect.addMaster(requirements, busHandle)

        val logic = add task new Area{
          val bus = slave(Bmb(requirements))
          busHandle.load(bus)
        }
      })

      def addSlave(address : BigInt, capabilities : BmbParameter) = wrap(new Generator{
        val requirements = Handle[BmbParameter]
        val busHandle = Handle[Bmb]
        interconnect.addSlave(capabilities, requirements, busHandle, address)
        dependencies += requirements
        val logic = add task new Area{
          val bus = master(Bmb(requirements))
          busHandle.load(bus)
        }
      })


      val mA = addMaster(BmbParameter(
        addressWidth = 20,
        dataWidth = 32,
        lengthWidth = 8,
        sourceWidth = 4,
        contextWidth = 4,
        canRead = true,
        canWrite = true,
        allowUnalignedWordBurst = true,
        allowUnalignedByteBurst = true,
        maximumPendingTransactionPerId = Int.MaxValue
      ))

      val mB = addMaster(BmbParameter(
        addressWidth = 20,
        dataWidth = 32,
        lengthWidth = 8,
        sourceWidth = 4,
        contextWidth = 4,
        canRead = true,
        canWrite = true,
        allowUnalignedWordBurst = true,
        allowUnalignedByteBurst = false,
        maximumPendingTransactionPerId = Int.MaxValue
      ))

      val mC = addMaster(BmbParameter(
        addressWidth = 20,
        dataWidth = 32,
        lengthWidth = 6,
        sourceWidth = 0,
        contextWidth = 5,
        canRead = true,
        canWrite = true,
        allowUnalignedWordBurst = false,
        allowUnalignedByteBurst = false,
        maximumPendingTransactionPerId = Int.MaxValue
      ))


      val mD = addMaster(BmbParameter(
        addressWidth = 20,
        dataWidth = 32,
        lengthWidth = 2,
        sourceWidth = 4,
        contextWidth = 3,
        canRead = true,
        canWrite = true,
        allowUnalignedWordBurst = false,
        allowUnalignedByteBurst = false,
        maximumPendingTransactionPerId = Int.MaxValue
      ))

      val sA = addSlave(0x00000, BmbParameter(
        addressWidth = 18,
        dataWidth = 32,
        lengthWidth = 10,
        sourceWidth = Int.MaxValue,
        contextWidth = Int.MaxValue,
        canRead = true,
        canWrite = true,
        allowUnalignedWordBurst = true,
        allowUnalignedByteBurst = true,
        maximumPendingTransactionPerId = Int.MaxValue
      ))

      val sB = addSlave(0x40000, BmbParameter(
        addressWidth = 17,
        dataWidth = 32,
        lengthWidth = Int.MaxValue,
        sourceWidth = 8,
        contextWidth = Int.MaxValue,
        canRead = true,
        canWrite = true,
        allowUnalignedWordBurst = true,
        allowUnalignedByteBurst = true,
        maximumPendingTransactionPerId = Int.MaxValue
      ))

      val sB2 = addSlave(0x60000, BmbParameter(
        addressWidth = 17,
        dataWidth = 32,
        lengthWidth = 2,
        sourceWidth = Int.MaxValue,
        contextWidth = Int.MaxValue,
        canRead = true,
        canWrite = true,
        allowUnalignedWordBurst = true,
        allowUnalignedByteBurst = true,
        maximumPendingTransactionPerId = Int.MaxValue
      ))

      val sC = addSlave(0x80000, BmbParameter(
        addressWidth = 17,
        dataWidth = 32,
        lengthWidth = Int.MaxValue,
        sourceWidth = Int.MaxValue,
        contextWidth = 9,
        canRead = false,
        canWrite = true,
        allowUnalignedWordBurst = true,
        allowUnalignedByteBurst = true,
        maximumPendingTransactionPerId = Int.MaxValue
      ))

      val sD = addSlave(0xA0000, BmbParameter(
        addressWidth = 17,
        dataWidth = 32,
        lengthWidth = Int.MaxValue,
        sourceWidth = Int.MaxValue,
        contextWidth = Int.MaxValue,
        canRead = true,
        canWrite = false,
        allowUnalignedWordBurst = true,
        allowUnalignedByteBurst = true,
        maximumPendingTransactionPerId = Int.MaxValue
      ))

      val sE = addSlave(0xC0000, BmbParameter(
        addressWidth = 17,
        dataWidth = 32,
        lengthWidth = Int.MaxValue,
        sourceWidth = Int.MaxValue,
        contextWidth = Int.MaxValue,
        canRead = false,
        canWrite = true,
        allowUnalignedWordBurst = true,
        allowUnalignedByteBurst = true,
        maximumPendingTransactionPerId = Int.MaxValue
      ))

      val sF = addSlave(0xC0000, BmbParameter(
        addressWidth = 17,
        dataWidth = 32,
        lengthWidth = Int.MaxValue,
        sourceWidth = Int.MaxValue,
        contextWidth = Int.MaxValue,
        canRead = true,
        canWrite = false,
        allowUnalignedWordBurst = true,
        allowUnalignedByteBurst = true,
        maximumPendingTransactionPerId = Int.MaxValue
      ))

      def fullAccess(bus : Handle[Bmb]) = interconnect.slaves.keys.foreach(s => interconnect.addConnection(bus, s))
      fullAccess(mA.busHandle)
      fullAccess(mB.busHandle)
      fullAccess(mC.busHandle)
      fullAccess(mD.busHandle)
//      interconnect.addConnection(mA.busHandle, List(sA.busHandle))
//      interconnect.addConnection(mB, List(sA))
    })).doSimUntilVoid("test1", 42){dut => //TODO remove seed

      Phase.boot() //Initialise phase. Phases are :  setup -> stimulus -> flush -> check -> end
//      Phase.flush.retainFor(3000*10) //Give 1000 cycle between the end of push stimulus and check phase to flush the hardware


      Phase.setup {
        dut.clockDomain.forkStimulus(10)
        dut.clockDomain.forkSimSpeedPrinter()

        val memorySize = 0x100000
        val allowedWrites = mutable.HashMap[Long, Byte]()
        val memory = new BmbMemorySim(memorySize){
          override def setByte(address: Long, value: Byte): Unit = {
            val option = allowedWrites.get(address)
            assert(option.isDefined)
            assert(option.get == value)
            super.setByte(address, value)
            allowedWrites.remove(address)
          }
        }
        for((bus, model) <- dut.generator.interconnect.slaves){
          memory.addPort(
            bus = bus.get,
            busAddress = model.mapping.get.lowerBound.toLong,
            clockDomain = dut.clockDomain
          )
        }

        val regions = mutable.HashSet[SizeMapping]()
        for((bus, model) <- dut.generator.interconnect.masters){
          val agent = new BmbMasterAgent(bus.get, dut.clockDomain){
            override def onRspRead(address: BigInt, data: Seq[Byte]): Unit = {
              val ref = (0 until data.length).map(i => memory.getByte(address.toLong + i))
              if(ref != data){
                simFailure(s"Read missmatch on $bus\n  REF=$ref\n  DUT=$data")
              }
            }


            override def getCmd(): () => Unit = if(Phase.stimulus.isActive || cmdQueue.nonEmpty) super.getCmd() else null

            override def onCmdWrite(address: BigInt, data: Byte): Unit = {
              val addressLong = address.toLong
              assert(!allowedWrites.contains(addressLong))
              allowedWrites(addressLong) = data
            }

            override def regionAllocate(sizeMax : Int): SizeMapping = {
              while(true){
                var address = Random.nextInt(memorySize)
                var size = Math.min(Bmb.boundarySize - (address & (Bmb.boundarySize-1)), Random.nextInt(sizeMax) + 1)
                if(!bus.get.p.allowUnalignedByteBurst) {
                  address &= ~bus.get.p.wordMask
                  if(size > bus.get.p.byteCount){
                    size = (size + bus.get.p.byteCount - 1) & ~bus.get.p.wordMask
                  }
                }
                val region = SizeMapping(address, size)
                if(regions.forall(r => r.base > region.end || r.end < region.base)) {
                  regions += region
                  return region
                }
              }
              return null
            }

            override def regionFree(region: SizeMapping): Unit = regions.remove(region)
            override def regionIsMapped(region: SizeMapping, opcode : Int): Boolean = {
              dut.generator.interconnect.slaves.values.exists{model =>
                val opcodeOk = opcode match {
                  case Bmb.Cmd.Opcode.WRITE => model.requirements.get.canWrite
                  case Bmb.Cmd.Opcode.READ => model.requirements.get.canRead
                }
                val addressOk = model.mapping.get.lowerBound <= region.end && model.mapping.get.asInstanceOf[SizeMapping].end >= region.base

                addressOk && opcodeOk
              }
            }
          }

          //Retain the flush phase until all Bmb rsp are received
          Phase.flush.retain()
          Phase.flush(fork{
            while(agent.rspQueue.exists(_.nonEmpty)) {
              dut.clockDomain.waitSampling(1000)
            }
            dut.clockDomain.waitSampling(1000)
            Phase.flush.release()
          })

          //Retain the stimulus phase until at least 300 transaction completed on each Bmb source id
          val retainers = List.fill(1 << bus.get.p.sourceWidth)(Phase.stimulus.retainer(300)) //TODO
          agent.rspMonitor.addCallback{_ =>
            if(bus.get.rsp.last.toBoolean){
              retainers(bus.get.rsp.source.toInt).release()
            }
          }
        }
      }
    }
  }
}
