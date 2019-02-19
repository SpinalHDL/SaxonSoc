package saxon

import java.awt
import java.awt.event.{ActionEvent, ActionListener}

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
import javax.swing.{BoxLayout, JButton, JFrame}
import spinal.core.internals.classNameOf
import spinal.core.sim.{SimConfig, fork, sleep}
import spinal.lib.com.jtag.sim.JtagTcp
import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}
import vexriscv.ip.InstructionCacheConfig
import vexriscv.plugin.{Plugin => _, _}
import vexriscv.test.{JLedArray, JSwitchArray}
import vexriscv.{VexRiscv, VexRiscvConfig, plugin}

import scala.collection.mutable
import scala.runtime.Nothing$



case class ExternalClockDomain(clkFrequency : Handle[HertzNumber] = Unset, withDebug : Handle[Boolean] = Unset) extends Plugin{
  val systemClockDomain = Handle[ClockDomain]
  val debugClockDomain = Handle[ClockDomain]
  val doSystemReset = Handle[() => Unit]

  dependencies ++= List(clkFrequency, withDebug)

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
    val systemReset = SB_GB(RegNext(resetUnbuffered || BufferCC(systemResetSet)))
    doSystemReset.set(() => systemResetSet := True)

    systemClockDomain.set(ClockDomain(
      clock = io.clk,
      reset = systemReset,
      frequency = FixedFrequency(clkFrequency),
      config = ClockDomainConfig(
        resetKind = spinal.core.SYNC
      )
    ))


    val debug = withDebug.get generate new Area {
      val reset = SB_GB(RegNext(resetUnbuffered))
      debugClockDomain set (ClockDomain(
        clock = io.clk,
        reset = reset,
        frequency = FixedFrequency(clkFrequency),
        config = ClockDomainConfig(
          resetKind = spinal.core.SYNC
        )
      ))
    }
  }

  def connectTo(s : BaseSoc): this.type ={
    s on(this.systemClockDomain)
    s.cpu.debugClockDomain.setFrom(this.debugClockDomain)
    s.cpu.debugAskReset.setFrom(this.doSystemReset)
    withDebug.setFrom(s.cpu.withJtag)
    this
  }
}

case class BlinkerPlugin() extends Plugin{
  val content = add task new Area {
    val blink = out(RegInit(False))
    blink := !blink
  }
}

class InstancePlugin[T](key : Handle[T], instance : => T) extends Plugin{
  val logic = add task key.set(instance)
}

class PipelinedMemoryBusInterconnectPlugin() extends Plugin{
  val factory = Handle[PipelinedMemoryBusInterconnect]
  val logic = add task factory.set(PipelinedMemoryBusInterconnect())
}

class PipelinedMemoryBusToApbBridgePlugin(output : Handle[Apb3]) extends Plugin{
  dependencies += output

  val input = Handle[PipelinedMemoryBus]
  val logic = add task new Area{
    val bridge = new PipelinedMemoryBusToApbBridge(
      apb3Config = output.config,
      pipelineBridge = false,
      pipelinedMemoryBusConfig = PipelinedMemoryBusConfig(output.config.addressWidth, output.config.dataWidth)
    )

    output << bridge.io.apb
    input.set(bridge.io.pipelinedMemoryBus)
  }
}


class Apb3GpioPlugin(p : Gpio.Parameter) extends Plugin{
  val bus = Handle[Apb3]
  val logic = add task new Area{
    val ctrl = Apb3Gpio2(p)
    val gpio = master(TriStateArray(p.width))
    gpio <> ctrl.io.gpio
    bus.set(ctrl.io.bus)
  }
}

class Apb3DecoderPlugin(config : Handle[Apb3Config] = Unset) extends Plugin {
  val mapping = Handle(ArrayBuffer[(Apb3, SizeMapping)]())
  val input = Handle[Apb3]

  dependencies ++= List(mapping, config)
  val logic = add task new Area {
    val inputBus = Apb3(config)
    val decoder = Apb3Decoder(
      master = inputBus,
      slaves = mapping
    )

    input.set(inputBus)
  }
}



