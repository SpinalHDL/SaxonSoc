package saxon.board.qmtech

import java.awt.image.BufferedImage
import java.awt.{Color, Dimension, Graphics}
import java.io.{BufferedWriter, ByteArrayInputStream, ByteArrayOutputStream, File, FileInputStream, FileOutputStream, FileWriter}
import javax.swing.{JFrame, JPanel, WindowConstants}
import saxon.common.I2cModel
import saxon._
import spinal.core._
import spinal.core.fiber._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.blackbox.xilinx.s7.{BSCANE2, BUFG, IBUF, Mmcme2CtrlGenerator, Mmcme2Dbus, STARTUPE2}
import spinal.lib.bus.bmb._
import spinal.lib.bus.bsb.BsbInterconnectGenerator
import spinal.lib.bus.misc.{AddressMapping, SizeMapping}
import spinal.lib.bus.simple.{PipelinedMemoryBus, PipelinedMemoryBusDecoder}
import spinal.lib.com.jtag.sim.JtagTcp
import spinal.lib.com.jtag.{Jtag, JtagTap, JtagTapDebuggerGenerator, JtagTapInstructionCtrl}
import spinal.lib.com.jtag.xilinx.Bscane2BmbMasterGenerator
import spinal.lib.com.spi.ddr.{SpiXdrMasterCtrl, SpiXdrParameter}
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}
import spinal.lib.com.usb.ohci.{OhciPortParameter, UsbOhciGenerator, UsbOhciParameter, UsbPid}
import spinal.lib.com.usb.sim.{UsbDeviceAgent, UsbDeviceAgentListener, UsbLsFsPhyAbstractIoAgent}
import spinal.lib.com.usb.udc.{UsbDeviceBmbGenerator, UsbDeviceCtrlParameter}
import spinal.lib.generator._
import spinal.lib.generator_backup.Handle.initImplicit
import spinal.lib.graphic.RgbConfig
import spinal.lib.graphic.vga.{BmbVgaCtrlGenerator, BmbVgaCtrlParameter, Vga}
import spinal.lib.io.{Gpio, InOutWrapper}
import spinal.lib.memory.sdram.sdr._
import spinal.lib.memory.sdram.xdr.CoreParameter
import spinal.lib.memory.sdram.xdr.phy.XilinxS7Phy
import spinal.lib.misc.analog.{BmbBsbToDeltaSigmaGenerator, BsbToDeltaSigmaParameter}
import spinal.lib.system.dma.sg.{DmaMemoryLayout, DmaSgGenerator}
import vexriscv.demo.smp.VexRiscvSmpClusterGen
import vexriscv.ip.{DataCache, InstructionCache}
import vexriscv.ip.fpu.{FpuCore, FpuParameter}
import vexriscv.plugin.{AesPlugin, FpuPlugin}

import scala.collection.mutable
import scala.util.Random
import spinal.lib.com.i2c.I2cSlaveMemoryMappedGenerics
import spinal.lib.com.i2c.I2cSlaveConfig
import spinal.lib.com.i2c.I2cSlaveGenerics
import spinal.lib.com.i2c.I2cMasterMemoryMappedGenerics
import vexriscv.plugin.CsrPlugin


// Define a SoC abstract enough to be used in simulation (no PLL, no PHY)
class QmtechK7SmpLinuxAbstract(cpuCount : Int) extends VexRiscvClusterGenerator(cpuCount){
  val fabric = withDefaultFabric()  

  val fpu = new FpuIntegration(){
    setParameters(extraStage = false)
  }

  val sdramA_cd = Handle[ClockDomain]
  val sdramA = sdramA_cd on SdramXdrBmbGenerator(memoryAddress = 0x80000000l)
  val sdramA0 = sdramA.addPort()

  val gpioA = BmbGpioGenerator(0x00000)

  val uartA = BmbUartGenerator(0x10000)
  uartA.connectInterrupt(plic, 1)

  val spiA = new BmbSpiGenerator(0x20000){
    val decoder = SpiPhyDecoderGenerator(phy)
    val user = decoder.spiMasterNone()
    val flash = decoder.spiMasterId(0)
    val sdcard = decoder.spiMasterId(1)
  }

  val i2c = BmbI2cGenerator(0x30000)
  i2c.connectInterrupt(plic, 17)

