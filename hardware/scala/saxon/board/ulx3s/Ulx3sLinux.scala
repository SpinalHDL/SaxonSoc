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




class Ulx3sLinuxSystem extends SaxonSocLinux{
  //Add components
  val sdramA = SdramSdrBmbGenerator(0x80000000l)
  val gpioA = Apb3GpioGenerator(0x00000)
  val spiA = Apb3SpiGenerator(0x20000)
  val spiB = Apb3SpiGenerator(0x21000)
  val noReset = Ulx3sNoResetGenerator()

  //Interconnect specification
  interconnect.addConnection(
    cpu.iBus -> List(sdramA.bmb),
    cpu.dBus -> List(sdramA.bmb, peripheralBridge.input)
  )
}

class Ulx3sLinux extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(255)
  clockCtrl.resetSynchronous.load(false)
  clockCtrl.powerOnReset.load(true)

  val system = new Ulx3sLinuxSystem()
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val clk_25mhz = in Bool()
    val sdram_clk = out Bool()

    val pll = Ulx3sLinuxPll()
    pll.clkin := clk_25mhz
    sdram_clk := pll.clkout1
    clockCtrl.clock.load(pll.clkout0)
  }
}

case class Ulx3sLinuxPll() extends BlackBox{
  setDefinitionName("pll")
  val clkin = in Bool()
  val clkout0 = out Bool()
  val clkout1 = out Bool()
  val locked = out Bool()
}


object Ulx3sLinuxSystem{
  def default(g : Ulx3sLinuxSystem, clockCtrl : ClockDomainGenerator) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.linux)
    cpu.enableJtag(clockCtrl)

    sdramA.layout.load(IS42x320D.layout)
    sdramA.timings.load(IS42x320D.timingGrade7)

    uartA.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 128,
      rxFifoDepth = 128
    )

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
    spiA.inferSpiSdrIo()

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
    spiB.produce(RegNext(spiB.phy.sclk.write(0)).asOutput.setName("system_spiB_spi_sclk2"))



    g
  }
}


object Ulx3sLinux {
  //Function used to configure the SoC
  def default(g : Ulx3sLinux) = g{
    import g._
    clockCtrl.clkFrequency.load(50 MHz)
    clockCtrl.resetSensitivity.load(ResetSensitivity.NONE)
    Ulx3sLinuxSystem.default(system, clockCtrl)
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(InOutWrapper(default(new Ulx3sLinux()).toComponent()))
    BspGenerator("Ulx3sLinux", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
  }
}




object Ulx3sLinuxSystemSim {
  import spinal.core.sim._

  def main(args: Array[String]): Unit = {

    val simConfig = SimConfig
    simConfig.allOptimisation
    simConfig.withWave
    simConfig.compile(new Ulx3sLinuxSystem(){
      val clockCtrl = ClockDomainGenerator()
      this.onClockDomain(clockCtrl.clockDomain)
      clockCtrl.makeExternal(ResetSensitivity.HIGH)
      clockCtrl.powerOnReset.load(true)
      clockCtrl.clkFrequency.load(50 MHz)
      clockCtrl.resetHoldDuration.load(15)
      Ulx3sLinuxSystem.default(this, clockCtrl)
    }.toComponent()).doSimUntilVoid("test", 42){dut =>
      val systemClkPeriod = (1e12/dut.clockCtrl.clkFrequency.toDouble).toLong
      val jtagClkPeriod = systemClkPeriod*4
      val uartBaudRate = 115200
      val uartBaudPeriod = (1e12/uartBaudRate).toLong

      val clockDomain = ClockDomain(dut.clockCtrl.clock, dut.clockCtrl.reset)
      clockDomain.forkStimulus(systemClkPeriod)
//      clockDomain.forkSimSpeedPrinter(4)

//      fork{
//        while(true){
//          sleep(systemClkPeriod*1000000)
//          println("\nsimTime : " + simTime())
//        }
//      }
      fork{
//        disableSimWave()
//        sleep(0.2e12.toLong)
//        enableSimWave()
//        sleep(systemClkPeriod*2000000)
//        simFailure()

        while(true){
          disableSimWave()
          sleep(systemClkPeriod*500000)
          enableSimWave()
          sleep(systemClkPeriod*100)
        }
      }

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

      val sdram = SdramModel(
        io = dut.sdramA.sdram,
        layout = dut.sdramA.logic.layout,
        clockDomain = clockDomain
      )


      val linuxPath = "../buildroot/output/images/"
      sdram.loadBin(0x00000000, "software/standalone/machineModeSbi/build/machineModeSbi.bin")
      sdram.loadBin(0x00400000, linuxPath + "Image")
      sdram.loadBin(0x00BF0000, linuxPath + "dtb")
      sdram.loadBin(0x00C00000, linuxPath + "rootfs.cpio")


//      sdram.loadBin(0, "software/standalone/dhrystone/build/dhrystone.bin")
//      sdram.loadBin(0, "software/standalone/machineModeSbi/build/machineModeSbi.bin")


//      val linuxPath = "/home/miaou/pro/litex/linux-on-litex-vexriscv/buildroot/"
//      sdram.loadBin(0x00000000, "software/standalone/machineModeSbi/build/machineModeSbi.bin")
//      sdram.loadBin(0x00400000, linuxPath + "Image")
//      sdram.loadBin(0x00BF0000, "../buildroot/output/images/dtb")
//      sdram.loadBin(0x00C00000, linuxPath + "rootfs.cpio")
    }
  }
}
