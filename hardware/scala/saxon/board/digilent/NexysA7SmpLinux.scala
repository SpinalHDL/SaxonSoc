package saxon.board.digilent

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
import vexriscv.ip.fpu.{FpuCore, FpuParameter}
import vexriscv.plugin.{AesPlugin, FpuPlugin}


// Define a SoC abstract enough to be used in simulation (no PLL, no PHY)
class NexysA7SmpLinuxAbtract(cpuCount : Int) extends VexRiscvClusterGenerator(cpuCount){
  val fabric = withDefaultFabric()

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
  val eth = mac.withPhyRmii()

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

  val ramA = BmbOnChipRamGenerator(0xA00000l)
  ramA.hexOffset = bmbPeripheral.mapping.lowerBound
  interconnect.addConnection(bmbPeripheral.bmb, ramA.ctrl)

  interconnect.addConnection(
    fabric.iBus.bmb -> List(sdramA0.bmb, bmbPeripheral.bmb),
    fabric.dBus.bmb -> List(sdramA0.bmb, bmbPeripheral.bmb)
  )

  val fpu = new Area{
    val logic = Handle{
      new FpuCore(
        portCount = cpuCount,
        p =  FpuParameter(
          withDouble = true,
          asyncRegFile = false
        )
      )
    }

    val connect = Handle{
      for(i <- 0 until cpuCount;
          vex = cores(i).logic.cpu;
          port = logic.io.port(i)) {
        val plugin = vex.service(classOf[FpuPlugin])
        plugin.port.cmd >> port.cmd
        plugin.port.commit >> port.commit
        plugin.port.completion := port.completion.stage()
        plugin.port.rsp << port.rsp

        if (i == 0) {
          println("cpuDecode to fpuDispatch " + LatencyAnalysis(vex.decode.arbitration.isValid, logic.decode.input.valid))
          println("fpuDispatch to cpuRsp    " + LatencyAnalysis(logic.decode.input.valid, plugin.port.rsp.valid))

          println("cpuWriteback to fpuAdd   " + LatencyAnalysis(vex.writeBack.input(plugin.FPU_COMMIT), logic.commitLogic(0).add.counter))

          println("add                      " + LatencyAnalysis(logic.decode.add.rs1.mantissa, logic.merge.arbitrated.value.mantissa))
          println("mul                      " + LatencyAnalysis(logic.decode.mul.rs1.mantissa, logic.merge.arbitrated.value.mantissa))
          println("fma                      " + LatencyAnalysis(logic.decode.mul.rs1.mantissa, logic.decode.add.rs1.mantissa, logic.merge.arbitrated.value.mantissa))
          println("short                    " + LatencyAnalysis(logic.decode.shortPip.rs1.mantissa, logic.merge.arbitrated.value.mantissa))

        }
      }
    }
  }
}

class NexysA7SmpLinux(cpuCount : Int) extends Component{
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

  val system = systemCd.outputClockDomain on new NexysA7SmpLinuxAbtract(cpuCount){
    val vgaPhy = vga.withRegisterPhy(withColorEn = false)
    sdramA_cd.load(sdramCd.outputClockDomain)

    // Enable native JTAG debug
    val debugBus = this.withDebugBus(debugCd.outputClockDomain, sdramCd, 0x10B80000)
    val nativeJtag = debugBus.withBscane2(userId = 2)
  }



  // The DDR controller use its own clock domain and need peripheral bus access for configuration
  val sdramDomain = sdramCd.outputClockDomain on  new Area{
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
      setDefinitionName("MMCME2_ADV")

      addGenerics(
        "CLKIN1_PERIOD" -> 10.0,
        "CLKFBOUT_MULT_F" -> 12,
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

      Clock.syncDrive(CLKIN1, CLKOUT0)
      Clock.syncDrive(CLKIN1, CLKOUT1)
      Clock.syncDrive(CLKIN1, CLKOUT2)
      Clock.syncDrive(CLKIN1, CLKOUT3)
      Clock.syncDrive(CLKIN1, CLKOUT4)
      Clock.syncDrive(CLKIN1, CLKOUT5)
      Clock.syncDrive(CLKIN1, CLKOUT6)
    }

    pll.CLKFBIN := pll.CLKFBOUT
    pll.CLKIN1 := GCLK100

    val clk50 = out Bool()
    clk50 := pll.CLKOUT5
    system.mac.txCd.load(ClockDomain(pll.CLKOUT5))
    system.mac.rxCd.load(ClockDomain(pll.CLKOUT5))

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
    vgaCd.setInput(
      ClockDomain(
        clock = pll.CLKOUT6,
        frequency = FixedFrequency(40 MHz)
      )
    )
    system.vga.vgaCd.load(vgaCd.outputClockDomain)

    sdramDomain.phyA.clk90.load(ClockDomain(pll.CLKOUT2))
    sdramDomain.phyA.serdesClk0.load(ClockDomain(pll.CLKOUT3))
    sdramDomain.phyA.serdesClk90.load(ClockDomain(pll.CLKOUT4))
  }

  val audioOut = new Area{
    val sd = out(True)
  }

  // Allow to access the native SPI flash clock pin
  val startupe2 = system.spiA.flash.produce(
    STARTUPE2.driveSpiClk(system.spiA.flash.sclk.setAsDirectionLess())
  )
}

