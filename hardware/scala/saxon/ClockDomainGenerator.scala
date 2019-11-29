package saxon

import spinal.core._
import spinal.lib.blackbox.xilinx.s7.BUFG
import spinal.lib.{BufferCC, ResetCtrl}
import spinal.lib.generator._


trait ResetSensitivity
object ResetSensitivity{
  object NONE extends ResetSensitivity
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
  val resetHoldDuration = createDependency[Int]
  val powerOnReset = Handle[Boolean]
  val resetBuffer : Handle[Bool => Bool] = Handle(defaultBuffer)

  def defaultBuffer(input : Bool) = input
  dependencies += resetBuffer


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
  dependencies += Dependable(config, resetSensitivity){
    if(config.useResetPin && resetSensitivity.get != ResetSensitivity.NONE) {
      dependencies += reset
      dependencies += resetSensitivity
      dependencies += resetSynchronous
    }
  }

  def makeExternal(resetSensitivity : ResetSensitivity): this.type = {
    this(Dependable(config){
      clock.load(in Bool() setCompositeName(this, "external_clk"))
      if(config.useResetPin && resetSensitivity != ResetSensitivity.NONE) {
        reset.load(in Bool()  setCompositeName(this, "external_reset"))
        this.resetSensitivity.load(resetSensitivity)
        resetSynchronous.load(false)
      }
    })

    this
  }

  lazy val controlClockDomain = produce(resetCtrlClockDomain.get(clockDomain.copy(clock = clockDomain.clock, reset = resetBuffer.get(RegNext(logic.resetUnbuffered)))))


  val resetCtrlClockDomain = add task ClockDomain(
    clock = clock,
    config = ClockDomainConfig(
      resetKind = if(powerOnReset) BOOT else null
    )
  )

  val logic = add task new ClockingArea(resetCtrlClockDomain) {
    import ResetSensitivity._
    val inputResetTrigger = resetSensitivity.get match {
      case ResetSensitivity.NONE => False
      case ResetSensitivity.HIGH => if(resetSynchronous)  CombInit(reset.get) else ResetCtrl.asyncAssertSyncDeassert(reset.get, resetCtrlClockDomain, inputPolarity = spinal.core.HIGH, config.resetActiveLevel)
      case ResetSensitivity.LOW =>  if(resetSynchronous) !CombInit(reset.get) else ResetCtrl.asyncAssertSyncDeassert(reset.get,resetCtrlClockDomain, inputPolarity = spinal.core.LOW, config.resetActiveLevel)
      case ResetSensitivity.RISE => if(resetSynchronous) reset.rise else BufferCC(reset.get).rise
      case ResetSensitivity.FALL => if(resetSynchronous) reset.fall else BufferCC(reset.get).fall
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
    val systemResetBeforeBuffer = RegNext(resetUnbuffered || BufferCC(resetRequest))
    val systemReset = resetBuffer.get(systemResetBeforeBuffer)
  }
}

case class Arty7BufgGenerator() extends Generator{
  val input = createDependency[ClockDomain]
  val output = produce{
    input.copy(
      clock = if(input.clock != null) BUFG.on(input.clock ) else input.clock ,
      reset = if(input.reset != null) BUFG.on(input.reset ) else input.reset ,
      clockEnable = if(input.clockEnable != null) BUFG.on(input.clockEnable ) else input.clockEnable ,
      softReset = if(input.softReset != null) BUFG.on(input.softReset ) else input.softReset
    )//.setSynchronousWith(input)
  }
}