class SpramPlugin() extends Plugin{
  val bus = Handle[PipelinedMemoryBus]
  val logic = add task new Area{
    val ram = Spram()
    bus.set(ram.io.bus)
  }
}


class VexRiscvPlugin(val config : Handle[VexRiscvConfig] = Unset,
                     val withJtag : Handle[Boolean] = Unset,
                     val debugClockDomain : Handle[ClockDomain] = Unset,
                     val debugAskReset : Handle[() => Unit] = Unset) extends Plugin{
  val iBus, dBus = Handle[PipelinedMemoryBus]
  val externalInterrupt, timerInterrupt = Handle[Bool]

  dependencies ++= List(config)
  dependencies += Dependable(withJtag){
    if(withJtag.value) dependencies ++= List(debugClockDomain, debugAskReset)
  }

  val jtag = add task (withJtag.get generate slave(Jtag()))
  val logic = add task new Area {
    withJtag.get generate new Area {
      config.add(new DebugPlugin(debugClockDomain, 2))
    }

    val cpu = new VexRiscv(config)
    val iBus = VexRiscvPlugin.this.iBus.set(PipelinedMemoryBus(32, 32))
    val dBus = VexRiscvPlugin.this.dBus.set(PipelinedMemoryBus(32, 32))

    for (plugin <- cpu.plugins) plugin match {
      case plugin : IBusCachedPlugin => iBus << plugin.iBus.toPipelinedMemoryBus()
      case plugin : DBusSimplePlugin => dBus << plugin.dBus.toPipelinedMemoryBus()
      case plugin : CsrPlugin => {
        externalInterrupt.set(plugin.externalInterrupt)
        timerInterrupt.set(plugin.timerInterrupt)
      }
      case plugin : DebugPlugin         => plugin.debugClockDomain{
        when(RegNext(plugin.io.resetOut)) { debugAskReset.get() }
        jtag.value <> plugin.io.bus.fromJtag()
      }
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


class ComposableComponent extends Component{
  val c = new Composable()
  addPrePopTask(() => c.build())
  def register[T <: Plugin](that : T) : T = {
    c.plugins += that
    that
  }
}




class PluginComponent[T <: Plugin](val plugin : T) extends Component{
  val c = new Composable()
  c.plugins += plugin
  c.build()
  plugin.setName("")
  this.setDefinitionName(classNameOf(plugin))
}



object CpuConfig{
  val withMemoryStage = false
  val executeRf = true
  val hardwareBreakpointsCount  = 2
  val bootloaderBin : String = null

  def minimal = VexRiscvConfig(
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


object SpiFlashXipPlugin{
  def default = SpiXdrMasterCtrl.MemoryMappingParameters(
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
}

class SpiFlashXipPlugin(p : SpiXdrMasterCtrl.MemoryMappingParameters) extends Plugin {

  val flash = add task master(SpiXdrMaster(p.ctrl.spi))
  val accessBus = Handle[PipelinedMemoryBus]
  val logic = add task new Area {
    val ctrl = Apb3SpiXdrMasterCtrl(p)
    ctrl.io.spi <> flash
    accessBus.set(ctrl.io.xip.fromPipelinedMemoryBus())
  }
}


class BaseSoc extends Plugin{
  val cpu = new VexRiscvPlugin()
  val interconnect = new PipelinedMemoryBusInterconnectPlugin()
  val apbDecoder = new Apb3DecoderPlugin(Apb3Config(20,32))
  val apbBridge = new PipelinedMemoryBusToApbBridgePlugin(apbDecoder.input)

  val interconnectMapping = new Plugin{
    dependencies ++= List(cpu, apbBridge)
    add task {
      interconnect.factory.addSlave( apbBridge.input, SizeMapping(0xF0000000l,  16 MiB))
      interconnect.factory.addMasters(
        (cpu.iBus,    List()),
        (cpu.dBus,    List(apbBridge.input))
      )
    }
  }


  def addGpio(apbOffset : Int, p : Gpio.Parameter) = this add new Plugin{
    locks += apbDecoder

    val logic = add task new Area {
      val ctrl = Apb3Gpio2(p)
      val gpio = master(TriStateArray(p.width))
      gpio <> ctrl.io.gpio

      apbDecoder.mapping += (ctrl.io.bus -> (apbOffset, 4 KiB))
    }
  }

  def addUart(apbOffset : Int, p : UartCtrlMemoryMappedConfig) = this add new Plugin{
    locks += apbDecoder

    val logic = add task new Area {
      val ctrl = Apb3UartCtrl(p)
      val uart = master(Uart())
      uart <> ctrl.io.uart

      apbDecoder.mapping += (ctrl.io.apb -> (apbOffset, 4 KiB))
    }
  }


  def addIce40Spram() = this add new Plugin{
    dependencies += cpu

    val logic = add task new Area {
      val ram = Spram()
      interconnect.factory.addSlave(ram.io.bus, SizeMapping(0x80000000l,  64 KiB))
      interconnect.factory.addConnection(cpu.iBus, ram.io.bus)
      interconnect.factory.addConnection(cpu.dBus, ram.io.bus)
    }
  }

  def addMachineTimer() = this add new Plugin{
    dependencies += cpu
    locks += apbDecoder

    val logic = add task new Area {
      val machineTimer = MachineTimer()
      apbDecoder.mapping += (machineTimer.io.bus -> (0x08000, 4 KiB))
      cpu.timerInterrupt := machineTimer.io.mTimeInterrupt
      cpu.externalInterrupt default(False)
    }
  }

  def addSpiXip(p : SpiXdrMasterCtrl.MemoryMappingParameters) = this add new Plugin{
    dependencies += cpu
    locks += apbDecoder

    val logic = add task new Area {
      val flash = master(SpiXdrMaster(p.ctrl.spi))
      val ctrl = Apb3SpiXdrMasterCtrl(p)
      ctrl.io.spi <> flash
      val accessBus = ctrl.io.xip.fromPipelinedMemoryBus()
      apbDecoder.mapping += (ctrl.io.apb -> (0x1F000, 4 KiB))

      interconnect.factory.addSlave(accessBus, SizeMapping(0x01000000l, 16 MiB))
      interconnect.factory.addConnection(cpu.iBus, accessBus)
      interconnect.factory.addConnection(cpu.dBus, accessBus)
    }
  }

  def addSimplePlic() = this add new Plugin {
    val gateways = Handle(ArrayBuffer[PlicGateway]())
    val priorityWidth = 1

    dependencies ++= List(cpu, gateways)
    locks += apbDecoder

    def addInterrupt[T <: Plugin](sourcePlugin : T, id : Int)(sourceAccess : T => Bool) = {
      sourcePlugin.locks += this
      sourcePlugin.add task {
        gateways += PlicGatewayActiveHigh(
          source = sourceAccess(sourcePlugin),
          id = id,
          priorityWidth = priorityWidth
        )
      }
    }

    val logic = add task new Area{
      val apb = Apb3(addressWidth = 16, dataWidth = 32)
      val bus = Apb3SlaveFactory(apb)

      val targets = Seq(
        PlicTarget(
          gateways = gateways,
          priorityWidth = priorityWidth
        )
      )

      val plicMapping = PlicMapping.light
      gateways.foreach(_.priority := 1)
      targets.foreach(_.threshold := 0)
      //      targets.foreach(_.ie.foreach(_ := True))
      val mapping = PlicMapper(bus, plicMapping)(
        gateways = gateways,
        targets = targets
      )
      apbDecoder.mapping += (apb -> (0xF0000, 64 KiB))
      cpu.externalInterrupt := targets(0).iep
    }
  }
}


class CustomSoc extends Plugin{
  val system = new BaseSoc()
  system.cpu.config.set(CpuConfig.minimalWithCsr)
  system.cpu.withJtag.set(true)

  val clockCtrl = ExternalClockDomain(12 MHz).connectTo(system)

  val ram = system.addIce40Spram()

  val gpioA = system.addGpio(
    apbOffset = 0x00000,
    Gpio.Parameter(
      width = 12,
      interrupt = List(0, 1)
    )
  )
  val gpioB = system.addGpio(
    apbOffset = 0x01000,
    Gpio.Parameter(
      width = 12,
      interrupt = List(0, 1)
    )
  )

  val uartA = system.addUart(
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

  val xip = system.addSpiXip(SpiFlashXipPlugin.default)

  val plic = system.addSimplePlic()
  plic.addInterrupt(gpioA, 2)(_.logic.ctrl.io.interrupt(0))

  val machineTimer = system.addMachineTimer()

}


object CustomSoc{
  def main(args: Array[String]): Unit = {
    SpinalVerilog(new PluginComponent(new CustomSoc))
  }
}

object CustomSocSim{
  import spinal.core.sim._
  def main(args: Array[String]): Unit = {
//    val flashBin = "software/standalone/blinkAndEcho/build/blinkAndEcho.bin"
    val flashBin = "software/zephyr/demo/build/zephyr/zephyr.bin"

    SimConfig.addRtl("test/common/up5k_cells_sim.v").compile(new PluginComponent(new CustomSoc)).doSimUntilVoid("test", seed = 42){dut =>
      import dut.plugin._
      val systemClkPeriod = (1e12/clockCtrl.clkFrequency.toDouble).toLong
      val jtagClkPeriod = systemClkPeriod*4
      val uartBaudRate = 115200
      val uartBaudPeriod = (1e12/uartBaudRate).toLong

      val clockDomain = ClockDomain(clockCtrl.io.clk, clockCtrl.io.reset)
      clockDomain.forkStimulus(systemClkPeriod)
      //      clockDomain.forkSimSpeedPrinter(4)

      val tcpJtag = JtagTcp(
        jtag = system.cpu.jtag,
        jtagClkPeriod = jtagClkPeriod
      )

      val uartTx = UartDecoder(
        uartPin = uartA.logic.uart.txd,
        baudPeriod = uartBaudPeriod
      )

      val uartRx = UartEncoder(
        uartPin = uartA.logic.uart.rxd,
        baudPeriod = uartBaudPeriod
      )

      val flash = FlashModel(xip.logic.flash, clockDomain)
      if(flashBin != null) flash.loadBinary(flashBin, 0x100000)

      val guiThread = fork{
        val guiToSim = mutable.Queue[Any]()

        var ledsValue = 0l
        var switchValue : () => BigInt = null
        val ledsFrame = new JFrame{
          setLayout(new BoxLayout(getContentPane, BoxLayout.Y_AXIS))

          add(new JLedArray(8){
            override def getValue = ledsValue
          })
          add{
            val switches = new JSwitchArray(8)
            switchValue = switches.getValue
            switches
          }

          add(new JButton("Reset"){
            addActionListener(new ActionListener {
              override def actionPerformed(actionEvent: ActionEvent): Unit = {
                println("ASYNC RESET")
                guiToSim.enqueue("asyncReset")
              }
            })
            setAlignmentX(awt.Component.CENTER_ALIGNMENT)
          })
          setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
          pack()
          setVisible(true)

        }

        //Slow refresh
        while(true){
          sleep(systemClkPeriod*50000)

          val dummy = if(guiToSim.nonEmpty){
            val request = guiToSim.dequeue()
            if(request == "asyncReset"){
              clockDomain.reset #= true
              sleep(systemClkPeriod*32)
              clockDomain.reset #= false
            }
          }

          gpioA.logic.gpio.read #= (gpioA.logic.gpio.write.toLong & gpioA.logic.gpio.writeEnable.toLong) | (switchValue())
          ledsValue = gpioA.logic.gpio.write.toLong
          ledsFrame.repaint()
        }
      }
    }
  }
}
