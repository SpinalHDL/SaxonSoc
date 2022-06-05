package saxon.board.blackice.sram

import org.scalatest.funsuite.AnyFunSuite
import spinal.core._
import spinal.core.sim.SimConfig
import spinal.lib._
import spinal.lib.io.TriState
import spinal.lib.bus.bmb._
import spinal.lib.bus.bmb.sim.BmbMemoryTester

import scala.util.Random

case class SramLayout(addressWidth: Int, dataWidth : Int) {
  val capacity = BigInt(1) << (addressWidth + log2Up(dataWidth/8))
}

case class SramInterface(g : SramLayout) extends Bundle with IMasterSlave{
  val addr = Bits((g.addressWidth) bits)
  val dat = TriState(Bits(g.dataWidth bits))
  val cs  = Bool
  val we  = Bool
  val oe  = Bool
  val lb  = Bool
  val ub  = Bool

  override def asMaster(): Unit = {
    out(addr,cs,we,oe,lb,ub)
    master(dat)
  }
}

object BmbSramCtrl{
  def bmbCapabilities(layout : SramLayout) = BmbParameter(
    addressWidth  = layout.addressWidth + log2Up(layout.dataWidth/8),
    dataWidth     = 32,
    lengthWidth   = log2Up(32/8),
    sourceWidth   = Int.MaxValue,
    contextWidth  = Int.MaxValue,
    canRead       = true,
    canWrite      = true,
    alignment = BmbParameter.BurstAlignement.LENGTH,
    maximumPendingTransactionPerId = Int.MaxValue
  )
}

case class BmbSramCtrl(bmbParameter: BmbParameter,
                       sramLayout : SramLayout) extends Component{
  val io = new Bundle{
    val bus = slave(Bmb(bmbParameter))
    val sram = master(SramInterface(sramLayout))
  }

  val we = Reg(Bool)
  io.sram.we := !we

  val oe = Reg(Bool)
  io.sram.oe := !oe

  val lb = Reg(Bool)
  io.sram.lb := !lb

  val ub = Reg(Bool)
  io.sram.ub := !ub

  val state = Reg(UInt(2 bits)) init(0)

  val datOut = Reg(Bits(sramLayout.dataWidth bits))
  io.sram.dat.write := datOut

  val addr = Reg(Bits(sramLayout.addressWidth bits))
  io.sram.addr := addr

  io.sram.cs := !io.bus.cmd.valid

  io.bus.rsp.valid := RegInit(False) clearWhen(io.bus.rsp.ready) setWhen(io.bus.cmd.fire)
  io.bus.rsp.source  := RegNextWhen(io.bus.cmd.source,  io.bus.cmd.ready)
  io.bus.rsp.context := RegNextWhen(io.bus.cmd.context, io.bus.cmd.ready)

  val rspData = Reg(Bits(32 bits))
  io.bus.rsp.data := rspData

  io.sram.dat.writeEnable := we

  io.bus.cmd.ready := state === 3

  we := False
  oe := False

  io.bus.rsp.setSuccess()
  io.bus.rsp.last := True

  when (io.bus.cmd.valid && !io.bus.rsp.isStall) {
    when(io.bus.cmd.isWrite) {
      when (state === 0) {
        addr := io.bus.cmd.address(sramLayout.addressWidth  downto 2) ## B"0"
        we := True
        datOut := io.bus.cmd.data(15 downto 0)
        lb := io.bus.cmd.mask(0)
        ub := io.bus.cmd.mask(1)
        state := 1
      } elsewhen (state === 1) {
        state := 2
      } elsewhen (state === 2) {
        addr := io.bus.cmd.address(sramLayout.addressWidth downto 2) ## B"1"
        we := True
        datOut := io.bus.cmd.data(31 downto 16)
        lb := io.bus.cmd.mask(2)
        ub := io.bus.cmd.mask(3)
        state := 3
      } elsewhen (state === 3) {
        state := 0
      }
    } otherwise { // Read
      lb := True
      ub := True
      when (state === 0) {
        oe := True
        addr := io.bus.cmd.address(sramLayout.addressWidth downto 2) ## B"0"
        state := 1
      } elsewhen (state === 1) {
        rspData(15 downto 0) := io.sram.dat.read
        state := 2
      } elsewhen (state === 2) {
        addr := io.bus.cmd.address(sramLayout.addressWidth  downto 2) ## B"1"
        oe := True
        state := 3
      } elsewhen (state === 3) {
        rspData(31 downto 16) := io.sram.dat.read
        state := 0
      }
    }
  }
}




object SpinalSimBmbSramCtrlTester extends App{
  import spinal.core.sim._
  val layout = SramLayout(addressWidth = 12, dataWidth = 16)
  val sramData = new Array[Byte](2 << layout.addressWidth)
  Random.nextBytes(sramData)

  SimConfig.withWave.compile{
    BmbSramCtrl(
      bmbParameter = BmbSramCtrl.bmbCapabilities(layout).copy(
        sourceWidth = 3,
        contextWidth = 6
      ),
      sramLayout = layout
    )
  }.doSimUntilVoid("test",42){ dut =>
    new BmbMemoryTester(bmb = dut.io.bus,
      cd = dut.clockDomain) {
      for (i <- 0 until sramData.length) memory.setByte(i, sramData(i))
    }


    dut.clockDomain.onSamplings{
      delayed(2){
        dut.io.sram.dat.read.randomize()
        if(!dut.io.sram.cs.toBoolean){
          val addr = dut.io.sram.addr.toInt << 1
          if (!dut.io.sram.we.toBoolean && dut.io.sram.oe.toBoolean) {
            val data = dut.io.sram.dat.write.toInt
            if (!dut.io.sram.lb.toBoolean) sramData(addr) = (data & 0xFF).toByte
            if (!dut.io.sram.ub.toBoolean) sramData(addr + 1) = ((data >> 8) & 0xFF).toByte
          } else {
            if(!dut.io.sram.oe.toBoolean) dut.io.sram.dat.read #= (sramData(addr).toInt & 0xFF) | (sramData(addr + 1).toInt & 0xFF) * 256
          }
        }
      }
    }
  }
}

