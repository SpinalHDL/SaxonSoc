package saxon.board.ulx3s

import saxon._
import saxon.board.ulx3s.peripheral._
import spinal.core._
import spinal.lib._
import spinal.lib.blackbox.lattice.ecp5.{BB, ODDRX1F, TSFF}
import spinal.lib.com.jtag.sim.JtagTcp
import spinal.lib.com.spi.ddr.{SpiXdrMasterCtrl, SpiXdrParameter}
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}
import spinal.lib.generator._
import spinal.lib.io.{Gpio, InOutWrapper}
import spinal.lib.memory.sdram.sdr._
import spinal.lib.memory.sdram.sdr.sim.SdramModel
import spinal.lib.com.spi._
import spinal.lib.memory.sdram.SdramLayout
import spinal.lib.memory.sdram.xdr.CoreParameter
import spinal.lib.memory.sdram.xdr.phy.{Ecp5Sdrx2Phy, SdrInferedPhy}

class Ulx3sLinuxUbootSystem extends SaxonSocLinux {
  val ramA = BmbOnChipRamGenerator(0x20000000l)
  val sdramA = SdramXdrBmbGenerator(memoryAddress = 0x80000000l).mapApbAt(0x0F000)
  val sdramA0 = sdramA.addPort()

  val gpioA = Apb3GpioGenerator(0x00000)
  val spiA = new Apb3SpiGenerator(0x20000){
    val decoder = SpiPhyDecoderGenerator(phy)
    val user = decoder.spiMasterNone()
    val flash = decoder.spiMasterId(0)
    val sdcard = decoder.spiMasterId(1)
  }

  val uartB = Apb3UartGenerator(0x11000) 
  val hdmiConsoleA = Apb3HdmiConsoleGenerator(0x30000)
  val usbKeyboardA = Apb3UsbKeyboardGenerator(0x40000)
  val noReset = Ulx3sNoResetGenerator()

  val bridge = BmbBridgeGenerator()
  interconnect.addConnection(
    cpu.iBus -> List(bridge.bmb),
    cpu.dBus -> List(bridge.bmb),
    bridge.bmb -> List(sdramA0.bmb, ramA.ctrl, peripheralBridge.input)
  )

  interconnect.setConnector(bridge.bmb){case (m,s) =>
    m.cmd >/-> s.cmd
    m.rsp << s.rsp
  }
}

class Ulx3sLinuxUboot extends Generator{
  val globalCd = ClockDomainResetGenerator()
  globalCd.holdDuration.load(255)
  globalCd.enablePowerOnReset()

  val systemCd = ClockDomainResetGenerator()
  systemCd.setInput(globalCd)
  systemCd.holdDuration.load(63)

  val system = new Ulx3sLinuxUbootSystem(){
    val phyA = Ecp5Sdrx2PhyGenerator().connect(sdramA)
  }
  system.onClockDomain(systemCd.outputClockDomain)

  val clocking = add task new Area{
    val clk_25mhz = in Bool()
    val sdram_clk = out Bool()
    val resetn = in Bool()

    val pll = Ulx3sLinuxUbootPll()
    pll.clkin := clk_25mhz
    globalCd.setInput(
      ClockDomain(
        clock = pll.clkout2,
        reset = resetn,
        frequency = FixedFrequency(52 MHz),
        config = ClockDomainConfig(
          resetKind = ASYNC,
          resetActiveLevel = LOW
        )
      )
    )

    val bb = ClockDomain(pll.clkout1, False)(ODDRX1F())
    bb.D0 <> True
    bb.D1 <> False
    bb.Q <> sdram_clk
  }
    
  Dependable(system, system.hdmiConsoleA){
    system.hdmiConsoleA.pixclk := clocking.pll.clkout3
    system.hdmiConsoleA.pixclk_x5 := clocking.pll.clkout0
    system.hdmiConsoleA.resetn := clocking.resetn
  }
}

case class Ulx3sLinuxUbootPll() extends BlackBox{
  setDefinitionName("pll_linux")
  val clkin = in Bool()
  val clkout0 = out Bool()
  val clkout1 = out Bool()
  val clkout2 = out Bool()
  val clkout3 = out Bool()
  val locked = out Bool()
}

