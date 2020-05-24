package saxon.board.scarab

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

class MS6PLinuxSystem extends SaxonSocLinux{
  //Add components
  val ramA = BmbOnChipRamGenerator(0x20000000l)
  val sdramA = SdramSdrBmbGenerator(0x80000000l)
  val gpioA = Apb3GpioGenerator(0x00000)
  val spiA = Apb3SpiGenerator(0x20000)
  val spiB = Apb3SpiGenerator(0x21000)

  //Interconnect specification
  interconnect.addConnection(
    cpu.iBus -> List(ramA.bmb, sdramA.bmb),
    cpu.dBus -> List(ramA.bmb, sdramA.bmb, peripheralBridge.input)
  )
}

case class BUFG() extends BlackBox{
  val O = out Bool()
  val I = in Bool()
}

case class ODDR2() extends BlackBox{
  val Q = out Bool()
  val C0 = in Bool()
  val C1 = in Bool()
  val D0 = in Bool()
  val D1 = in Bool()
  val S = in Bool()
  val R = in Bool()
  val CE = in Bool()
}

class MS6PLinux extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(255)
  clockCtrl.resetSynchronous.load(false)
  clockCtrl.powerOnReset.load(true)

  val system = new MS6PLinuxSystem()
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val CLOCK_50 = in Bool()
    val resetN = in Bool()
    val sdramClk = out Bool()

    val oddr = ODDR2()
    val bufg = BUFG()

    bufg.I := CLOCK_50
    oddr.C0 := bufg.O
    oddr.C1 := ~bufg.O
    oddr.D0 := True
    oddr.D1 := False
    oddr.R := False
    oddr.S := False
    oddr.CE := True

    sdramClk := oddr.Q
    clockCtrl.clock.load(bufg.O)
    clockCtrl.reset.load(resetN)
  }
}

object MS6PLinuxSystem{
  def default(g : MS6PLinuxSystem, clockCtrl : ClockDomainGenerator, inferSpiAPhy : Boolean = true) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.linux(0x20000000l))
    cpu.enableJtag(clockCtrl)

    ramA.dataWidth.load(32)
    ramA.size.load(2 KiB)
    ramA.hexInit.load(null)

    sdramA.layout.load(W9825G6JH6.layout)
    sdramA.timings.load(W9825G6JH6.timingGrade7)

    uartA.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 128,
      rxFifoDepth = 128
    )

    gpioA.parameter load Gpio.Parameter(
      width = 8,
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
          ssWidth = 1
        )
      ) .addFullDuplex(id = 0),
      cmdFifoDepth = 256,
      rspFifoDepth = 256
    )
    spiB.inferSpiSdrIo()

    g
  }
}


object MS6PLinux {
  //Function used to configure the SoC
  def default(g : MS6PLinux) = g{
    import g._
    clockCtrl.clkFrequency.load(50 MHz)
    clockCtrl.resetSensitivity.load(ResetSensitivity.LOW)
    MS6PLinuxSystem.default(system, clockCtrl)
    system.ramA.hexInit.load("software/standalone/bootloader/build/bootloader.hex")
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(InOutWrapper(default(new MS6PLinux()).toComponent()))
    BspGenerator("MS6PLinux", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
  }
}

object MS6PLinuxSystemSim {
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

    simConfig.compile(new MS6PLinuxSystem(){
      val clockCtrl = ClockDomainGenerator()
      this.onClockDomain(clockCtrl.clockDomain)
      clockCtrl.makeExternal(ResetSensitivity.HIGH)
      clockCtrl.powerOnReset.load(true)
      clockCtrl.clkFrequency.load(50 MHz)
      clockCtrl.resetHoldDuration.load(15)
      val sdcard = SdcardEmulatorGenerator()
      sdcard.connect(spiA.phy, gpioA.gpio.produce(gpioA.gpio.write(7) && gpioA.gpio.writeEnable(7)))
      MS6PLinuxSystem.default(this, clockCtrl,inferSpiAPhy = false)
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
      sdram.loadBin(0x00080000, linuxPath + "Image")
      sdram.loadBin(0x0007F000, linuxPath + "dtb")
      sdram.loadBin(0x00800000, linuxPath + "rootfs.cpio")
    }
  }
}
