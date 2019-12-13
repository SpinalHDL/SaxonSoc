package saxon.board.ulx3s

import saxon.{ResetSensitivity, _}
import spinal.core._
import spinal.lib.com.jtag.sim.JtagTcp
import spinal.lib.com.spi.ddr.{SpiXdrMasterCtrl, SpiXdrParameter}
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}
import spinal.lib.generator._
import spinal.lib.io.{Gpio, InOutWrapper}
import spinal.lib.memory.sdram.sdr._
import spinal.lib.memory.sdram.sdr.sim.SdramModel
import spinal.lib.com.spi._

class Ulx3sLinuxUbootSystem extends SaxonSocLinux{
  //Add components
  val ramA = BmbOnChipRamGenerator(0x20000000l) 
  val sdramA = SdramSdrBmbGenerator(0x80000000l)
  val gpioA = Apb3GpioGenerator(0x00000)
  val spiA = Apb3SpiGenerator(0x20000)
  val spiB = Apb3SpiGenerator(0x21000)
  val spiC = Apb3SpiGenerator(0x22000)
  val noReset = Ulx3sNoResetGenerator()

  val bridge = BmbBridgeGenerator()
  interconnect.addConnection(
    cpu.iBus -> List(bridge.bmb),
    cpu.dBus -> List(bridge.bmb),
    bridge.bmb -> List(sdramA.bmb, ramA.bmb, peripheralBridge.input)
  )

  interconnect.setConnector(bridge.bmb){case (m,s) =>
    m.cmd >/-> s.cmd
    m.rsp << s.rsp
  }
}

class Ulx3sLinuxUboot extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(255)
  clockCtrl.resetSynchronous.load(false)
  clockCtrl.powerOnReset.load(true)

  val system = new Ulx3sLinuxUbootSystem()
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val clk_25mhz = in Bool()
    val sdram_clk = out Bool()
    val resetn = in Bool()

    val pll = Ulx3sLinuxUbootPll()
    pll.clkin := clk_25mhz
    sdram_clk := pll.clkout0
    clockCtrl.clock.load(pll.clkout1)
    clockCtrl.reset.load(resetn)
  }
}

case class Ulx3sLinuxUbootPll() extends BlackBox{
  setDefinitionName("pll_linux")
  val clkin = in Bool()
  val clkout0 = out Bool()
  val clkout1 = out Bool()
  val locked = out Bool()
}

object Ulx3sLinuxUbootSystem{
  def default(g : Ulx3sLinuxUbootSystem, clockCtrl : ClockDomainGenerator, inferSpiAPhy : Boolean = true) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.linux(0x20000000l))
    cpu.enableJtag(clockCtrl)

    // Configure ram
    ramA.dataWidth.load(32)
    ramA.size.load(8 KiB)
    ramA.hexInit.load(null)

    sdramA.layout.load(MT48LC16M16A2.layout)
    sdramA.timings.load(MT48LC16M16A2.timingGrade7)

    uartA.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 128,
      rxFifoDepth = 128
    )

    gpioA.parameter load Gpio.Parameter(
      width = 24,
      interrupt = List(0, 1, 2, 3)
    )
    gpioA.connectInterrupts(plic, 4)

    spiA.parameter load SpiXdrMasterCtrl.MemoryMappingParameters(
      SpiXdrMasterCtrl.Parameters(
        dataWidth = 8,
        timerWidth = 12,
        spi = SpiXdrParameter(
          dataWidth = 2,
          ioRate = 1,
          ssWidth = 1
        )
      ) .addFullDuplex(id = 0),
      cmdFifoDepth = 256,
      rspFifoDepth = 256
    )
    if(inferSpiAPhy) spiA.inferSpiSdrIo()

    spiB.parameter load SpiXdrMasterCtrl.MemoryMappingParameters(
      SpiXdrMasterCtrl.Parameters(
        dataWidth = 8,
        timerWidth = 12,
        spi = SpiXdrParameter(
          dataWidth = 2,
          ioRate = 1,
          ssWidth = 0
        )
      ) .addFullDuplex(id = 0),
      cmdFifoDepth = 256,
      rspFifoDepth = 256
    )
    spiB.inferSpiSdrIo()

    spiC.parameter load SpiXdrMasterCtrl.MemoryMappingParameters(
      SpiXdrMasterCtrl.Parameters(
        dataWidth = 8,
        timerWidth = 12,
        spi = SpiXdrParameter(
          dataWidth = 2,
          ioRate = 1,
          ssWidth = 0
        )
      ) .addFullDuplex(id = 0),
      cmdFifoDepth = 256,
      rspFifoDepth = 256
    )

    g
  }
}

