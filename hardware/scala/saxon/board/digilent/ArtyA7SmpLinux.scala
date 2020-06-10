package saxon.board.digilent

import java.nio.file.{Files, Paths}

import saxon.board.blackice.peripheral.Apb3I2cGenerator
import saxon.common.I2cModel
import saxon.{ResetSensitivity, _}
import spinal.core._
import spinal.core.sim._
import spinal.lib.blackbox.xilinx.s7.{BSCANE2, BUFG, STARTUPE2}
import spinal.lib.bus.amba3.apb.Apb3Config
import spinal.lib.bus.amba3.apb.sim.{Apb3Listener, Apb3Monitor}
import spinal.lib.bus.bmb.{Bmb, BmbDecoder}
import spinal.lib.bus.bmb.sim.BmbMonitor
import spinal.lib.bus.misc.{AddressMapping, SizeMapping}
import spinal.lib.bus.simple.{PipelinedMemoryBus, PipelinedMemoryBusDecoder}
import spinal.lib.com.i2c.{I2cMasterMemoryMappedGenerics, I2cSlaveGenerics, I2cSlaveMemoryMappedGenerics}
import spinal.lib.com.i2c.sim.OpenDrainInterconnect
import spinal.lib.com.jtag.{Jtag, JtagTap, JtagTapInstructionCtrl}
import spinal.lib.com.jtag.sim.JtagTcp
import spinal.lib.com.jtag.xilinx.Bscane2BmbMasterGenerator
import spinal.lib.com.spi.SpiHalfDuplexMaster
import spinal.lib.com.spi.ddr.{SpiXdrMasterCtrl, SpiXdrParameter}
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}
import spinal.lib.eda.bench.{Bench, Rtl, XilinxStdTargets}
import spinal.lib.generator._
import spinal.lib.io.{Gpio, InOutWrapper}
import spinal.lib.{master, slave}
import spinal.lib.memory.sdram.sdr._
import spinal.lib.memory.sdram.sdr.sim.SdramModel
import spinal.lib.memory.sdram.xdr.CoreParameter
import spinal.lib.memory.sdram.xdr.phy.XilinxS7Phy
import spinal.lib.misc.plic.PlicMapping
import spinal.lib.system.debugger.{JtagBridge, JtagBridgeNoTap, SystemDebugger, SystemDebuggerConfig}
import vexriscv.demo.smp.{VexRiscvSmpCluster, VexRiscvSmpClusterGen}
import vexriscv.plugin.CsrPlugin





class VexRiscvSmpGenerator  extends Generator {
  implicit val interconnect = BmbSmpInterconnectGenerator()
  val bmbPeripheral = BmbSmpBridgeGenerator(mapping = SizeMapping(0x10000000, 16 MiB)).peripheral(dataWidth = 32)
  implicit val peripheralDecoder = bmbPeripheral.asPeripheralDecoder()

  val plic = BmbPlicGenerator(0xC00000)
  plic.priorityWidth.load(2)
  plic.mapping.load(PlicMapping.sifive)

  val clint = BmbClintGenerator(0xB00000)

  val uartA = BmbUartGenerator(0x10000)
  uartA.connectInterrupt(plic, 1)
}


class ArtyA7SmpLinuxSystem() extends VexRiscvSmpGenerator{
  val ramA = BmbSmpOnChipRamGenerator(0xA00000l)
  ramA.hexOffset = 0x10000000 //TODO
  ramA.dataWidth.load(32)
  interconnect.addConnection(bmbPeripheral.bmb, ramA.bmb)

  val sdramA = SdramXdrBmb2SmpGenerator(memoryAddress = 0x80000000l)
  val sdramA0 = sdramA.addPort()


  val iBridge = BmbSmpBridgeGenerator()
  val exclusiveMonitor = BmbExclusiveMonitorGenerator()
  val invalidationMonitor = BmbInvalidateMonitorGenerator() //TODO add context remover

  interconnect.addConnection(exclusiveMonitor.output, invalidationMonitor.input)


