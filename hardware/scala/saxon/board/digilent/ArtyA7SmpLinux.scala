package saxon.board.digilent

import java.awt.image.BufferedImage
import java.awt.{Color, Dimension, Graphics}

import javax.swing.{JFrame, JPanel, WindowConstants}
import saxon.common.I2cModel
import saxon._
import spinal.core._
import spinal.core.sim._
import spinal.lib.blackbox.xilinx.s7.{BSCANE2, BUFG, STARTUPE2}
import spinal.lib.bus.bmb._
import spinal.lib.bus.bsb.BsbInterconnectGenerator
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
import spinal.lib.graphic.RgbConfig
import spinal.lib.graphic.vga.{BmbVgaCtrlGenerator, BmbVgaCtrlParameter, Vga}
import spinal.lib.io.{Gpio, InOutWrapper}
import spinal.lib.memory.sdram.sdr._
import spinal.lib.memory.sdram.xdr.CoreParameter
import spinal.lib.memory.sdram.xdr.phy.XilinxS7Phy
import spinal.lib.misc.analog.{BmbBsbToDeltaSigmaGenerator, BsbToDeltaSigmaParameter}
import spinal.lib.system.dma.sg.{DmaMemoryLayout, DmaSgGenerator}
import vexriscv.demo.smp.VexRiscvSmpClusterGen


// Define a SoC abstract enough to be used in simulation (no PLL, no PHY)
class ArtyA7SmpLinuxAbstract(cpuCount : Int) extends VexRiscvClusterGenerator(cpuCount){
  val fabric = withDefaultFabric()

  val sdramA = SdramXdrBmbGenerator(memoryAddress = 0x80000000l)
  val sdramA0 = sdramA.addPort()

  val gpioA = BmbGpioGenerator(0x00000)

  val uartA = BmbUartGenerator(0x10000)
  uartA.connectInterrupt(plic, 1)

  val spiA = new BmbSpiGenerator(0x20000){
    val decoder = SpiPhyDecoderGenerator(phy)
    val user = decoder.spiMasterNone()
    val flash = decoder.spiMasterId(0)
    val sdcard = decoder.spiMasterId(1)
    val md = decoder.mdioMasterId(2) //Ethernet phy
  }

  val mac = BmbMacEthGenerator(0x40000)
  mac.connectInterrupt(plic, 3)
  val eth = mac.withPhyMii()

  implicit val bsbInterconnect = BsbInterconnectGenerator()
  val dma = new DmaSgGenerator(0x80000){
    val vga = new Area{
      val channel = createChannel()
      channel.fixedBurst(64)
      channel.withCircularMode()
      channel.fifoMapping load Some(0, 256)
      channel.connectInterrupt(plic, 12)

      val stream = createOutput(byteCount = 8)
      channel.outputsPorts += stream

    }

    val audioOut = new Area{
      val channel = createChannel()
      channel.fixedBurst(64)
      channel.withScatterGatter()
      channel.fifoMapping load Some(256, 256)
      channel.connectInterrupt(plic, 13)

      val stream = createOutput(byteCount = 4)
      channel.outputsPorts += stream
    }
  }
 // interconnect.addConnection(dma.write, fabric.dBusCoherent.bmb)
  interconnect.addConnection(dma.read,  fabric.dBus.bmb)
  interconnect.addConnection(dma.readSg,  fabric.dBus.bmb)
  interconnect.addConnection(dma.writeSg,  fabric.dBusCoherent.bmb)

  val vga = BmbVgaCtrlGenerator(0x90000)
  bsbInterconnect.connect(dma.vga.stream.output, vga.input)

  val audioOut = BmbBsbToDeltaSigmaGenerator(0x94000)
  bsbInterconnect.connect(dma.audioOut.stream.output, audioOut.input)

  val ramA = BmbOnChipRamGenerator(0xA00000l)
  ramA.hexOffset = bmbPeripheral.mapping.lowerBound
  ramA.dataWidth.load(32)
  interconnect.addConnection(bmbPeripheral.bmb, ramA.ctrl)

