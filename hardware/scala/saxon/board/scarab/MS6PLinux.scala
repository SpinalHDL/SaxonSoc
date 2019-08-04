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
  val sdramA = SdramSdrBmbGenerator(0x80000000l)
  val gpioA = Apb3GpioGenerator(0x00000)
  val spiA = Apb3SpiGenerator(0x20000, xipOffset = 0x20000000)
  val spiB = Apb3SpiGenerator(0x21000)

  //Interconnect specification
  val bridge = BmbBridgeGenerator()
  interconnect.addConnection(
    cpu.iBus -> List(bridge.bmb),
    cpu.dBus -> List(bridge.bmb),
    bridge.bmb -> List(sdramA.bmb,
      peripheralBridge.input)
  )
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

    val pll = MS6PLinuxPll()
    pll.refclk := CLOCK_50
    pll.rst := False
    sdramClk := pll.outclk_1
    clockCtrl.clock.load(pll.outclk_0)
    clockCtrl.reset.load(resetN)
  }
}

case class MS6PLinuxPll() extends BlackBox{
  setDefinitionName("pll_0002")
  val refclk = in Bool()
  val rst = in Bool()
  val outclk_0 = out Bool()
  val outclk_1 = out Bool()
  val outclk_2 = out Bool()
  val outclk_3 = out Bool()
  val locked = out Bool()
}


object MS6PLinuxSystem{
  def default(g : MS6PLinuxSystem, clockCtrl : ClockDomainGenerator) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.linux(0x20100000l))
    cpu.enableJtag(clockCtrl)

    sdramA.layout.load(W9825G6JH6.layout)
    sdramA.timings.load(W9825G6JH6.timingGrade7)

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
      ) .addFullDuplex(id = 0, rate = 1)
.addHalfDuplex(id=1, rate=1, ddr=false, spiWidth=2),
      cmdFifoDepth = 256,
      rspFifoDepth = 256,
      cpolInit = false,
      cphaInit = false,
      modInit = 0,
      sclkToogleInit = 0,
      ssSetupInit = 0,
      ssHoldInit = 0,
      ssDisableInit = 0,
      xipConfigWritable = false,
      xipEnableInit = true,
      xipInstructionEnableInit = true,
      xipInstructionModInit = 0,
      xipAddressModInit = 0,
      xipDummyModInit = 0,
      xipPayloadModInit = 1,
      xipInstructionDataInit = 0x3B,
      xipDummyCountInit = 0,
      xipDummyDataInit = 0xFF
    )

    spiA.withXip.load(true)

    interconnect.addConnection(
      bridge.bmb -> List(spiA.bmb)
    )

    //Cut dBus address path
    interconnect.setConnector(bridge.bmb){(m,s) =>
      m.cmd >-> s.cmd
      m.rsp << s.rsp
    }

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


object MS6PLinux {
  //Function used to configure the SoC
  def default(g : MS6PLinux) = g{
    import g._
    clockCtrl.clkFrequency.load(50 MHz)
    clockCtrl.resetSensitivity.load(ResetSensitivity.LOW)
    MS6PLinuxSystem.default(system, clockCtrl)
    system.spiA.inferSpiSdrIo()
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
    simConfig.compile(new MS6PLinuxSystem(){
      val clockCtrl = ClockDomainGenerator()
      this.onClockDomain(clockCtrl.clockDomain)
      clockCtrl.makeExternal(ResetSensitivity.HIGH)
      clockCtrl.powerOnReset.load(true)
      clockCtrl.clkFrequency.load(50 MHz)
      MS6PLinuxSystem.default(this, clockCtrl)
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

      val flash = new FlashModel(
        dut.spiA.phy,
        clockDomain)

      flash.loadBinary("software/standalone/machineModeSbi/build/machineModeSbi.bin", 0x00100000)
      //flash.loadBinary("../u-boot/u-boot.bin", 0x00200000)

      val sdram = SdramModel(
        io = dut.sdramA.sdram,
        layout = dut.sdramA.logic.layout,
        clockDomain = clockDomain
      )

      val linuxPath = "../buildroot/output/images/"
      sdram.loadBin(0x00200000, "../u-boot/u-boot.bin")
      sdram.loadBin(0x003fffc0, linuxPath + "uImage")
      sdram.loadBin(0x00ff0000, linuxPath + "dtb")
      sdram.loadBin(0x007fffc0, linuxPath + "rootfs.cpio.uboot")
    }
  }
}
