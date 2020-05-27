package saxon.board.digilent

import java.nio.file.{Files, Paths}

import saxon.board.blackice.peripheral.Apb3I2cGenerator
import saxon.common.I2cModel
import saxon.{ResetSensitivity, _}
import spinal.core._
import spinal.core.sim._
import spinal.lib.blackbox.xilinx.s7.{BUFG, STARTUPE2}
import spinal.lib.bus.amba3.apb.Apb3Config
import spinal.lib.bus.amba3.apb.sim.{Apb3Listener, Apb3Monitor}
import spinal.lib.bus.bmb.Bmb
import spinal.lib.bus.bmb.sim.BmbMonitor
import spinal.lib.com.i2c.{I2cMasterMemoryMappedGenerics, I2cSlaveGenerics, I2cSlaveMemoryMappedGenerics}
import spinal.lib.com.i2c.sim.OpenDrainInterconnect
import spinal.lib.com.jtag.sim.JtagTcp
import spinal.lib.com.spi.SpiHalfDuplexMaster
import spinal.lib.com.spi.ddr.{SpiXdrMasterCtrl, SpiXdrParameter}
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}
import spinal.lib.eda.bench.{Bench, Rtl, XilinxStdTargets}
import spinal.lib.generator._
import spinal.lib.io.{Gpio, InOutWrapper}
import spinal.lib.master
import spinal.lib.memory.sdram.sdr._
import spinal.lib.memory.sdram.sdr.sim.SdramModel
import spinal.lib.memory.sdram.xdr.CoreParameter
import spinal.lib.memory.sdram.xdr.phy.XilinxS7Phy
import spinal.lib.misc.plic.PlicMapping
import vexriscv.demo.smp.{VexRiscvSmpCluster, VexRiscvSmpClusterGen}
import vexriscv.plugin.CsrPlugin





class VexRiscvSmpGenerator  extends Generator {
  implicit val interconnect = BmbInterconnectGenerator()
  implicit val apbDecoder = Apb3DecoderGenerator()
  implicit val peripheralBridge = BmbToApb3Decoder(address = 0x10000000)

//  val exclusiveMonitor = BmbExclusiveMonitorGenerator()
//  val invalidationMonitor = BmbInvalidateMonitorGenerator()
//
//  interconnect.addConnection(exclusiveMonitor.output, invalidationMonitor.input)

  val plic = Apb3PlicGenerator(0xC00000)
  plic.priorityWidth.load(2)
  plic.mapping.load(PlicMapping.sifive)

  val clint = Apb3ClintGenerator(0xB00000)

  val uartA = Apb3UartGenerator(0x10000)
  uartA.connectInterrupt(plic, 1)
}


class ArtyA7SmpLinuxSystem() extends VexRiscvSmpGenerator{
  val ramA = BmbOnChipRamGenerator(0x20000000l)
  ramA.dataWidth.load(32)

  val sdramA = SdramXdrBmbGenerator(memoryAddress = 0x80000000l)
//
  val sdramA0 = sdramA.addPort()


  val bridge = BmbBridgeGenerator()

  val cpuCount = 1
  val cores = for(cpuId <- 0 until cpuCount) yield new Area{
    val cpu = VexRiscvBmbGenerator()
    interconnect.addConnection(
      cpu.iBus -> List(bridge.bmb),
      cpu.dBus -> List(bridge.bmb)
//      cpu.dBus -> List(exclusiveMonitor.input)
    )
    cpu.setTimerInterrupt(clint.timerInterrupt(cpuId))
    cpu.setSoftwareInterrupt(clint.softwareInterrupt(cpuId))
    plic.priorityWidth.load(2)
    plic.mapping.load(PlicMapping.sifive)
    plic.addTarget(cpu.externalInterrupt)
    plic.addTarget(cpu.externalSupervisorInterrupt)
  }

  clint.cpuCount.load(cpuCount)
  
  interconnect.addConnection(
//    invalidationMonitor.output -> List(bridge.bmb),
    bridge.bmb -> List(ramA.bmb, sdramA0.bmb, peripheralBridge.input)
  )
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

