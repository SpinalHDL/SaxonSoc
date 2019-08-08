package saxon.board.blackice.peripheral

import spinal.core._
import spinal.lib._

case class Ili9341() extends Bundle with IMasterSlave {
  val nReset = Reg(Bool)
  val cmdData = Reg(Bool)
  val writeEdge = Reg(Bool)
  val dout = Reg(Bits(8 bits))
  val backLight = Bool

  override def asMaster(): Unit = {
    out(nReset, cmdData, writeEdge, dout, backLight)
  }
}

case class Ili9341Ctrl() extends Component {
  val io = new Bundle {
    val ili9341 = master(Ili9341())
    val resetCursor = in Bool
    val pixels = slave Stream(Bits(16 bits))
    val diag = out Bits(8 bits)
  }

  object State {
    val RESET = 0
    val NOT_RESET = 1
    val WAKEUP = 2
    val INIT = 3
    val INIT_FIN = 4
    val READY = 5
    val CURSOR = 6
  }

  val CD_DATA = B"1"
  val CD_CMD = B"0"

  val SOFTRESET      = B(0x01, 8 bits)
  val SLEEPIN        = B(0x10, 8 bits)
  val SLEEPOUT       = B(0x11, 8 bits)
  val NORMALDISP     = B(0x13, 8 bits)
  val INVERTOFF      = B(0x20, 8 bits)
  val INVERTON       = B(0x21, 8 bits)
  val GAMMASET       = B(0x26, 8 bits)
  val DISPLAYOFF     = B(0x28, 8 bits)
  val DISPLAYON      = B(0x29, 8 bits)
  val COLADDRSET     = B(0x2A, 8 bits)
  val PAGEADDRSET    = B(0x2B, 8 bits)
  val MEMORYWRITE    = B(0x2C, 8 bits)
  val PIXELFORMAT    = B(0x3A, 8 bits)
  val FRAMECONTROL   = B(0xB1, 8 bits)
  val DISPLAYFUNC    = B(0xB6, 8 bits)
  val ENTRYMODE      = B(0xB7, 8 bits)
  val POWERCONTROL1  = B(0xC0, 8 bits)
  val POWERCONTROL2  = B(0xC1, 8 bits)
  val VCOMCONTROL1   = B(0xC5, 8 bits)
  val VCOMCONTROL2   = B(0xC7, 8 bits)
  val MEMCONTROL     = B(0x36, 8 bits)
  val MADCTL         = B(0x36, 8 bits)

  // below 3-bits control MCU -> LCD memory read/write direction
  val MADCTL_MY  = B(0x80, 8 bits)  // row address order
  val MADCTL_MX  = B(0x40, 8 bits)  // column address order
  val MADCTL_MV  = B(0x20, 8 bits)  // row/column exchange

  val MADCTL_ML  = B(0x10, 8 bits)  // vertical refresh order (flip vertical) (set = bottom-to-top)
  val MADCTL_RGB = B(0x00, 8 bits)  // RGB bit ordering
  val MADCTL_BGR = B(0x08, 8 bits)  // BGR bit ordering
  val MADCTL_MH  = B(0x04, 8 bits)  // horizontal refresh order (flip horizontal)

  val initSeqLen = 20
  val cursorSeqLen = 11

  val clkFreq = 16000000
  val txClkFreq = 16000000
  val txClkDiv = (clkFreq / txClkFreq) - 1

  val secPerTick = (1.0 / txClkFreq)
  val ms120 = (0.120 / secPerTick).toInt
  val ms50  = (0.050 / secPerTick).toInt
  val ms10   = (0.005 / secPerTick).toInt
  val ms5   = (0.005 / secPerTick).toInt
  val ms500 = (0.500 / secPerTick).toInt

  // Simulation only
  //val ms120 = 120
  //val ms50  = 50
  //val ms10   = 10
  //val ms5   = 5
  //val ms500 = 500

  val state = Reg(UInt(3 bits)) init State.RESET
  val txReady = Reg(Bool) init False
  val sendingPixel = Reg(Bool) init False
  val delayTicks = Reg(UInt(24 bits)) init 0
  val initSeqCounter = Reg(UInt(log2Up(initSeqLen) bits)) init 0
  val cursorSeqCounter = Reg(UInt(log2Up(cursorSeqLen) bits)) init 0

  val initSeq = Vec(Bits(9 bits), initSeqLen)
  val cursorSeq = Vec(Bits(9 bits), cursorSeqLen)

  io.diag := B"000" ## sendingPixel ## txReady ## state
  //io.diag := initSeqCounter.asBits.resized

  initSeq(0)  := CD_CMD ## DISPLAYOFF
  initSeq(1)  := CD_CMD ## POWERCONTROL1
  initSeq(2)  := CD_DATA ## B(0x23, 8 bits)
  initSeq(3)  := CD_CMD ## POWERCONTROL2
  initSeq(4)  := CD_DATA ## B(0x10, 8 bits)
  initSeq(5)  := CD_CMD ## VCOMCONTROL1
  initSeq(6)  := CD_DATA ## B(0x2b, 8 bits)
  initSeq(7)  := CD_DATA ## B(0x2b, 8 bits)
  initSeq(8)  := CD_CMD ## VCOMCONTROL2
  initSeq(9)  := CD_DATA ## B(0xc0, 8 bits)
  initSeq(10) := CD_CMD ## MEMCONTROL
  initSeq(11) := CD_DATA ## (MADCTL_BGR | MADCTL_MV | MADCTL_MX | MADCTL_MY)
  initSeq(12) := CD_CMD ## PIXELFORMAT
  initSeq(13) := CD_DATA ## B(0x55, 8 bits)
  initSeq(14) := CD_CMD ## FRAMECONTROL
  initSeq(15) := CD_DATA ## B(0x00, 8 bits)
  initSeq(16) := CD_DATA ## B(0x1b, 8 bits)
  initSeq(17) := CD_CMD ## ENTRYMODE
  initSeq(18) := CD_DATA ## B(0x07, 8 bits)
  initSeq(19) := CD_CMD ## SLEEPOUT

  cursorSeq(0)  := CD_CMD ## COLADDRSET
  cursorSeq(1)  := CD_DATA ## B(0x00, 8 bits)
  cursorSeq(2)  := CD_DATA ## B(0x00, 8 bits)
  cursorSeq(3)  := CD_DATA ## B(0x01, 8 bits)
  cursorSeq(4)  := CD_DATA ## B(0x3f, 8 bits)
  cursorSeq(5)  := CD_CMD ## PAGEADDRSET
  cursorSeq(6)  := CD_DATA ## B(0x00, 8 bits)
  cursorSeq(7)  := CD_DATA ## B(0x00, 8 bits)
  cursorSeq(8)  := CD_DATA ## B(0x00, 8 bits)
  cursorSeq(9)  := CD_DATA ## B(0xEF, 8 bits)
  cursorSeq(10) := CD_CMD ## MEMORYWRITE

  io.pixels.ready := (state === State.READY) && !sendingPixel && !txReady

  io.ili9341.backLight := True

  when(!txReady) {
    io.ili9341.writeEdge := False
  } otherwise {
    io.ili9341.writeEdge := True
    txReady := False
  }

  when (delayTicks =/= 0) {
    delayTicks := delayTicks - 1
  } otherwise {
    switch(state) {
      is(State.RESET) {
        io.ili9341.nReset := False
        io.ili9341.dout := 0
        io.ili9341.writeEdge := False
        io.ili9341.cmdData := False
        delayTicks := ms10
        state := State.NOT_RESET
      }
      is(State.NOT_RESET) {
        io.ili9341.nReset := True;
        state := State.WAKEUP
        delayTicks := ms120
      }
      is(State.WAKEUP) {
        when (!txReady) {
          io.ili9341.cmdData := False
          io.ili9341.dout := 0x01
          txReady := True
          initSeqCounter := 0
          state := State.INIT
          delayTicks := ms5
        }
      }
      is(State.INIT) {
        when (initSeqCounter < initSeqLen) {
          when (!txReady) {
            io.ili9341.cmdData := initSeq(initSeqCounter)(8)
            io.ili9341.dout := initSeq(initSeqCounter)(7 downto 0)
            initSeqCounter := initSeqCounter + 1
            txReady := True
          }
        } otherwise {
          state := State.INIT_FIN
          delayTicks := ms120
        }
      }
      is(State.INIT_FIN) {
        when(!txReady) {
          io.ili9341.cmdData := False
          io.ili9341.dout := DISPLAYON
          txReady := True
          state := State.CURSOR
          delayTicks := ms500
        }
      }
      is(State.CURSOR) {
        when (cursorSeqCounter < cursorSeqLen) {
          when (!txReady) {
            io.ili9341.cmdData := cursorSeq(cursorSeqCounter)(8)
            io.ili9341.dout := cursorSeq(cursorSeqCounter)(7 downto 0)
            cursorSeqCounter := cursorSeqCounter + 1
            txReady := True
          }
        } otherwise {
          state := State.READY
          cursorSeqCounter := 0
        }
      }
      is(State.READY) {
        when (!sendingPixel) {
          when (io.resetCursor) {
            state := State.CURSOR
          } elsewhen (io.pixels.valid && !txReady) {
            io.ili9341.cmdData := True
            io.ili9341.dout := io.pixels.payload(15 downto 8)
            txReady := True
            sendingPixel := True
          }
        } otherwise {
          when (!txReady) {
            io.ili9341.cmdData := True
            io.ili9341.dout := io.pixels.payload(7 downto 0)
            txReady := True
            sendingPixel := False
          }
        }
      }
    }
  }
}

class BlinkingIli9341() extends Component{
  val io = new Bundle{
    val ili9341 = master(Ili9341())
    val leds = out Bits(8 bits)
  }

  val colorCounter = Reg(UInt(16 bits)) init 31
  val pixelCounter = Reg(UInt(17 bits)) init 0

  val ctrl = new Ili9341Ctrl()
  ctrl.io.resetCursor := False
  ctrl.io.pixels.valid := True
  ctrl.io.pixels.payload := colorCounter.asBits
  ctrl.io.ili9341 <> io.ili9341
  io.leds := ctrl.io.diag

  when (ctrl.io.pixels.ready) {
    pixelCounter := pixelCounter + 1
  }

  when (pixelCounter === 76800) {
    colorCounter := colorCounter.rotateLeft(1)
    pixelCounter := 0
  }
}

object BlinkingIli9341 {
  def main(args: Array[String]) {
    SpinalVerilog(new BlinkingIli9341())
  }
}

object BlinkingIli9341Sim {
  import spinal.core.sim._

  def main(args: Array[String]) {
    SimConfig.withWave.compile(new BlinkingIli9341()).doSim{ dut =>
      dut.clockDomain.forkStimulus(100)

      dut.clockDomain.waitSampling(100000)
    }
  }
}
