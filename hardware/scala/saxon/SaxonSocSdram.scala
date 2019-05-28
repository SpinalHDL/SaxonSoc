package saxon

import spinal.core._
import spinal.lib.bus.bmb.{BmbParameter, BmbToApb3Bridge}
import spinal.lib.bus.misc.SizeMapping
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.eda.bench.{Bench, Rtl, XilinxStdTargets}
import spinal.lib.eda.icestorm.IcestormStdTargets
import spinal.lib.generator._
import spinal.lib.io.Gpio
import spinal.lib.memory.sdram.{BmbSdramCtrl, IS42x320D, SdramLayout, SdramTimings}




class SaxonSocSdram extends Generator {
  val clockCtrl = ClockDomainGenerator()

  val system = new SaxonSocLinux() {
    onClockDomain(clockCtrl.clockDomain)

    val sdramA = SdramSdrBmbGenerator(address = 0x80000000l)

    interconnect.addConnection(
      cpu.iBus -> List(sdramA.bmb),
      cpu.dBus -> List(sdramA.bmb)
    )
  }

  def defaultSetting(): this.type = this {
    clockCtrl.makeExternal()
    clockCtrl.clkFrequency.load(50 MHz)
    clockCtrl.powerOnReset.load(true)

    system.uartA.parameter.load(
      UartCtrlMemoryMappedConfig(
        baudrate = 1000000,
        txFifoDepth = 128,
        rxFifoDepth = 128
      )
    )

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