  val cpuCount = Handle(2)
  val cores = for(cpuId <- 0 until cpuCount) yield new Area{
    val cpu = VexRiscvBmbGenerator()
    interconnect.addConnection(
      cpu.iBus -> List(iBridge.bmb),
      cpu.dBus -> List(exclusiveMonitor.input)
    )
    cpu.setTimerInterrupt(clint.timerInterrupt(cpuId))
    cpu.setSoftwareInterrupt(clint.softwareInterrupt(cpuId))
    plic.priorityWidth.load(2)
    plic.mapping.load(PlicMapping.sifive)
    plic.addTarget(cpu.externalInterrupt)
    plic.addTarget(cpu.externalSupervisorInterrupt)
    List(clint.logic, cpu.logic).produce{
      for (plugin <- cpu.config.plugins) plugin match {
        case plugin : CsrPlugin if plugin.utime != null =>plugin.utime := clint.logic.io.time
        case _ =>
      }
    }
  }
  export(cpuCount)

  clint.cpuCount.merge(cpuCount)

  interconnect.addConnection(
    iBridge.bmb -> List(sdramA0.bmb, bmbPeripheral.bmb),
    invalidationMonitor.output -> List(sdramA0.bmb, bmbPeripheral.bmb)
  )
  interconnect.masters(invalidationMonitor.output).withOutOfOrderDecoder()


  val gpioA = BmbGpioGenerator(0x00000)

  val spiA = new BmbSpiGenerator(0x20000){
    val decoder = SpiPhyDecoderGenerator(phy)
    val user = decoder.spiMasterNone()
    val flash = decoder.spiMasterId(0)
    val sdcard = decoder.spiMasterId(1)
  }
}

class ArtyA7SmpLinux extends Generator{
  val debugCd = ClockDomainResetGenerator()
  debugCd.holdDuration.load(4095)
  debugCd.enablePowerOnReset()

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

  val system = new ArtyA7SmpLinuxSystem()
  system.onClockDomain(systemCd.outputClockDomain)
  system.sdramA.onClockDomain(sdramCd.outputClockDomain)

  val debug = Bscane2BmbMasterGenerator(userId = 2)(system.interconnect) onClockDomain(debugCd.outputClockDomain)
  for(i <- 0 until system.cpuCount) {
    system.cores(i).cpu.enableDebugBmb(debugCd, sdramCd, SizeMapping(0x10B80000 + i*0x1000, 0x1000))
    system.interconnect.addConnection(debug.bmb, system.cores(i).cpu.debugBmb)
  }

  val sdramDomain = new Generator{
    implicit val interconnect = system.interconnect

    onClockDomain(sdramCd.outputClockDomain)

    val bmbCc = BmbSmpBridgeGenerator(mapping = SizeMapping(0x100000l, 8 KiB))
    interconnect.addConnection(system.bmbPeripheral.bmb, bmbCc.bmb).ccByToggle()

    val phyA = XilinxS7PhyBmbGenerator(configAddress = 0x1000)
    phyA.connect(system.sdramA)
    interconnect.addConnection(bmbCc.bmb, phyA.ctrl)

    system.sdramA.mapCtrlAt(0x0000)
    interconnect.addConnection(bmbCc.bmb, system.sdramA.ctrlBus)
  }

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
        "CLKOUT4_PHASE" -> 90
      )

      val CLKIN1   = in Bool()
      val CLKFBIN  = in Bool()
      val CLKFBOUT = out Bool()
      val CLKOUT0  = out Bool()
      val CLKOUT1  = out Bool()
      val CLKOUT2  = out Bool()
      val CLKOUT3  = out Bool()
      val CLKOUT4  = out Bool()

      Clock.syncDrive(CLKIN1, CLKOUT1)
      Clock.syncDrive(CLKIN1, CLKOUT2)
      Clock.syncDrive(CLKIN1, CLKOUT3)
      Clock.syncDrive(CLKIN1, CLKOUT4)
    }

    pll.CLKFBIN := pll.CLKFBOUT
    pll.CLKIN1 := GCLK100

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
    sdramDomain.phyA.clk90.load(ClockDomain(pll.CLKOUT2))
    sdramDomain.phyA.serdesClk0.load(ClockDomain(pll.CLKOUT3))
    sdramDomain.phyA.serdesClk90.load(ClockDomain(pll.CLKOUT4))
  }

  val startupe2 = system.spiA.flash.produce(
    STARTUPE2.driveSpiClk(system.spiA.flash.sclk.setAsDirectionLess())
  )
}

