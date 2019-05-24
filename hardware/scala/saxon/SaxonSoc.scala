package saxon

import spinal.core._
import spinal.lib._
import spinal.core.internals.classNameOf
import spinal.lib.bus.amba3.apb.{Apb3, Apb3Config, Apb3Decoder, Apb3SlaveFactory}
import spinal.lib.bus.bmb._
import spinal.lib.bus.misc.{BusSlaveFactory, SizeMapping}
import spinal.lib.com.jtag.Jtag
import spinal.lib.com.uart._
import spinal.lib.eda.bench.{AlteraStdTargets, Bench, Rtl, XilinxStdTargets}
import spinal.lib.io.{Apb3Gpio2, Gpio, TriStateArray}
import spinal.lib.memory.sdram.{BmbSdramCtrl, IS42x320D, SdramLayout, SdramTimings}
import spinal.lib.misc.HexTools
import spinal.lib.generator.{BmbInterconnectGenerator, Composable, Dependable, Generator, GeneratorComponent, Handle, Unset}
import spinal.lib.misc.plic.{PlicGateway, PlicGatewayActiveHigh, PlicMapper, PlicMapping, PlicTarget}
import vexriscv.plugin._
import vexriscv._
import vexriscv.ip.{DataCacheConfig, InstructionCacheConfig}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer





class SaxonSoc extends Generator {
  val clockCtrl = ExternalClockDomain()

  val core = new Generator(clockCtrl.systemClockDomain) {
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

    val sdramA = addSdramSdrCtrl(
      address = 0x80000000l,
      layout = IS42x320D.layout,
      timings = IS42x320D.timingGrade7
    )

    val gpioA = addGpio(
      apbOffset = 0x00000,
      Gpio.Parameter(
        width = 8,
        interrupt = List(0, 1)
      )
    )

    plic.dependencies += Dependable(gpioA) {
      plic.addInterrupt(source = gpioA.logic.io.interrupt(0), id = 4)
      plic.addInterrupt(source = gpioA.logic.io.interrupt(1), id = 5)
    }

    val uartA = addBasicUart(
      apbOffset = 0x10000,
      baudrate = 1000000,
      txFifoDepth = 128,
      rxFifoDepth = 128
    )
    plic.addInterrupt(source = uartA.interrupt, id = 1)

    val peripheralBridge = bmbToApb3Decoder(address = 0x10000000)


    interconnect.addConnection(
      cpu.iBus -> List(sdramA.bmb),
      cpu.dBus -> List(sdramA.bmb, peripheralBridge.input)
    )
  }

  def defaultSetting(): this.type = {
    clockCtrl.withDebug.load(true)
    clockCtrl.clkFrequency.load(50 MHz)
    core.cpu.config.load(VexRiscvConfigs.linux)
    this
  }
}


class SaxonSocEmbedded extends Generator {
  val clockCtrl = ExternalClockDomain()

  val core = new Generator(clockCtrl.systemClockDomain) {
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

    val ramA = bmbOnChipRam(
      address = 0x80000000l,
      size = 32 KiB,
      dataWidth = 32
    )

    val gpioA = addGpio(
      apbOffset = 0x00000,
      Gpio.Parameter(
        width = 8,
        interrupt = List(0, 1)
      )
    )

    plic.dependencies += Dependable(gpioA) {
      plic.addInterrupt(source = gpioA.logic.io.interrupt(0), id = 4)
      plic.addInterrupt(source = gpioA.logic.io.interrupt(1), id = 5)
    }

    val uartA = addBasicUart(
      apbOffset = 0x10000,
      baudrate = 1000000,
      txFifoDepth = 128,
      rxFifoDepth = 128
    )
    plic.addInterrupt(source = uartA.interrupt, id = 1)

    val peripheralBridge = bmbToApb3Decoder(address = 0x10000000)


    interconnect.addConnection(
      cpu.iBus -> List(ramA.bmb),
      cpu.dBus -> List(ramA.bmb, peripheralBridge.input)
    )
  }

  def defaultSetting(): this.type = {
    clockCtrl.withDebug.load(true)
    clockCtrl.clkFrequency.load(50 MHz)
    core.cpu.config.load(VexRiscvConfigs.linux)
    this
  }
}


object SaxonSocDefault {
  def main(args: Array[String]): Unit = {
    SpinalRtlConfig.generateVerilog(new GeneratorComponent(new SaxonSoc().defaultSetting()))
  }
}


object SaxonSynthesisBench {
  def main(args: Array[String]) {
    val briey = new Rtl {
      override def getName(): String = "SaxonSoc"

      override def getRtlPath(): String = "SaxonSoc.v"

      SpinalVerilog({
        val soc = new GeneratorComponent(new SaxonSoc().defaultSetting()).setDefinitionName(getRtlPath().split("\\.").head)
        soc.generator.clockCtrl.io.clk.setName("clk")
        soc
      })
    }


    val rtls = List(briey)

    val targets = XilinxStdTargets(
      vivadoArtix7Path = "/media/miaou/HD/linux/Xilinx/Vivado/2018.3/bin"
    ) /*++ AlteraStdTargets(
      quartusCycloneIVPath = "/media/miaou/HD/linux/intelFPGA_lite/18.1/quartus/bin",
      quartusCycloneVPath = "/media/miaou/HD/linux/intelFPGA_lite/18.1/quartus/bin"
    )*/

    Bench(rtls, targets, "/media/miaou/HD/linux/tmp")
  }
}