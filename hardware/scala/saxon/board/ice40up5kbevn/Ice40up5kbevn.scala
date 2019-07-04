package saxon.board.Ice40up5kbevn

import saxon._
import saxon.board.blackice.IceStormInOutWrapper
import spinal.core._
import spinal.lib.com.jtag.sim.JtagTcp
import spinal.lib.com.spi.ddr.{SpiXdrMasterCtrl, SpiXdrParameter}
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}
import spinal.lib.generator.Generator
import spinal.lib.io.{Gpio, InOutWrapper}
import spinal.lib.misc.plic.PlicMapping


class Ice40up5kbevnSystem extends BmbApbVexRiscvGenerator{
  //Add components
  val ramA = BmbIce40SpramGenerator(0x80000000l)

  val plic = Apb3PlicGenerator(0xC00000)
  plic.priorityWidth.load(2)
  plic.mapping.load(PlicMapping.sifive)
  plic.addTarget(cpu.externalInterrupt)

  val uartA = Apb3UartGenerator(0x10000)
  uartA.connectInterrupt(plic, 1)
  val gpioA = Apb3GpioGenerator(0x00000)
  val spiA = Apb3SpiGenerator(0x20000, xipOffset = 0x20000000)
  val machineTimer = Apb3MachineTimerGenerator(0x08000)
  cpu.setTimerInterrupt(machineTimer.interrupt)



//  ramA.dataWidth.load(32)


  //Interconnect specification
  val bridge = BmbBridgeGenerator()
  interconnect.addConnection(
    cpu.iBus -> List(bridge.bmb),
    cpu.dBus -> List(bridge.bmb),
    bridge.bmb -> List(ramA.bmb, peripheralBridge.input)
  )
}



class Ice40up5kbevn extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(255)
  clockCtrl.powerOnReset.load(true)
  clockCtrl.clkFrequency.load(12 MHz)

  val system = new Ice40up5kbevnSystem
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val clk_12M = in Bool()

    clockCtrl.clock.load(clk_12M)
  }
}


object Ice40up5kbevnSystem{
  def default(g : Ice40up5kbevnSystem, clockCtrl : ClockDomainGenerator) = g {
    import g._

    cpu.config.load(VexRiscvConfigs.xip.fastWithCsr)
    cpu.enableJtag(clockCtrl)

    ramA.size.load(128 KiB)

    uartA.parameter load UartCtrlMemoryMappedConfig(
      baudrate = 115200,
      txFifoDepth = 1,
      rxFifoDepth = 1
    )

    gpioA.parameter load Gpio.Parameter(
      width = 4,
      interrupt = List(0, 1)
    )
    gpioA.connectInterrupts(plic, 4)

    spiA.parameter load SpiXdrMasterCtrl.MemoryMappingParameters(
      SpiXdrMasterCtrl.Parameters(
        dataWidth = 8,
        timerWidth = 8,
        spi = SpiXdrParameter(
          dataWidth = 2,
          ioRate = 2,
          ssWidth = 1
        )
      ) .addFullDuplex(id = 0, rate = 2).addHalfDuplex(id=1, rate=2, ddr=false, spiWidth=2),
      cmdFifoDepth = 64,
      rspFifoDepth = 64,
      cpolInit = false,
      cphaInit = false,
      modInit = 0,
      sclkToogleInit = 0,
      ssSetupInit = 0,
      ssHoldInit = 0,
      ssDisableInit = 0,
      xipConfigWritable = false,
      xipEnableInit = true,
      xipInstructionEnableInit = true,
      xipInstructionModInit = 0,
      xipAddressModInit = 0,
      xipDummyModInit = 0,
      xipPayloadModInit = 1,
      xipInstructionDataInit = 0x3B,
      xipDummyCountInit = 0,
      xipDummyDataInit = 0xFF
    )
    spiA.withXip.load(true)
    cpu.hardwareBreakpointCount.load(4)

    interconnect.addConnection(
      bridge.bmb -> List(spiA.bmb)
    )

    //Cut dBus address path
    interconnect.setConnector(bridge.bmb){(m,s) =>
      m.cmd >-> s.cmd
      m.rsp << s.rsp
    }

    //Cut Xip rsp path
    //    interconnect.setConnector(spiA.bmb){(m,s) =>
    //      m.cmd >> s.cmd
    //      m.rsp <-< s.rsp
    //    }

    g
  }
}


object Ice40up5kbevn {
  //Function used to configure the SoC
  def default(g : Ice40up5kbevn) = g{
    import g._
    Ice40up5kbevnSystem.default(system, clockCtrl)
    clockCtrl.resetSensitivity.load(ResetSensitivity.NONE)
    system.spiA.inferSpiIce40()
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(IceStormInOutWrapper(default(new Ice40up5kbevn()).toComponent()))
    BspGenerator("Ice40up5kbevn", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
  }
}




object Ice40up5kbevnSystemSim {
  import spinal.core.sim._

  def main(args: Array[String]): Unit = {

    val simConfig = SimConfig
    simConfig.allOptimisation
    //    simConfig.withWave
    simConfig.addRtl("hardware/netlist/up5k_cells_sim.v").compile(new Ice40up5kbevnSystem(){
      val clockCtrl = ClockDomainGenerator()
      this.onClockDomain(clockCtrl.clockDomain)
      clockCtrl.makeExternal(ResetSensitivity.HIGH)
      clockCtrl.powerOnReset.load(true)
      clockCtrl.clkFrequency.load(12 MHz)

      Ice40up5kbevnSystem.default(this, clockCtrl)
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

      val flash = new FlashModel(dut.spiA.phy, clockDomain)
//      flash.loadBinary("software/standalone/blinkAndEcho/build/blinkAndEcho.bin", 0x100000)
    }
  }
}
