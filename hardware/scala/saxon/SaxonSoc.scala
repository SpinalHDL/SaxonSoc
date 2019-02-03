package saxon

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb._
import spinal.lib.bus.misc._
import spinal.lib.bus.simple._
import spinal.lib.com.jtag.Jtag
import spinal.lib.com.spi.SpiMaster
import spinal.lib.com.spi.ddr.{Apb3SpiXdrMasterCtrl, SpiXdrMaster, SpiXdrMasterCtrl, SpiXdrParameter}
import spinal.lib.com.uart._
import spinal.lib.io.{Apb3Gpio2, Gpio, TriStateArray}
import spinal.lib.misc.plic._
import vexriscv.demo.Apb3Rom
import vexriscv.{plugin, _}
import vexriscv.ip.InstructionCacheConfig
import vexriscv.plugin._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

//Class used to store the SoC configure
case class SaxonSocParameters(clkFrequency : HertzNumber,
                              uartBaudRate : Int,
                              withMemoryStage : Boolean,
                              executeRf : Boolean,
                              hardwareBreakpointsCount : Int,
                              gpioA : Gpio.Parameter,
                              uartACtrlConfig : UartCtrlMemoryMappedConfig,
                              flashCtrl: SpiXdrMasterCtrl.MemoryMappingParameters,
                              bootloaderBin : String,
                              withJtag : Boolean){

  def withArgs(args : Seq[String]) = this.copy(

  )

  //Create a VexRiscv configuration from the SoC configuration
  def toVexRiscvConfig() = {
    val config = VexRiscvConfig(
      withMemoryStage = withMemoryStage,
      withWriteBackStage = false,
      List(
        new IBusCachedPlugin(
          resetVector = if(bootloaderBin != null) 0xF001E000l else 0x01100000l,
          withoutInjectorStage = true,
          config = InstructionCacheConfig(
            cacheSize = 8192,
            bytePerLine = 32,
            wayCount = 1,
            addressWidth = 32,
            cpuDataWidth = 32,
            memDataWidth = 32,
            catchIllegalAccess = false,
            catchAccessFault = false,
            catchMemoryTranslationMiss = false,
            asyncTagMemory = false,
            twoCycleRam = false,
            twoCycleCache = false
          )
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
    config
  }
}


object SaxonSocParameters{
  def default = up5kEvnDefault
  def up5kEvnDefault = SaxonSocParameters(
    clkFrequency = 12 MHz,
    uartBaudRate = 115200,
    withMemoryStage = false,
    executeRf = true,
    hardwareBreakpointsCount  = 2,
    gpioA = Gpio.Parameter(
      width = 8,
      interrupt = List(0, 1)
    ),
    withJtag = true,
    bootloaderBin = null,  //"software/bootloader/up5kEvn.bin"
    uartACtrlConfig = UartCtrlMemoryMappedConfig(
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
    ),
    flashCtrl = SpiXdrMasterCtrl.MemoryMappingParameters(
      SpiXdrMasterCtrl.Parameters(
        dataWidth = 8,
        timerWidth = 0,
        spi = SpiXdrParameter(2, 2, 1)
//      ).addFullDuplex(0,2,false),
      ).addFullDuplex(0,2,false).addHalfDuplex(id=1, rate=2, ddr=false, spiWidth=2),
      cmdFifoDepth = 1,
      rspFifoDepth = 1,
      cpolInit = false,
      cphaInit = false,
      modInit = 0,
      sclkToogleInit = 0,
      ssSetupInit = 0,
      ssHoldInit = 0,
      ssDisableInit = 0,
      xipConfigWritable = false,
      xipEnableInit = true,
      xipInstructionEnableInit = true,
      xipInstructionModInit = 0,
      xipAddressModInit = 0,
      xipDummyModInit = 0,
      xipPayloadModInit = 1,
        //      xipInstructionDataInit = 0x0B,
//      xipDummyCountInit = 0,
//      xipDummyDataInit = 0xFF,
      xipInstructionDataInit = 0x3B,
      xipDummyCountInit = 0,
      xipDummyDataInit = 0xFF,
      xip = SpiXdrMasterCtrl.XipBusParameters(addressWidth = 24, dataWidth = 32)
    )
  )
}

//Board agnostic SoC toplevel
case class SaxonSoc(p : SaxonSocParameters) extends Component {
  val io = new Bundle {
    val clk, reset = in Bool()
    val gpioA = master(TriStateArray(p.gpioA.width bits))
    val uartA = master(Uart())
    val flash = master(SpiXdrMaster(p.flashCtrl.ctrl.spi))
    val jtag = p.withJtag generate slave(Jtag())
  }

  val resetCtrlClockDomain = ClockDomain(
    clock = io.clk,
    config = ClockDomainConfig(
      resetKind = BOOT //Bitstream loaded FF
    )
  )

  val resetCtrl = new ClockingArea(resetCtrlClockDomain) {
    val resetUnbuffered  = False

    //Power on reset counter
    val resetCounter = Reg(UInt(8 bits)) init(0)
    when(!resetCounter.andR){
      resetCounter := resetCounter + 1
      resetUnbuffered := True
    }
    when(BufferCC(io.reset)){
      resetCounter := 0
    }

    //Create all reset used later in the design
    val systemResetSet = False
    val debugReset = SB_GB(RegNext(resetUnbuffered))
    val systemReset = SB_GB(RegNext(resetUnbuffered || systemResetSet))
  }

  val debugClockDomain = ClockDomain(
    clock = io.clk,
    reset = resetCtrl.debugReset,
    frequency = FixedFrequency(p.clkFrequency),
    config = ClockDomainConfig(
      resetKind = spinal.core.SYNC
    )
  )

  val systemClockDomain = ClockDomain(
    clock = io.clk,
    reset = resetCtrl.systemReset,
    frequency = FixedFrequency(p.clkFrequency),
    config = ClockDomainConfig(
      resetKind = spinal.core.SYNC
    )
  )

  //There is defined the whole SoC stuff
  val system = new ClockingArea(systemClockDomain) {
    //Define the different memory busses and interconnect that will be use in the SoC
    val interconnect = PipelinedMemoryBusInterconnect()

    val mainBus = PipelinedMemoryBus(addressWidth = 32, dataWidth = 32)
    interconnect.addSlave(mainBus, DefaultMapping)

    val apbMapping = ArrayBuffer[(Apb3, SizeMapping)]()
    val apbBridge = PipelinedMemoryBusToApbBridge(
      apb3Config = Apb3Config(
        addressWidth = 20,
        dataWidth = 32
      ),
      pipelineBridge = false,
      pipelinedMemoryBusConfig = mainBus.config
    )
    interconnect.addSlave(apbBridge.io.pipelinedMemoryBus, SizeMapping(0xF0000000l, 16 MiB))


    //Define slave/peripheral components
    val ram = Spram()
    interconnect.addSlave(ram.io.bus, SizeMapping(0x80000000l,  64 KiB))
//    //Alternatively one may use Bram on boards without SPRAM like hx8k
//    val ram = Bram(onChipRamSize = 8 KiB)
//    interconnect.addSlave(ram.io.bus, SizeMapping(0x80000000l,  8 KiB))


    val xip = new Area {
      val ctrl = Apb3SpiXdrMasterCtrl(p.flashCtrl)
      ctrl.io.spi <> io.flash
      apbMapping += ctrl.io.apb -> (0x1F000, 4 KiB)

      val accessBus = ctrl.io.xip.fromPipelinedMemoryBus()
      interconnect.addSlave(accessBus, SizeMapping(0x01000000l, 16 MiB))
    }

    val bootloader = (p.bootloaderBin != null) generate new Area{
      val bootloader = Apb3Rom(p.bootloaderBin)
      apbMapping += bootloader.io.apb -> (0x1E000, 4 KiB)
    }

    val machineTimer = MachineTimer()
    apbMapping += machineTimer.io.bus -> (0x08000, 4 KiB)

    val gpioACtrl = Apb3Gpio2(p.gpioA)
    apbMapping += gpioACtrl.io.bus -> (0x00000, 4 KiB)
    gpioACtrl.io.gpio <> io.gpioA

    val uartCtrl = Apb3UartCtrl(p.uartACtrlConfig)
    uartCtrl.io.uart <> io.uartA
    apbMapping += uartCtrl.io.apb -> (0x10000, 4 KiB)

    val plic = new Area{
      val apb = Apb3(addressWidth = 16, dataWidth = 32)
      val bus = Apb3SlaveFactory(apb)

      val priorityWidth = 1
      val gateways = ArrayBuffer[PlicGateway]()

      gateways += PlicGatewayActiveHigh(
        source = uartCtrl.io.interrupt,
        id = 1,
        priorityWidth = priorityWidth
      )
//      gateways += PlicGatewayActiveHigh(
//        source = RegNext(uartCtrl.io.interrupt) init(False),
//        id = 2,
//        priorityWidth = priorityWidth
//      )
//      PlicGatewayActiveHigh(
//        source = gpioACtrl.io.interrupt =/= 0,
//        id = 2,
//        priorityWidth = priorityWidth
//      )
//      for(i <- 0 until p.gpioAWidth) gateways += PlicGatewayActiveHigh(
//        source = gpioACtrl.io.interrupt(i),
//        id = 2 + i,
//        priorityWidth = priorityWidth
//      )
      for(i <- p.gpioA.interrupt) gateways += PlicGatewayActiveHigh(
        source = gpioACtrl.io.interrupt(i),
        id = 2 + i,
        priorityWidth = priorityWidth
      )
      val targets = Seq(
        PlicTarget(
          gateways = gateways,
          priorityWidth = priorityWidth
        )
      )

      val plicMapping = PlicMapping.light.copy(
//        gatewayPriorityReadGen = true,
//        gatewayPendingReadGen = true,
//        targetThresholdReadGen = true
      )
      gateways.foreach(_.priority := 1)
      targets.foreach(_.threshold := 0)
//      targets.foreach(_.ie.foreach(_ := True))
      val mapping = PlicMapper(bus, plicMapping)(
        gateways = gateways,
        targets = targets
      )
      apbMapping += apb -> (0xF0000, 64 KiB)
    }


    //Specify which master bus can access to which slave/peripheral
    val dBus = PipelinedMemoryBus(mainBus.config)
    val iBus = PipelinedMemoryBus(mainBus.config)

    interconnect.addMasters(
      dBus   -> List(mainBus),
      iBus   -> List(mainBus),
      mainBus-> List(ram.io.bus, xip.accessBus, apbBridge.io.pipelinedMemoryBus)
    )

    interconnect.setConnector(dBus)((m,s) => {
      m.cmd >> s.cmd
      m.rsp << s.rsp
    })
    interconnect.setConnector(xip.accessBus)((m,s) => {
      m.cmd.halfPipe() >> s.cmd
      m.rsp <-< s.rsp
    })


    val apbDecoder = Apb3Decoder(
      master = apbBridge.io.apb,
      slaves = apbMapping
    )





    //Map the CPU into the SoC depending the Plugins used
    val cpuConfig = p.toVexRiscvConfig()
    p.withJtag generate cpuConfig.add(new DebugPlugin(debugClockDomain, p.hardwareBreakpointsCount))
//    io.jtag.flatten.filter(_.isOutput).foreach(_.assignDontCare())

    val cpu = new VexRiscv(cpuConfig)
    for (plugin <- cpu.plugins) plugin match {
      case plugin : IBusCachedPlugin => iBus << plugin.iBus.toPipelinedMemoryBus()
      case plugin : DBusSimplePlugin => dBus << plugin.dBus.toPipelinedMemoryBus()
      case plugin : CsrPlugin => {
        plugin.externalInterrupt := plic.targets(0).iep //Not used
        plugin.timerInterrupt := machineTimer.io.mTimeInterrupt
      }
      case plugin : DebugPlugin         => plugin.debugClockDomain{
        resetCtrl.systemResetSet setWhen(RegNext(plugin.io.resetOut))
        io.jtag <> plugin.io.bus.fromJtag()
      }
      case _ =>
    }
  }
}


//Scala main used to generate the Up5kArea toplevel
object SaxonSoc {
  def main(args: Array[String]): Unit = {
    SpinalRtlConfig.generateVerilog(SaxonSoc(SaxonSocParameters.default.withArgs(args)))
  }
}