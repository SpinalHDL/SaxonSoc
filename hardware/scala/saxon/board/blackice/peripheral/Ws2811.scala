package saxon.board.blackice.peripheral

import spinal.core._
import spinal.lib._
import spinal.lib.fsm._
import spinal.lib.bus.amba3.apb._
import spinal.lib.bus.misc._
import spinal.lib.graphic._

import spinal.lib.generator.Generator
import saxon.Apb3DecoderGenerator

case class Ws2811() extends Bundle with IMasterSlave {
  val dout = Bool

  override def asMaster(): Unit = {
    out(dout)
  }
}

case class Ws2811Ctrl(maxLeds: Int, clockHz: Int = 50000000) extends Component {
  val io = new Bundle {
    val ws2811 = master(Ws2811())
    val pixels = slave Stream (Rgb(RgbConfig(8,8,8)))
    val address = out UInt(log2Up(maxLeds) bits)
    val numLeds = in UInt(32 bits)
    val newAddress = out Bool
  }
  // Drive at 800khz
  val cycleCount = clockHz / 800000

  // 0.4us to zero bit
  val b0CycleCount = (0.32 * cycleCount).toInt

  // 0.8us for 1 bit
  val b1CycleCount = (0.64 * cycleCount).toInt

  // Send in GRB order
  object Colors extends SpinalEnum { 
    val colorG, colorR, colorB = newElement()
  }

  // Return the address (led number starting at zero)
  val address = Reg(UInt(log2Up(maxLeds) bits)) init 0
  io.address := address

  // Register for dout signal
  val dout = Reg(Bool) init False
  io.ws2811.dout := dout

  io.pixels.ready := False
  io.newAddress := False

  val fsm = new StateMachine {
    import Colors._

    // Current color, byte and bit being trasmitted
    val currentColor = RegInit(colorG)
    val currentByte = Reg(Bits(8 bits)) init 0
    val currentBit = Reg(UInt(3 bits)) init 7

    // Clock divided into 1.25us cycles
    val clockDiv = Reg(UInt(log2Up(cycleCount) bits)) init 0

    // Latched color data
    val latchedRed = Reg(Bits(8 bits)) init 0
    val latchedBlue = Reg(Bits(8 bits)) init 0

    // Wait for 50us for Leds to latch
    val stateReset : State = new StateDelay(40 * cycleCount) with EntryPoint {
      whenCompleted {
        io.newAddress := True
        goto(stateLatch)
      }
    }
   
    // Latch the incoming RGB data
    val stateLatch : State = new State {
      whenIsActive {
        when (io.pixels.valid) {
          latchedRed := io.pixels.payload.r.asBits
          latchedBlue := io.pixels.payload.b.asBits
          currentByte := io.pixels.payload.g.asBits
          currentColor := colorG
          address := address + 1
          currentBit := 7
          io.pixels.ready := True
          goto(stateTransmit)
        }
      }  
    }

    // Transmit the bit
    val stateTransmit : State = new State {
      onEntry {
        clockDiv := 0
        dout := True
      }
      whenIsActive {
        when (!currentByte(currentBit) && clockDiv === b0CycleCount - 1) {
          dout := False
        } elsewhen (currentByte(currentBit) && clockDiv === b1CycleCount - 1) {
          dout := False
        }
  
        when (clockDiv === cycleCount - 1) {
          goto(stateNextBit)
        } otherwise {
          clockDiv := clockDiv + 1
        }
      }     
    }

    // Move on to next bit
    val stateNextBit : State = new State {
      whenIsActive {
        currentBit := currentBit - 1
        when (currentBit =/= 0) {
          goto(stateTransmit)
        } otherwise {
          // Move on to next color
          when (currentColor === colorG) {
            currentColor := colorR
            currentByte := latchedRed
            goto(stateTransmit)
          } elsewhen (currentColor === colorR) {
            currentColor := colorB
            currentByte := latchedBlue
            goto(stateTransmit)
          } otherwise  { // Pixel done
            when (address === 0 || address === io.numLeds) {
              // All pixels done
              address := 0
              goto(stateReset)
            } otherwise {
              // Latch the next pixel
              io.newAddress := True
              goto(stateLatch)
            }
          }
        }
      }
    }
  }
}

