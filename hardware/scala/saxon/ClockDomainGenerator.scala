package saxon

import spinal.core._
import spinal.lib.blackbox.xilinx.s7.BUFG
import spinal.lib.{BufferCC}
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
      case ResetSensitivity.HIGH => if(resetSynchronous)  CombInit(reset.get) else spinal.lib.ResetCtrl.asyncAssertSyncDeassert(reset.get, resetCtrlClockDomain, inputPolarity = spinal.core.HIGH, config.resetActiveLevel)
      case ResetSensitivity.LOW =>  if(resetSynchronous) !CombInit(reset.get) else spinal.lib.ResetCtrl.asyncAssertSyncDeassert(reset.get,resetCtrlClockDomain, inputPolarity = spinal.core.LOW, config.resetActiveLevel)
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



case class ClockDomainResetGenerator() extends Generator {
  noClockDomain()

  val inputClockDomain = createDependency[ClockDomain]
  val holdDuration = createDependency[Int]
  val powerOnReset = createDependency[Boolean]
  powerOnReset.load(false)

  def setInput(input : ClockDomain) = inputClockDomain.load(input)

  def setInput(input : ClockDomainResetGenerator) = inputClockDomain.merge(input.outputClockDomain)

  def setInput(clock : Bool,
               frequency : IClockDomainFrequency = UnknownFrequency,
               powerOnReset : Boolean = false) = inputClockDomain.load(
    ClockDomain(
      clock = clock,
      frequency = frequency,
      config = ClockDomainConfig(
        resetKind = BOOT
      )
    )
  )

  val outputClockDomain = produce(
    ClockDomain(
      clock = inputClockDomain.clock,
      reset = logic.outputReset,
      frequency = inputClockDomain.frequency,
      config = ClockDomainConfig(
        resetKind = spinal.core.SYNC
      )
    )
  )

  val logic = add task new ClockingArea(inputClockDomain.copy(reset = null, config = inputClockDomain.config.copy(resetKind = BOOT))) {
    val inputResetTrigger = False
    val outputResetUnbuffered = False

    val inputResetAdapter = (inputClockDomain.reset != null) generate {
      val generator = ResetGenerator(ClockDomainResetGenerator.this)
      generator.reset.load(inputClockDomain.reset)
      generator.kind.load(inputClockDomain.config.resetKind)
      generator.sensitivity.load(inputClockDomain.config.resetActiveLevel match {
        case HIGH => ResetSensitivity.HIGH
        case LOW => ResetSensitivity.LOW
      })
      generator
    }

    //Keep reset active for a while
    val duration = holdDuration.get
    val noHold = (duration == 0) generate outputResetUnbuffered.setWhen(inputResetTrigger)
    val holdingLogic = (duration != 0) generate new Area{
      val resetCounter = Reg(UInt(log2Up(duration + 1) bits))

      when(resetCounter =/= duration) {
        resetCounter := resetCounter + 1
        outputResetUnbuffered := True
      }
      when(inputResetTrigger) {
        resetCounter := 0
      }
    }

    //Create all reset used later in the design
    val outputReset = RegNext(outputResetUnbuffered)

    if(inputClockDomain.config.resetKind == BOOT || powerOnReset.get){
      outputReset init(True)
      holdingLogic.resetCounter init(0)
    }

//    val inputResetAdapter = (inputClockDomain.reset != null) generate new Area {
//      val reset = inputClockDomain.config.resetKind match {
//        case ASYNC => spinal.lib.ResetCtrl.asyncAssertSyncDeassert(
//          input = inputClockDomain.isResetActive,
//          clockDomain = inputClockDomain,
//          inputPolarity = HIGH,
//          outputPolarity = HIGH
//        )
//        case SYNC  => RegNext(inputClockDomain.isResetActive)
//      }
//
//      when(reset){
//        inputResetTrigger := True
//      }
//    }
  }

  case class ResetGenerator(dady : ClockDomainResetGenerator) extends Generator{
    val reset = createDependency[Bool]
    val kind = createDependency[ResetKind]
    val sensitivity = createDependency[ResetSensitivity]
    dependencies += dady.logic

    val stuff = add task new ClockingArea(inputClockDomain){
      import ResetSensitivity._
      val syncTrigger = kind.get match {
        case SYNC => {
          RegNext(reset.get)
        }
        case ASYNC => {
          sensitivity.get match {
            case ResetSensitivity.NONE => ???
            case ResetSensitivity.HIGH => spinal.lib.ResetCtrl.asyncAssertSyncDeassert(reset.get, dady.inputClockDomain, inputPolarity = spinal.core.HIGH, spinal.core.HIGH)
            case ResetSensitivity.LOW => spinal.lib.ResetCtrl.asyncAssertSyncDeassert(reset.get, dady.inputClockDomain, inputPolarity = spinal.core.LOW, spinal.core.HIGH)
            case ResetSensitivity.RISE => BufferCC(reset.get).rise
            case ResetSensitivity.FALL => BufferCC(reset.get).fall
          }
        }
      }
      dady.logic.inputResetTrigger setWhen(syncTrigger)
    }
  }


  def asyncReset(reset : Bool, sensitivity : ResetSensitivity) = {
    val generator = ResetGenerator(this)
    generator.reset.load(reset)
    generator.sensitivity.load(sensitivity)
    generator.kind.load(ASYNC)
    generator
  }

  def makeExternal(frequency : IClockDomainFrequency = UnknownFrequency): this.type = {
    this(Dependable(){
      val clock = in Bool() setCompositeName(ClockDomainResetGenerator.this, "external_clk")
      val reset = in Bool()  setCompositeName(ClockDomainResetGenerator.this, "external_reset")

      inputClockDomain.load(
        ClockDomain(
          clock = clock,
          reset = reset,
          frequency = frequency,
          config = ClockDomainConfig(
            resetKind = ASYNC,
            resetActiveLevel = HIGH
          )
        )
      )
    })

    this
  }

  def enablePowerOnReset() = powerOnReset.load(true)
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