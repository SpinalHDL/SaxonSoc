package saxon

import spinal.core._
import spinal.lib._
import spinal.lib.blackbox.lattice.ice40.SB_SPRAM256KA
import spinal.lib.bus.simple._



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


case class Bram(onChipRamSize : BigInt) extends Component{
  val io = new Bundle{
    val bus = slave(PipelinedMemoryBus(32, 32))
  }

  val mem = Mem(Bits(32 bits), onChipRamSize / 4)
  io.bus.rsp.valid := RegNext(io.bus.cmd.fire && !io.bus.cmd.write) init(False)
  io.bus.rsp.data := mem.readWriteSync(
    address = (io.bus.cmd.address >> 2).resized,
    data  = io.bus.cmd.data,
    enable  = io.bus.cmd.valid,
    write  = io.bus.cmd.write,
    mask  = io.bus.cmd.mask
  )
  io.bus.cmd.ready := True
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


case class SB_IO_SCLK() extends BlackBox{
  addGeneric("PIN_TYPE", B"010000")
  val PACKAGE_PIN = out Bool()
  val OUTPUT_CLK = in Bool()
  val CLOCK_ENABLE = in Bool()
  val D_OUT_0 = in Bool()
  val D_OUT_1 = in Bool()
  setDefinitionName("SB_IO")
}

case class SB_IO_DATA() extends BlackBox{
  addGeneric("PIN_TYPE", B"110000")
  val PACKAGE_PIN = inout(Analog(Bool))
  val CLOCK_ENABLE = in Bool()
  val INPUT_CLK = in Bool()
  val OUTPUT_CLK = in Bool()
  val OUTPUT_ENABLE = in Bool()
  val D_OUT_0 = in Bool()
  val D_OUT_1 = in Bool()
  val D_IN_0 = out Bool()
  val D_IN_1 = out Bool()
  setDefinitionName("SB_IO")
}

