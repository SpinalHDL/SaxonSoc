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
import saxon.board.blackice.peripheral.{Apb3SpiMasterGenerator}
import spinal.lib.com.spi._

class Ulx3sSdCardSystem extends BmbApbVexRiscvGenerator{
  //Add components
  val ramA = BmbOnChipRamGenerator(0x80000000l)
  val sdramA = SdramSdrBmbGenerator(0x90000000l)
  val uartA = Apb3UartGenerator(0x10000)
  val gpioA = Apb3GpioGenerator(0x00000)
  val spiA = Apb3SpiMasterGenerator(0x20000)
  val spiB = Apb3SpiMasterGenerator(0x30000)
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

case class Ulx3sSdCardPll() extends BlackBox{
  setDefinitionName("pll_sdcard")
  val clkin = in Bool()
  val clkout0 = out Bool()
  val clkout1 = out Bool()
  val locked = out Bool()
}

class Ulx3sSdCard extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(255)
  clockCtrl.resetSynchronous.load(false)
  clockCtrl.powerOnReset.load(true)
  clockCtrl.clkFrequency.load(40 MHz)

  val system = new Ulx3sSdCardSystem
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val clk_25mhz = in Bool()
    val sdram_clk = out Bool()
    val resetn = in Bool()
    val wifi_en = out Bool()

    wifi_en := False

    val pll = Ulx3sSdCardPll()
    pll.clkin := clk_25mhz
    sdram_clk := pll.clkout0
    clockCtrl.clock.load(pll.clkout1)
    clockCtrl.reset.load(resetn)
  }
}

object Ulx3sSdCardSystem{
  def default(g : Ulx3sSdCardSystem, clockCtrl : ClockDomainGenerator) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.minimal)
    cpu.enableJtag(clockCtrl)

    ramA.size.load(32 KiB)
    ramA.hexInit.load("software/standalone/readSdcard/build/readSdcard.hex")

    sdramA.layout.load(MT48LC16M16A2.layout)
    sdramA.timings.load(MT48LC16M16A2.timingGrade7)

    uartA.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 32,
      rxFifoDepth = 32
    )

    gpioA.parameter load Gpio.Parameter(width = 8)

    // Configure spiA
    spiA.parameter load SpiMasterCtrlMemoryMappedConfig(
      SpiMasterCtrlGenerics(
        dataWidth = 8,
        timerWidth = 32,
        ssWidth = 1
      )
    )

    // Configure spiB
    spiB.parameter load SpiMasterCtrlMemoryMappedConfig(
      SpiMasterCtrlGenerics(
        dataWidth = 8,
        timerWidth = 32,
        ssWidth = 1
      )
    )

    g
  }
}


object Ulx3sSdCard {
  //Function used to configure the SoC
  def default(g : Ulx3sSdCard) = g{
    import g._
    Ulx3sSdCardSystem.default(system, clockCtrl)
    clockCtrl.resetSensitivity.load(ResetSensitivity.LOW)
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(InOutWrapper(default(new Ulx3sSdCard()).toComponent()))
    BspGenerator("Ulx3sSdCard", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
  }

}