  val sdramDomain = new Generator{
    onClockDomain(sdramCd.outputClockDomain)

    val apbDecoder = Apb3DecoderGenerator()
    apbDecoder.addSlave(system.sdramA.apb, 0x0000)

    val phyA = XilinxS7PhyGenerator(configAddress = 0x1000)(apbDecoder)
    phyA.connect(system.sdramA)

    val sdramApbBridge = Apb3CCGenerator() //TODO size optimisation
    sdramApbBridge.mapAt(0x100000l)(system.apbDecoder)
    sdramApbBridge.setOutput(apbDecoder.input)
    sdramApbBridge.inputClockDomain.merge(systemCd.outputClockDomain)
    sdramApbBridge.outputClockDomain.merge(sdramCd.outputClockDomain)
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

//  val startupe2 = system.spiA.flash.produce(
//    STARTUPE2.driveSpiClk(system.spiA.flash.sclk.setAsDirectionLess())
//  )
}

object ArtyA7SmpLinuxSystem{
  def default(g : ArtyA7SmpLinuxSystem, debugCd : ClockDomainResetGenerator, resetCd : ClockDomainResetGenerator) = g {
    import g._

//    cores(0).cpu.config.load(VexRiscvSmpClusterGen.vexRiscvConfig(
//      hartId      = 0,
//      ioRange     = _(31 downto 28) === 0x1,
//      resetVector = 0x80000000l,
//      iBusWidth  = 32,
//      dBusWidth  = 32
//    ))
    cores(0).cpu.config.load(VexRiscvConfigs.linuxTest(0x20000000l, openSbi = true))
    cores(0).cpu.enableJtag(debugCd, resetCd)

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

//    gpioA.parameter load Gpio.Parameter(
//      width = 14,
//      interrupt = List(0, 1, 2, 3)
//    )
//    gpioA.connectInterrupts(plic, 4)
//
//    spiA.parameter load SpiXdrMasterCtrl.MemoryMappingParameters(
//      SpiXdrMasterCtrl.Parameters(
//        dataWidth = 8,
//        timerWidth = 12,
//        spi = SpiXdrParameter(
//          dataWidth = 2,
//          ioRate = 1,
//          ssWidth = 2
//        )
//      ) .addFullDuplex(id = 0),
//      cmdFifoDepth = 256,
//      rspFifoDepth = 256
//    )

    interconnect.setConnector(peripheralBridge.input){case (m,s) =>
      m.cmd.halfPipe >> s.cmd
      m.rsp << s.rsp.halfPipe()
    }
    interconnect.setConnector(sdramA0.bmb){case (m,s) =>
      m.cmd >/-> s.cmd
      m.rsp <-< s.rsp
    }
    interconnect.setConnector(bridge.bmb){case (m,s) =>
      m.cmd >/-> s.cmd
      m.rsp <-< s.rsp
    }
    g
  }
}


object ArtyA7SmpLinux {
  //Function used to configure the SoC
  def default(g : ArtyA7SmpLinux) = g{
    import g._
    sdramDomain.phyA.sdramLayout.load(MT41K128M16JT.layout)
    ArtyA7SmpLinuxSystem.default(system, debugCd, sdramCd)
    system.ramA.hexInit.load("software/standalone/bootloader/build/bootloader.hex")
//    system.cpu.produce(out(Bool).setName("inWfi") := system.cpu.config.plugins.find(_.isInstanceOf[CsrPlugin]).get.asInstanceOf[CsrPlugin].inWfi)
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig
      .copy(
        defaultConfigForClockDomains = ClockDomainConfig(resetKind = SYNC),
        inlineRom = true
      ).generateVerilog(InOutWrapper(default(new ArtyA7SmpLinux()).toComponent()))
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

      apbDecoder.addSlave(sdramA.apb, 0x100000l)

      ArtyA7SmpLinuxSystem.default(this, debugCd, systemCd)
      ramA.hexInit.load("software/standalone/bootloader/build/bootloader_spinal_sim.hex")
      cores(0).cpu.dBus.derivate(_.simPublic())
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
        sleep(duration*1000000000l)
        println("********************\n\n")
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
        jtag = dut.cores(0).cpu.jtag,
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

      dut.phy.io.loadBin(0x00000000, opensbi + "build/platform/spinal/saxon/digilent/artyA7Smp/firmware/fw_jump.bin")
      dut.phy.io.loadBin(0x00100000, uboot + "u-boot.bin")
      dut.phy.io.loadBin(0x003FFFC0, linuxPath + "uImage")
      dut.phy.io.loadBin(0x00FF0000, linuxPath + "dtb")
      dut.phy.io.loadBin(0x00FFFFC0, linuxPath + "rootfs.cpio.uboot")


//      fork{
//        clockDomain.waitSampling(10)
//        new BmbMonitor(dut.cores(0).cpu.dBus, clockDomain) {
//          val bin = Files.readAllBytes(Paths.get(linuxPath + "uImage"))
//          val binSize = bin.size
////          override def getByte(address: Long, value: Byte): Unit = println(f"R $address%8x $value%2x")
////          override def setByte(address: Long, value: Byte): Unit = println(f"W $address%8x $value%2x")
//
//          override def getByte(address: Long, value: Byte): Unit = {
//            val offset = (address - 0x803FFFC0l).toInt
//            if(offset >= 0 && offset < binSize) {
//              if(bin(offset) != value){
//                println(f"R failed at $address%8x $value%2x")
//              }
//            }
//          }
//          override def setByte(address: Long, value: Byte): Unit = {
//            val offset = (address - 0x803FFFC0l).toInt
//            if(offset >= 0 && offset < binSize) {
//              println(f"W failed at $address%8x $value%2x")
//            }
//          }
//        }
//      }

//      dut.phy.io.loadBin(0x01FF0000, "software/standalone/blinkAndEcho/build/blinkAndEcho_spinal_sim.bin")
//      dut.phy.io.loadBin(0x01FF0000, "software/standalone/dhrystone/build/dhrystone.bin")
//      dut.phy.io.loadBin(0x01FF0000, "software/standalone/freertosDemo/build/freertosDemo_spinal_sim.bin")
//      dut.phy.io.loadBin(0x01FF0000, "software/standalone/i2cDemo/build/i2cDemo.bin")
//      dut.phy.io.loadBin(0x01FF0000, "software/standalone/timerAndGpioInterruptDemo/build/timerAndGpioInterruptDemo_spinal_sim.bin")

//      val linuxPath = "../buildroot/output/images/"
//      dut.phy.io.loadBin(0x00000000, "software/standalone/machineModeSbi/build/machineModeSbi.bin")
//      dut.phy.io.loadBin(0x00400000, linuxPath + "Image")
//      dut.phy.io.loadBin(0x00BF0000, linuxPath + "dtb")
//      dut.phy.io.loadBin(0x00C00000, linuxPath + "rootfs.cpio")

      println("DRAM loading done")

//      fork{
//        while(true){
//          dut.gpioA.gpio.read #= dut.gpioA.gpio.read.toLong ^ 1
//          sleep((1e12/100).toLong)
//        }
//      }

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
