package saxon.board.digilent

import saxon.{ResetSensitivity, _}
import spinal.core._
import spinal.core.sim._
import spinal.lib.blackbox.xilinx.s7.BUFG
import spinal.lib.bus.amba3.apb.Apb3Config
import spinal.lib.bus.amba3.apb.sim.{Apb3Listener, Apb3Monitor}
import spinal.lib.com.jtag.sim.JtagTcp
import spinal.lib.com.spi.ddr.{SpiXdrMasterCtrl, SpiXdrParameter}
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}
import spinal.lib.eda.bench.{Bench, Rtl, XilinxStdTargets}
import spinal.lib.generator._
import spinal.lib.io.{Gpio, InOutWrapper}
import spinal.lib.memory.sdram.sdr._
import spinal.lib.memory.sdram.sdr.sim.SdramModel
import spinal.lib.memory.sdram.xdr.CoreParameter
import spinal.lib.memory.sdram.xdr.phy.XilinxS7Phy




class Arty7LinuxSystem() extends SaxonSocLinux{
  //Add components
  val gpioA = Apb3GpioGenerator(0x00000)


  val ramA = BmbOnChipRamGenerator(0x20000000l)
  ramA.dataWidth.load(32)

  val sdramA = SdramXdrBmbGenerator(
    memoryAddress = 0x80000000l
  )

  val sdramA0 = sdramA.addPort()
//  val sdramA1 = sdramA.addPort()

  val bridge = BmbBridgeGenerator()
  interconnect.addConnection(
    cpu.iBus -> List(bridge.bmb),
    cpu.dBus -> List(bridge.bmb),
    bridge.bmb -> List(ramA.bmb, sdramA0.bmb, peripheralBridge.input)
  )

//Interconnect specification
//  interconnect.addConnection(
//    cpu.iBus -> List(sdramA0.bmb),
//    cpu.dBus -> List(sdramA0.bmb, peripheralBridge.input)
//  )


  //  val bridge = BmbBridgeGenerator()
//  interconnect.addConnection(
//    cpu.iBus -> List(sdramA1.bmb),
//    cpu.dBus -> List(ramA.bmb, sdramA0.bmb, peripheralBridge.input)
//  )
//
//  interconnect.setConnector(sdramA1.bmb){case (m,s) =>
//    m.cmd.halfPipe() >> s.cmd
//    m.rsp << s.rsp.halfPipe()
//  }
}

class Arty7Linux extends Generator{
  val mainClockCtrl = ClockDomainGenerator()
  mainClockCtrl.resetHoldDuration.load(255)
  mainClockCtrl.resetSynchronous.load(false)
  mainClockCtrl.powerOnReset.load(true)
  mainClockCtrl.resetBuffer.load(e => BUFG.on(e))


  val sdramClockCtrl = ClockDomainGenerator()
  sdramClockCtrl.resetHoldDuration.load(0)
  sdramClockCtrl.resetSynchronous.load(false)
  sdramClockCtrl.powerOnReset.load(false)
  sdramClockCtrl.resetBuffer.load(e => BUFG.on(e))
  sdramClockCtrl.resetSensitivity.load(ResetSensitivity.HIGH)
  mainClockCtrl.clockDomain.produce(sdramClockCtrl.reset.load(mainClockCtrl.clockDomain.reset))


  val system = new Arty7LinuxSystem()
  system.onClockDomain(mainClockCtrl.clockDomain)
  system.sdramA.onClockDomain(sdramClockCtrl.clockDomain)

