package saxon.board.Ice40Hx8kBreakoutLinux

import saxon._
import saxon.board.blackice.IceStormInOutWrapper
import spinal.core._
import spinal.lib.com.jtag.sim.JtagTcp
import spinal.lib.com.spi.ddr.{SpiXdrMasterCtrl, SpiXdrParameter}
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}
import spinal.lib.generator.Generator
import spinal.lib.io.{Gpio, InOutWrapper}
import spinal.lib.memory.sdram.sdr.IS42x320D
import spinal.lib.memory.sdram.sdr.sim.SdramModel


class Ice40Hx8kBreakoutSystem extends SaxonSocLinux{
  //Add components
  val sdramA = SdramSdrBmbGenerator(0x80000000l)
  val gpioA = Apb3GpioGenerator(0x00000)


  //Interconnect specification
  val bridge = BmbBridgeGenerator()
  interconnect.addConnection(
    cpu.iBus -> List(bridge.bmb),
    cpu.dBus -> List(bridge.bmb),
    bridge.bmb -> List(sdramA.bmb, peripheralBridge.input)
  )
}



class Ice40Hx8kBreakoutLinux extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(255)
  clockCtrl.powerOnReset.load(true)
  clockCtrl.clkFrequency.load(12 MHz)

  val system = new Ice40Hx8kBreakoutSystem
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val clk_12M = in Bool()

    clockCtrl.clock.load(clk_12M)
  }
}


object Ice40Hx8kBreakoutSystem{
  def default(g : Ice40Hx8kBreakoutSystem, clockCtrl : ClockDomainGenerator) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.linuxIce40)
    cpu.enableJtag(clockCtrl)

    sdramA.layout.load(IS42x320D.layout)
    sdramA.timings.load(IS42x320D.timingGrade7)

    uartA.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 1,
      rxFifoDepth = 1
    )

    gpioA.parameter load Gpio.Parameter(
      width = 8,
      interrupt = List(0, 1)
    )
    gpioA.connectInterrupts(plic, 4)

    g
  }
}


object Ice40Hx8kBreakoutLinux {
  //Function used to configure the SoC
  def default(g : Ice40Hx8kBreakoutLinux) = g{
    import g._
    Ice40Hx8kBreakoutSystem.default(system, clockCtrl)
    clockCtrl.resetSensitivity.load(ResetSensitivity.NONE)
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(IceStormInOutWrapper(default(new Ice40Hx8kBreakoutLinux()).toComponent()))
    BspGenerator("Ice40Hx8kBreakoutLinux", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
  }
}




object Ice40Hx8kBreakoutSystemSim {
  import spinal.core.sim._

  def main(args: Array[String]): Unit = {

    val simConfig = SimConfig
    simConfig.allOptimisation
//    simConfig.withWave
    simConfig.compile(new Ice40Hx8kBreakoutSystem(){
      val clockCtrl = ClockDomainGenerator()
      this.onClockDomain(clockCtrl.clockDomain)
      clockCtrl.makeExternal(ResetSensitivity.HIGH)
      clockCtrl.powerOnReset.load(true)
      clockCtrl.clkFrequency.load(12 MHz)

      Ice40Hx8kBreakoutSystem.default(this, clockCtrl)
    }.toComponent()).doSimUntilVoid("test", 42){dut =>
      val systemClkPeriod = (1e12/dut.clockCtrl.clkFrequency.toDouble).toLong
      val jtagClkPeriod = systemClkPeriod*4
      val uartBaudRate = 115200
      val uartBaudPeriod = (1e12/uartBaudRate).toLong

      val clockDomain = ClockDomain(dut.clockCtrl.clock, dut.clockCtrl.reset)
      clockDomain.forkStimulus(systemClkPeriod)


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
      sdram.loadBin(0, "software/standalone/dhrystone/build/dhrystone.bin")
    }
  }
}
