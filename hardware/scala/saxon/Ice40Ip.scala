package saxon

import spinal.core._
import spinal.lib._
import spinal.lib.bus.simple._

case class SB_SPRAM256KA() extends BlackBox{
  val DATAIN = in Bits(16 bits)
  val ADDRESS = in UInt(14 bits)
  val MASKWREN = in Bits(4 bits)
  val WREN = in Bool()
  val CHIPSELECT = in Bool()
  val CLOCK = in Bool()
  val DATAOUT = out Bits(16 bits)
  val STANDBY = in Bool()
  val SLEEP = in Bool()
  val POWEROFF = in Bool()
  mapCurrentClockDomain(CLOCK)
}

object SB_GB{
  def apply(input : Bool) : Bool = {
    val c = SB_GB().setCompositeName(input, "SB_GB")
    c.USER_SIGNAL_TO_GLOBAL_BUFFER := input
    c.GLOBAL_BUFFER_OUTPUT
  }
}

case class SB_GB() extends BlackBox{
  val USER_SIGNAL_TO_GLOBAL_BUFFER = in Bool()
  val GLOBAL_BUFFER_OUTPUT = out Bool()
}


case class SB_RGBA_DRV() extends BlackBox{
  addGeneric("CURRENT_MODE", "0b1")
  addGeneric("RGB0_CURRENT ", "0b000001")
  addGeneric("RGB1_CURRENT ", "0b000001")
  addGeneric("RGB2_CURRENT ", "0b000001")
  val CURREN = in Bool()
  val RGBLEDEN = in Bool()
  val RGB0PWM = in Bool()
  val RGB1PWM = in Bool()
  val RGB2PWM = in Bool()
  val RGB0    = out Bool()
  val RGB1    = out Bool()
  val RGB2    = out Bool()
}


case class SB_PLL40_PAD() extends BlackBox{
  val PACKAGEPIN = in Bool()
  val PLLOUTCORE = out Bool()
  val PLLOUTGLOBAL = out Bool()
  val RESETB = in Bool()
  val BYPASS = in Bool()


  addGeneric("DIVR", B"0000")
  addGeneric("DIVF", B"1000111")
  addGeneric("DIVQ",B"101")
  addGeneric("FILTER_RANGE", B"001")
  addGeneric("FEEDBACK_PATH", "SIMPLE")
  addGeneric("DELAY_ADJUSTMENT_MODE_FEEDBACK", "FIXED")
  addGeneric("FDA_FEEDBACK", B"0000")
  addGeneric("DELAY_ADJUSTMENT_MODE_RELATIVE", "FIXED")
  addGeneric("FDA_RELATIVE", B"0000")
  addGeneric("SHIFTREG_DIV_MODE", B"00")
  addGeneric("PLLOUT_SELECT", "GENCLK")
  addGeneric("ENABLE_ICEGATE", False)
}


//Provide a 64 KB on-chip-ram via the Up5k SPRAM.
case class Spram() extends Component{
  val io = new Bundle{
    val bus = slave(PipelinedMemoryBus(16, 32))
  }

  val cmd = Flow(PipelinedMemoryBusCmd(io.bus.config))
  cmd << io.bus.cmd.toFlow

  val rspPending = RegNext(cmd.valid && !cmd.write) init(False)
  val rspTarget = RegNext(io.bus.cmd.valid)


  val mems = List.fill(2)(SB_SPRAM256KA())
  mems(0).DATAIN := cmd.data(15 downto 0)
  mems(0).MASKWREN := cmd.mask(1) ## cmd.mask(1) ## cmd.mask(0) ## cmd.mask(0)
  mems(1).DATAIN := cmd.data(31 downto 16)
  mems(1).MASKWREN := cmd.mask(3) ## cmd.mask(3) ## cmd.mask(2) ## cmd.mask(2)
  for(mem <- mems){
    mem.CHIPSELECT := cmd.valid
    mem.ADDRESS := (cmd.address >> 2).resized
    mem.WREN := cmd.write
    mem.STANDBY  := False
    mem.SLEEP    := False
    mem.POWEROFF := True
  }

  val readData = mems(1).DATAOUT ## mems(0).DATAOUT


  io.bus.rsp.valid := rspPending && rspTarget
  io.bus.rsp.data  := readData
}