  implicit val bsbInterconnect = BsbInterconnectGenerator()
  val dma = new DmaSgGenerator(0x80000){
    val vga = new Area{
      val channel = createChannel()
      channel.fixedBurst(64)
      channel.withCircularMode()
      channel.withScatterGatter()
      channel.fifoMapping load Some(0, 256)
      channel.connectInterrupt(plic, 14)

      val stream = createOutput(byteCount = 8)
      channel.outputsPorts += stream

    }

    val audioOut = new Area{
      val channel = createChannel()
      channel.fixedBurst(64)
      channel.withScatterGatter()
      channel.fifoMapping load Some(256, 256)
      channel.connectInterrupt(plic, 15)

      val stream = createOutput(byteCount = 4)
      channel.outputsPorts += stream
    }
  }
 // interconnect.addConnection(dma.write, fabric.dBusCoherent.bmb)
  interconnect.addConnection(dma.read,     fabric.iBus.bmb)
  interconnect.addConnection(dma.readSg,   fabric.iBus.bmb)
  interconnect.addConnection(dma.writeSg,  fabric.dBusCoherent.bmb)

  val vga = BmbVgaCtrlGenerator(0x90000)
  bsbInterconnect.connect(dma.vga.stream.output, vga.input)

  val audioOut = BmbBsbToDeltaSigmaGenerator(0x94000)
  bsbInterconnect.connect(dma.audioOut.stream.output, audioOut.input)

  val usbACtrl = new UsbOhciGenerator(0xA0000)
  plic.addInterrupt(usbACtrl.interrupt, 16)
  interconnect.addConnection(usbACtrl.dma, fabric.dBusCoherent.bmb)

  val pllReconfig = new Mmcme2CtrlGenerator(0x91000)

  val ramA = BmbOnChipRamGenerator(0xA00000l)
  ramA.hexOffset = bmbPeripheral.mapping.lowerBound
  interconnect.addConnection(bmbPeripheral.bmb, ramA.ctrl)

  interconnect.addConnection(
    fabric.iBus.bmb -> List(sdramA0.bmb, bmbPeripheral.bmb),
    fabric.dBus.bmb -> List(sdramA0.bmb, bmbPeripheral.bmb)
  )
}

class QmtechK7SmpLinux(cpuCount : Int) extends Component{
  // Define the clock domains used by the SoC
  val debugCdCtrl = ClockDomainResetGenerator()
  debugCdCtrl.holdDuration.load(4095)
  debugCdCtrl.enablePowerOnReset()

  val vgaCdCtrl = ClockDomainResetGenerator()
  vgaCdCtrl.holdDuration.load(63)
  vgaCdCtrl.asyncReset(debugCdCtrl)

  val usbCdCtrl = ClockDomainResetGenerator()
  usbCdCtrl.holdDuration.load(63)
  usbCdCtrl.asyncReset(debugCdCtrl)

  val sdramCdCtrl = ClockDomainResetGenerator()
  sdramCdCtrl.holdDuration.load(63)
  sdramCdCtrl.asyncReset(debugCdCtrl)

  val systemCdCtrl = ClockDomainResetGenerator()
  systemCdCtrl.holdDuration.load(63)
  systemCdCtrl.asyncReset(sdramCdCtrl)
  systemCdCtrl.setInput(
    debugCdCtrl.outputClockDomain,
    omitReset = true
  )

  val debugCd  = BUFG.onReset(debugCdCtrl.outputClockDomain)
  val sdramCd  = BUFG.onReset(sdramCdCtrl.outputClockDomain)
  val systemCd = BUFG.onReset(systemCdCtrl.outputClockDomain)
  val vgaCd    = vgaCdCtrl.outputClockDomain
  val usbCd    = usbCdCtrl.outputClockDomain

  val system = systemCd on new QmtechK7SmpLinuxAbstract(cpuCount){
    val vgaPhy = vga.withRegisterPhy(withColorEn = false)
    sdramA_cd.load(sdramCd)

    // Enable native JTAG debug
    val debugBus = this.withDebugBus(debugCd, sdramCdCtrl, 0x10B80000)
    val nativeJtag = debugBus.withBscane2(userId = 2)

    val usbAPhy = usbCd(usbACtrl.createPhyDefault())
    val usbAPort = usbCd(usbAPhy.createInferableIo())
  }

  // The DDR3 controller use its own clock domain and need peripheral bus access for configuration
  val sdramDomain = sdramCd on new Area{
    implicit val interconnect = system.interconnect

    val bmbCc = BmbBridgeGenerator(mapping = SizeMapping(0x100000l, 8 KiB))
    interconnect.addConnection(system.bmbPeripheral.bmb, bmbCc.bmb).ccByToggle()

    val phyA = XilinxS7PhyBmbGenerator(configAddress = 0x1000)
    phyA.connect(system.sdramA)
    interconnect.addConnection(bmbCc.bmb, phyA.ctrl)

    system.sdramA.mapCtrlAt(0x0000)
    interconnect.addConnection(bmbCc.bmb, system.sdramA.ctrl)
  }

