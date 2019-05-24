package saxon

import spinal.core._
import spinal.lib.BufferCC
import spinal.lib.generator.{Generator, Handle, Unset}

case class ExternalClockDomain(clkFrequency: Handle[HertzNumber] = Unset,
                               withDebug: Handle[Boolean] = Unset) extends Generator {
  val systemClockDomain = Handle[ClockDomain]
  val debugClockDomain = Handle[ClockDomain]
  val doSystemReset = Handle[() => Unit]

  dependencies ++= List(clkFrequency, withDebug)

  def global(input: Bool): Bool = input

  val io = add task new Bundle {
    val clk, reset = in Bool()
  }

  val resetCtrlClockDomain = add task ClockDomain(
    clock = io.clk,
    config = ClockDomainConfig(
      resetKind = BOOT
    )
  )


  val logic = add task new ClockingArea(resetCtrlClockDomain) {
    val resetUnbuffered = False

    //Power on reset counter
    val resetCounter = Reg(UInt(8 bits)) init (0)
    when(!resetCounter.andR) {
      resetCounter := resetCounter + 1
      resetUnbuffered := True
    }
    when(BufferCC(io.reset)) {
      resetCounter := 0
    }

    //Create all reset used later in the design
    val systemResetSet = False
    val systemReset = global(RegNext(resetUnbuffered || BufferCC(systemResetSet)))
    doSystemReset.load(() => systemResetSet := True)

    systemClockDomain.load(ClockDomain(
      clock = io.clk,
      reset = systemReset,
      frequency = FixedFrequency(clkFrequency),
      config = ClockDomainConfig(
        resetKind = spinal.core.SYNC
      )
    ))


    val debug = withDebug.get generate new Area {
      val reset = global(RegNext(resetUnbuffered))
      debugClockDomain load (ClockDomain(
        clock = io.clk,
        reset = reset,
        frequency = FixedFrequency(clkFrequency),
        config = ClockDomainConfig(
          resetKind = spinal.core.SYNC
        )
      ))
    }
  }
}
