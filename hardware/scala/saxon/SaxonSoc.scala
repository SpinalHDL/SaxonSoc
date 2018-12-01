package saxon

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb.{Apb3, Apb3Config, Apb3Decoder}
import spinal.lib.bus.misc._
import spinal.lib.bus.simple._
import spinal.lib.com.jtag.Jtag
import spinal.lib.com.spi.SpiMaster
import spinal.lib.com.spi.ddr.{Apb3SpiXdrMasterCtrl, SpiXdrMaster, SpiXdrMasterCtrl, SpiXdrParameter}
import spinal.lib.com.uart.Uart
import spinal.lib.io.TriStateArray
import vexriscv.demo.Apb3Rom
import vexriscv.{plugin, _}
import vexriscv.ip.InstructionCacheConfig
import vexriscv.plugin._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

//Class used to store the SoC configur
case class SaxonSocParameters(ioClkFrequency : HertzNumber,
                              ioSerialBaudRate : Int,
                              withMemoryStage : Boolean = false,
                              hardwareBreakpointsCount : Int = 2,
                              gpioAWidth : Int = 8,
                              flashCtrl: SpiXdrMasterCtrl.MemoryMappingParameters = SpiXdrMasterCtrl.MemoryMappingParameters(
                                SpiXdrMasterCtrl.Parameters(
                                  dataWidth = 8,
                                  timerWidth = 4,
                                  spi = SpiXdrParameter(2, 2, 1)
                                ).addFullDuplex(0,1,false),
                                cmdFifoDepth = 2,
                                rspFifoDepth = 2,
                                xip = SpiXdrMasterCtrl.XipBusParameters(addressWidth = 24, dataWidth = 32)
                              )){

  def withArgs(args : Seq[String]) = this.copy(

  )

  //Create a VexRiscv configuration from the SoC configuration
  def toVexRiscvConfig() = {
    val config = VexRiscvConfig(
      withMemoryStage = withMemoryStage,
      withWriteBackStage = false,
      List(
        new IBusCachedPlugin(
          resetVector = 0xF001E000l,
          config = InstructionCacheConfig(
            cacheSize = 4096,
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
          readInExecute = true,
          syncUpdateOnStall = true
        ),
        new IntAluPlugin,
        new SrcPlugin(
          separatedAddSub = false,
          executeInsertion = true,
          decodeAddSub = false
        ),
        new LightShifterPlugin(),
        new BranchPlugin(
          earlyBranch = true,
          catchAddressMisaligned = false,
          fenceiGenAsAJump = true
        ),
        new HazardSimplePlugin(
          bypassExecute = false,
          bypassWriteBackBuffer = false
        ),
        new CsrPlugin(new CsrPluginConfig(
          catchIllegalAccess = true,
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
      )
    )
    config
  }
}

//Board agnostic SoC toplevel
case class SaxonSoc(p : SaxonSocParameters) extends Component {
  val io = new Bundle {
    val clk, reset = in Bool()
    val gpioA = master(TriStateArray(p.gpioAWidth bits))
    val uartA = master(Uart())
    val flash = master(SpiXdrMaster(p.flashCtrl.ctrl.spi))
    val jtag = slave(Jtag())
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
    val resetCounter = Reg(UInt(6 bits)) init(0)
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
    frequency = FixedFrequency(p.ioClkFrequency),
    config = ClockDomainConfig(
      resetKind = spinal.core.SYNC
    )
  )

  val systemClockDomain = ClockDomain(
    clock = io.clk,
    reset = resetCtrl.systemReset,
    frequency = FixedFrequency(p.ioClkFrequency),
    config = ClockDomainConfig(
      resetKind = spinal.core.SYNC
    )
  )

  //There is defined the whole SoC stuff
  val system = new ClockingArea(systemClockDomain) {

    val mainBusConfig = PipelinedMemoryBusConfig(
      addressWidth = 20,
      dataWidth = 32
    )

    //Define the different memory busses and interconnect that will be use in the SoC
    val interconnect = PipelinedMemoryBusInterconnect()

    val dBus = PipelinedMemoryBus(mainBusConfig)
    val iBus = PipelinedMemoryBus(mainBusConfig)
    val mainBus = PipelinedMemoryBus(mainBusConfig)
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
    interconnect.addSlave(apbBridge.io.pipelinedMemoryBus, SizeMapping(0xF0000000l, 16 MB))


    //Define slave/peripheral components
    val ram = Spram()
    interconnect.addSlave(ram.io.bus, SizeMapping(0x80000000,  64 kB))

    val xip = new Area {
      val ctrl = Apb3SpiXdrMasterCtrl(p.flashCtrl)
      ctrl.io.spi <> io.flash
      apbMapping += ctrl.io.apb -> (0x1F000, 4 kB)

      val accessBus = ctrl.io.xip.fromPipelinedMemoryBus()
      interconnect.addSlave(accessBus, SizeMapping(0x01000000l, 16 MB))

      val bootloader = Apb3Rom("src/main/c/murax/xipBootloader/crt.bin")
      apbMapping += bootloader.io.apb -> (0x1E000, 4 kB)
    }


    val machineTimer = MachineTimer()
    apbMapping += machineTimer.io.bus -> (0x08000, 4 kB)

    //Specify which master bus can access to which slave/peripheral
    interconnect.addMasters(
      dBus   -> List(mainBus),
      iBus   -> List(mainBus,    xip.accessBus),
      mainBus-> List(ram.io.bus, xip.accessBus, apbBridge.io.pipelinedMemoryBus)
    )

    val apbDecoder = Apb3Decoder(
      master = apbBridge.io.apb,
      slaves = apbMapping
    )


    //Map the CPU into the SoC depending the Plugins used
    val cpuConfig = p.toVexRiscvConfig()
    cpuConfig.add(new DebugPlugin(debugClockDomain, p.hardwareBreakpointsCount))

    val cpu = new VexRiscv(cpuConfig)
    for (plugin <- cpu.plugins) plugin match {
      case plugin : IBusCachedPlugin => iBus << plugin.iBus.toPipelinedMemoryBus()
      case plugin : DBusSimplePlugin => dBus << plugin.dBus.toPipelinedMemoryBus()
      case plugin : CsrPlugin => {
        plugin.externalInterrupt := False //Not used
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
    SpinalRtlConfig.generateVerilog(SaxonSoc(SaxonSocParameters(
      ioClkFrequency = 12 MHz,
      ioSerialBaudRate = 115200
    ).withArgs(args)))
  }
}