object ArtyA7SmpLinuxSystem{
  def default(g : ArtyA7SmpLinuxSystem, debugCd : ClockDomainResetGenerator, resetCd : ClockDomainResetGenerator) = g {
    import g._

    for(coreId <- 0 until cpuCount) {
      cores(coreId).cpu.config.load(VexRiscvSmpClusterGen.vexRiscvConfig(
        hartId = coreId,
        ioRange = _ (31 downto 28) === 0x1,
        resetVector = 0x10A00000l,
        iBusWidth = 64,
        dBusWidth = 64
      ))
//      cores(coreId).cpu.enableDebugBus(debugCd, resetCd)
//
//      cores.map(_.cpu.debugBus)
//      if(coreId == 0){
//        cores(0).cpu.enableJtag(debugCd, resetCd)
//      } else {
//        cores(coreId).cpu.disableDebug()
//      }
    }

//    new Generator{
//      dependencies ++= cores.map(_.cpu.debugBus)
//      onClockDomain(debugCd.outputClockDomain)
//
//      val logic = add task new Area{
//        val jtagConfig = SystemDebuggerConfig(
//          memAddressWidth = 32,
//          memDataWidth    = 32,
//          remoteCmdWidth  = 1
//        )
//        val jtagBridge = new JtagBridge(jtagConfig)
//        val debugger = new SystemDebugger(jtagConfig)
//        debugger.io.remote <> jtagBridge.io.remote
//
//        val mmMaster = debugger.io.mem.toPipelinedMemoryBus()
//        val mmDecoder = PipelinedMemoryBusDecoder(
//          busConfig = mmMaster.config,
//          mappings = Seq.tabulate(cpuCount)(c => SizeMapping(0x10B80000 + c*0x100, 0x1000))
//        )
//        mmDecoder.io.input << mmMaster
//        for(coreId <- 0 until cpuCount) {
//          mmDecoder.io.outputs(coreId) >> cores(coreId).cpu.debugBus.fromPipelinedMemoryBus()
//        }
//      }
//    }
//    cores(0).cpu.config.load(VexRiscvConfigs.linuxTest(0x20000000l, openSbi = true))



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

    for(core <- cores) interconnect.setConnector(core.cpu.dBus)(_.pipelined(cmdValid = true, invValid = true, ackValid = true, syncValid = true) >> _)
    interconnect.setConnector(exclusiveMonitor.input)(_.pipelined(cmdValid = true, cmdReady = true, rspValid = true) >> _)
    interconnect.setConnector(invalidationMonitor.output)(_.pipelined(cmdValid = true, cmdReady = true, rspValid = true) >> _)
    interconnect.setConnector(bmbPeripheral.bmb)(_.pipelined(cmdHalfRate = true, rspHalfRate = true) >> _)
    interconnect.setConnector(sdramA0.bmb)(_.pipelined(cmdValid = true, cmdReady = true, rspValid = true) >> _)
    interconnect.setConnector(iBridge.bmb)(_.pipelined(cmdValid = true) >> _)

    g
  }
}