  val sdramDomain = new Generator{
    onClockDomain(sdramClockCtrl.clockDomain)

    val apbDecoder = Apb3DecoderGenerator()
    apbDecoder.addSlave(system.sdramA.apb, 0x0000)

    val phyA = XilinxS7PhyGenerator(configAddress = 0x1000)(apbDecoder)
    phyA.connect(system.sdramA)

    val sdramApbBridge = Apb3CCGenerator() //TODO size optimisation
    sdramApbBridge.mapAt(0x100000l)(system.apbDecoder)
    sdramApbBridge.setOutput(apbDecoder.input)
    sdramApbBridge.inputClockDomain.merge(mainClockCtrl.clockDomain)
    sdramApbBridge.outputClockDomain.merge(sdramClockCtrl.clockDomain)
  }

//  val clocking = add task new Area{
//    val GCLK100 = in Bool()
//
//
//    mainClockCtrl.clkFrequency.load(100 MHz)
//    sdramClockCtrl.clkFrequency.load(150 MHz)
//    val pll = new BlackBox{
//      setDefinitionName("PLLE2_ADV")
//
//      addGenerics(
//        "CLKIN1_PERIOD" -> 10.0,
//        "CLKFBOUT_MULT" -> 12,
//        "CLKOUT0_DIVIDE" -> 16,
//        "CLKOUT0_PHASE" -> 0,
//        "CLKOUT1_DIVIDE" -> 16,
//        "CLKOUT1_PHASE" -> 45,
//        "CLKOUT2_DIVIDE" -> 8,
//        "CLKOUT2_PHASE" -> 0,
//        "CLKOUT3_DIVIDE" -> 8,
//        "CLKOUT3_PHASE" -> 90
//      )
//
//      val CLKIN1   = in Bool()
//      val CLKFBIN  = in Bool()
//      val CLKFBOUT = out Bool()
//      val CLKOUT0  = out Bool()
//      val CLKOUT1  = out Bool()
//      val CLKOUT2  = out Bool()
//      val CLKOUT3  = out Bool()
//    }
//
//    pll.CLKFBIN := pll.CLKFBOUT
//    pll.CLKIN1 := GCLK100
//
//    mainClockCtrl.clock.load(BUFG.on(pll.CLKOUT0))
//
//    sdramClockCtrl.clock.load(mainClockCtrl.clock.get)
//    sdramDomain.phyA.clk90.load(ClockDomain(BUFG.on(pll.CLKOUT1)))
//    sdramDomain.phyA.serdesClk0.load(ClockDomain(BUFG.on(pll.CLKOUT2)))
//    sdramDomain.phyA.serdesClk90.load(ClockDomain(BUFG.on(pll.CLKOUT3)))
//  }

  val clocking = add task new Area{
    val GCLK100 = in Bool()


    mainClockCtrl.clkFrequency.load(100 MHz)
    sdramClockCtrl.clkFrequency.load(150 MHz)
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

//      Clock.syncDrive(CLKIN1, CLKOUT0)
      Clock.syncDrive(CLKIN1, CLKOUT1)
      Clock.syncDrive(CLKIN1, CLKOUT2)
      Clock.syncDrive(CLKIN1, CLKOUT3)
      Clock.syncDrive(CLKIN1, CLKOUT4)
    }

    pll.CLKFBIN := pll.CLKFBOUT
    pll.CLKIN1 := GCLK100

    mainClockCtrl.clock.load(pll.CLKOUT0)

    sdramClockCtrl.clock.load(pll.CLKOUT1)
    sdramDomain.phyA.clk90.load(ClockDomain(pll.CLKOUT2))
    sdramDomain.phyA.serdesClk0.load(ClockDomain(pll.CLKOUT3))
    sdramDomain.phyA.serdesClk90.load(ClockDomain(pll.CLKOUT4))
  }
}



object Arty7LinuxSystem{
  def default(g : Arty7LinuxSystem, clockCtrl : ClockDomainGenerator) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.linuxTest(0x20000000l))
    cpu.produce(cpu.externalSupervisorInterrupt.load(Bool)) //TMP patch
    cpu.enableJtag(clockCtrl)

    ramA.size.load(8 KiB)
    ramA.hexInit.load(null)

    sdramA.coreParameter.load(CoreParameter(
      portTockenMin = 4,
      portTockenMax = 8,
      timingWidth = 4,
      refWidth = 16,
      writeLatencies = List(3),
      readLatencies = List(5+3, 5+4)
    ))

    uartA.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 128,
      rxFifoDepth = 128
    )

    gpioA.parameter load Gpio.Parameter(
      width = 0
    )

    interconnect.setConnector(peripheralBridge.input){case (m,s) =>
      m.cmd.halfPipe >> s.cmd
      m.rsp << s.rsp.halfPipe()
    }
    interconnect.setConnector(sdramA0.bmb){case (m,s) =>
      m.cmd >/-> s.cmd
      m.rsp <-< s.rsp
    }