  interconnect.addConnection(
    fabric.iBus.bmb -> List(sdramA0.bmb, bmbPeripheral.bmb),
    fabric.dBus.bmb -> List(sdramA0.bmb, bmbPeripheral.bmb)
  )


//  def debug(that : Data) = that.addAttribute("""mark_debug = "true"""")
//  audioOut.logic.derivate{c =>
//    debug(c.core.io.input.valid)
//    debug(c.core.io.input.ready)
//    debug(c.core.io.run)
//    debug(c.core.io.rate)
//  }
//  dma.logic.derivate{c =>
//    debug(c.io.interrupts)
//    debug(c.io.outputs(1).valid)
//    debug(c.io.outputs(1).ready)
//    debug(c.io.sgRead.cmd.valid)
//    debug(c.io.sgRead.cmd.address)
//    debug(c.io.sgRead.rsp.valid)
//    debug(c.io.sgRead.rsp.data)
//    debug(c.channels(1).channelValid)
//    debug(c.channels(1).descriptorValid)
//    debug(c.channels(1).interrupts.completion.enable)
//    debug(c.channels(1).interrupts.completion.valid)
//    debug(c.channels(1).push.memory)
//    debug(c.channels(1).push.m2b.address)
//    debug(c.channels(1).pop.memory)
//    debug(c.channels(1).pop.b2s.portId)
//    debug(c.channels(1).fifo.push.available)
//    debug(c.channels(1).fifo.pop.empty)
//    debug(c.io.read.cmd.valid)
//    debug(c.io.read.cmd.address)
//    debug(c.io.read.rsp.valid)
//  }
}

class ArtyA7SmpLinux(cpuCount : Int) extends Generator{
  // Define the clock domains used by the SoC
  val debugCd = ClockDomainResetGenerator()
  debugCd.holdDuration.load(4095)
  debugCd.enablePowerOnReset()


  val vgaCd = ClockDomainResetGenerator()
  vgaCd.holdDuration.load(63)
  vgaCd.asyncReset(debugCd)

  val sdramCd = ClockDomainResetGenerator()
  sdramCd.holdDuration.load(63)
  sdramCd.asyncReset(debugCd)

  val systemCd = ClockDomainResetGenerator()
  systemCd.holdDuration.load(63)
  systemCd.asyncReset(sdramCd)
  systemCd.setInput(
    debugCd.outputClockDomain,
    omitReset = true
  )

  // ...
  val system = new ArtyA7SmpLinuxAbstract(cpuCount){
    val vgaPhy = vga.withRegisterPhy(withColorEn = false)
  }
  system.onClockDomain(systemCd.outputClockDomain)
  system.sdramA.onClockDomain(sdramCd.outputClockDomain)

  // Enable native JTAG debug
  val debug = system.withDebugBus(debugCd, sdramCd, 0x10B80000).withBscane2(userId = 2)

  // The DDR3 controller use its own clock domain and need peripheral bus access for configuration
  val sdramDomain = new Generator{
    implicit val interconnect = system.interconnect

    onClockDomain(sdramCd.outputClockDomain)

    val bmbCc = BmbBridgeGenerator(mapping = SizeMapping(0x100000l, 8 KiB))
    interconnect.addConnection(system.bmbPeripheral.bmb, bmbCc.bmb).ccByToggle()

    val phyA = XilinxS7PhyBmbGenerator(configAddress = 0x1000)
    phyA.connect(system.sdramA)
    interconnect.addConnection(bmbCc.bmb, phyA.ctrl)

    system.sdramA.mapCtrlAt(0x0000)
    interconnect.addConnection(bmbCc.bmb, system.sdramA.ctrl)
  }