object ArtyA7SmpLinux {
  //Function used to configure the SoC
  def default(g : ArtyA7SmpLinux) = g{
    import g._

    sdramDomain.phyA.sdramLayout.load(MT41K128M16JT.layout)
    ArtyA7SmpLinuxSystem.default(system, debugCd, sdramCd)

   /* def vivadoDebug(that : Data) : Unit = that.addAttribute("""mark_debug = "true"""")
    system.plic.logic.derivate{plic =>
      plic.targets.foreach{target =>
        vivadoDebug(target.ie)
        vivadoDebug(target.claim)
        vivadoDebug(target.threshold)
        vivadoDebug(target.iep)
      }
      system.plic.gateways.foreach{gateway =>
        vivadoDebug(gateway.ip)
      }
    }*/

//    system.ramA.bmb.derivate{bus =>
//      bus.cmd.valid.addAttribute("""mark_debug = "true"""")
//      bus.cmd.ready.addAttribute("""mark_debug = "true"""")
//      bus.cmd.address.addAttribute("""mark_debug = "true"""")
//      bus.cmd.data.addAttribute("""mark_debug = "true"""")
//    }
//    system.clint.logic.derivate(_.logic.time.addAttribute("""mark_debug = "true""""))
 /*   system.cores.foreach(_.cpu.externalInterrupt.derivate(_.addAttribute("""mark_debug = "true"""")))
    system.cores.foreach(_.cpu.externalSupervisorInterrupt.derivate(_.addAttribute("""mark_debug = "true"""")))

    system.cores.foreach{ core =>
      core.cpu.logic derivate{ cpu =>
        List(
          "CsrPlugin_privilege",
          "CsrPlugin_sip_SEIP_SOFT",
          "CsrPlugin_sip_SEIP_INPUT",
          "CsrPlugin_sip_SEIP_OR",
          "CsrPlugin_sip_STIP",
          "CsrPlugin_sip_SSIP",
          "CsrPlugin_sie_SEIE",
          "CsrPlugin_sie_STIE",
          "CsrPlugin_sie_SSIE",
          "CsrPlugin_sstatus_SIE"
        ).foreach(n =>  vivadoDebug(cpu.cpu.reflectBaseType(n)))
      }
    }*/

//    debug.logic.derivate(d => Cat(
//      d.bscane2.TMS.pull(),
//      d.bscane2.RESET.pull(),
//      d.bscane2.SHIFT.pull(),
//      d.bscane2.UPDATE.pull(),
//      d.bscane2.CAPTURE.pull(),
//      d.bscane2.SEL.pull(),
//      d.bscane2.DRCK.pull(),
//      d.bscane2.TCK.pull()
//    ).asOutput().setName("ja"))



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
       .generateVerilog(InOutWrapper(default(new ArtyA7SmpLinux()).toComponent()))
    BspGenerator("digilent/ArtyA7SmpLinux", report.toplevel.generator, report.toplevel.generator.system.cores(0).cpu.dBus)
  }
}






object ArtyA7SmpLinuxSystemSim {
  import spinal.core.sim._

