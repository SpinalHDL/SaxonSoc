package saxon

import spinal.core._
import spinal.lib.eda.bench.{Bench, Rtl, XilinxStdTargets}
import spinal.lib.generator._
import spinal.lib.io.Gpio
import spinal.lib.memory.sdram.IS42x320D





class SaxonSocSdram extends Generator {
  val clockCtrl = ClockDomainGenerator().makeExternal()

  val core = new Generator(clockCtrl.clockDomain) {
    implicit val interconnect = BmbInterconnectGenerator()
    implicit val apbDecoder = Apb3DecoderGenerator()

    import Apb3DecoderStdGenerators._
    import BmbInterconnectStdGenerators._

    interconnect.setDefaultArbitration(BmbInterconnectGenerator.STATIC_PRIORITY)

    implicit val cpu = VexRiscvBmbGenerator()
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
    clockCtrl.clkFrequency.load(50 MHz)
    clockCtrl.powerOnReset.load(true)

    core.cpu.config.load(VexRiscvConfigs.linux)
    core.cpu.enableJtag(clockCtrl)

    this
  }
}



object SaxonSocSdram {
  def main(args: Array[String]): Unit = {
    SpinalRtlConfig.generateVerilog(new GeneratorComponent(new SaxonSocSdram().defaultSetting()))
  }
}



object SaxonSocSdramSynthesisBench {
  def main(args: Array[String]) {
    val briey = new Rtl {
      override def getName(): String = "SaxonSoc"

      override def getRtlPath(): String = "SaxonSoc.v"

      SpinalVerilog({
        val soc = new GeneratorComponent(new SaxonSocSdram().defaultSetting()).setDefinitionName(getRtlPath().split("\\.").head)
        soc.generator.clockCtrl.clock.get.setName("clk")
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