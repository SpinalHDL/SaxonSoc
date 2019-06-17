package saxon

import spinal.core._
import spinal.lib.{BufferCC, ResetCtrl}
import spinal.lib.generator._


trait ResetSensitivity
object ResetSensitivity{
  object HIGH extends ResetSensitivity
  object LOW extends ResetSensitivity
  object RISE extends ResetSensitivity
  object FALL extends ResetSensitivity
}

case class ClockDomainGenerator() extends Generator {
  import ClockDomainGenerator._
  val config = Handle(ClockDomain.defaultConfig)
  val clkFrequency = Handle[HertzNumber]
  val clock, reset = Handle[Bool]
  val resetSensitivity = Handle[ResetSensitivity]
  val resetSynchronous = Handle[Boolean]
  val resetHoldDuration = Handle[Int]
  val powerOnReset = Handle[Boolean]

  noClockDomain()

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

  dependencies ++= List(clkFrequency, clock, resetSensitivity, powerOnReset)
  dependencies += Dependable(config){
    if(config.useResetPin) {
      dependencies += reset
      dependencies += resetSensitivity
      dependencies += resetSynchronous
    }
  }

  def makeExternal(resetSensitivity : ResetSensitivity): this.type = {
    this(Dependable(config){
      clock.load(in Bool())
      if(config.useResetPin) {
        reset.load(in Bool())
        this.resetSensitivity.load(resetSensitivity)
        resetSynchronous.load(false)
      }
    })

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
    import ResetSensitivity._
    val inputResetTrigger = resetSensitivity.get match {
      case HIGH => if(resetSynchronous)  CombInit(reset.get) else ResetCtrl.asyncAssertSyncDeassert(reset.get, resetCtrlClockDomain, inputPolarity = spinal.core.HIGH, config.resetActiveLevel)
      case LOW =>  if(resetSynchronous) !CombInit(reset.get) else ResetCtrl.asyncAssertSyncDeassert(reset.get,resetCtrlClockDomain, inputPolarity = spinal.core.LOW, config.resetActiveLevel)
      case RISE => assert(resetSynchronous); reset.rise
      case FALL => assert(resetSynchronous); reset.fall
    }

    val resetUnbuffered = False

    //Keep reset active for a while
    val holdingLogic = resetHoldDuration.get match {
      case 0 => resetUnbuffered setWhen(inputResetTrigger)
      case duration => new Area{
        val resetCounter = Reg(UInt(log2Up(duration + 1) bits))
        if(powerOnReset) resetCounter init(0)

        when(resetCounter =/= duration) {
          resetCounter := resetCounter + 1
          resetUnbuffered := True
        }
        when(inputResetTrigger) {
          resetCounter := 0
        }
      }
    }

    //Create all reset used later in the design
    val resetRequest = False
    val systemReset = clockTree(RegNext(resetUnbuffered || BufferCC(resetRequest)))
  }
}