  def main(args: Array[String]): Unit = {

    val simConfig = SimConfig
    simConfig.allOptimisation
    simConfig.withWave
    simConfig.withFstWave
//    simConfig.withConfig(SpinalConfig(anonymSignalPrefix = "zz_"))
    simConfig.addSimulatorFlag("-Wno-MULTIDRIVEN")

    simConfig.compile(new ArtyA7SmpLinuxSystem(){
      val debugCd = ClockDomainResetGenerator()
      debugCd.enablePowerOnReset()
      debugCd.holdDuration.load(63)
      debugCd.makeExternal(
        frequency = FixedFrequency(100 MHz)
      )

      val systemCd = ClockDomainResetGenerator()
      systemCd.holdDuration.load(63)
      systemCd.setInput(debugCd)

      this.onClockDomain(systemCd.outputClockDomain)

      val phy = RtlPhyGenerator()
      phy.layout.load(XilinxS7Phy.phyLayout(MT41K128M16JT.layout, 2))
      phy.connect(sdramA)
//      phy.logic.derivate(_.ram.simPublic())

      sdramA.mapCtrlAt(0x100000)
      interconnect.addConnection(bmbPeripheral.bmb, sdramA.ctrlBus)

      val bridge = JtagTapDebuggerGenerator() onClockDomain(debugCd.outputClockDomain)
      for(i <- 0 until cpuCount) {
        cores(i).cpu.enableDebugBmb(debugCd, systemCd, SizeMapping(0x10B80000 + i*0x1000, 0x1000))
        interconnect.addConnection(bridge.bmb, cores(i).cpu.debugBmb)
      }

      ArtyA7SmpLinuxSystem.default(this, debugCd, systemCd)
      ramA.hexInit.load("software/standalone/bootloader/build/bootloader_spinal_sim.hex")
    }.toComponent()).doSimUntilVoid("test", 42){dut =>
      val debugClkPeriod = (1e12/dut.debugCd.inputClockDomain.frequency.getValue.toDouble).toLong
      val jtagClkPeriod = debugClkPeriod*4
      val uartBaudRate = 115200
      val uartBaudPeriod = (1e12/uartBaudRate).toLong

      val clockDomain = dut.debugCd.inputClockDomain.get
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
        println("********************\n\n")
        sleep(duration*1000000000l)
        while(true) {
          disableSimWave()
          sleep(100000 * 10000)
          enableSimWave()
          sleep(  100 * 10000)
        }
      }

//      fork{
//        val at = 40
//        val duration = 999
//        disableSimWave()
//        clockDomain.waitSampling(100)
//        waitUntil(dut.uartA.rxd.get.toBoolean == false)
//        println("\n\n********************")
//        enableSimWave()
//        sleep(duration*1000000000l)
//        println("********************\n\n")
//        while(true) {
//          disableSimWave()
//          sleep(100000 * 10000)
//          enableSimWave()
//          sleep(  100 * 10000)
//        }
//      }

//      fork{
//        while(true) {
//          disableSimWave()
//          sleep(100000 * 10000)
//          enableSimWave()
//          sleep(  100 * 10000)
//        }
//      }

      val tcpJtag = JtagTcp(
        jtag = dut.bridge.jtag,
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

      val uboot = "../u-boot/"
      val opensbi = "../opensbi/"
      val linuxPath = "../buildroot/output/images/"

      dut.phy.logic.loadBin(0x00F80000, opensbi + "build/platform/spinal/saxon/digilent/artyA7Smp/firmware/fw_jump.bin")
      dut.phy.logic.loadBin(0x00F00000, uboot + "u-boot.bin")
      dut.phy.logic.loadBin(0x00000000, linuxPath + "uImage")
      dut.phy.logic.loadBin(0x00FF0000, linuxPath + "dtb")
      dut.phy.logic.loadBin(0x00FFFFC0, linuxPath + "rootfs.cpio.uboot")

      println("DRAM loading done")
    }
  }
}



//
//object ArtyA7SmpLinuxSynthesis{
//  def main(args: Array[String]): Unit = {
//    val soc = new Rtl {
//      override def getName(): String = "ArtyA7SmpLinux"
//      override def getRtlPath(): String = "ArtyA7SmpLinux.v"
//      SpinalConfig(defaultConfigForClockDomains = ClockDomainConfig(resetKind = SYNC), inlineRom = true)
//        .generateVerilog(InOutWrapper(ArtyA7SmpLinux.default(new ArtyA7SmpLinux()).toComponent()).setDefinitionName(getRtlPath().split("\\.").head))
//    }
//
//    val rtls = List(soc)
////    val targets = XilinxStdTargets(
////      vivadoArtix7Path = "/media/miaou/HD/linux/Xilinx/Vivado/2018.3/bin"
////    )
//    val targets = List(
//      new Target {
//        override def getFamilyName(): String = "Artix 7"
//        override def synthesise(rtl: Rtl, workspace: String): Report = {
//          VivadoFlow(
//            vivadoPath=vivadoArtix7Path,
//            workspacePath=workspace,
//            toplevelPath=rtl.getRtlPath(),
//            family=getFamilyName(),
//            device="xc7a35ticsg324-1L" // xc7k70t-fbg676-3"
//          )
//        }
//      }
//    )
//
//    Bench(rtls, targets, "/media/miaou/HD/linux/tmp")
//  }
//}
//
//
//
//
