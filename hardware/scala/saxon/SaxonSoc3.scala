package saxon

import spinal.core._
import spinal.lib._
import saxon.experimental._
import spinal.core.internals.classNameOf
import spinal.lib.bus.amba3.apb.{Apb3, Apb3Config, Apb3Decoder, Apb3SlaveFactory}
import spinal.lib.bus.bmb._
import spinal.lib.bus.misc.SizeMapping
import spinal.lib.com.jtag.Jtag
import spinal.lib.com.uart._
import spinal.lib.io.{Apb3Gpio2, Gpio, TriStateArray}
import spinal.lib.misc.HexTools
import spinal.lib.misc.plic.{PlicGateway, PlicGatewayActiveHigh, PlicMapper, PlicMapping, PlicTarget}
import vexriscv.plugin._
import vexriscv._

import scala.collection.mutable.ArrayBuffer


case class ExternalClockDomain(clkFrequency : Handle[HertzNumber] = Unset,
                               withDebug : Handle[Boolean] = Unset) extends Generator{
  val systemClockDomain = Handle[ClockDomain]
  val debugClockDomain = Handle[ClockDomain]
  val doSystemReset = Handle[() => Unit]

  dependencies ++= List(clkFrequency, withDebug)

  def global(input : Bool) : Bool = input

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



class GeneratorComponent[T <: Generator](val generator : T) extends Component{
  val c = new Composable()
  c.rootGenerators += generator
  c.build()
  generator.setName("")
  this.setDefinitionName(classNameOf(generator))
}


object VexRiscvBmbGenerator{
  def apply(withJtag : Handle[Boolean] ,
            debugClockDomain : Handle[ClockDomain],
            debugAskReset : Handle[() => Unit])
           (implicit interconnect: BmbInterconnectGenerator) : VexRiscvBmbGenerator = {
    val g = VexRiscvBmbGenerator()
    g.withJtag.merge(withJtag)
    g.debugClockDomain.merge(debugClockDomain)
    g.debugAskReset.merge(debugAskReset)
    g
  }
}

case class VexRiscvBmbGenerator(/*debugAskReset : Handle[() => Unit] = Unset,
                                withJtag : Handle[Boolean] = Unset,
                                debugClockDomain : Handle[ClockDomain] = Unset*/)(implicit interconnect: BmbInterconnectGenerator = null) extends Generator{
  val config = Handle[VexRiscvConfig]
  val withJtag = Handle[Boolean]
  val debugClockDomain = Handle[ClockDomain]
  val debugAskReset = Handle[() => Unit]


  val iBus, dBus = product[Bmb]
  val externalInterrupt, timerInterrupt = product[Bool]

  def setExternalInterrupt(that : Handle[Bool]) = externalInterrupt.merge(that)
  def setTimerInterrupt(that : Handle[Bool]) = timerInterrupt.merge(that)

  dependencies ++= List(config)
  dependencies += Dependable(withJtag){
    if(withJtag) {
      dependencies ++= List(debugClockDomain, debugAskReset)
    }
  }
  dependencies += Dependable(config){
    if(config.plugins.exists(_.isInstanceOf[CsrPlugin])) {
      dependencies ++= List(externalInterrupt, timerInterrupt)
    }
  }

  val jtag = add task (withJtag.get generate slave(Jtag()))
  val logic = add task new Area {
    withJtag.get generate new Area {
      config.add(new DebugPlugin(debugClockDomain, 2))
    }

    val cpu = new VexRiscv(config)
    for (plugin <- cpu.plugins) plugin match {
      case plugin : IBusSimplePlugin => iBus.load(plugin.iBus.toBmb())
      case plugin : DBusSimplePlugin => dBus.load(plugin.dBus.toBmb())
      case plugin : CsrPlugin => {
        externalInterrupt <> (plugin.externalInterrupt)
        timerInterrupt <> (plugin.timerInterrupt)
      }
      case plugin : DebugPlugin         => plugin.debugClockDomain{
        when(RegNext(plugin.io.resetOut)) { debugAskReset.get() }
        jtag.value <> plugin.io.bus.fromJtag()
      }
      case _ =>
    }
  }

  if(interconnect != null){
    interconnect.addMaster(IBusSimpleBus.getBmbParameter(), iBus)
    interconnect.addMaster(DBusSimpleBus.getBmbParameter(), dBus)
  }
}

object CpuConfig{
  val withMemoryStage = false
  val executeRf = true
  val hardwareBreakpointsCount  = 0
  val bootloaderBin : String = null

  def regular = VexRiscvConfig(
    withMemoryStage = true,
    withWriteBackStage = true,
    List(
      new IBusSimplePlugin(
        resetVector = 0x80000000l,
        cmdForkOnSecondStage = false,
        cmdForkPersistence = true
      ),
      new DBusSimplePlugin(
        catchAddressMisaligned = false,
        catchAccessFault = false
      ),
      new DecoderSimplePlugin(
        catchIllegalInstruction = false
      ),
      new RegFilePlugin(
        regFileReadyKind = plugin.SYNC,
        zeroBoot = true,
        x0Init = false,
        readInExecute = false,
        syncUpdateOnStall = true
      ),
      new IntAluPlugin,
      new SrcPlugin(
        separatedAddSub = false,
        executeInsertion = false,
        decodeAddSub = false
      ),
//      new LightShifterPlugin(),
      new FullBarrelShifterPlugin(earlyInjection = false),
      new BranchPlugin(
        earlyBranch = false,
        catchAddressMisaligned = false,
        fenceiGenAsAJump = true
      ),
      new HazardSimplePlugin(
        bypassExecute = true,
        bypassMemory = true,
        bypassWriteBack = true,
        bypassWriteBackBuffer = true
      ),
      new MulPlugin,
      new MulDivIterativePlugin(
        genMul = false
      ),
      new CsrPlugin(new CsrPluginConfig(
        catchIllegalAccess = false,
        mvendorid = null,
        marchid = null,
        mimpid = null,
        mhartid = null,
        misaExtensionsInit = 0,
        misaAccess = CsrAccess.NONE,
        mtvecAccess = CsrAccess.WRITE_ONLY,
        mtvecInit = null,
        mepcAccess = CsrAccess.READ_WRITE,
        mscratchGen = false,
        mcauseAccess = CsrAccess.READ_ONLY,
        mbadaddrAccess = CsrAccess.NONE,
        mcycleAccess = CsrAccess.NONE,
        minstretAccess = CsrAccess.NONE,
        ecallGen = true,
        ebreakGen = false,
        wfiGenAsWait = false,
        wfiGenAsNop = true,
        ucycleAccess = CsrAccess.NONE
      )),
      new YamlPlugin("cpu0.yaml")
    )
  )


  def minimal = VexRiscvConfig(
    withMemoryStage = withMemoryStage,
    withWriteBackStage = false,
    List(
      new IBusSimplePlugin(
        resetVector = 0x80000000l,
        cmdForkOnSecondStage = false,
        cmdForkPersistence = true
      ),
      new DBusSimplePlugin(
        catchAddressMisaligned = false,
        catchAccessFault = false
      ),
      new DecoderSimplePlugin(
        catchIllegalInstruction = false
      ),
      new RegFilePlugin(
        regFileReadyKind = plugin.SYNC,
        zeroBoot = true,
        x0Init = false,
        readInExecute = executeRf,
        syncUpdateOnStall = true
      ),
      new IntAluPlugin,
      new SrcPlugin(
        separatedAddSub = false,
        executeInsertion = executeRf,
        decodeAddSub = false
      ),
      new LightShifterPlugin(),
      //        new FullBarrelShifterPlugin(earlyInjection = true),
      new BranchPlugin(
        earlyBranch = true,
        catchAddressMisaligned = false,
        fenceiGenAsAJump = true
      ),
      new HazardSimplePlugin(
        bypassExecute = false,
        bypassWriteBackBuffer = false
      ),
      //        new MulDivIterativePlugin(),
      //            new CsrPlugin(new CsrPluginConfig(
      //              catchIllegalAccess = false,
      //              mvendorid = null,
      //              marchid = null,
      //              mimpid = null,
      //              mhartid = null,
      //              misaExtensionsInit = 0,
      //              misaAccess = CsrAccess.NONE,
      //              mtvecAccess = CsrAccess.WRITE_ONLY,
      //              mtvecInit = null,
      //              mepcAccess = CsrAccess.READ_WRITE,
      //              mscratchGen = false,
      //              mcauseAccess = CsrAccess.READ_ONLY,
      //              mbadaddrAccess = CsrAccess.NONE,
      //              mcycleAccess = CsrAccess.NONE,
      //              minstretAccess = CsrAccess.NONE,
      //              ecallGen = true,
      //              ebreakGen = false,
      //              wfiGenAsWait = false,
      //              wfiGenAsNop = true,
      //              ucycleAccess = CsrAccess.NONE
      //            )),
      new YamlPlugin("cpu0.yaml")
    )
  )
  def minimalWithCsr = {
    val c = minimal
    c.plugins += new CsrPlugin(new CsrPluginConfig(
      catchIllegalAccess = false,
      mvendorid = null,
      marchid = null,
      mimpid = null,
      mhartid = null,
      misaExtensionsInit = 0,
      misaAccess = CsrAccess.NONE,
      mtvecAccess = CsrAccess.WRITE_ONLY,
      mtvecInit = null,
      mepcAccess = CsrAccess.READ_WRITE,
      mscratchGen = false,
      mcauseAccess = CsrAccess.READ_ONLY,
      mbadaddrAccess = CsrAccess.NONE,
      mcycleAccess = CsrAccess.NONE,
      minstretAccess = CsrAccess.NONE,
      ecallGen = true,
      ebreakGen = false,
      wfiGenAsWait = false,
      wfiGenAsNop = true,
      ucycleAccess = CsrAccess.NONE
    ))
    c
  }
}



object BmbInterconnectStdGenerators {
  def bmbOnChipRam(address: BigInt,
                   size: BigInt,
                   dataWidth: Int,
                   hexInit: String = null)
                  (implicit interconnect: BmbInterconnectGenerator) = wrap(new Generator {
    val requirements = Handle[BmbParameter]()
    val bus = productOf(logic.io.bus)

    dependencies += requirements

    interconnect.addSlave(
      capabilities = BmbOnChipRam.busCapabilities(size, dataWidth),
      requirements = requirements,
      bus = bus,
      mapping = SizeMapping(address, BigInt(1) << log2Up(size))
    )

    val logic = add task BmbOnChipRam(
      p = requirements,
      size = size,
      hexOffset = address,
      hexInit = hexInit
    )
  })

  def bmbOnChipRamMultiPort( portCount : Int,
                             address: BigInt,
                             size: BigInt,
                             dataWidth: Int,
                             hexInit: String = null)
                            (implicit interconnect: BmbInterconnectGenerator) = wrap(new Generator {
    val requirements = List.fill(portCount)(Handle[BmbParameter]())
    val busses = List.tabulate(portCount)(id => productOf(logic.io.buses(id)))

    dependencies ++= requirements

    for(portId <- 0 until portCount) interconnect.addSlave(
      capabilities = BmbOnChipRam.busCapabilities(size, dataWidth),
      requirements = requirements(portId),
      bus = busses(portId),
      mapping = SizeMapping(address, BigInt(1) << log2Up(size))
    )

    val logic = add task BmbOnChipRamMultiPort(
      portsParameter = requirements,
      size = size,
      hexOffset = address,
      hexInit = hexInit
    )
  })


  def bmbToApb3Decoder(address : BigInt)
                      (implicit interconnect: BmbInterconnectGenerator, apbDecoder : Apb3DecoderGenerator) = wrap(new Generator {
    val input = productOf(logic.bridge.io.input)
    val requirements = Handle[BmbParameter]()

    val requirementsGenerator = Dependable(apbDecoder.inputConfig){
      interconnect.addSlave(
        capabilities = BmbToApb3Bridge.busCapabilities(
          addressWidth = apbDecoder.inputConfig.addressWidth,
          dataWidth = apbDecoder.inputConfig.dataWidth
        ),
        requirements = requirements,
        bus = input,
        mapping = SizeMapping(address, BigInt(1) << apbDecoder.inputConfig.addressWidth)
      )
    }

    dependencies += requirements
    dependencies += apbDecoder

    val logic = add task new Area {
      val bridge = BmbToApb3Bridge(
        apb3Config = apbDecoder.inputConfig,
        bmbParameter = requirements,
        pipelineBridge = false
      )
      apbDecoder.input << bridge.io.output
    }

    //    dependencies += output
  })
}


case class Apb3DecoderGenerator() extends Generator {
  case class SlaveModel(slave : Handle[Apb3], address : BigInt){
    def mapping = SizeMapping(address, (BigInt(1)) << slave.config.addressWidth)
  }
  val models = ArrayBuffer[SlaveModel]()
  val input = productOf(logic.inputBus)
  val inputConfig = productOf(logic.inputBus.config)

  def addSlave(slave : Handle[Apb3], address : BigInt): Unit ={
    dependencies += slave
    models += SlaveModel(slave, address)
  }

  val logic = add task new Area {
    val inputBus = Apb3(
      addressWidth = log2Up(models.map(m => m.mapping.end + 1).max),
      dataWidth =  models.head.slave.config.dataWidth
    )
    for(m <- models) assert(m.slave.config.dataWidth == inputBus.config.dataWidth)
    val decoder = Apb3Decoder(
      master = inputBus,
      slaves = models.map(m => (m.slave.get, m.mapping))
    )
  }
}


object Apb3DecoderStdGenerators {

  def apbUart(apbOffset : BigInt,
              p : UartCtrlMemoryMappedConfig)
             (implicit decoder: Apb3DecoderGenerator) = wrap(new Generator {
    val interrupt = productOf(logic.io.interrupt)
    val uart = productIoOf(logic.io.uart)
    val apb = productOf(logic.io.apb)
    val logic = add task Apb3UartCtrl(p)

    decoder.addSlave(apb, apbOffset)
  })

  def addGpio(apbOffset : BigInt,
              p : spinal.lib.io.Gpio.Parameter)
             (implicit decoder: Apb3DecoderGenerator) = wrap(new Generator{

    val gpio = productIoOf(logic.io.gpio)
    val apb = productOf(logic.io.bus)
    val logic = add task Apb3Gpio2(p)

    decoder.addSlave(apb, apbOffset)
  })

  def addPlic(apbOffset : BigInt) (implicit decoder: Apb3DecoderGenerator) = wrap(new Generator {
    val gateways = ArrayBuffer[Handle[PlicGateway]]()
    val interrupt = productOf(logic.targets(0).iep)
    val apb = productOf(logic.apb)

    val priorityWidth = 1

    def addInterrupt[T <: Generator](interrupt : Handle[Bool], id : Int) = {
      this.dependencies += wrap(new Generator {
        dependencies += interrupt
        add task new Area {
          gateways += PlicGatewayActiveHigh(
            source = interrupt,
            id = id,
            priorityWidth = priorityWidth
          )
        }
      })
    }

    val logic = add task new Area{
      val apb = Apb3(addressWidth = 16, dataWidth = 32)
      val bus = Apb3SlaveFactory(apb)

      val targets = Seq(
        PlicTarget(
          gateways = gateways.map(_.get),
          priorityWidth = priorityWidth
        )
      )

      val plicMapping = PlicMapping.light
      gateways.foreach(_.priority := 1)
      targets.foreach(_.threshold := 0)
      targets.foreach(_.ie.foreach(_ := True))
      val mapping = PlicMapper(bus, plicMapping)(
        gateways = gateways.map(_.get),
        targets = targets
      )
    }


    decoder.addSlave(apb, apbOffset)
  })

  def addMachineTimer(apbOffset : BigInt) (implicit decoder: Apb3DecoderGenerator) = wrap(new Generator{
    val interrupt = productOf(logic.io.mTimeInterrupt) //TODO fix error report when this one isn't drived
    val apb = productOf(logic.io.bus)
    val logic = add task MachineTimer()

    decoder.addSlave(apb, apbOffset)
  })
}


//class SaxonSocParameter(mainClkFrequency : HertzNumber)
class SaxonSoc extends Generator{
  val clockCtrl = ExternalClockDomain()

  val core = new Generator(clockCtrl.systemClockDomain){
    implicit val interconnect = BmbInterconnectGenerator()
    implicit val apbDecoder = Apb3DecoderGenerator()
    import BmbInterconnectStdGenerators._
    import Apb3DecoderStdGenerators._

    interconnect.setDefaultArbitration(BmbInterconnectGenerator.STATIC_PRIORITY)

    implicit val cpu = VexRiscvBmbGenerator(
      withJtag = clockCtrl.withDebug,
      debugClockDomain = clockCtrl.debugClockDomain,
      debugAskReset = clockCtrl.doSystemReset
    )
    interconnect.setPriority(cpu.iBus, 1)
    interconnect.setPriority(cpu.dBus, 2)

    val plic = addPlic(0xF0000)
    cpu.setExternalInterrupt(plic.interrupt)

    val machineTimer = addMachineTimer(0x08000)
    cpu.setTimerInterrupt(machineTimer.interrupt)

//    val ramA = bmbOnChipRam(
//      address = 0x80000000l,
//      size = 32 KiB,
//      dataWidth = 32,
//      hexInit = "software/standalone/dhrystone/build/dhrystone.hex"
//    )

    val ramA = bmbOnChipRamMultiPort(
      portCount = 2,
      address = 0x80000000l,
      size = 32 KiB,
      dataWidth = 32,
      hexInit = "software/standalone/dhrystone/build/dhrystone.hex"
    )

    val gpioA = addGpio(
      apbOffset = 0x00000,
      Gpio.Parameter(
        width = 8,
        interrupt = List(0,1)
      )
    )
    plic.dependencies += Dependable(gpioA){
      plic.addInterrupt(gpioA.logic.io.interrupt(0), 4)
      plic.addInterrupt(gpioA.logic.io.interrupt(1), 5)
    }

    val uartA = apbUart(
      apbOffset = 0x10000,
      UartCtrlMemoryMappedConfig(
        uartCtrlConfig = UartCtrlGenerics(
          dataWidthMax      = 8,
          clockDividerWidth = 12,
          preSamplingSize   = 1,
          samplingSize      = 3,
          postSamplingSize  = 1
        ),
        initConfig = UartCtrlInitConfig(
          baudrate = 115200,
          dataLength = 7,  //7 => 8 bits
          parity = UartParityType.NONE,
          stop = UartStopType.ONE
        ),
        busCanWriteClockDividerConfig = false,
        busCanWriteFrameConfig = false,
        txFifoDepth = 1,
        rxFifoDepth = 1
      )
    )
    plic.addInterrupt(uartA.interrupt, 1)

    val peripheralBridge = bmbToApb3Decoder(address = 0xF0000000L)


    interconnect.addConnection(
      cpu.dBus -> List(ramA.busses(0), peripheralBridge.input),
      cpu.iBus -> List(ramA.busses(1))
    )
  }

  def defaultSetting() : this.type = {
    clockCtrl.withDebug.load(true)
    clockCtrl.clkFrequency.load(12 MHz)
    core.cpu.config.load(CpuConfig.regular)
    this
  }
}




object SaxonSocDefault{
  def main(args: Array[String]): Unit = {
    SpinalRtlConfig.generateVerilog(new GeneratorComponent(new SaxonSoc().defaultSetting()))
  }
}