  //Manage clocks and PLL
  val clocking = add task new Area{
    val GCLK100 = in Bool()

    val pll = new BlackBox{
      setDefinitionName("PLLE2_ADV")

      addGenerics(
        "CLKIN1_PERIOD" -> 10.0,
        "CLKFBOUT_MULT" -> 12,
        "CLKOUT0_DIVIDE" -> 12,
        "CLKOUT0_PHASE" -> 0,
        "CLKOUT1_DIVIDE" -> 8,
        "CLKOUT1_PHASE" -> 0,
        "CLKOUT2_DIVIDE" -> 8,
        "CLKOUT2_PHASE" -> 45,
        "CLKOUT3_DIVIDE" -> 4,
        "CLKOUT3_PHASE" -> 0,
        "CLKOUT4_DIVIDE" -> 4,
        "CLKOUT4_PHASE" -> 90,
        "CLKOUT5_DIVIDE" -> 48,
        "CLKOUT5_PHASE" -> 0
      )

      val CLKIN1   = in Bool()
      val CLKFBIN  = in Bool()
      val CLKFBOUT = out Bool()
      val CLKOUT0  = out Bool()
      val CLKOUT1  = out Bool()
      val CLKOUT2  = out Bool()
      val CLKOUT3  = out Bool()
      val CLKOUT4  = out Bool()
      val CLKOUT5  = out Bool()

      Clock.syncDrive(CLKIN1, CLKOUT1)
      Clock.syncDrive(CLKIN1, CLKOUT2)
      Clock.syncDrive(CLKIN1, CLKOUT3)
      Clock.syncDrive(CLKIN1, CLKOUT4)
      Clock.syncDrive(CLKIN1, CLKOUT5)
    }

    pll.CLKFBIN := pll.CLKFBOUT
    pll.CLKIN1 := GCLK100

    val clk25 = out Bool()
    clk25 := pll.CLKOUT5

    debugCd.setInput(
      ClockDomain(
        clock = pll.CLKOUT0,
        frequency = FixedFrequency(100 MHz)
      )
    )
    sdramCd.setInput(
      ClockDomain(
        clock = pll.CLKOUT1,
        frequency = FixedFrequency(150 MHz)
      )
    )
    vgaCd.setInput(ClockDomain(clk25))
    system.vga.vgaCd.merge(vgaCd.outputClockDomain)

    sdramDomain.phyA.clk90.load(ClockDomain(pll.CLKOUT2))
    sdramDomain.phyA.serdesClk0.load(ClockDomain(pll.CLKOUT3))
    sdramDomain.phyA.serdesClk90.load(ClockDomain(pll.CLKOUT4))
  }

  // Allow to access the native SPI flash clock pin
  val startupe2 = system.spiA.flash.produce(
    STARTUPE2.driveSpiClk(system.spiA.flash.sclk.setAsDirectionLess())
  )
}

object ArtyA7SmpLinuxAbstract{
  def default(g : ArtyA7SmpLinuxAbstract) = g {
    import g._



    // Configure the CPUs
    for((cpu, coreId) <- cores.zipWithIndex) {
      cpu.config.load(VexRiscvSmpClusterGen.vexRiscvConfig(
        hartId = coreId,
        ioRange = _ (31 downto 28) === 0x1,
        resetVector = 0x10A00000l,
        iBusWidth = 64,
        dBusWidth = 64
      ))
    }

    // Configure the peripherals
    ramA.size.load(8 KiB)
    ramA.hexInit.load(null)

    sdramA.coreParameter.load(CoreParameter(
      portTockenMin = 4,
      portTockenMax = 8,
      timingWidth = 4,
      refWidth = 16,
      stationCount  = 2,
      bytePerTaskMax = 64,
      writeLatencies = List(3),
      readLatencies = List(5+3, 5+4)
    ))

    uartA.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 128,
      rxFifoDepth = 128
    )

    gpioA.parameter load Gpio.Parameter(
      width = 32,
      interrupt = List(24, 25, 26, 27)
    )
    gpioA.connectInterrupts(plic, 4)

    spiA.parameter load SpiXdrMasterCtrl.MemoryMappingParameters(
      SpiXdrMasterCtrl.Parameters(
        dataWidth = 8,
        timerWidth = 12,
        spi = SpiXdrParameter(
          dataWidth = 2,
          ioRate = 1,
          ssWidth = 3
        )
      ) .addFullDuplex(id = 0).addHalfDuplex(id = 1, rate = 1, ddr = false, spiWidth = 1, lateSampling = false),
      cmdFifoDepth = 256,
      rspFifoDepth = 256
    )

    mac.parameter load MacEthParameter(
      phy = PhyParameter(
        txDataWidth = 4,
        rxDataWidth = 4
      ),
      rxDataWidth = 32,
      rxBufferByteSize = 4096,
      txDataWidth = 32,
      txBufferByteSize = 4096
    )

