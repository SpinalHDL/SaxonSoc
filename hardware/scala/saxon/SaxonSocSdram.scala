package saxon

import spinal.core._
import spinal.lib.bus.bmb.{BmbParameter, BmbToApb3Bridge}
import spinal.lib.bus.misc.SizeMapping
import spinal.lib.eda.bench.{Bench, Rtl, XilinxStdTargets}
import spinal.lib.eda.icestorm.IcestormStdTargets
import spinal.lib.generator._
import spinal.lib.io.Gpio
import spinal.lib.memory.sdram.{BmbSdramCtrl, IS42x320D, SdramLayout, SdramTimings}





case class SdramSdrBmbGenerator(address: BigInt)
                               (implicit interconnect: BmbInterconnectGenerator) extends Generator {

  val layout = newDependency[SdramLayout]
  val timings = newDependency[SdramTimings]
  val requirements = newDependency[BmbParameter]

  val bmb   =   produce(logic.io.bmb)
  val sdram = produceIo(logic.io.sdram)

  layout.produce{
    interconnect.addSlave(
      capabilities = BmbSdramCtrl.bmbCapabilities(layout),
      requirements = requirements,
      bus = bmb,
      mapping = SizeMapping(address, layout.capacity)
    )
  }

  val logic = add task BmbSdramCtrl(
    bmbParameter = requirements,
    layout = layout,
    timing = timings,
    CAS = 3
  )
}


case class  BmbToApb3Decoder(address : BigInt)
                            (implicit interconnect: BmbInterconnectGenerator, apbDecoder : Apb3DecoderGenerator) extends Generator {
  val input = produce(logic.bridge.io.input)
  val requirements = newDependency[BmbParameter]

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
}

class SaxonSocSdram extends Generator {
  val clockCtrl = ClockDomainGenerator()

  val system = new Generator(clockCtrl.clockDomain) {
    implicit val interconnect = BmbInterconnectGenerator()
    implicit val apbDecoder = Apb3DecoderGenerator()

    interconnect.setDefaultArbitration(BmbInterconnectGenerator.STATIC_PRIORITY)

    implicit val cpu = VexRiscvBmbGenerator()
    interconnect.setPriority(cpu.iBus, 1)
    interconnect.setPriority(cpu.dBus, 2)

    val sdramA = SdramSdrBmbGenerator(address = 0x80000000l)

    val plic = Apb3PlicGenerator(0xF0000)
    cpu.setExternalInterrupt(plic.interrupt)

    val machineTimer = Apb3MachineTimerGenerator(0x08000)
    cpu.setTimerInterrupt(machineTimer.interrupt)


    val gpioA = Apb3GpioGenerator(
      apbOffset = 0x00000,
      Gpio.Parameter(
        width = 8,
        interrupt = List(0, 1)
      )
    )

    plic.addInterrupt(source = gpioA.produce(gpioA.logic.io.interrupt(0)), id = 4)
    plic.addInterrupt(source = gpioA.produce(gpioA.logic.io.interrupt(1)), id = 5)

    val uartA = Apb3UartGenerator(
      apbOffset = 0x10000,
      baudrate = 1000000,
      txFifoDepth = 128,
      rxFifoDepth = 128
    )
    plic.addInterrupt(source = uartA.interrupt, id = 1)

    val peripheralBridge = BmbToApb3Decoder(address = 0x10000000)


    interconnect.addConnection(
      cpu.iBus -> List(sdramA.bmb),
      cpu.dBus -> List(sdramA.bmb, peripheralBridge.input)
    )
  }

  def defaultSetting(): this.type = this {
    clockCtrl.makeExternal()
    clockCtrl.clkFrequency.load(50 MHz)
    clockCtrl.powerOnReset.load(true)


    system.sdramA.layout load(IS42x320D.layout)
    system.sdramA.timings load(IS42x320D.timingGrade7)

    system.cpu.config.load(VexRiscvConfigs.linux)
    system.cpu.enableJtag(clockCtrl)

    this
  }
}






object SaxonSocSdram {
  def main(args: Array[String]): Unit = {
    SpinalRtlConfig.generateVerilog(new SaxonSocSdram().defaultSetting().toComponent())
  }
}



object SaxonSocSdramSynthesisBench {
  def main(args: Array[String]) {
    val briey = new Rtl {
      override def getName(): String = "SaxonSoc"

      override def getRtlPath(): String = "SaxonSoc.v"

      SpinalConfig(inlineRom = true).generateVerilog({
        val soc = new SaxonSocSdram().defaultSetting().toComponent().setDefinitionName(getRtlPath().split("\\.").head)
        soc.generator.clockCtrl.clock.get.setName("clk")
        soc
      })
    }


    val rtls = List(briey)

    val targets = IcestormStdTargets().take(1)/*XilinxStdTargets(
      vivadoArtix7Path = "/media/miaou/HD/linux/Xilinx/Vivado/2018.3/bin"
    ) *//*++ AlteraStdTargets(
      quartusCycloneIVPath = "/media/miaou/HD/linux/intelFPGA_lite/18.1/quartus/bin",
      quartusCycloneVPath = "/media/miaou/HD/linux/intelFPGA_lite/18.1/quartus/bin"
    )*/

    Bench(rtls, targets, "/media/miaou/HD/linux/tmp")
  }
}