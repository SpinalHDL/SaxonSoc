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
import spinal.lib.memory.sdram.SdramLayout
import spinal.lib.memory.sdram.SdramGeneration.SDR

class Ulx3sLinuxXip85System extends SaxonSocLinux{
  //Add components
  val ramA = BmbOnChipRamGenerator(0x70000000l) 
  val sdramA = SdramSdrBmbGenerator(0x80000000l)
  val gpioA = Apb3GpioGenerator(0x00000)
  val spiA = Apb3SpiGenerator(0x20000)
  val spiB = Apb3SpiGenerator(0x21000)
  val spiC = Apb3SpiGenerator(0x22000, xipOffset = 0x20000000)
  val noReset = Ulx3sNoResetGenerator()
  val uartB = Apb3UartGenerator(0x11000)

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

class Ulx3sLinuxXip85 extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(1023)
  clockCtrl.resetSynchronous.load(false)
  clockCtrl.powerOnReset.load(true)

  val system = new Ulx3sLinuxXip85System()
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val clk_25mhz = in Bool()
    val sdram_clk = out Bool()
    val resetn = in Bool()

    val pll = Ulx3sLinuxXip85Pll()
    pll.clkin := clk_25mhz
    sdram_clk := pll.clkout0
    clockCtrl.clock.load(pll.clkout1)
    clockCtrl.reset.load(resetn)
  }
}

case class Ulx3sLinuxXip85Pll() extends BlackBox{
  setDefinitionName("pll_linux")
  val clkin = in Bool()
  val clkout0 = out Bool()
  val clkout1 = out Bool()
  val locked = out Bool()
}


object Ulx3sLinuxXip85System{
  def default(g : Ulx3sLinuxXip85System, clockCtrl : ClockDomainGenerator, inferSpiAPhy : Boolean = true) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.linux(0x70000000l))
    cpu.enableJtag(clockCtrl)

    // Configure ram
    ramA.dataWidth.load(32)
    ramA.size.load(8 KiB)
    ramA.hexInit.load("software/standalone/machineModeSbi/build/machineModeSbi.hex")

    //sdramA.layout.load(MT48LC16M16A2.layout)
    //sdramA.timings.load(MT48LC16M16A2.timingGrade7)
    sdramA.layout.load(AS4C32M16SB.layout)
    sdramA.timings.load(AS4C32M16SB.timingGrade7)

    uartA.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 128,
      rxFifoDepth = 128
    )

    uartB.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 128,
      rxFifoDepth = 128
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

    spiC.parameter load SpiXdrMasterCtrl.MemoryMappingParameters(
      SpiXdrMasterCtrl.Parameters(
        dataWidth = 8,
        timerWidth = 12,
        spi = SpiXdrParameter(
          dataWidth = 2,
          ioRate= 1,
          ssWidth = 1
        )
      ).addFullDuplex(id = 0, rate = 1).addHalfDuplex(id=1, rate=1, ddr=false, spiWidth=2),
      cmdFifoDepth = 64,
      rspFifoDepth = 64,
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
    spiC.withXip.load(true)
    
    cpu.hardwareBreakpointCount.load(4)

    interconnect.addConnection(
      bridge.bmb -> List(spiC.bmb)
    )

    g
  }
}

object Ulx3sLinuxXip85 {
  //Function used to configure the SoC
  def default(g : Ulx3sLinuxXip85) = g{
    import g._
    clockCtrl.clkFrequency.load(50 MHz)
    clockCtrl.resetSensitivity.load(ResetSensitivity.LOW)
    //g.system.sdramA.logic.produce {
    //  g.system.sdramA.logic.ctrl.chip.sdram.addAttribute("syn_useioff")
    //}

    
    system.spiC.inferSpiSdrIo()

    system.spiC.spi.produce {
      val sclk = system.spiC.spi.get.asInstanceOf[SpiHalfDuplexMaster].sclk
      sclk.setAsDirectionLess()
      val usrMclk = Ulx3sUsrMclk()
      usrMclk.USRMCLKTS := False
      usrMclk.USRMCLKI := sclk
    }

    Ulx3sLinuxXip85System.default(system, clockCtrl)
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(InOutWrapper(default(new Ulx3sLinuxXip85()).toComponent()))
    BspGenerator("Ulx3sLinuxXip85", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
  }
}

object Ulx3sLinuxXip85SystemSim {
  import spinal.core.sim._

  def main(args: Array[String]): Unit = {
    val simConfig = SimConfig
    simConfig.allOptimisation
    //simConfig.withWave
    simConfig.addSimulatorFlag("-Wno-CMPCONST")

    simConfig.addSimulatorFlag("-Wno-CASEINCOMPLETE")

    simConfig.compile(new Ulx3sLinuxXip85System(){
      val clockCtrl = ClockDomainGenerator()
      this.onClockDomain(clockCtrl.clockDomain)
      clockCtrl.makeExternal(ResetSensitivity.HIGH)
      clockCtrl.powerOnReset.load(true)
      clockCtrl.clkFrequency.load(50 MHz)
      clockCtrl.resetHoldDuration.load(15)
      Ulx3sLinuxXip85System.default(this, clockCtrl, inferSpiAPhy = false)
    }.toComponent()).doSimUntilVoid("test", 42){dut =>
      val systemClkPeriod = (1e12/dut.clockCtrl.clkFrequency.toDouble).toLong
      val jtagClkPeriod = systemClkPeriod*4
      val uartBaudRate = 115200
      val uartBaudPeriod = (1e12/uartBaudRate).toLong

      val clockDomain = ClockDomain(dut.clockCtrl.clock, dut.clockCtrl.reset)
      clockDomain.forkStimulus(systemClkPeriod)
      /*fork{
        while(true){
          disableSimWave()
          sleep(systemClkPeriod*500000)
          enableSimWave()
          sleep(systemClkPeriod*100)
        }
      }*/

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
      
      val flash = new FlashModel(dut.spiC.phy, clockDomain)
      flash.loadBinary(linuxPath + "Image", 0x100000)
      flash.loadBinary(linuxPath + "dtb", 0x3FC000)

      //sdram.loadBin(0x00400000, linuxPath + "Image")
      //sdram.loadBin(0x006FC000, linuxPath + "dtb")
      sdram.loadBin(0x00800000, linuxPath + "rootfs.cpio")
    }
  }
}

