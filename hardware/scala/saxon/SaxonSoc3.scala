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



//
//class SaxonSocBase extends Generator{
////  val cpu = new VexRiscvPlugin()
//  val interconnect = new BmbInterconnectGenerator()
////  val apbDecoder = new Apb3DecoderPlugin(Apb3Config(20,32))
////  val apbBridge = new PipelinedMemoryBusToApbBridgePlugin(apbDecoder.input)
////  val mainBus = add task PipelinedMemoryBus(addressWidth = 32, dataWidth = 32)
//
//  val mapper = new Generator{
//    dependencies ++= List(cpu, apbBridge)
//    val logic = add task new Area {
//      interconnect.factory.addSlave(mainBus, DefaultMapping)
//      interconnect.factory.addSlave(apbBridge.input, SizeMapping(0xF0000000l,  16 MiB))
//      interconnect.factory.addMasters(
//        (cpu.dBus, List(mainBus)),
//        (cpu.iBus, List(mainBus)),
//        ( mainBus, List(apbBridge.input))
//      )
//    }
//  }
//
//  def addOneChipRam() = this add new Generator{
//    dependencies ++= List(mainBus, interconnect.factory)
//
//    val logic = add task new Area {
//      val ram = Spram()
//      interconnect.factory.addSlave(ram.io.bus, SizeMapping(0x80000000l,  64 KiB))
//      interconnect.factory.addConnection(mainBus, ram.io.bus)
//    }
//  }
//}



case class VexRiscvBmbGenerator(val config : Handle[VexRiscvConfig] = Unset,
                                val withJtag : Handle[Boolean] = Unset,
                                val debugClockDomain : Handle[ClockDomain] = Unset,
                                val debugAskReset : Handle[() => Unit] = Unset)(implicit interconnect: BmbInterconnectGenerator = null) extends Generator{
  val iBus, dBus = Handle[Bmb]
  val externalInterrupt, timerInterrupt = Handle[Bool]

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
                   hexOffset : BigInt = null,
                   hexInit: String = null)
                  (implicit interconnect: BmbInterconnectGenerator) = wrap(new Generator {
    val requirements = Handle[BmbParameter]()
    val bus = Handle[Bmb]()

    interconnect.addSlave(
      capabilities = BmbOnChipRam.busCapabilities(size, dataWidth),
      requirements = requirements,
      bus = bus,
      mapping = SizeMapping(address, BigInt(1) << log2Up(size))
    )

    dependencies += requirements
    val logic = add task new Area {
      val ram = BmbOnChipRam(
        p = requirements,
        size = size,
        hexOffset = hexOffset,
        hexInit = hexInit
      )
      bus.load(ram.io.bus)
    }
  })


  def bmbToApb3Decoder(address : BigInt)
                      (implicit interconnect: BmbInterconnectGenerator, apbDecoder : Apb3DecoderGenerator) = wrap(new Generator {
    val input = Handle[Bmb]
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
      input.load(bridge.io.input)
    }

    //    dependencies += output
  })
}


case class Apb3DecoderGenerator() extends Generator {
  case class SlaveModel(slave : Handle[Apb3], address : BigInt){
    def mapping = SizeMapping(address, (BigInt(1)) << slave.config.addressWidth)
  }
  val models = ArrayBuffer[SlaveModel]()
  val inputConfig = Handle[Apb3Config]
  val input = Handle[Apb3]

  def addSlave(slave : Handle[Apb3], address : BigInt): Unit ={
    dependencies += slave
    models += SlaveModel(slave, address)
  }

  val logic = add task new Area {
    inputConfig.load(
      Apb3Config(
        addressWidth = log2Up(models.map(m => m.mapping.end + 1).max),
        dataWidth =  models.head.slave.config.dataWidth
      )
    )
    for(m <- models) assert(m.slave.config.dataWidth == inputConfig.dataWidth)
    val inputBus = Apb3(inputConfig)
    val decoder = Apb3Decoder(
      master = inputBus,
      slaves = models.map(m => (m.slave.get, m.mapping))
    )

    input.load(inputBus)
  }
}


