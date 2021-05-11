package saxon.board.digilent

import java.awt.image.BufferedImage
import java.awt.{Color, Dimension, Graphics}
import java.io.{BufferedWriter, ByteArrayInputStream, ByteArrayOutputStream, File, FileInputStream, FileOutputStream, FileWriter}
import javax.swing.{JFrame, JPanel, WindowConstants}
import saxon.common.I2cModel
import saxon._
import spinal.core._
import spinal.core.fiber._
import spinal.core.sim._
import spinal.lib.{Delay, LatencyAnalysis}
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
import spinal.lib.com.usb.ohci.{OhciPortParameter, UsbOhciGenerator, UsbOhciParameter, UsbPid}
import spinal.lib.com.usb.sim.{UsbDeviceAgent, UsbDeviceAgentListener, UsbLsFsPhyAbstractIoAgent}
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
import vexriscv.ip.fpu.{FpuCore, FpuParameter}
import vexriscv.plugin.{AesPlugin, FpuPlugin}

import scala.collection.mutable


// Define a SoC abstract enough to be used in simulation (no PLL, no PHY)
class ArtyA7SmpLinuxAbstract(cpuCount : Int) extends VexRiscvClusterGenerator(cpuCount){
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


  val ramA = BmbOnChipRamGenerator(0xA00000l)
  ramA.hexOffset = bmbPeripheral.mapping.lowerBound
  interconnect.addConnection(bmbPeripheral.bmb, ramA.ctrl)

  interconnect.addConnection(
    fabric.iBus.bmb -> List(sdramA0.bmb, bmbPeripheral.bmb),
    fabric.dBus.bmb -> List(sdramA0.bmb, bmbPeripheral.bmb)
  )
}

class ArtyA7SmpLinux(cpuCount : Int) extends Component{
  // Define the clock domains used by the SoC
  val debugCdCtrl = ClockDomainResetGenerator()
  debugCdCtrl.holdDuration.load(4095)
  debugCdCtrl.enablePowerOnReset()

  val vgaCdCtrl = ClockDomainResetGenerator()
  vgaCdCtrl.holdDuration.load(63)
  vgaCdCtrl.asyncReset(debugCdCtrl)

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


