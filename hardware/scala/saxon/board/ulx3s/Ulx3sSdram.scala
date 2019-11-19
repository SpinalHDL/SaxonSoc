package saxon.board.ulx3s


import saxon._
import spinal.core._
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.generator._
import spinal.lib.io.{Gpio, InOutWrapper}

import spinal.lib.memory.sdram.SdramGeneration.SDR
import spinal.lib.memory.sdram.SdramLayout
import spinal.lib.memory.sdram.sdr._
import spinal.lib.memory.sdram.sdr.sim.SdramModel

class Ulx3sSdramSystem extends BmbApbVexRiscvGenerator{
  //Add components
  val ramA = BmbOnChipRamGenerator(0x80000000l)
  val sdramA = SdramSdrBmbGenerator(0x90000000l)
  val uartA = Apb3UartGenerator(0x10000)
  val gpioA = Apb3GpioGenerator(0x00000)
  val noReset = Ulx3sNoResetGenerator()

  ramA.dataWidth.load(32)

  //Interconnect specification
  interconnect.addConnection(
    cpu.iBus -> List(ramA.bmb, sdramA.bmb),
    cpu.dBus -> List(ramA.bmb, sdramA.bmb, peripheralBridge.input)
  )

  interconnect.setConnector(sdramA.bmb){(m,s) =>
    m.cmd >-> s.cmd
    m.rsp << s.rsp
  }
}

case class Ulx3sSdramPll() extends BlackBox{
  setDefinitionName("pll_sdram")
  val clkin = in Bool()
  val clkout0 = out Bool()
  val clkout1 = out Bool()
  val locked = out Bool()
}

class Ulx3sSdram extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(255)
  clockCtrl.resetSynchronous.load(false)
  clockCtrl.powerOnReset.load(true)
  clockCtrl.clkFrequency.load(40 MHz)

  val system = new Ulx3sSdramSystem
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val clk_25mhz = in Bool()
    val sdram_clk = out Bool()
    val resetn = in Bool()

    val pll = Ulx3sSdramPll()
    pll.clkin := clk_25mhz
    sdram_clk := pll.clkout0
    clockCtrl.clock.load(pll.clkout1)
    clockCtrl.reset.load(resetn)
  }
}

object Ulx3sSdramSystem{
  def default(g : Ulx3sSdramSystem, clockCtrl : ClockDomainGenerator) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.minimal)
    cpu.enableJtag(clockCtrl)

    ramA.size.load(32 KiB)
    ramA.hexInit.load("software/standalone/memTest/build/memTest.hex")

    sdramA.layout.load(MT48LC16M16A2.layout)
    sdramA.timings.load(MT48LC16M16A2.timingGrade7)

    uartA.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 32,
      rxFifoDepth = 32
    )

    gpioA.parameter load Gpio.Parameter(width = 8)

    g
  }
}


object Ulx3sSdram {
  //Function used to configure the SoC
  def default(g : Ulx3sSdram) = g{
    import g._
    Ulx3sSdramSystem.default(system, clockCtrl)
    clockCtrl.resetSensitivity.load(ResetSensitivity.LOW)
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(InOutWrapper(default(new Ulx3sSdram()).toComponent()))
    BspGenerator("Ulx3sSdram", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
  }

}