object Apb3DecoderStdGenerators {

  def apbUart(apbOffset : BigInt,
              p : UartCtrlMemoryMappedConfig)
              (implicit decoder: Apb3DecoderGenerator) = wrap(new Generator {
    decoder.dependencies += this
    val interrupt = Handle[Bool]

    val logic = add task new Area {
      val ctrl = Apb3UartCtrl(p)
      val uart = master(Uart())
      uart <> ctrl.io.uart
      interrupt.load(ctrl.io.interrupt)
      decoder.addSlave(ctrl.io.apb, apbOffset)
    }
  })

  def addGpio(apbOffset : BigInt,
              p : spinal.lib.io.Gpio.Parameter)
             (implicit decoder: Apb3DecoderGenerator) = wrap(new Generator{
    decoder.dependencies += this

    val logic = add task new Area {
      val ctrl = Apb3Gpio2(p)
      decoder.addSlave(ctrl.io.bus, apbOffset)
    }

    val io = add task new Area{
      val gpio = master(TriStateArray(p.width))
      gpio <> logic.ctrl.io.gpio
    }
  })

  def addPlic(apbOffset : BigInt) (implicit decoder: Apb3DecoderGenerator) = wrap(new Generator {
    val gateways = ArrayBuffer[Handle[PlicGateway]]()
    val interrupt = Handle(Bool)
    val priorityWidth = 1

    decoder.dependencies += this

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
      decoder.addSlave(apb, apbOffset)
      interrupt := targets(0).iep
    }
  })

  def addMachineTimer(apbOffset : BigInt) (implicit decoder: Apb3DecoderGenerator) = wrap(new Generator{
    val interrupt = Handle(Bool) //TODO fix error report when this one isn't drived
    decoder.dependencies += this

    val logic = add task new Area {
      val machineTimer = MachineTimer()
      decoder.addSlave(machineTimer.io.bus, apbOffset)
      interrupt := machineTimer.io.mTimeInterrupt
    }
  })
}

class SaxonSoc extends Generator{
  val clockCtrl = ExternalClockDomain(
    clkFrequency = 12 MHz,
    withDebug = true
  )

  val core = new Generator(clockCtrl.systemClockDomain){
    implicit val interconnect = BmbInterconnectGenerator()
    implicit val apbDecoder = Apb3DecoderGenerator()
    import BmbInterconnectStdGenerators._
    import Apb3DecoderStdGenerators._

    interconnect.setDefaultArbitration(BmbInterconnectGenerator.STATIC_PRIORITY)

    implicit val cpu = VexRiscvBmbGenerator(
      withJtag = clockCtrl.withDebug,
      debugClockDomain = clockCtrl.debugClockDomain,
      debugAskReset = clockCtrl.doSystemReset,
      config = CpuConfig.minimalWithCsr
    )
    interconnect.setPriority(cpu.iBus, 1)
    interconnect.setPriority(cpu.dBus, 2)

    val plic = addPlic(0xF0000)
    cpu.setExternalInterrupt(plic.interrupt)

    val machineTimer = addMachineTimer(0x08000)
    cpu.setTimerInterrupt(machineTimer.interrupt)

    val ramA = bmbOnChipRam(
      address = 0x80000000l,
      size = 4 KiB,
      dataWidth = 32
    )

    val gpioA = addGpio(
      apbOffset = 0x00000,
      Gpio.Parameter(
        width = 8,
        interrupt = List(0,1)
      )
    )
    plic.dependencies += Dependable(gpioA){
      plic.addInterrupt(gpioA.logic.ctrl.io.interrupt(0), 4)
      plic.addInterrupt(gpioA.logic.ctrl.io.interrupt(1), 5)
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
      cpu.dBus -> List(ramA.bus, peripheralBridge.input),
      cpu.iBus -> List(ramA.bus)
    )
  }
}




object SaxonSocDefault{
  def main(args: Array[String]): Unit = {
    SpinalRtlConfig.generateVerilog(new GeneratorComponent(new SaxonSoc))
  }
}