    dma.parameter.layout load DmaMemoryLayout(
      bankCount     = 2,
      bankWords     = 128,
      bankWidth     = 32,
      priorityWidth = 2
    )

    dma.setBmbParameter(
      addressWidth = 32,
      dataWidth = 64,
      lengthWidth = 6
    )

    vga.parameter load BmbVgaCtrlParameter(
      rgbConfig = RgbConfig(4,4,4)
    )

    audioOut.parameter load BsbToDeltaSigmaParameter(
      channels = 2,
      channelWidth = 16,
      rateWidth = 16
    )

    // Add some interconnect pipelining to improve FMax
    for(cpu <- cores) interconnect.setPipelining(cpu.dBus)(cmdValid = true, invValid = true, ackValid = true, syncValid = true)
    interconnect.setPipelining(fabric.exclusiveMonitor.input)(cmdValid = true, cmdReady = true, rspValid = true)
    interconnect.setPipelining(fabric.invalidationMonitor.output)(cmdValid = true, cmdReady = true, rspValid = true)
    interconnect.setPipelining(bmbPeripheral.bmb)(cmdHalfRate = true, rspHalfRate = true)
    interconnect.setPipelining(sdramA0.bmb)(cmdValid = true, cmdReady = true, rspValid = true)
    interconnect.setPipelining(fabric.iBus.bmb)(cmdValid = true)
    interconnect.setPipelining(dma.read)(cmdHalfRate = true)

    g
  }
}


object ArtyA7SmpLinux {
  //Function used to configure the SoC
  def default(g : ArtyA7SmpLinux) = g{
    import g._

    sdramDomain.phyA.sdramLayout.load(MT41K128M16JT.layout)
    ArtyA7SmpLinuxAbstract.default(system)

    system.ramA.hexInit.load("software/standalone/bootloader/build/bootloader.hex")
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig
      .copy(
        defaultConfigForClockDomains = ClockDomainConfig(resetKind = SYNC),
        inlineRom = true
      ).addStandardMemBlackboxing(blackboxByteEnables)
       .generateVerilog(InOutWrapper(default(new ArtyA7SmpLinux(2)).toComponent()))
    BspGenerator("digilent/ArtyA7SmpLinux", report.toplevel.generator, report.toplevel.generator.system.cores(0).dBus)
  }
}

object VgaDisplaySim{
  def apply(vga : Vga, cd : ClockDomain): Unit ={

    var width = 160
    var height = 120
    val image = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);

    val frame = new JFrame{
      setPreferredSize(new Dimension(800, 600));

      add(new JPanel{
        this.setPreferredSize(new Dimension(width, height))
        override def paintComponent(g : Graphics) : Unit = {
          g.drawImage(image, 0, 0, width*4,height*4, null)
        }
      })

      pack();
      setVisible(true);
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

//    def resize(newWidth : Int, newHeight : Int): Unit ={
//
//    }
    var overflow = false
    var x,y = 0
    cd.onSamplings{
      val vsync = vga.vSync.toBoolean
      val hsync = vga.hSync.toBoolean
      val colorEn = vga.colorEn.toBoolean
      if(colorEn) {
        val color = vga.color.r.toInt << (16 + 8 - vga.rgbConfig.rWidth) | vga.color.g.toInt << (8 + 8 - vga.rgbConfig.gWidth) | vga.color.b.toInt << (0 + 8 - vga.rgbConfig.bWidth)
        if(x < width && y < height) {
          image.setRGB(x, y, color)
        }
        x+=1
      }
      if(!vsync){
        y = 0
      }
      if(!hsync){
        if(x != 0){
          y+=1
          frame.repaint()
        }
        x = 0
      }
    }


  }
}


object ArtyA7SmpLinuxSystemSim {
  import spinal.core.sim._

