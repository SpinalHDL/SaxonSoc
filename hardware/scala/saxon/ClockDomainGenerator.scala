package saxon

import spinal.core._
import spinal.lib.BufferCC
import spinal.lib.generator._



trait ResetSourceKind
object ResetSourceKind{
  object EXTERNAL extends ResetSourceKind
}
case class ClockDomainGenerator() extends Generator {
  import ResetSourceKind._

  val config = Handle(ClockDomain.defaultConfig)
  val clkFrequency = Handle[HertzNumber]
  val clock, reset = Handle[Bool]
  val resetSourceKind = Handle[ResetSourceKind]
  val powerOnReset = Handle[Boolean]

  val clockDomain = produce(
    ClockDomain(
      clock = clock,
      reset = logic.systemReset,
      frequency = FixedFrequency(clkFrequency),
      config = ClockDomainConfig(
        resetKind = spinal.core.SYNC
      )
    )
  )
  val doSystemReset = produce(() => logic.resetRequest := True)

  dependencies ++= List(clkFrequency, clock, resetSourceKind, powerOnReset)
  dependencies += Dependable(config){
    if(config.useResetPin) dependencies += reset
  }

  def makeExternal(): this.type = {
    Dependable(config){
      clock.load(in Bool())
      if(config.useResetPin) reset.load(in Bool())
      resetSourceKind.load(EXTERNAL)
    }

    this
  }
  def clockTree(input: Bool): Bool = input

  def controlClockDomain() = produce(resetCtrlClockDomain(clockDomain.copy(clock = clockDomain.clock, reset = clockTree(RegNext(logic.resetUnbuffered)))))


  val resetCtrlClockDomain = add task ClockDomain(
    clock = clock,
    config = ClockDomainConfig(
      resetKind = if(powerOnReset) BOOT else null
    )
  )

  val logic = add task new ClockingArea(resetCtrlClockDomain) {
    val resetUnbuffered = False

    //Keep reset active for a while
    val resetCounter = Reg(UInt(8 bits))
    if(powerOnReset) resetCounter init(0)

    when(!resetCounter.andR) {
      resetCounter := resetCounter + 1
      resetUnbuffered := True
    }
    when(BufferCC(reset)) {
      resetCounter := 0
    }

    //Create all reset used later in the design
    val resetRequest = False
    val systemReset = clockTree(RegNext(resetUnbuffered || BufferCC(resetRequest)))
  }
}