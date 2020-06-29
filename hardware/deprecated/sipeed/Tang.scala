package saxon.board.sipeed

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
import spinal.lib.blackbox.anlogic.eagle._

class TangLinuxSystem extends SaxonSocLinux{
  //Add components
  val ramA = BmbEg4S20Bram32Generator(0x20000000l)
  val sdramA = SdramSdrBmbGenerator(0x80000000l)
  val gpioA = Apb3GpioGenerator(0x00000)
  val spiA = new Apb3SpiGenerator(0x20000){
    val decoder = SpiPhyDecoderGenerator(phy)
    val user = decoder.spiMasterNone()
    val flash = decoder.spiMasterId(0)
    val sdcard = decoder.spiMasterId(1)
  }

  //Interconnect specification
  interconnect.addConnection(
    cpu.iBus -> List(ramA.bmb, sdramA.bmb),
    cpu.dBus -> List(ramA.bmb, sdramA.bmb, peripheralBridge.input)
  )
}

class TangLinux extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(255)
  clockCtrl.resetSynchronous.load(false)
  clockCtrl.powerOnReset.load(true)

  val system = new TangLinuxSystem()
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val CLOCK_24 = in Bool()
    val resetN = in Bool()
    val sdramClk = out Bool()

    val oddr = EG_LOGIC_ODDR()
    val bufg = EG_LOGIC_BUFG()
    bufg.i := CLOCK_24
    oddr.clk := bufg.o
    oddr.rst := ~resetN
    oddr.d0 := True
    oddr.d1 := False
    sdramClk := oddr.q

    clockCtrl.clock.load(bufg.o)
    clockCtrl.reset.load(resetN)
  }
}

object TangLinuxSystem{
  def default(g : TangLinuxSystem, clockCtrl : ClockDomainGenerator) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.linux(0x20000000l))
    cpu.enableJtag(clockCtrl)

    ramA.size.load(64 KiB)
    ramA.hexInit.load("bram16x32k.bin")

    sdramA.layout.load(EG4S20.layout)
    sdramA.timings.load(EG4S20.timingGrade7)

    uartA.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 128,
      rxFifoDepth = 128
    )

    gpioA.parameter load Gpio.Parameter(
      width = 14,
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


object TangLinux {
  //Configure the SoC
  def default(g : TangLinux) = g{
    import g._
    clockCtrl.clkFrequency.load(24 MHz)
    clockCtrl.resetSensitivity.load(ResetSensitivity.LOW)
    TangLinuxSystem.default(system, clockCtrl)
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(InOutWrapper(default(new TangLinux()).toComponent()))
    BspGenerator("TangLinux", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
  }
}

object TangLinuxSystemSim {
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

    simConfig.compile(new TangLinuxSystem(){
      val clockCtrl = ClockDomainGenerator()
      this.onClockDomain(clockCtrl.clockDomain)
      clockCtrl.makeExternal(ResetSensitivity.HIGH)
      clockCtrl.powerOnReset.load(true)
      clockCtrl.clkFrequency.load(24 MHz)
      clockCtrl.resetHoldDuration.load(15)
      val sdcard = SdcardEmulatorGenerator()
      sdcard.connect(spiA.phy, gpioA.gpio.produce(gpioA.gpio.write(8) && gpioA.gpio.writeEnable(8)))
      TangLinuxSystem.default(this, clockCtrl)
    }.toComponent()).doSimUntilVoid("test", 42){dut =>
      val systemClkPeriod = (1e12/dut.clockCtrl.clkFrequency.toDouble).toLong
      val jtagClkPeriod = systemClkPeriod*4
      val uartBaudRate = 115200
      val uartBaudPeriod = (1e12/uartBaudRate).toLong

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
      sdram.loadBin(0x00FF0000, linuxPath + "dtb")
      sdram.loadBin(0x00800000, linuxPath + "rootfs.cpio")
    }
  }
}