  def main(args: Array[String]): Unit = {

    case class Config(trace : Boolean, bin : String)
    val parser = new scopt.OptionParser[Config]("SpinalCore") {
      opt[Boolean]("trace") action { (v, c) => c.copy(trace = v)} text("Store fst wave")
      opt[String]("bin") action { (v, c) => c.copy(bin = v) } text("Baremetal app")
    }

    val config = parser.parse(args, Config(
      trace = false,
      bin = "software/standalone/timerAndGpioInterruptDemo/build/timerAndGpioInterruptDemo_spinal_sim.bin"
    )) match {
      case Some(config) => config
      case None         => ???
    }


    val simConfig = SimConfig
    simConfig.allOptimisation
    if(config.trace) simConfig.withFstWave
    simConfig.addSimulatorFlag("-Wno-MULTIDRIVEN")

    simConfig.compile(new ArtyA7SmpLinuxAbstract(cpuCount = 2){
      val debugCd = ClockDomainResetGenerator()
      debugCd.enablePowerOnReset()
      debugCd.holdDuration.load(63)
      debugCd.makeExternal(
        frequency = FixedFrequency(100 MHz)
      )

      val systemCd = ClockDomainResetGenerator()
      systemCd.holdDuration.load(63)
      systemCd.setInput(debugCd)

//      val vgaCd = ClockDomainResetGenerator()
//      vgaCd.holdDuration.load(63)
//      vgaCd.makeExternal(withResetPin = false)
//      vgaCd.asyncReset(debugCd)
//
//      vga.vgaCd.merge(vgaCd.outputClockDomain)
      vga.output.derivate(_.simPublic())

      vga.vgaCd.merge(vga.generatorClockDomain)

      this.onClockDomain(systemCd.outputClockDomain)

      val phy = RtlPhyGenerator()
      phy.layout.load(XilinxS7Phy.phyLayout(MT41K128M16JT.layout, 2))
      phy.connect(sdramA)

      sdramA.mapCtrlAt(0x100000)
      interconnect.addConnection(bmbPeripheral.bmb, sdramA.ctrl)

      val jtagTap = withDebugBus(debugCd, systemCd, address = 0x10B80000).withJtag()

      ArtyA7SmpLinuxAbstract.default(this)
      ramA.hexInit.load("software/standalone/bootloader/build/bootloader_spinal_sim.hex")
    }.toComponent().setDefinitionName("miaou2")).doSimUntilVoid("test", 42){dut =>
      val debugClkPeriod = (1e12/dut.debugCd.inputClockDomain.frequency.getValue.toDouble).toLong
      val jtagClkPeriod = debugClkPeriod*4
      val uartBaudRate = 115200
      val uartBaudPeriod = (1e12/uartBaudRate).toLong

      val clockDomain = dut.debugCd.inputClockDomain.get
      clockDomain.forkStimulus(debugClkPeriod)

//      dut.vgaCd.inputClockDomain.get.forkStimulus(40000)
//      clockDomain.forkSimSpeedPrinter(2.0)


      fork{
        val at = 0
        val duration = 100
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

//      val vga = VgaDisplaySim(dut.vga.output, dut.vgaCd.inputClockDomain)
      val vga = VgaDisplaySim(dut.vga.output, clockDomain)

      dut.spiA.sdcard.data.read #= 3

      val uboot = "../u-boot/"
      val opensbi = "../opensbi/"
      val linuxPath = "../buildroot/output/images/"


      dut.phy.logic.loadBin(0x00F80000, config.bin)
//      dut.phy.logic.loadBin(0x00F80000, opensbi + "build/platform/spinal/saxon/digilent/artyA7Smp/firmware/fw_jump.bin")
//      dut.phy.logic.loadBin(0x00F00000, uboot + "u-boot.bin")
//      dut.phy.logic.loadBin(0x00000000, linuxPath + "uImage")
//      dut.phy.logic.loadBin(0x00FF0000, linuxPath + "dtb")
//      dut.phy.logic.loadBin(0x00FFFFC0, linuxPath + "rootfs.cpio.uboot")


//      dut.phy.logic.loadBin(0x00F80000, "software/standalone/audioOut/build/audioOut.bin")
//      dut.phy.logic.loadBin(0x00F80000, "software/standalone/dhrystone/build/dhrystone.bin")
//      dut.phy.logic.loadBin(0x00F80000, "software/standalone/timerAndGpioInterruptDemo/build/timerAndGpioInterruptDemo_spinal_sim.bin")
//      dut.phy.logic.loadBin(0x00F80000, "software/standalone/freertosDemo/build/freertosDemo_spinal_sim.bin")
      println("DRAM loading done")
    }
  }
}
