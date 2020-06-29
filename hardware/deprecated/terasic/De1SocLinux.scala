package saxon.board.terasic

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




class De1SocLinuxSystem extends SaxonSocLinux{
  //Add components
  val sdramA = SdramSdrBmbGenerator(0x80000000l)
  val gpioA = Apb3GpioGenerator(0x00000)
  val spiA = Apb3SpiGenerator(0x20000)
  val spiB = Apb3SpiGenerator(0x21000)

  //Interconnect specification
  interconnect.addConnection(
    cpu.iBus -> List(sdramA.bmb),
    cpu.dBus -> List(sdramA.bmb, peripheralBridge.input)
  )
}

class De1SocLinux extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(255)
  clockCtrl.resetSynchronous.load(false)
  clockCtrl.powerOnReset.load(true)

  val system = new De1SocLinuxSystem()
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val CLOCK_50 = in Bool()
    val resetN = in Bool()
    val sdramClk = out Bool()

    val pll = De1SocLinuxPll()
    pll.refclk := CLOCK_50
    pll.rst := False
    sdramClk := pll.outclk_1
    clockCtrl.clock.load(pll.outclk_0)
    clockCtrl.reset.load(resetN)
  }
}

case class De1SocLinuxPll() extends BlackBox{
  setDefinitionName("pll_0002")
  val refclk = in Bool()
  val rst = in Bool()
  val outclk_0 = out Bool()
  val outclk_1 = out Bool()
  val outclk_2 = out Bool()
  val outclk_3 = out Bool()
  val locked = out Bool()
}


object De1SocLinuxSystem{
  def default(g : De1SocLinuxSystem, clockCtrl : ClockDomainGenerator, inferSpiAPhy : Boolean = true) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.linux)
    cpu.enableJtag(clockCtrl)

    sdramA.layout.load(IS42x320D.layout)
    sdramA.timings.load(IS42x320D.timingGrade7)

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
          ssWidth = 0
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
    spiB.produce(RegNext(spiB.phy.sclk.write(0)).asOutput.setName("system_spiB_spi_sclk2"))



    g
  }
}


object De1SocLinux {
  //Function used to configure the SoC
  def default(g : De1SocLinux) = g{
    import g._
    clockCtrl.clkFrequency.load(100 MHz)
    clockCtrl.resetSensitivity.load(ResetSensitivity.LOW)
    De1SocLinuxSystem.default(system, clockCtrl)
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(InOutWrapper(default(new De1SocLinux()).toComponent()))
    BspGenerator("De1SocLinux", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
  }
}




object De1SocLinuxSystemSim {
  import spinal.core.sim._

  def main(args: Array[String]): Unit = {

    val simConfig = SimConfig
    simConfig.allOptimisation
    simConfig.withWave

    val sdcardEmulatorRtlFolder = "ext/sd_device/rtl/verilog"
    val sdcardEmulatorFiles = List("common.v", "sd_brams.v", "sd_link.v", "sd_mgr.v",  "sd_phy.v", "sd_top.v", "sd_wishbone.v")
    sdcardEmulatorFiles.map(s => s"$sdcardEmulatorRtlFolder/$s").foreach(simConfig.addRtl(_))
    simConfig.addSimulatorFlag(s"-I../../$sdcardEmulatorRtlFolder")
    simConfig.addSimulatorFlag("-Wno-CASEINCOMPLETE")

    simConfig.compile(new De1SocLinuxSystem(){
      val clockCtrl = ClockDomainGenerator()
      this.onClockDomain(clockCtrl.clockDomain)
      clockCtrl.makeExternal(ResetSensitivity.HIGH)
      clockCtrl.powerOnReset.load(true)
      clockCtrl.clkFrequency.load(100 MHz)
      clockCtrl.resetHoldDuration.load(15)
      val sdcard = SdcardEmulatorGenerator()
      sdcard.connect(spiA.phy, gpioA.gpio.produce(gpioA.gpio.write(8) && gpioA.gpio.writeEnable(8)))
      De1SocLinuxSystem.default(this, clockCtrl,inferSpiAPhy = false)
    }.toComponent()).doSimUntilVoid("test", 42){dut =>
      val systemClkPeriod = (1e12/dut.clockCtrl.clkFrequency.toDouble).toLong
      val jtagClkPeriod = systemClkPeriod*4
      val uartBaudRate = 115200
      val uartBaudPeriod = (1e12/uartBaudRate).toLong

      val clockDomain = ClockDomain(dut.clockCtrl.clock, dut.clockCtrl.reset)
      clockDomain.forkStimulus(systemClkPeriod)
//      clockDomain.forkSimSpeedPrinter(4)

//      fork{
//        while(true){
//          sleep(systemClkPeriod*1000000)
//          println("\nsimTime : " + simTime())
//        }
//      }
      fork{
//        disableSimWave()
//        sleep(0.2e12.toLong)
//        enableSimWave()
//        sleep(systemClkPeriod*2000000)
//        simFailure()

        while(true){
          disableSimWave()
          sleep(systemClkPeriod*500000)
          enableSimWave()
          sleep(systemClkPeriod*100)
        }
      }

      val sdcard = SdcardEmulatorIoSpinalSim(
        io = dut.sdcard.io,
        nsPeriod = 1000,
        storagePath = "../sdcard/image"
      )

      val tcpJtag = JtagTcp(
        jtag = dut.cpu.jtag,
        jtagClkPeriod = jtagClkPeriod
      )

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
      sdram.loadBin(0x00000000, "software/standalone/machineModeSbi/build/machineModeSbi.bin")
      sdram.loadBin(0x00400000, linuxPath + "Image")
      sdram.loadBin(0x00BF0000, linuxPath + "dtb")
      sdram.loadBin(0x00C00000, linuxPath + "rootfs.cpio")


//      sdram.loadBin(0, "software/standalone/dhrystone/build/dhrystone.bin")
//      sdram.loadBin(0, "software/standalone/machineModeSbi/build/machineModeSbi.bin")


//      val linuxPath = "/home/miaou/pro/litex/linux-on-litex-vexriscv/buildroot/"
//      sdram.loadBin(0x00000000, "software/standalone/machineModeSbi/build/machineModeSbi.bin")
//      sdram.loadBin(0x00400000, linuxPath + "Image")
//      sdram.loadBin(0x00BF0000, "../buildroot/output/images/dtb")
//      sdram.loadBin(0x00C00000, linuxPath + "rootfs.cpio")
    }
  }
}