case class Ws2811BusCtrl(maxLeds: Int, clockHz: Int = 50000000) extends Component {
  val io = new Bundle {
    val ws2811 = master(Ws2811())
    val pixel = Rgb(RgbConfig(8,8,8)).asInput
    val address = in UInt(log2Up(maxLeds) bits)
    val write = in Bool
    val numLeds = in UInt(32 bits)
  }

  val leds = Mem(Rgb(RgbConfig(8,8,8)), maxLeds)

  val write = RegNext(io.write)

  when (write && io.address < maxLeds) {
    leds(io.address) := io.pixel
  }
 
  val ws2811Ctrl = new Ws2811Ctrl(maxLeds = maxLeds, clockHz = clockHz)
  ws2811Ctrl.io.ws2811 <> io.ws2811

  ws2811Ctrl.io.numLeds := io.numLeds
  ws2811Ctrl.io.pixels.payload := leds(ws2811Ctrl.io.address)
  ws2811Ctrl.io.pixels.valid := True

  def driveFrom(busCtrl : BusSlaveFactory, baseAddress : Int = 0) () = new Area {
    val busSetting = False

    busCtrl.drive(io.pixel, baseAddress)
    busCtrl.drive(io.address, baseAddress + 4)
    busCtrl.drive(io.numLeds, baseAddress + 8)

    busSetting setWhen(busCtrl.isWriting(baseAddress + 4))
    io.write := busSetting
  }
}

class Ws2811Test extends Component {
  val io = new Bundle {
    val ws2811 = master(Ws2811())
  }

  val ws2811Ctrl = new Ws2811Ctrl(maxLeds = 8, clockHz=100000000)
  ws2811Ctrl.io.ws2811 <> io.ws2811
  ws2811Ctrl.io.numLeds := 6

  val count = Reg(UInt(12 bits))

  // Increase intensity after showing all leds
  when (ws2811Ctrl.io.address === 0 && ws2811Ctrl.io.newAddress) {
    count := count + 1
  }

  val colorSel = ws2811Ctrl.io.address  % 6
  val intensity = count(11 downto 4)

  ws2811Ctrl.io.pixels.r := (colorSel === 0 || colorSel === 3 || colorSel === 5) ? intensity | 0
  ws2811Ctrl.io.pixels.g := (colorSel === 1 || colorSel === 3 || colorSel === 4) ? intensity | 0
  ws2811Ctrl.io.pixels.b := (colorSel === 2 || colorSel === 4 || colorSel === 5) ? intensity | 0
  
  ws2811Ctrl.io.pixels.valid := True
}

object Ws2811Test {
  def main(args: Array[String]): Unit = {
    SpinalVerilog(new Ws2811Test)
  }
}

/*
 * Color -> 0 Write register to set the color
 * Address -> 4 Write register to specify the led to write to
 * numLeds -> 8 Write register to specify the mumber of leds
 **/
case class Apb3Ws2811Ctrl(maxLeds : Int, clockHz: Int = 50000000) extends Component {
  val io = new Bundle {
    val apb = slave(Apb3(Apb3Config(addressWidth = 8, dataWidth = 32)))
    val ws2811 = master(Ws2811())
  }

  val busCtrl = Apb3SlaveFactory(io.apb)
  val ws2811Ctrl = Ws2811BusCtrl(maxLeds = maxLeds, clockHz = clockHz)
  io.ws2811 <> ws2811Ctrl.io.ws2811

  ws2811Ctrl.driveFrom(busCtrl)
}

case class  Apb3Ws2811Generator(apbOffset : BigInt)
                             (implicit decoder: Apb3DecoderGenerator) extends Generator{
  val maxLeds = createDependency[Int]
  val ws2811 = produceIo(logic.io.ws2811)
  val apb = produce(logic.io.apb)
  val logic = add task Apb3Ws2811Ctrl(maxLeds)

  decoder.addSlave(apb, apbOffset)
}