  //Manage clocks and PLL
  val clocking = new Area{
    val GCLK50 = in Bool()
    val GCLK50_B = BUFG.on(GCLK50)

    val pll = new BlackBox{
      setDefinitionName("MMCME2_BASE") //MMCME2_BASE

      addGenerics(
        "CLKIN1_PERIOD" -> 20.0,
        "CLKFBOUT_MULT_F" -> 24,
        "CLKOUT0_DIVIDE_F" -> 12,
        "CLKOUT0_PHASE" -> 0,
        "CLKOUT1_DIVIDE" -> 8,
        "CLKOUT1_PHASE" -> 0,
        "CLKOUT2_DIVIDE" -> 8,
        "CLKOUT2_PHASE" -> 45,
        "CLKOUT3_DIVIDE" -> 4,
        "CLKOUT3_PHASE" -> 0,
        "CLKOUT4_DIVIDE" -> 4,
        "CLKOUT4_PHASE" -> 90,
        "CLKOUT5_DIVIDE" -> 24,
        "CLKOUT5_PHASE" -> 0,
        "CLKOUT6_DIVIDE" -> 25,
        "CLKOUT6_PHASE" -> 0
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
      val CLKOUT6  = out Bool()

      Clock.syncDrive(CLKIN1, CLKOUT1)
      Clock.syncDrive(CLKIN1, CLKOUT2)
      Clock.syncDrive(CLKIN1, CLKOUT3)
      Clock.syncDrive(CLKIN1, CLKOUT4)
      Clock.syncDrive(CLKIN1, CLKOUT5)
    }

    pll.CLKFBIN := pll.CLKFBOUT
    pll.CLKIN1 := IBUF.on(GCLK50_B)

    val pll2 = new BlackBox{
      setDefinitionName("MMCME2_ADV")

      addGenerics(
        "CLKIN1_PERIOD"    -> 10.0,
        "CLKFBOUT_MULT_F"  -> 12,
//        "DIVCLK_DIVIDE"    -> ,
        "CLKOUT0_DIVIDE_F" -> 48,
        "CLKOUT0_PHASE"    -> 0
      )

      val CLKIN1   = in Bool()
      val CLKFBIN  = in Bool()
      val CLKFBOUT = out Bool()
      val CLKOUT0  = out Bool()

      val dbus = slave(Mmcme2Dbus()).setName("")
      val DCLK = in Bool()
    }

    pll2.CLKFBIN := pll2.CLKFBOUT
    pll2.CLKIN1 := pll.CLKOUT0
    Handle{
      pll2.DCLK := systemCd.clock
      pll2.dbus <> system.pllReconfig.dbus
    }

    val cd50 = ClockDomain(BUFG.on(pll.CLKOUT5))
    val clk25_gen = cd50(Reg(Bool))
    clk25_gen := !clk25_gen
    //val clk25 = out(cd50(Delay(clk25_gen, 2)))

    debugCdCtrl.setInput(
      ClockDomain(
        clock = BUFG.on(pll.CLKOUT0),
        frequency = FixedFrequency(100 MHz)
      )
    )
    sdramCdCtrl.setInput(
      ClockDomain(
        clock = BUFG.on(pll.CLKOUT1),
        frequency = FixedFrequency(150 MHz)
      )
    )
    vgaCdCtrl.setInput(ClockDomain(BUFG.on(pll2.CLKOUT0)))
    usbCdCtrl.setInput(ClockDomain(BUFG.on(pll.CLKOUT6), frequency = FixedFrequency(48 MHz)))
    system.vga.vgaCd.load(vgaCd)

    sdramDomain.phyA.clk90.load(ClockDomain(BUFG.on(pll.CLKOUT2)))
    sdramDomain.phyA.serdesClk0.load(ClockDomain(BUFG.on(pll.CLKOUT3)))
    sdramDomain.phyA.serdesClk90.load(ClockDomain(BUFG.on(pll.CLKOUT4)))
  }

  // Allow to access the native SPI flash clock pin
  val startupe2 = system.spiA.flash.produce(
    STARTUPE2.driveSpiClk(system.spiA.flash.sclk.setAsDirectionLess())
  )
}

object QmtechK7SmpLinuxAbstract{
  def default(g : QmtechK7SmpLinuxAbstract) = g.rework {
    import g._

    // Configure the CPUs
    for((cpu, coreId) <- cores.zipWithIndex) {
      cpu.config.load(VexRiscvSmpClusterGen.vexRiscvConfig(
        hartId = coreId,
        ioRange = _ (31 downto 28) === 0x1,
        resetVector = 0x10A00000l,
        iBusWidth = 64,
        dBusWidth = 64,
        loadStoreWidth = 64,
        iCacheSize = 4096*2,
        dCacheSize = 4096*2,
        iCacheWays = 2,
        dCacheWays = 2,
        iBusRelax = true,
        earlyBranch = true,
        withFloat = true,
        withDouble = true,
        externalFpu = true,
        rvc = true,
        injectorStage = true,
        prediction = vexriscv.plugin.NONE
      ))
      cpu.config.plugins += AesPlugin()
    }

    // Configure the peripherals
    ramA.size.load(8 KiB)
    ramA.hexInit.loadNothing()

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
      width = 22,
      output = List(0, 1, 2, 3, 4, 18, 19, 20),
      input = List(5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21),
      interrupt = List(5, 6, 7, 8, 9, 18, 19, 20, 21)
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

    i2c.parameter load I2cSlaveMemoryMappedGenerics(
      ctrlGenerics = I2cSlaveGenerics(
          samplingWindowSize = 3,
          samplingClockDividerWidth = 10 bits,
          timeoutWidth = 20 bits
        ),
        addressFilterCount = 4,
        masterGenerics = I2cMasterMemoryMappedGenerics(
          timerWidth = 12
        )
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
      rgbConfig = RgbConfig(5,6,5)
    )

    audioOut.parameter load BsbToDeltaSigmaParameter(
      channels = 2,
      channelWidth = 16,
      rateWidth = 16
    )

    usbACtrl.parameter load UsbOhciParameter(
      noPowerSwitching = true,
      powerSwitchingMode = true,
      noOverCurrentProtection = true,
      powerOnToPowerGoodTime = 10,
      dataWidth = 64,
      portsConfig = List.fill(4)(OhciPortParameter())
    )

    // Add some interconnect pipelining to improve FMax
    for(cpu <- cores) interconnect.setPipelining(cpu.dBus)(cmdValid = true, invValid = true, ackValid = true, syncValid = true)
    interconnect.setPipelining(fabric.exclusiveMonitor.input)(cmdValid = true, cmdReady = true, rspValid = true)
    interconnect.setPipelining(fabric.invalidationMonitor.output)(cmdValid = true, cmdReady = true, rspValid = true)
    interconnect.setPipelining(fabric.dBus.bmb)(cmdValid = true, cmdReady = true)
    interconnect.setPipelining(bmbPeripheral.bmb)(cmdHalfRate = false, rspHalfRate = true)
    interconnect.setPipelining(sdramA0.bmb)(cmdValid = true, cmdReady = true, rspValid = true)
    interconnect.setPipelining(fabric.iBus.bmb)(cmdValid = true)
    interconnect.setPipelining(dma.read)(cmdHalfRate = true)
    interconnect.setPipelining(usbACtrl.dma)(cmdValid = true, cmdReady = true, rspValid = true)
    interconnect.setPipelining(usbACtrl.ctrl)(cmdHalfRate = true)

    g
  }
}


object QmtechK7SmpLinux {
  //Generate the SoC
  def main(args: Array[String]): Unit = {

    val cpuCount = sys.env.withDefaultValue("2").apply("SAXON_CPU_COUNT").toInt

    val report = SpinalRtlConfig
      .copy(
        defaultConfigForClockDomains = ClockDomainConfig(resetKind = SYNC),
        inlineRom = true,
        verbose=true
      ).addStandardMemBlackboxing(blackboxByteEnables)
       .generateVerilog(InOutWrapper(new QmtechK7SmpLinux(cpuCount){
         sdramDomain.phyA.sdramLayout.load(MT41K128M16JT.layout)
         QmtechK7SmpLinuxAbstract.default(system)

         system.ramA.hexInit.load("software/standalone/bootloader/build/bootloader.hex")
         setDefinitionName("QmtechK7SmpLinux")

        //  //Debug
        //  val debug = out(Bits(8 bits))
        //  Handle{systemCdCtrl.outputClockDomain on {
        //    debug := 0

        //    def pip[T <: Data](that : T) = Delay(that, 3)

        //    debug(0) := pip(clocking.pll2.dbus.DEN.pull())
        //    debug(1) := pip(clocking.pll2.dbus.DWE.pull())
        //    debug(2) := pip(clocking.pll2.dbus.DRDY.pull())
        //    debug(3) := pip(system.pllReconfig.ctrl.cmd.valid.pull())
        //    debug(4) := pip(system.pllReconfig.ctrl.rsp.valid.pull())
        //  }}

       }))
    BspGenerator("bsp/qmtech/QmtechK7SmpLinux", report.toplevel, report.toplevel.system.cores(0).dBus)
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

object QmtechK7SmpLinuxSystemSim {
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
    simConfig.withFstWave
    simConfig.addSimulatorFlag("-Wno-MULTIDRIVEN")

    simConfig.compile(new Component{
      val debugCd = ClockDomainResetGenerator()
      debugCd.enablePowerOnReset()
      debugCd.holdDuration.load(63)
      debugCd.makeExternal(
        frequency = FixedFrequency(96 MHz)
      )

      val systemCd = ClockDomainResetGenerator()
      systemCd.holdDuration.load(63)
      systemCd.setInput(debugCd)

      val top = systemCd.outputClockDomain on new QmtechK7SmpLinuxAbstract(cpuCount = 2) {

        pllReconfig.dbus.loadAsync(pllReconfig.dbus.toIo())
        //      val vgaCd = ClockDomainResetGenerator()
        //      vgaCd.holdDuration.load(63)
        //      vgaCd.makeExternal(withResetPin = false)
        //      vgaCd.asyncReset(debugCd)
        //
        //      vga.vgaCd.merge(vgaCd.outputClockDomain)

        vga.output.derivate(_.simPublic())

        vga.vgaCd.load(systemCd.outputClockDomain)

        val phy = RtlPhyGenerator()
        phy.layout.load(XilinxS7Phy.phyLayout(MT41K128M16JT.layout, 2))
        phy.connect(sdramA)

        sdramA.mapCtrlAt(0x100000)

        val jtagTap = withDebugBus(debugCd.outputClockDomain, systemCd, address = 0x10B80000).withJtag()
//        withoutDebug

        sdramA_cd.load(systemCd.outputClockDomain)

        sdramA0.bmb.derivate(_.cmd.simPublic())

        val usbAPhy = usbACtrl.createPhyDefault()
        val usbAPort = usbAPhy.createSimIo()

        Handle(fabric.dBusCoherent.bmb.get.simPublic())

        QmtechK7SmpLinuxAbstract.default(this)
        ramA.hexInit.load("software/standalone/bootloader/build/bootloader_spinal_sim.hex")
      }
    }.setDefinitionName("miaou2")).doSimUntilVoid("test", 42){dut =>
      val debugClkPeriod = (1e12/dut.debugCd.inputClockDomain.frequency.getValue.toDouble).toLong
      val jtagClkPeriod = debugClkPeriod*4
      val uartBaudRate = 115200
      val uartBaudPeriod = (1e12/uartBaudRate).toLong

      val clockDomain = dut.debugCd.inputClockDomain.get
      clockDomain.forkStimulus(debugClkPeriod)

      forkSimSporadicWave(
        captures = Seq(
          0e-3 -> 1e-3
          //            400e-3 -> 750e-3
        )
      )

      val tcpJtag = JtagTcp(
        jtag = dut.top.jtagTap.jtag,
        jtagClkPeriod = jtagClkPeriod
      )

      val uartTx = UartDecoder(
        uartPin =  dut.top.uartA.uart.txd,
        baudPeriod = uartBaudPeriod
      )

      val uartRx = UartEncoder(
        uartPin = dut.top.uartA.uart.rxd,
        baudPeriod = uartBaudPeriod
      )

      dut.top.usbAPort._1.foreach{usb =>
        usb.rx.dp #= false
        usb.rx.dm #= false
      }

      clockDomain.onSamplings(dut.top.pllReconfig.dbus.DRDY #= Random.nextDouble() < 0.1)

      val images = "../buildroot-build/images/"

      //dut.top.phy.logic.loadBin(0x00F80000, images + "fw_jump.bin")
      //dut.top.phy.logic.loadBin(0x00E00000, images + "u-boot.bin")
      //dut.top.phy.logic.loadBin(0x00000000, images + "Image")
      //dut.top.phy.logic.loadBin(0x00FF0000, images + "linux.dtb")
//      dut.top.phy.logic.loadBin(0x00FFFFC0, images + "rootfs.cpio.uboot")
//Bypass uboot  WARNING maybe the following line need to bu updated
      //dut.top.phy.logic.loadBytes(0x00E00000, Seq(0xb7, 0x0f, 0x00, 0x80, 0xe7, 0x80, 0x0f,0x00).map(_.toByte))  //Seq(0x80000fb7, 0x000f80e7)

      dut.top.phy.logic.loadBin(0x00F80000, config.bin)
      println("DRAM loading done")
    }
  }
}