  val system = systemCd on new ArtyA7SmpLinuxAbstract(cpuCount){
    val vgaPhy = vga.withRegisterPhy(withColorEn = false)
    sdramA_cd.load(sdramCd)

    // Enable native JTAG debug
    val debugBus = this.withDebugBus(debugCd, sdramCdCtrl, 0x10B80000)
    val nativeJtag = debugBus.withBscane2(userId = 2)

    val usbAPhy = usbACtrl.createPhyDefault()
    val usbAPort = usbAPhy.createInferableIo()
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
    val GCLK100 = in Bool()

    val pll = new BlackBox{
      setDefinitionName("MMCME2_BASE") //MMCME2_BASE

      addGenerics(
        "CLKIN1_PERIOD" -> 10.0,
        "CLKFBOUT_MULT_F" -> 12,
        "CLKOUT0_DIVIDE_F" -> 12.5,
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
        "CLKOUT5_PHASE" -> 0,
        "CLKOUT6_DIVIDE" -> 30,
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
    pll.CLKIN1 := GCLK100

//    val pll2 = new BlackBox{
//      setDefinitionName("PLLE2_ADV")
//
//      addGenerics(
//        "CLKIN1_PERIOD" -> 10.0,
//        "CLKFBOUT_MULT" -> 48,
//        "DIVCLK_DIVIDE" -> 5,
//        "CLKOUT0_DIVIDE" -> 10,
//        "CLKOUT0_PHASE" -> 0//,
////        "CLKOUT1_DIVIDE" -> 24,
////        "CLKOUT1_PHASE" -> 0
//      )
//
//      val CLKIN1   = in Bool()
//      val CLKFBIN  = in Bool()
//      val CLKFBOUT = out Bool()
//      val CLKOUT0  = out Bool()
////      val CLKOUT1  = out Bool()
//      //      Clock.syncDrive(CLKIN1, CLKOUT0)
//    }
//
//
//    pll2.CLKFBIN := pll2.CLKFBOUT
//    pll2.CLKIN1 := GCLK100

    val clk25 = out Bool()
    clk25 := pll.CLKOUT5

    debugCdCtrl.setInput(
      ClockDomain(
        clock = pll.CLKOUT0,
        frequency = FixedFrequency(96 MHz)
      )
    )
    sdramCdCtrl.setInput(
      ClockDomain(
        clock = pll.CLKOUT1,
        frequency = FixedFrequency(150 MHz)
      )
    )
    vgaCdCtrl.setInput(ClockDomain(pll.CLKOUT6))
    system.vga.vgaCd.load(vgaCd)

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
  def default(g : ArtyA7SmpLinuxAbstract) = g.rework {
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
        externalFpu = true
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

//    interconnect.lock.retain()

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
      rxBufferByteSize = 8*1024,
      txDataWidth = 32,
      txBufferByteSize = 4*1024
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

    usbACtrl.parameter load UsbOhciParameter(
      noPowerSwitching = true,
      powerSwitchingMode = true,
      noOverCurrentProtection = true,
      powerOnToPowerGoodTime = 10,
      fsRatio = 96/12,
      dataWidth = 64,
      portsConfig = List.fill(4)(OhciPortParameter())
    )

    // Add some interconnect pipelining to improve FMax
    for(cpu <- cores) interconnect.setPipelining(cpu.dBus)(cmdValid = true, invValid = true, ackValid = true, syncValid = true)
    interconnect.setPipelining(fabric.exclusiveMonitor.input)(cmdValid = true, cmdReady = true, rspValid = true)
    interconnect.setPipelining(fabric.invalidationMonitor.output)(cmdValid = true, cmdReady = true, rspValid = true)
    interconnect.setPipelining(fabric.dBus.bmb)(cmdValid = true, cmdReady = true)
    interconnect.setPipelining(bmbPeripheral.bmb)(cmdHalfRate = true, rspHalfRate = true)
    interconnect.setPipelining(sdramA0.bmb)(cmdValid = true, cmdReady = true, rspValid = true)
    interconnect.setPipelining(fabric.iBus.bmb)(cmdValid = true)
    interconnect.setPipelining(dma.read)(cmdHalfRate = true)
    interconnect.setPipelining(usbACtrl.dma)(cmdValid = true, cmdReady = true, rspValid = true)

    g
  }
}


object ArtyA7SmpLinux {
  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val cpuCount = sys.env.apply("SAXON_CPU_COUNT").toInt

    val report = SpinalRtlConfig
      .copy(
        defaultConfigForClockDomains = ClockDomainConfig(resetKind = SYNC),
        inlineRom = true
      ).addStandardMemBlackboxing(blackboxByteEnables)
       .generateVerilog(InOutWrapper(new ArtyA7SmpLinux(cpuCount){
         sdramDomain.phyA.sdramLayout.load(MT41K128M16JT.layout)
         ArtyA7SmpLinuxAbstract.default(system)

         system.ramA.hexInit.load("software/standalone/bootloader/build/bootloader.hex")
         setDefinitionName("ArtyA7SmpLinux")

         //Debug
//         val ja = out(Bits(8 bits))
//         systemCdCtrl.outputClockDomain on {
//           ja := 0
//           ja(0, 2 bits) := Delay(system.usbACtrl.logic.endpoint.flowType.pull.asBits, 3)
//           ja(2) := Delay(system.usbACtrl.logic.endpoint.ED.F.pull, 3)
//           ja(3) := Delay(system.usbACtrl.logic.endpoint.TD.retire.pull, 3)
//           ja(4, 4 bits) := Delay(system.usbACtrl.logic.endpoint.TD.CC.pull, 3)
////           ja(0, cpuCount bits) := Delay(B(system.fpu.logic.io.port.map(_.cmd.fire)), 3)
////           ja(4, cpuCount bits) := Delay(B(system.fpu.logic.io.port.map(_.cmd.isStall)), 3)
//         }

       }))
    BspGenerator("digilent/ArtyA7SmpLinux", report.toplevel, report.toplevel.system.cores(0).dBus)
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

      val top = systemCd.outputClockDomain on new ArtyA7SmpLinuxAbstract(cpuCount = 1) {

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

        ArtyA7SmpLinuxAbstract.default(this)
        ramA.hexInit.load("software/standalone/bootloader/build/bootloader_spinal_sim.hex")
      }
    }.setDefinitionName("miaou2")).doSimUntilVoid("test", 42){dut =>
      val debugClkPeriod = (1e12/dut.debugCd.inputClockDomain.frequency.getValue.toDouble).toLong
      val jtagClkPeriod = debugClkPeriod*4
      val uartBaudRate = 115200
      val uartBaudPeriod = (1e12/uartBaudRate).toLong

      val clockDomain = dut.debugCd.inputClockDomain.get
      clockDomain.forkStimulus(debugClkPeriod)

//      dut.vgaCd.inputClockDomain.get.forkStimulus(40000)
//      clockDomain.forkSimSpeedPrinter(2.0)



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

      delayed(10e9.toLong) {
        val fakeIn = new ByteArrayInputStream("\n\n\nusb start\nusb stop\nusb start\n".map(_.toByte).toArray);
        System.setIn(fakeIn);
      }

      val usbAgent = new UsbLsFsPhyAbstractIoAgent(dut.top.usbAPort.get.apply(0), clockDomain, 96/12)
      val usbDevice = new UsbDeviceAgent(usbAgent)
      usbDevice.allowSporadicReset = true
      usbDevice.connect(lowSpeed = true)
      usbDevice.listener = new UsbDeviceAgentListener{
        def log(msg : String) = println(simTime() + " : " + msg)
        def rsp(body : => Unit) = delayed(2000000){body}
        override def reset() = {
          log("USB RESET")
        }

        override def hcToUsb(addr: Int, endp: Int, tockenPid: Int, dataPid: Int, data: Seq[Int]) = {
          log("USB OUT ACK")
          rsp(usbAgent.emitBytes(UsbPid.ACK, Nil, false, false, true))
        }

        val inTasks = mutable.Queue[() => Unit]()
        def scheduleInRsp(pid : Int, data : Seq[Int]) = inTasks += (() => rsp(usbAgent.emitBytes(pid, data, true, false, true)))
        def scheduleInRspStr(pid : Int, data : String) = {
          assert(data.size % 2 == 0)
          scheduleInRsp(pid, data.grouped(2).map(Integer.parseInt(_, 16)).toSeq)
        }
        override def usbToHc(addr: Int, endp: Int) = {
          log("USB IN")
          if(inTasks.nonEmpty){
            inTasks.dequeue().apply()
            true
          }else {
            log("USB IN ERROR")
            false
          }
        }

        scheduleInRsp(UsbPid.DATA1, Nil)
        scheduleInRsp(UsbPid.DATA1, List(0x12, 0x01, 0x00, 0x02, 0x00, 0x00, 0x00, 0x08))
        scheduleInRsp(UsbPid.DATA0, List(0x6D, 0x04, 0x16, 0xc0, 0x40, 0x03, 0x01, 0x02))
        scheduleInRsp(UsbPid.DATA1, List(0x00, 0x01))
        scheduleInRspStr(UsbPid.DATA1, "09022200010100A0")
        scheduleInRspStr(UsbPid.DATA0, "32")
        scheduleInRspStr(UsbPid.DATA1, "09022200010100A0")
        scheduleInRspStr(UsbPid.DATA0, "3209040000010301")
        scheduleInRspStr(UsbPid.DATA1, "0200092110010001")
        scheduleInRspStr(UsbPid.DATA0, "2234000705810304")
        scheduleInRspStr(UsbPid.DATA1, "000a")
        scheduleInRspStr(UsbPid.DATA1, "")
        scheduleInRspStr(UsbPid.DATA1, "04030904")
        scheduleInRspStr(UsbPid.DATA1, "12034c006f006700")
        scheduleInRspStr(UsbPid.DATA0, "6900740065006300")
        scheduleInRspStr(UsbPid.DATA1, "6800")
        scheduleInRspStr(UsbPid.DATA1, "24034f0070007400")
        scheduleInRspStr(UsbPid.DATA0, "6900630061006c00")
        scheduleInRspStr(UsbPid.DATA1, "2000550053004200")
        scheduleInRspStr(UsbPid.DATA0, "20004d006f007500")
        scheduleInRspStr(UsbPid.DATA1, "73006500")

      }

//      fork{
//        val d = dut.top.fabric.dBusCoherent.bmb.cmd
//        var timeout = 500
//        dut.debugCd.inputClockDomain.onSamplings{
//          if(timeout == 1){
//            disableSimWave()
//          }
//          timeout -= 1
//          if(d.valid.toBoolean && (d.address.toLong & 0xFFFFF000) == 0x100a0000 || dut.top.usbAPort.get.apply(0).tx.enable.toBoolean && !dut.top.usbAPort.get.apply(0).tx.se0.toBoolean && dut.top.usbAPort.get.apply(0).tx.data.toBoolean || usbAgent.rx.enable){
//            if(timeout < 10) enableSimWave()
//            timeout = 500
//          }
//          if(timeout == -50000){
//            timeout = 500
//            enableSimWave()
//          }
//        }

        //        val at = 0
        //        val duration = 0
        //        while(simTime() < at*1000000000l) {
        //          disableSimWave()
        //          sleep(10000 * 10000)
        //          enableSimWave()
        //          sleep(  100 * 10000)
        //        }
        //        println("\n\n********************")
        //        sleep(duration*1000000000l)
        //        println("********************\n\n")
        //        while(true) {
        //          disableSimWave()
        //          sleep(100000 * 10000)
        //          enableSimWave()
        //          sleep(  100 * 10000)
        //        }
//      }


      dut.top.usbAPort.get.apply(0).overcurrent #= false

////      val vga = VgaDisplaySim(dut.vga.output, dut.vgaCd.inputClockDomain)
//      val vga = VgaDisplaySim(dut.top.vga.output, clockDomain)
//
//      dut.top.eth.mii.RX.DV #= false
//      dut.top.eth.mii.RX.ER #= false
//      dut.top.eth.mii.RX.CRS #= false
//      dut.top.eth.mii.RX.COL #= false
//      dut.top.spiA.sdcard.data.read #= 3

//      val memoryTraceFile = new FileOutputStream("memoryTrace")
//      clockDomain.onSamplings{
//        val cmd = dut.sdramA0.bmb.cmd
//        if(cmd.valid.toBoolean){
//          val address = cmd.address.toLong
//          val opcode = cmd.opcode.toInt
//          val source = cmd.source.toInt
//          val bytes = Array[Byte](opcode.toByte, source.toByte, (address >> 0).toByte, (address >> 8).toByte, (address >> 16).toByte, (address >> 24).toByte)
//          memoryTraceFile.write(bytes)
//        }
//      }

      val images = "../buildroot-build/images/"

      dut.top.phy.logic.loadBin(0x00F80000, images + "fw_jump.bin")
      dut.top.phy.logic.loadBin(0x00E00000, images + "u-boot.bin")
//      dut.top.phy.logic.loadBin(0x00000000, images + "Image")
//      dut.top.phy.logic.loadBin(0x00FF0000, images + "linux.dtb")
//      dut.top.phy.logic.loadBin(0x00FFFFC0, images + "rootfs.cpio.uboot")

      //Bypass uboot  WARNING maybe the following line need to bu updated
//      dut.top.phy.logic.loadBytes(0x00E00000, Seq(0xb7, 0x0f, 0x00, 0x80, 0xe7, 0x80, 0x0f,0x00).map(_.toByte))  //Seq(0x80000fb7, 0x000f80e7)


//        dut.top.phy.logic.loadBin(0x00F80000, "software/standalone/fpu/build/fpu.bin")
      dut.top.phy.logic.loadBin(0x00F80000, "software/standalone/test/aes/build/aes.bin")
      //dut.phy.logic.loadBin(0x00F80000, "software/standalone/dhrystone/build/dhrystone.bin")
//      dut.phy.logic.loadBin(0x00F80000, "software/standalone/timerAndGpioInterruptDemo/build/timerAndGpioInterruptDemo_spinal_sim.bin")
//      dut.phy.logic.loadBin(0x00F80000, "software/standalone/freertosDemo/build/freertosDemo_spinal_sim.bin")
      println("DRAM loading done")
    }
  }
}


