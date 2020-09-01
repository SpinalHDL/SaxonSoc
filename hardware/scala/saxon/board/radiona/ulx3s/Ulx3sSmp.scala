package saxon.board.radiona.ulx3s

import saxon.common.I2cModel
import saxon._
import spinal.core._
import spinal.core.sim._
import spinal.lib.blackbox.lattice.ecp5.ODDRX1F
import spinal.lib.blackbox.xilinx.s7.{BSCANE2, BUFG, STARTUPE2}
import spinal.lib.bus.bmb._
import spinal.lib.bus.misc.{AddressMapping, SizeMapping}
import spinal.lib.bus.simple.{PipelinedMemoryBus, PipelinedMemoryBusDecoder}
import spinal.lib.com.eth.{MacEthParameter, PhyParameter}
import spinal.lib.com.jtag.sim.JtagTcp
import spinal.lib.com.jtag.{Jtag, JtagTap, JtagTapDebuggerGenerator, JtagTapInstructionCtrl}
import spinal.lib.com.jtag.xilinx.Bscane2BmbMasterGenerator
import spinal.lib.com.spi.ddr.{SpiXdrMasterCtrl, SpiXdrParameter}
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}
import spinal.lib.generator._
import spinal.lib.io.{Gpio, InOutWrapper}
import spinal.lib.memory.sdram.sdr._
import spinal.lib.memory.sdram.xdr.CoreParameter
import spinal.lib.memory.sdram.xdr.phy.{Ecp5Sdrx2Phy, XilinxS7Phy}
import vexriscv.demo.smp.VexRiscvSmpClusterGen


// Define a SoC abstract enough to be used in simulation (no PLL, no PHY)
class Ulx3sSmpAbstract() extends VexRiscvClusterGenerator{
  val fabric = withDefaultFabric()

  val sdramA = SdramXdrBmbGenerator(memoryAddress = 0x80000000l).mapCtrlAt(0x100000)
  val sdramA0 = sdramA.addPort()

  val gpioA = BmbGpioGenerator(0x00000)

  val uartA = BmbUartGenerator(0x10000)

  val uartB = BmbUartGenerator(0x11000)

  val spiA = new BmbSpiGenerator(0x20000){
    val decoder = SpiPhyDecoderGenerator(phy)
    val user = decoder.spiMasterNone()
    val flash = decoder.spiMasterId(0)
    val sdcard = decoder.spiMasterId(1)
  }

  val ramA = BmbOnChipRamGenerator(0xA00000l)
  ramA.hexOffset = bmbPeripheral.mapping.lowerBound
  ramA.dataWidth.load(32)
  interconnect.addConnection(bmbPeripheral.bmb, ramA.ctrl)

  interconnect.addConnection(
    fabric.iBus.bmb -> List(sdramA0.bmb, bmbPeripheral.bmb),
    fabric.dBus.bmb -> List(sdramA0.bmb, bmbPeripheral.bmb)
  )
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

case class Ulx3sUsrMclk() extends BlackBox{
  setDefinitionName("USRMCLK")

  val USRMCLKI = in Bool()
  val USRMCLKTS = in Bool()
}



class Ulx3sSmp extends Generator{
  // Define the clock domains used by the SoC
  val globalCd = ClockDomainResetGenerator()
  globalCd.holdDuration.load(255)
  globalCd.enablePowerOnReset()

  val systemCd = ClockDomainResetGenerator()
  systemCd.setInput(globalCd)
  systemCd.holdDuration.load(63)

  // ...
  val system = new Ulx3sSmpAbstract(){
    val phyA = Ecp5Sdrx2PhyGenerator().connect(sdramA)
  }
  system.onClockDomain(systemCd.outputClockDomain)

  // Enable native JTAG debug
  val debug = system.withDebugBus(globalCd, systemCd, 0x10B80000).withJtag()