object Ulx3sLinuxUbootSystem{
  def default(g : Ulx3sLinuxUbootSystem, debugCd : ClockDomainResetGenerator, resetCd : ClockDomainResetGenerator, sdramSize: Int) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.ulx3sLinux(0x20000000l))
    cpu.enableJtag(debugCd, resetCd)

    // Configure ram
    ramA.dataWidth.load(32)
    ramA.size.load(8 KiB)
    ramA.hexInit.load(null)

    sdramA.coreParameter.load(CoreParameter(
      portTockenMin = 16,
      portTockenMax = 32,
      timingWidth = 4,
      refWidth = 16,
      stationCount  = 2,
      bytePerTaskMax = 64,
      writeLatencies = List(0),
      readLatencies = List(5, 6, 7)
    ))

    uartA.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 128,
      rxFifoDepth = 128
    )

    uartB.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 512,
      rxFifoDepth = 512,
      writeableConfig = true,
      clockDividerWidth = 20
    )

    uartB.connectInterrupt(plic, 2)

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
          ssWidth = 2
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
  def default(g : Ulx3sLinuxUboot, sdramSize: Int) = g{
    import g._

    system.spiA.flash.produce {
      val sclk = system.spiA.flash.sclk
      sclk.setAsDirectionLess()
      val usrMclk = Ulx3sUsrMclk()
      usrMclk.USRMCLKTS := False
      usrMclk.USRMCLKI := sclk
    }

    if (sdramSize == 32) {
      system.phyA.sdramLayout.load(MT48LC16M16A2.layout)
    } else {
      system.phyA.sdramLayout.load(AS4C32M16SB.layout)
    }

    Ulx3sLinuxUbootSystem.default(system, globalCd, systemCd, sdramSize = sdramSize)
    system.ramA.hexInit.load("software/standalone/bootloader/build/bootloader.hex")

    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val sdramSize = if (args.length > 0 && args(0) == "64")  64 else 32
    val report = SpinalRtlConfig.generateVerilog(InOutWrapper(default(new Ulx3sLinuxUboot, sdramSize).toComponent()))
    BspGenerator("Ulx3sLinuxUboot", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
  }
}

object Ulx3sLinuxUbootSystemSim {
  import spinal.core.sim._

  def main(args: Array[String]): Unit = {
    val simConfig = SimConfig
    simConfig.allOptimisation
//    simConfig.withWave
    simConfig.addSimulatorFlag("-Wno-CMPCONST")

    val sdcardEmulatorRtlFolder = "ext/sd_device/rtl/verilog"
    val sdcardEmulatorFiles = List("common.v", "sd_brams.v", "sd_link.v", "sd_mgr.v", "sd_phy.v", "sd_top.v", "sd_wishbone.v")
    sdcardEmulatorFiles.map(s => s"$sdcardEmulatorRtlFolder/$s").foreach(simConfig.addRtl(_))
    simConfig.addSimulatorFlag(s"-I../../$sdcardEmulatorRtlFolder")
    simConfig.addSimulatorFlag("-Wno-CASEINCOMPLETE")
    simConfig.addSimulatorFlag("-Wno-MULTIDRIVEN")

    simConfig.compile(new Ulx3sLinuxUbootSystem(){
      val globalCd = ClockDomainResetGenerator()
      globalCd.holdDuration.load(255)
      globalCd.enablePowerOnReset()

      val systemCd = ClockDomainResetGenerator()
      systemCd.setInput(globalCd)
      systemCd.holdDuration.load(63)

      this.onClockDomain(systemCd.outputClockDomain)

      globalCd.makeExternal(
        frequency = FixedFrequency(52 MHz)
      )

      val phyA = RtlPhyGenerator()
      phyA.layout.load(Ecp5Sdrx2Phy.phyLayout(MT48LC16M16A2.layout))
      phyA.connect(sdramA)

      val sdcard = SdcardEmulatorGenerator()
      sdcard.connectSpi(spiA.flash, spiA.flash.derivate(_.ss.lsb))
      Ulx3sLinuxUbootSystem.default(this, globalCd, systemCd, sdramSize = 32)
      ramA.hexInit.load("software/standalone/bootloader/build/bootloader_spinal_sim.hex")
    }.toComponent()).doSimUntilVoid("test", 42){dut =>
      val systemClkPeriod = (1e12/dut.globalCd.outputClockDomain.frequency.getValue.toDouble).toLong
      val jtagClkPeriod = systemClkPeriod*4
      val uartBaudRate = 115200
      val uartBaudPeriod = (1e12/uartBaudRate).toLong

      val sdcard = SdcardEmulatorIoSpinalSim(
        io = dut.sdcard.io,
        nsPeriod = 1000,
        storagePath = "../saxonsoc-ulx3s-bin/linux/u-boot/images/sdimage"
      )

      dut.globalCd.inputClockDomain.get.forkStimulus(systemClkPeriod)

//      fork{
//        disableSimWave()
//        clockDomain.waitSampling(1000)
//        waitUntil(!dut.uartA.uart.rxd.toBoolean)
//        enableSimWave()
//      }

      val tcpJtag = JtagTcp(
        jtag = dut.cpu.jtag,
        jtagClkPeriod = jtagClkPeriod
      )

      dut.globalCd.inputClockDomain.get.waitSampling(10)

      val uartTx = UartDecoder(
        uartPin =  dut.uartA.uart.txd,
        baudPeriod = uartBaudPeriod
      )
      
      val uartRx = UartEncoder(
        uartPin = dut.uartA.uart.rxd,
        baudPeriod = uartBaudPeriod
      )

      val linuxPath = "../buildroot/output/images/"
      val uboot = "../u-boot/"
      dut.phyA.io.loadBin(0x00800000, "software/standalone/machineModeSbi/build/machineModeSbi.bin")
      dut.phyA.io.loadBin(0x01F00000, uboot + "u-boot.bin")
    }
  }
}