//object MemoryTraceAnalyse extends App{
//  val stream = new FileInputStream("memoryTrace")
//  val data = stream.readAllBytes()
//  val size = data.size
//  println(s"Size : ${size}")
//
//  for( cacheBytes <- List(32 KiB, 64 KiB, 128 KiB, 256 KiB).map(_.toInt);
//       wayCount <- List(1, 2, 4, 8);
//       bytePerLine <- List(64)) {
//    val wayBytes = cacheBytes / wayCount
//    val linesPerWay = wayBytes / bytePerLine
//    val lineAddressShift = log2Up(bytePerLine)
//    val lineAddressMask = linesPerWay - 1
//    val tagMask = -wayBytes
//
//    var wayAllocator = 0
//    val ways = for (wayId <- 0 until wayCount) yield new {
//      val lines = for (lineId <- 0 until linesPerWay) yield new {
//        var address = 0
//        var valid = false
//        var age = 0
//
//        def hit(target: Int) = valid && (target & tagMask) == address
//      }
//    }
//
//
//    var writeThrough = true
//    var readHits = 0
//    var readMiss = 0
//    var writeHits = 0
//    var writeMiss = 0
//    for (i <- 0 until size by 6) {
//      val opcode = data(i + 0)
//      val source = data(i + 1)
//      val address = (data(i + 2) << 0) | (data(i + 3) << 8) | (data(i + 4) << 16) | (data(i + 5) << 24)
//      val lineId = (address >> lineAddressShift) & lineAddressMask
//      val allocate = !writeThrough || opcode == 0
//      ways.exists(_.lines(lineId).hit(address)) match {
//        case false => {
//          if (opcode == 0) readMiss += 1
//          else writeMiss += 1
//          if (allocate) {
//            var line = ways(0).lines(lineId)
//            for(way <- ways){
//              val alternative = way.lines(lineId)
//              if(alternative.age < line.age) line = alternative
//            }
//
//           // val line = ways(wayAllocator).lines(lineId)
//            line.valid = true
//            line.address = address & tagMask
//            line.age = i
//
//            wayAllocator += 1
//            wayAllocator %= wayCount
//          }
//        }
//        case true => {
//          if (opcode == 0) readHits += 1
//          else if(!writeThrough) writeHits += 1
//        }
//      }
//    }
//    println(f"cacheBytes=${cacheBytes/1024} KB wayCount=$wayCount bytePerLine=${bytePerLine} => readMissRate=${readMiss.toFloat/(readMiss+readHits)}%1.3f writeMissRate=${writeMiss.toFloat/(writeMiss+writeHits)}%1.3f readMiss=$readMiss writeMiss=$writeMiss")
//  }
//
//}



