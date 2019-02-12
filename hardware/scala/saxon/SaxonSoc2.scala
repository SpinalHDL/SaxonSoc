package saxon

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb._
import spinal.lib.bus.misc._
import spinal.lib.bus.simple._
import spinal.lib.com.jtag.Jtag
import spinal.lib.com.spi.ddr.{Apb3SpiXdrMasterCtrl, SpiXdrMaster, SpiXdrMasterCtrl, SpiXdrParameter}
import spinal.lib.com.uart._
import spinal.lib.io.{Apb3Gpio2, Gpio, TriStateArray}
import spinal.lib.misc.plic._

import scala.collection.mutable.ArrayBuffer
import experimental._
import vexriscv.ip.InstructionCacheConfig
import vexriscv.plugin.{Plugin => _, _}
import vexriscv.{VexRiscv, VexRiscvConfig, plugin}
case class SaxonSoc2Parameters(plugins : ArrayBuffer[Plugin])


object SaxonSoc2Keys{

//]
}

import SaxonSoc2Keys._

class ExternalClockDomain(clkFrequency : HertzNumber, cd : Key[ClockDomain]) extends Plugin{
  override def products = Seq(cd)

  override lazy val logic = new Area{
    val io = new Bundle {
      val clk, reset = in Bool()
    }

    val resetCtrlClockDomain = ClockDomain(
      clock = io.clk,
      config = ClockDomainConfig(
        resetKind = BOOT
      )
    )

    val ctrl = new ClockingArea(resetCtrlClockDomain) {
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
      val systemReset = SB_GB(RegNext(resetUnbuffered || systemResetSet))

      cd.set(ClockDomain(
        clock = io.clk,
        reset = systemReset,
        frequency = FixedFrequency(clkFrequency),
        config = ClockDomainConfig(
          resetKind = spinal.core.SYNC
        )
      ))
    }
  }
}

class BlinkerPlugin(clockDomain : Key[ClockDomain]) extends Plugin(clockDomain){
  override lazy val logic = new Area {
    val blink = out(RegInit(False))
    blink := !blink
  }
}

class InstancePlugin[T](clockDomain : Key[ClockDomain], key : Key[T], instance : => T) extends Plugin(clockDomain){
  override def products = List(key)
  override lazy val logic = key.set(instance)
}

class PipelinedMemoryBusInterconnectPlugin(clockDomain : Key[ClockDomain], interconnect : Key[PipelinedMemoryBusInterconnect]) extends Plugin(clockDomain){
  override def products = List(interconnect)
  override lazy val logic = interconnect.set(PipelinedMemoryBusInterconnect())
}

class PipelinedMemoryBusToApbBridgePlugin(clockDomain : Key[ClockDomain], input : Key[PipelinedMemoryBus], output : Key[Apb3]) extends Plugin(clockDomain){
  override def dependancies = List(output)
  override def products = List(input)

  override lazy val logic = new Area{
    val bridge = new PipelinedMemoryBusToApbBridge(
      apb3Config = output.config,
      pipelineBridge = false,
      pipelinedMemoryBusConfig = PipelinedMemoryBusConfig(output.config.addressWidth, output.config.dataWidth)
    )

    output << bridge.io.apb
    input.set(bridge.io.pipelinedMemoryBus)
  }
}


class Apb3GpioPlugin(clockDomain : Key[ClockDomain], bus : Key[Apb3], p : Gpio.Parameter) extends Plugin(clockDomain){
  override lazy val logic = new Area{
    val ctrl = Apb3Gpio2(p)
    val gpio = master(TriStateArray(p.width))
    gpio <> ctrl.io.gpio
    bus.set(ctrl.io.bus)
  }
}

class Apb3DecoderPlugin(clockDomain : Key[ClockDomain], mapping : Key[ArrayBuffer[(Apb3, SizeMapping)]], input : Key[Apb3], config : Apb3Config) extends Plugin(clockDomain) {
  override def dependancies = List(mapping)
  override def products = List(input)

  override lazy val logic = new Area {
    val inputBus = Apb3(config)
    val decoder = Apb3Decoder(
      master = inputBus,
      slaves = mapping
    )

    input.set(inputBus)
  }
}



class SpramPlugin(clockDomain : Key[ClockDomain], bus : Key[PipelinedMemoryBus]) extends Plugin(clockDomain){
  override def products = List(bus)
  override lazy val logic = new Area{
    val ram = Spram()
    bus.set(ram.io.bus)
  }
}

class VexRiscvPlugin(clockDomain : Key[ClockDomain], iBusKey : Key[PipelinedMemoryBus], dBusKey : Key[PipelinedMemoryBus], cpuConfig : VexRiscvConfig) extends Plugin(clockDomain){