object Ulx3sLinuxUboot {
  //Function used to configure the SoC
  def default(g : Ulx3sLinuxUboot) = g{
    import g._
    clockCtrl.clkFrequency.load(50 MHz)
    clockCtrl.resetSensitivity.load(ResetSensitivity.LOW)

    system.spiC.inferSpiSdrIo()

    system.spiC.spi.produce {
      val sclk = system.spiC.spi.get.asInstanceOf[SpiHalfDuplexMaster].sclk
      sclk.setAsDirectionLess()
      val usrMclk = Ulx3sUsrMclk()
      usrMclk.USRMCLKTS := False
      usrMclk.USRMCLKI := sclk
    }

    Ulx3sLinuxUbootSystem.default(system, clockCtrl)
    system.ramA.hexInit.load("software/standalone/bootloader/build/bootloader.hex")

    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(InOutWrapper(default(new Ulx3sLinuxUboot()).toComponent()))
    BspGenerator("Ulx3sLinuxUboot", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
  }
}

object Ulx3sLinuxUbootSystemSim {
  import spinal.core.sim._

  def main(args: Array[String]): Unit = {
    val simConfig = SimConfig
    simConfig.allOptimisation
    simConfig.withWave
    simConfig.addSimulatorFlag("-Wno-CMPCONST")

    val sdcardEmulatorRtlFolder = "ext/sd_device/rtl/verilog"
    val sdcardEmulatorFiles = List("common.v", "sd_brams.v", "sd_link.v", "sd_mgr.v", "sd_phy.v", "sd_top.v", "sd_wishbone.v")
    sdcardEmulatorFiles.map(s => s"$sdcardEmulatorRtlFolder/$s").foreach(simConfig.addRtl(_))
    simConfig.addSimulatorFlag(s"-I../../$sdcardEmulatorRtlFolder")
    simConfig.addSimulatorFlag("-Wno-CASEINCOMPLETE")

    simConfig.compile(new Ulx3sLinuxUbootSystem(){
      val clockCtrl = ClockDomainGenerator()
      this.onClockDomain(clockCtrl.clockDomain)
      clockCtrl.makeExternal(ResetSensitivity.HIGH)
      clockCtrl.powerOnReset.load(true)
      clockCtrl.clkFrequency.load(50 MHz)
      clockCtrl.resetHoldDuration.load(15)
      val sdcard = SdcardEmulatorGenerator()
      sdcard.connect(spiA.phy, spiA.phy.produce(RegNext(spiA.phy.ss(0))))

      Ulx3sLinuxUbootSystem.default(this, clockCtrl, inferSpiAPhy = false)
      ramA.hexInit.load("software/standalone/bootloader/build/bootloader_spinal_sim.hex")
    }.toComponent()).doSimUntilVoid("test", 42){dut =>
      val systemClkPeriod = (1e12/dut.clockCtrl.clkFrequency.toDouble).toLong
      val jtagClkPeriod = systemClkPeriod*4
      val uartBaudRate = 115200
      val uartBaudPeriod = (1e12/uartBaudRate).toLong

      val sdcard = SdcardEmulatorIoSpinalSim(
        io = dut.sdcard.io,
        nsPeriod = 1000,
        storagePath = "../saxonsoc-ulx3s-bin/linux/images/sdimage"
      )

      val clockDomain = ClockDomain(dut.clockCtrl.clock, dut.clockCtrl.reset)
      clockDomain.forkStimulus(systemClkPeriod)

      fork{
        while(true){
          disableSimWave()
          sleep(systemClkPeriod*500000)
          enableSimWave()
          sleep(systemClkPeriod*100)
        }
      }

      val tcpJtag = JtagTcp(
        jtag = dut.cpu.jtag,
        jtagClkPeriod = jtagClkPeriod
      )

      clockDomain.waitSampling(10)

      val uartTx = UartDecoder(
        uartPin =  dut.uartA.uart.txd,
        baudPeriod = uartBaudPeriod
      )
      
      val uartRx = UartEncoder(
        uartPin = dut.uartA.uart.rxd,
        baudPeriod = uartBaudPeriod
      )

      val sdram = SdramModel(
        io = dut.sdramA.sdram,
        layout = dut.sdramA.logic.layout,
        clockDomain = clockDomain
      )

      val linuxPath = "../buildroot/output/images/"
      val uboot = "../u-boot/"
      sdram.loadBin(0x00000000, "software/standalone/machineModeSbi/build/machineModeSbi.bin")
      sdram.loadBin(0x00010000, uboot + "u-boot.bin")

    }
  }
}