//    interconnect.setConnector(cpu.iBus){case (m,s) =>
//      m.cmd.halfPipe() >> s.cmd
//      m.rsp << s.rsp.halfPipe()
//    }
//    interconnect.setConnector(cpu.dBus){case (m,s) =>
//      m.cmd.halfPipe() >> s.cmd
//      m.rsp << s.rsp.halfPipe()
//    }
    interconnect.setConnector(bridge.bmb){case (m,s) =>
      m.cmd >/-> s.cmd
      m.rsp <-< s.rsp
    }
    g
  }
}


object Arty7Linux {
  //Function used to configure the SoC
  def default(g : Arty7Linux) = g{
    import g._
    mainClockCtrl.resetSensitivity.load(ResetSensitivity.NONE)
    sdramDomain.phyA.sdramLayout.load(MT41K128M16JT.layout)
    Arty7LinuxSystem.default(system, mainClockCtrl)
//    system.ramA.hexInit.load("software/standalone/bootloader/build/bootloader.hex")
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig
      .copy(
        defaultConfigForClockDomains = ClockDomainConfig(resetKind = SYNC),
        inlineRom = true
      ).generateVerilog(InOutWrapper(default(new Arty7Linux()).toComponent()))
    BspGenerator("Arty7Linux", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
  }
}






object Arty7LinuxSystemSim {
  import spinal.core.sim._

  def main(args: Array[String]): Unit = {

    val simConfig = SimConfig
    simConfig.allOptimisation
//    simConfig.withWave
    simConfig.addSimulatorFlag("-Wno-MULTIDRIVEN")
    simConfig.compile(new Arty7LinuxSystem(){
      val clockCtrl = ClockDomainGenerator()
      this.onClockDomain(clockCtrl.clockDomain)
      clockCtrl.makeExternal(ResetSensitivity.HIGH)
      clockCtrl.powerOnReset.load(true)
      clockCtrl.clkFrequency.load(100 MHz)
      clockCtrl.resetHoldDuration.load(15)

      val phy = RtlPhyGenerator()
      phy.layout.load(XilinxS7Phy.phyLayout(MT41K128M16JT.layout, 2))
      phy.connect(sdramA)

      apbDecoder.addSlave(sdramA.apb, 0x100000l)

      Arty7LinuxSystem.default(this, clockCtrl)
      ramA.hexInit.load("software/standalone/bootloader/build/bootloader.hex")
    }.toComponent()).doSimUntilVoid("test", 42){dut =>
      val systemClkPeriod = (1e12/dut.clockCtrl.clkFrequency.toDouble).toLong
      val jtagClkPeriod = systemClkPeriod*4
      val uartBaudRate = 115200
      val uartBaudPeriod = (1e12/uartBaudRate).toLong

      val clockDomain = ClockDomain(dut.clockCtrl.clock, dut.clockCtrl.reset)
      clockDomain.forkStimulus(systemClkPeriod)
//      dut.sdramClockDomain.get.forkStimulus((systemClkPeriod/0.7).toInt)

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

      val linuxPath = "../buildroot/output/images/"
      dut.phy.io.loadBin(0x00000000, "software/standalone/machineModeSbi/build/machineModeSbi.bin")
      dut.phy.io.loadBin(0x00400000, linuxPath + "Image")
      dut.phy.io.loadBin(0x00BF0000, linuxPath + "dtb")
      dut.phy.io.loadBin(0x00C00000, linuxPath + "rootfs.cpio")

      println("DRAM loading done")

    }
  }
}



//
//object Arty7LinuxSynthesis{
//  def main(args: Array[String]): Unit = {
//    val soc = new Rtl {
//      override def getName(): String = "Arty7Linux"
//      override def getRtlPath(): String = "Arty7Linux.v"
//      SpinalConfig(defaultConfigForClockDomains = ClockDomainConfig(resetKind = SYNC), inlineRom = true)
//        .generateVerilog(InOutWrapper(Arty7Linux.default(new Arty7Linux()).toComponent()).setDefinitionName(getRtlPath().split("\\.").head))
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