  override def products = List(iBusKey, dBusKey)

  override lazy val logic = new Area {
    val cpu = new VexRiscv(cpuConfig)
    val iBus = iBusKey.set(PipelinedMemoryBus(32, 32))
    val dBus = dBusKey.set(PipelinedMemoryBus(32, 32))

    for (plugin <- cpu.plugins) plugin match {
      case plugin : IBusCachedPlugin => iBus << plugin.iBus.toPipelinedMemoryBus()
      case plugin : DBusSimplePlugin => dBus << plugin.dBus.toPipelinedMemoryBus()
//      case plugin : CsrPlugin => {
//        plugin.externalInterrupt := plic.targets(0).iep
//        plugin.timerInterrupt := machineTimer.io.mTimeInterrupt
//      }
//      case plugin : DebugPlugin         => plugin.debugClockDomain{
//        resetCtrl.systemResetSet setWhen(RegNext(plugin.io.resetOut))
//        io.jtag <> plugin.io.bus.fromJtag()
//      }
      case _ =>
    }


//    val mainBus = PipelinedMemoryBus(addressWidth = 32, dataWidth = 32)
//    interconnect.addSlave(mainBus, DefaultMapping)
//    interconnect.addMasters(
//      dBus   -> List(mainBus),
//      iBus   -> List(mainBus),
//      mainBus-> List(ram)
//    )
  }
}




//Board agnostic SoC toplevel
case class SaxonSoc2(p : SaxonSoc2Parameters) extends Component {
  val c = new Composable()
  c.plugins ++= p.plugins
  c.build()
}

//Scala main used to generate the Up5kArea toplevel
object SaxonSoc2 {
  def main(args: Array[String]): Unit = {



    SpinalRtlConfig.generateVerilog{
      val withMemoryStage = false
      val executeRf = true
      val hardwareBreakpointsCount  = 2
      val bootloaderBin : String = null

      val vexRiscvConfig = VexRiscvConfig(
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

      val clockDomain = new Key[ClockDomain]
      val interconnect = new Key[PipelinedMemoryBusInterconnect]
      val ram = new Key[PipelinedMemoryBus]
      val iBus, dBus = new Key[PipelinedMemoryBus]
      val apbBridgeInput = new Key[PipelinedMemoryBus]
      val apbDecoderInput = new Key[Apb3]
      val apbMapping = new DefaultKey(ArrayBuffer[(Apb3, SizeMapping)]())
      val gpioABus = new Key[Apb3]
      val gpioBBus = new Key[Apb3]

      val p = SaxonSoc2Parameters(
        plugins = ArrayBuffer(
          new BlinkerPlugin(clockDomain),
          new ExternalClockDomain(12 MHz, clockDomain),
          new PipelinedMemoryBusInterconnectPlugin(clockDomain, interconnect),
          new SpramPlugin(clockDomain, ram),
          new Apb3DecoderPlugin(clockDomain,apbMapping, apbDecoderInput, Apb3Config(20,32)),
          new Apb3GpioPlugin(clockDomain, gpioABus, Gpio.Parameter(8, List(0, 1))).setName("gpioA"),
          new Apb3GpioPlugin(clockDomain, gpioBBus, Gpio.Parameter(8, List(0, 1))).setName("gpioB"),
          new PipelinedMemoryBusToApbBridgePlugin(clockDomain, apbBridgeInput, apbDecoderInput),
          new VexRiscvPlugin(clockDomain, iBus, dBus, vexRiscvConfig),
          new Plugin(clockDomain) {
            override def dependancies = List(interconnect, iBus, dBus, ram, apbBridgeInput)

            override lazy val logic = new Area {
              val mainBus = PipelinedMemoryBus(addressWidth = 32, dataWidth = 32)
              interconnect.addSlave(apbBridgeInput, SizeMapping(0xF0000000l, 16 MiB))
              interconnect.addSlave(ram, SizeMapping(0x80000000l,  64 KiB))
              interconnect.addSlave(mainBus, DefaultMapping)
              interconnect.addMasters(
                (dBus,    List(mainBus)),
                (iBus,    List(mainBus)),
                (mainBus, List(ram, apbBridgeInput))
              )
            }
          },
          new Plugin(clockDomain) {
            override def dependancies = List(gpioABus, gpioBBus)
            override def products = List(apbMapping)
            override lazy val logic = new Area {
              apbMapping += ((gpioABus , (0x00000, 4 KiB)))
              apbMapping += ((gpioBBus , (0x01000, 4 KiB)))
            }
          }
        )
      )

      SaxonSoc2(p)
    }
  }
}