object NexysA7SmpLinuxAbstract{
  def default(g : NexysA7SmpLinuxAbtract) = g.rework {
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
      writeLatencies = List(2),
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
        txDataWidth = 2,
        rxDataWidth = 2
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
    interconnect.setPipelining(fabric.dBus.bmb)(cmdValid = true, cmdReady = true)
    interconnect.setPipelining(bmbPeripheral.bmb)(cmdHalfRate = true, rspHalfRate = true)
    interconnect.setPipelining(sdramA0.bmb)(cmdValid = true, cmdReady = true, rspValid = true)
    interconnect.setPipelining(fabric.iBus.bmb)(cmdValid = true)
    interconnect.setPipelining(dma.read)(cmdHalfRate = true)

    g
  }
}


object NexysA7SmpLinux {
  def default(g : NexysA7SmpLinux) = g.rework{
    import g._
    sdramDomain.phyA.sdramLayout.load(MT47H64M16HR.layout)
    NexysA7SmpLinuxAbstract.default(system)
    system.ramA.hexInit.load("software/standalone/bootloader/build/bootloader.hex")
    g
  }

  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig
      .copy(
        defaultConfigForClockDomains = ClockDomainConfig(resetKind = SYNC),
        inlineRom = true
      ).addStandardMemBlackboxing(blackboxByteEnables)
       .generateVerilog(InOutWrapper(default(new NexysA7SmpLinux(2))))
    BspGenerator("digilent/NexysA7SmpLinux", report.toplevel, report.toplevel.system.cores(0).dBus)
  }
}






object NexysA7SmpLinuxSystemSim {
  import spinal.core.sim._

  def main(args: Array[String]): Unit = {

    val simConfig = SimConfig
    simConfig.allOptimisation
//    simConfig.withFstWave
//    simConfig.withConfig(SpinalConfig(anonymSignalPrefix = "zz_"))
    simConfig.addSimulatorFlag("-Wno-MULTIDRIVEN")

    simConfig.compile(new Component {
      val debugCd = ClockDomainResetGenerator()
      debugCd.enablePowerOnReset()
      debugCd.holdDuration.load(63)
      debugCd.makeExternal(
        frequency = FixedFrequency(100 MHz)
      )

      val systemCd = ClockDomainResetGenerator()
      systemCd.holdDuration.load(63)
      systemCd.setInput(debugCd)


      val top = systemCd.outputClockDomain on new NexysA7SmpLinuxAbtract(cpuCount = 2){
        val phy = RtlPhyGenerator()
        phy.layout.load(XilinxS7Phy.phyLayout(MT47H64M16HR.layout, 2))
        phy.connect(sdramA)
        //      phy.logic.derivate(_.ram.simPublic())

        sdramA.mapCtrlAt(0x100000)

        val jtagTap = withDebugBus(debugCd.outputClockDomain, systemCd, address = 0x10B80000).withJtag()

        NexysA7SmpLinuxAbstract.default(this)

        val macCd = ClockDomain.external("macCd", withReset = false)
        mac.txCd.load(macCd)
        mac.rxCd.load(macCd)

        vga.vgaCd.load(systemCd.outputClockDomain)
        sdramA_cd.load(systemCd.outputClockDomain)

        ramA.hexInit.load("software/standalone/bootloader/build/bootloader_spinal_sim.hex")
        //      ramA.hexInit.load("software/standalone/ethernet/build/ethernet.hex")
      }

    }.setDefinitionName("miaou2")).doSimUntilVoid("test", 42){dut =>
      val debugClkPeriod = (1e12/dut.debugCd.inputClockDomain.frequency.getValue.toDouble).toLong
      val jtagClkPeriod = debugClkPeriod*4
      val uartBaudRate = 115200
      val uartBaudPeriod = (1e12/uartBaudRate).toLong

      val clockDomain = dut.debugCd.inputClockDomain.get
      clockDomain.forkStimulus(debugClkPeriod)
//      clockDomain.forkSimSpeedPrinter(2.0)


      fork{
        val at = 0
        val duration = 1
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

      val images = "../buildroot-build/images/"

      dut.top.phy.logic.loadBin(0x00F80000, images + "fw_jump.bin")
      dut.top.phy.logic.loadBin(0x00F00000, images + "u-boot.bin")
//      dut.phy.logic.loadBin(0x00000000, images + "Image")
//      dut.phy.logic.loadBin(0x00FF0000, images + "linux.dtb")
//      dut.phy.logic.loadBin(0x00FFFFC0, images + "rootfs.cpio.uboot")
//
//      //Bypass uboot
//      dut.phy.logic.loadBytes(0x00F00000, Seq(0xb7, 0x0f, 0x00, 0x80, 0xe7, 0x80, 0x0f,0x00).map(_.toByte))  //Seq(0x80000fb7, 0x000f80e7)

//      dut.top.phy.logic.loadBin(0x00F80000, "software/standalone/ethernet/build/ethernet_spinal_sim.bin")
//      dut.phy.logic.loadBin(0x00F80000, "software/standalone/dhrystone/build/dhrystone.bin")
      println("DRAM loading done")

      dut.top.macCd.forkStimulus(20000)
      var inPacket = false
      var packet = BigInt(0)
      var counter = 0
      dut.top.macCd.onSamplings{
        if(dut.top.eth.mii.TX.EN.toBoolean){
          inPacket = true
          if(counter % 2 == 0){
            packet |= dut.top.eth.mii.TX.D.toInt
          } else {
            packet |= dut.top.eth.mii.TX.D.toInt << 2
            packet <<= 4
          }
          counter += 1
        } else {
          if(inPacket){
            println(packet.toString(16))
            packet = 0
            counter = 0
          }
          inPacket = false
        }
      }
    }
  }
}
