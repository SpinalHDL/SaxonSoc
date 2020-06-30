package saxon.board.digilent

import saxon._
import spinal.core._
import spinal.lib.bus.misc.{AddressMapping, SizeMapping}
import spinal.lib.com.jtag.JtagTapDebuggerGenerator
import spinal.lib.com.jtag.sim.JtagTcp
import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}
import spinal.lib.generator.ClockDomainResetGenerator
import spinal.lib.io.{Gpio, InOutWrapper}
import spinal.lib.memory.sdram.sdr._
import spinal.lib.memory.sdram.xdr.CoreParameter
import spinal.lib.memory.sdram.xdr.phy.XilinxS7Phy

object NexysA7SmpLinux {
  def default(g : ArtyA7SmpLinux) = g{
    import g._

    sdramDomain.phyA.sdramLayout.load(MT47H64M16HR.layout)
    ArtyA7SmpLinuxSystem.default(system, debugCd, sdramCd).sdramA.coreParameter.load(CoreParameter(
      portTockenMin = 4,
      portTockenMax = 8,
      timingWidth = 4,
      refWidth = 16,
      stationCount  = 2,
      bytePerTaskMax = 64,
      writeLatencies = List(2),
      readLatencies = List(5+3, 5+4)
    ))

    system.ramA.hexInit.load("software/standalone/bootloader/build/bootloader.hex")
    g
  }

  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig
      .copy(
        defaultConfigForClockDomains = ClockDomainConfig(resetKind = SYNC),
        inlineRom = true
      ).addStandardMemBlackboxing(blackboxByteEnables)
       .generateVerilog(InOutWrapper(default(new ArtyA7SmpLinux()).toComponent()))
    BspGenerator("digilent/NexysA7SmpLinux", report.toplevel.generator, report.toplevel.generator.system.cores(0).cpu.dBus)
  }
}

object NexysA7SmpLinuxSystemSim {
  import spinal.core.sim._

  def main(args: Array[String]): Unit = {

    val simConfig = SimConfig
    simConfig.allOptimisation
    simConfig.withWave
    simConfig.withFstWave
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
      phy.layout.load(XilinxS7Phy.phyLayout(MT47H64M16HR.layout, 2))
      phy.connect(sdramA)

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
      val opensbi = "../opensbi/build/platform/spinal/saxon/digilent/artyA7Smp/firmware/"
      val linuxPath = "../buildroot/output/images/"

      dut.phy.logic.loadBin(0x00F80000, opensbi + "fw_jump.bin")
      dut.phy.logic.loadBin(0x00F00000, uboot + "u-boot.bin")
      dut.phy.logic.loadBin(0x00000000, linuxPath + "uImage")
      dut.phy.logic.loadBin(0x00FF0000, linuxPath + "dtb")
      dut.phy.logic.loadBin(0x00FFFFC0, linuxPath + "rootfs.cpio.uboot")

      println("DRAM loading done")
    }
  }
}