  //Manage clocks and PLL

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

//  Dependable(system, system.hdmiConsoleA){
//    system.hdmiConsoleA.pixclk := clocking.pll.clkout3
//    system.hdmiConsoleA.pixclk_x5 := clocking.pll.clkout0
//    system.hdmiConsoleA.resetn := clocking.resetn
//  }
}

object Ulx3sSmpAbstract{
  def default(g : Ulx3sSmpAbstract) = g {
    import g._

    cpuCount.load(1)

    // Configure the CPUs
    cores.produce{
      for((cpu, coreId) <- cores.cpu.zipWithIndex) {
        cpu.config.load(VexRiscvSmpClusterGen.vexRiscvConfig(
          hartId = coreId,
          ioRange = _ (31 downto 28) === 0x1,
          resetVector = 0x10A00000l,
          iBusWidth = 64,
          dBusWidth = 64
        ))
      }
    }

    // Configure the peripherals
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
    uartA.connectInterrupt(plic, 1)

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


    // Add some interconnect pipelining to improve FMax
    interconnect.dependencies += cores.produce{for(cpu <- cores.cpu) interconnect.setPipelining(cpu.dBus)(cmdValid = true, invValid = true, ackValid = true, syncValid = true)}
    interconnect.setPipelining(fabric.exclusiveMonitor.input)(cmdValid = true, cmdReady = true, rspValid = true)
    interconnect.setPipelining(fabric.invalidationMonitor.output)(cmdValid = true, cmdReady = true, rspValid = true)
    interconnect.setPipelining(bmbPeripheral.bmb)(cmdHalfRate = true, rspHalfRate = true)
    interconnect.setPipelining(sdramA0.bmb)(cmdValid = true, cmdReady = true, rspValid = true)
    interconnect.setPipelining(fabric.iBus.bmb)(cmdValid = true)

    g
  }
}


object Ulx3sSmp {
  //Function used to configure the SoC
  def default(g : Ulx3sSmp, sdramSize : Int) = g{
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

    Ulx3sSmpAbstract.default(system)
    system.ramA.hexInit.load("software/standalone/bootloader/build/bootloader.hex")

    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val sdramSize = if (args.length > 0 && args(0) == "64")  64 else 32
    val report = SpinalRtlConfig.generateVerilog(InOutWrapper(default(new Ulx3sSmp, sdramSize).toComponent()))
    BspGenerator("Ulx3sLinuxUboot", report.toplevel.generator, report.toplevel.generator.system.cores.cpu.get(0).dBus)
  }
}




object Ulx3sSmpSystemSim {
  import spinal.core.sim._

  def main(args: Array[String]): Unit = {

    val simConfig = SimConfig
    simConfig.allOptimisation
//    simConfig.withFstWave
    simConfig.addSimulatorFlag("-Wno-MULTIDRIVEN")

    simConfig.compile(new Ulx3sSmpAbstract(){
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

      val phy = RtlPhyGenerator()
      phy.layout.load(Ecp5Sdrx2Phy.phyLayout(MT48LC16M16A2.layout))
      phy.connect(sdramA)

      Ulx3sSmpAbstract.default(this)
      ramA.hexInit.load("software/standalone/bootloader/build/bootloader_spinal_sim.hex")

      val jtagTap = withDebugBus(globalCd, systemCd, address = 0x10B80000).withJtag()
    }.toComponent().setDefinitionName("miaou2")).doSimUntilVoid("test", 42){dut =>
      val debugClkPeriod = (1e12/dut.globalCd.inputClockDomain.frequency.getValue.toDouble).toLong
      val jtagClkPeriod = debugClkPeriod*4
      val uartBaudRate = 115200
      val uartBaudPeriod = (1e12/uartBaudRate).toLong

      val clockDomain = dut.globalCd.inputClockDomain.get
      clockDomain.forkStimulus(debugClkPeriod)
//      clockDomain.forkSimSpeedPrinter(2.0)


      fork{
        val at = 0
        val duration = 0
        while(simTime() < at*1000000000l) {
          disableSimWave()
          sleep(100000 * 10000)
          enableSimWave()
          sleep(  100 * 10000)
        }
        println("\n\n********************")
        sleep(duration*1000000000l)
        println("********************\n\n")
        while(true) {
          disableSimWave()
          sleep(100000 * 10000)
          enableSimWave()
          sleep(  100 * 10000)
        }
      }

      val tcpJtag = JtagTcp(
        jtag = dut.jtagTap.jtag,
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

      dut.spiA.sdcard.data.read #= 3

      val uboot = "../u-boot/"
      val opensbi = "../opensbi/"
      val linuxPath = "../buildroot/output/images/"

      dut.phy.logic.loadBin(0x00F80000, opensbi + "build/platform/spinal/saxon/digilent/artyA7Smp/firmware/fw_jump.bin")
      dut.phy.logic.loadBin(0x00F00000, uboot + "u-boot.bin")
//      dut.phy.logic.loadBin(0x00000000, linuxPath + "uImage")
//      dut.phy.logic.loadBin(0x00FF0000, linuxPath + "dtb")
//      dut.phy.logic.loadBin(0x00FFFFC0, linuxPath + "rootfs.cpio.uboot")


//      dut.phy.logic.loadBin(0x00F80000, "software/standalone/ethernet/build/ethernet.bin")
//      dut.phy.logic.loadBin(0x00F80000, "software/standalone/dhrystone/build/dhrystone.bin")
//      dut.phy.logic.loadBin(0x00F80000, "software/standalone/timerAndGpioInterruptDemo/build/timerAndGpioInterruptDemo_spinal_sim.bin")
//      dut.phy.logic.loadBin(0x00F80000, "software/standalone/freertosDemo/build/freertosDemo_spinal_sim.bin")
      println("DRAM loading done")
    }
  }
}