object UsbDebug extends App{
  val str =
    """Time [s],PID,Address,Endpoint,Frame #,Data,CRC
      |9.672193296000000,SETUP,0x00,0x00,,,0x02
      |9.672220127999999,DATA0,,,,0x00 0x05 0x02 0x00 0x00 0x00 0x00 0x00,0x16EB
      |9.672292327999999,ACK,,,,,
      |9.672307608000001,IN,0x00,0x00,,,0x02
      |9.672334255999999,DATA1,,,,,0x0000
      |9.672360191999999,ACK,,,,,
      |9.693060352000000,SETUP,0x02,0x00,,,0x15
      |9.693087183999999,DATA0,,,,0x80 0x06 0x00 0x01 0x00 0x00 0x12 0x00,0xF4E0
      |9.693158907999999,ACK,,,,,
      |9.693174184000000,IN,0x02,0x00,,,0x15
      |9.693200836000001,NAK,,,,,
      |9.693216120000001,IN,0x02,0x00,,,0x15
      |9.693242767999999,DATA1,,,,0x12 0x01 0x00 0x02 0x00 0x00 0x00 0x08,0xE757
      |9.693311292000001,ACK,,,,,
      |9.693327044000000,IN,0x02,0x00,,,0x15
      |9.693354584000000,NAK,,,,,
      |9.693369860000001,IN,0x02,0x00,,,0x15
      |9.693396512000000,DATA0,,,,0x6D 0x04 0x16 0xC0 0x40 0x03 0x01 0x02,0xF35A
      |9.693465040000000,ACK,,,,,
      |9.693480792000001,IN,0x02,0x00,,,0x15
      |9.693508324000000,DATA1,,,,0x00 0x01,0x8F3F
      |9.693545576000000,ACK,,,,,
      |9.693561327999999,OUT,0x02,0x00,,,0x15
      |9.693588172000000,DATA1,,,,,0x0000
      |9.693615480000000,ACK,,,,,
      |9.704563112000001,SETUP,0x02,0x00,,,0x15
      |9.704589944000000,DATA0,,,,0x80 0x06 0x00 0x02 0x00 0x00 0x09 0x00,0x04AE
      |9.704661752000000,ACK,,,,,
      |9.704677031999999,IN,0x02,0x00,,,0x15
      |9.704703680000000,DATA1,,,,0x09 0x02 0x22 0x00 0x01 0x01 0x00 0xA0,0x980A
      |9.704772203999999,ACK,,,,,
      |9.704787956000001,IN,0x02,0x00,,,0x15
      |9.704815496000000,DATA0,,,,0x32,0x6AC1
      |9.704846748000000,ACK,,,,,
      |9.704862500000001,OUT,0x02,0x00,,,0x15
      |9.704889344000000,DATA1,,,,,0x0000
      |9.704916660000000,ACK,,,,,
      |9.715029540000000,SETUP,0x02,0x00,,,0x15
      |9.715056371999999,DATA0,,,,0x80 0x06 0x00 0x02 0x00 0x00 0x22 0x00,0xF4B0
      |9.715128320000000,ACK,,,,,
      |9.715143604000000,IN,0x02,0x00,,,0x15
      |9.715170252000000,DATA1,,,,0x09 0x02 0x22 0x00 0x01 0x01 0x00 0xA0,0x980A
      |9.715238771999999,ACK,,,,,
      |9.715254527999999,IN,0x02,0x00,,,0x15
      |9.715282064000000,DATA0,,,,0x32 0x09 0x04 0x00 0x00 0x01 0x03 0x01,0x4D35
      |9.715350588000000,ACK,,,,,
      |9.715366336000001,IN,0x02,0x00,,,0x15
      |9.715393880000001,DATA1,,,,0x02 0x00 0x09 0x21 0x10 0x01 0x00 0x01,0x7316
      |9.715462408000000,ACK,,,,,
      |9.715478160000000,IN,0x02,0x00,,,0x15
      |9.715505692000001,NAK,,,,,
      |9.715520972000000,IN,0x02,0x00,,,0x15
      |9.715547620000001,DATA0,,,,0x22 0x34 0x00 0x07 0x05 0x81 0x03 0x04,0xE1AD
      |9.715616144000000,ACK,,,,,
      |9.715742896000000,IN,0x02,0x00,,,0x15
      |9.715770583999999,DATA1,,,,0x00 0x0A,0x487E
      |9.715807831999999,ACK,,,,,
      |9.715823584000001,OUT,0x02,0x00,,,0x15
      |9.715850428000000,DATA1,,,,,0x0000
      |9.715877740000000,ACK,,,,,
      |9.726541328000000,SETUP,0x02,0x00,,,0x15
      |9.726568159999999,DATA0,,,,0x00 0x09 0x01 0x00 0x00 0x00 0x00 0x00,0x2527
      |9.726639816000000,ACK,,,,,
      |9.726655096000000,IN,0x02,0x00,,,0x15
      |9.726681748000001,DATA1,,,,,0x0000
      |9.726707680000001,ACK,,,,,
      |9.747407084000001,SETUP,0x02,0x00,,,0x15
      |9.747433916000000,DATA0,,,,0x80 0x06 0x00 0x03 0x00 0x00 0xFF 0x00,0x64D4
      |9.747506403999999,ACK,,,,,
      |9.747521688000001,IN,0x02,0x00,,,0x15
      |9.747548331999999,NAK,,,,,
      |9.747563612000000,IN,0x02,0x00,,,0x15
      |9.747590263999999,DATA1,,,,0x04 0x03 0x09 0x04,0x7809
      |9.747637495999999,ACK,,,,,
      |9.747653248000001,OUT,0x02,0x00,,,0x15
      |9.747680092000000,DATA1,,,,,0x0000
      |9.747707404000000,ACK,,,,,
      |9.757863892000000,SETUP,0x02,0x00,,,0x15
      |9.757890723999999,DATA0,,,,0x80 0x06 0x01 0x03 0x09 0x04 0xFF 0x00,0xE897
      |9.757962996000000,ACK,,,,,
      |9.757978280000000,IN,0x02,0x00,,,0x15
      |9.758004924000000,DATA1,,,,0x12 0x03 0x4C 0x00 0x6F 0x00 0x67 0x00,0x0935
      |9.758073447999999,ACK,,,,,
      |9.758089200000001,IN,0x02,0x00,,,0x15
      |9.758116736000000,NAK,,,,,
      |9.758132015999999,IN,0x02,0x00,,,0x15
      |9.758158668000000,DATA0,,,,0x69 0x00 0x74 0x00 0x65 0x00 0x63 0x00,0x3E45
      |9.758227196000000,ACK,,,,,
      |9.758242947999999,IN,0x02,0x00,,,0x15
      |9.758270480000000,NAK,,,,,
      |9.758383112000001,IN,0x02,0x00,,,0x15
      |9.758410251999999,DATA1,,,,0x68 0x00,0x8FD1
      |9.758447496000000,ACK,,,,,
      |9.758463248000000,OUT,0x02,0x00,,,0x15
      |9.758490092000001,DATA1,,,,,0x0000
      |9.758517403999999,ACK,,,,,
      |9.769367400000000,SETUP,0x02,0x00,,,0x15
      |9.769394232000000,DATA0,,,,0x80 0x06 0x02 0x03 0x09 0x04 0xFF 0x00,0xDB97
      |9.769466508000001,ACK,,,,,
      |9.769481788000000,IN,0x02,0x00,,,0x15
      |9.769508436000001,NAK,,,,,
      |9.769523712000000,IN,0x02,0x00,,,0x15
      |9.769550368000001,NAK,,,,,
      |9.769565648000000,IN,0x02,0x00,,,0x15
      |9.769592296000001,DATA1,,,,0x24 0x03 0x4F 0x00 0x70 0x00 0x74 0x00,0xE0BC
      |9.769660820000000,ACK,,,,,
      |9.769676572000000,IN,0x02,0x00,,,0x15
      |9.769704111999999,NAK,,,,,
      |9.769823212000000,IN,0x02,0x00,,,0x15
      |9.769850536000000,DATA0,,,,0x69 0x00 0x63 0x00 0x61 0x00 0x6C 0x00,0xD942
      |9.769919056000001,ACK,,,,,
      |9.769934808000000,IN,0x02,0x00,,,0x15
      |9.769962348000000,DATA1,,,,0x20 0x00 0x55 0x00 0x53 0x00 0x42 0x00,0x0D90
      |9.770030876000000,ACK,,,,,
      |9.770046627999999,IN,0x02,0x00,,,0x15
      |9.770074160000000,NAK,,,,,
      |9.770089444000000,IN,0x02,0x00,,,0x15
      |9.770116092000000,DATA0,,,,0x20 0x00 0x4D 0x00 0x6F 0x00 0x75 0x00,0xB589
      |9.770184616000000,ACK,,,,,
      |9.770200364000001,IN,0x02,0x00,,,0x15
      |9.770227908000001,NAK,,,,,
      |9.770243192000001,IN,0x02,0x00,,,0x15
      |9.770269836000001,DATA1,,,,0x73 0x00 0x65 0x00,0x0FCE
      |9.770317732000001,ACK,,,,,
      |9.770333488000000,OUT,0x02,0x00,,,0x15
      |9.770360331999999,DATA1,,,,,0x0000
      |""".stripMargin

  var isOut = false
//  for(line <- str.lines{
////    if(line.con)
//  }


}



object UsbCaptureDecode extends App{
  import scala.io.Source

//  val filename = "/media/data/open/waves/linux_hub1_yellow_underflow"
  val filename = "/media/data/open/waves/pc_hub1_yellow"
  var state = "idle"

  val file = new File(filename + "_decoded.txt")
  val bw = new BufferedWriter(new FileWriter(file))

  var ignore = false
  for (line <- Source.fromFile(filename).getLines) {
    val split = line.split(",")
    val content = split(4).drop(1).dropRight(1)

    state match {
      case "idle" => {
        if(!ignore) {
          if(content.contains("SOF")){
            ignore = true
          } else if(content.contains("EOP")){
            bw.write("\n")
          } else if(content.contains("SYNC")){
          } else if(content.contains("Byte")){
            bw.write(content.drop(7))
          } else {
            bw.write(" " + content + " ")
          }
        }
        if(content.contains("EOP")){
          ignore = false
        }
      }
      case "packet" =>
    }
  }
  bw.flush()
  bw.close()
}