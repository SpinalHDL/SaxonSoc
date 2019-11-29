package saxon.board.Ice40up5kbevnNoXip

import saxon.VexRiscvConfigs.executeRf
import saxon._
import saxon.board.blackice.IceStormInOutWrapper
import spinal.core._
import spinal.lib.bus.bmb.Bmb
import spinal.lib.com.jtag.sim.JtagTcp
import spinal.lib.com.spi.ddr.{SpiXdrMasterCtrl, SpiXdrParameter}
import spinal.lib.com.uart.UartCtrlMemoryMappedConfig
import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}
import spinal.lib.generator.{Generator, Handle}
import spinal.lib.io.{Gpio, InOutWrapper}
import spinal.lib.misc.plic.PlicMapping
import vexriscv.{VexRiscvConfig, plugin}
import vexriscv.plugin.{BranchPlugin, CfuBb, CfuBusParameter, CfuPlugin, CfuTest, CsrAccess, CsrPlugin, CsrPluginConfig, DBusSimplePlugin, DecoderSimplePlugin, FullBarrelShifterPlugin, HazardSimplePlugin, IBusSimplePlugin, IntAluPlugin, LightShifterPlugin, RegFilePlugin, SrcPlugin, YamlPlugin}


class Ice40up5kbevnNoXipSystem extends BmbApbVexRiscvGenerator{
//  val ramABmb = Handle[Bmb]

  val plic = Apb3PlicGenerator(0xC00000)
  plic.priorityWidth.load(2)
  plic.mapping.load(PlicMapping.sifive)
  plic.addTarget(cpu.externalInterrupt)

  val uartA = Apb3UartGenerator(0x10000)
  val gpioA = Apb3GpioGenerator(0x00000)
  val machineTimer = Apb3MachineTimerGenerator(0x08000)

  cpu.setTimerInterrupt(machineTimer.interrupt)
  uartA.connectInterrupt(plic, 1)

  //Interconnect specification
  val bridge = BmbBridgeGenerator()
  interconnect.addConnection(
    cpu.iBus -> List(bridge.bmb),
    cpu.dBus -> List(bridge.bmb),
    bridge.bmb -> List(peripheralBridge.input)
  )

  interconnect.setConnector(bridge.bmb){(m,s) =>
    m.cmd.s2mPipe() >> s.cmd
    m.rsp << s.rsp
  }
}



class Ice40up5kbevnNoXip extends Generator{
  val clockCtrl = ClockDomainGenerator()
  clockCtrl.resetHoldDuration.load(255)
  clockCtrl.powerOnReset.load(true)
  clockCtrl.clkFrequency.load(12 MHz)

  val system = new Ice40up5kbevnNoXipSystem
  system.onClockDomain(clockCtrl.clockDomain)

  val clocking = add task new Area{
    val clk_12M = in Bool()

    clockCtrl.clock.load(clk_12M)
  }
}


object Ice40up5kbevnNoXipSystem{
  def simRam(g : Ice40up5kbevnNoXipSystem, hex : String): Unit ={
    val ramA = BmbOnChipRamGenerator(0x80000000l)(g.interconnect)
    ramA.dataWidth.load(32)
    ramA.size.load(128 KiB)
    ramA.hexInit.load(hex)
//    ramA.bmb.merge(g.ramABmb)
//      val ramA = BmbIce40SpramGenerator(0x80000000l)
//      ramA.size.load(128 KiB)
  }


  def default(g : Ice40up5kbevnNoXipSystem, clockCtrl : ClockDomainGenerator) = g {
    import g._

    val cpuConfig = VexRiscvConfig(
      withMemoryStage = true,
      withWriteBackStage = false,
      List(
        new IBusSimplePlugin(
          resetVector = 0x80000000l,
          cmdForkOnSecondStage = false,
          cmdForkPersistence = true
        ),
        new DBusSimplePlugin(
          catchAddressMisaligned = false,
          catchAccessFault = false
        ),
        new DecoderSimplePlugin(
          catchIllegalInstruction = false
        ),
        new RegFilePlugin(
          regFileReadyKind = plugin.SYNC,
          zeroBoot = true,
          x0Init = false,
          readInExecute = false,
          syncUpdateOnStall = true
        ),
        new IntAluPlugin,
        new SrcPlugin(
          separatedAddSub = false,
          executeInsertion = false,
          decodeAddSub = false
        ),
        new FullBarrelShifterPlugin(),
        new BranchPlugin(
          earlyBranch = false,
          catchAddressMisaligned = false,
          fenceiGenAsAJump = false
        ),
        new HazardSimplePlugin(
          bypassExecute = true,
          bypassMemory = true,
          bypassWriteBackBuffer = true
        ),
        new CsrPlugin(new CsrPluginConfig(
          catchIllegalAccess = true,
          mvendorid = null,
          marchid = null,
          mimpid = null,
          mhartid = null,
          misaExtensionsInit = 0,
          misaAccess = CsrAccess.NONE,
          mtvecAccess = CsrAccess.WRITE_ONLY,
          mtvecInit = null,
          mepcAccess = CsrAccess.READ_WRITE,
          mscratchGen = false,
          mcauseAccess = CsrAccess.READ_ONLY,
          mbadaddrAccess = CsrAccess.NONE,
          mcycleAccess = CsrAccess.NONE,
          minstretAccess = CsrAccess.NONE,
          ecallGen = true,
          ebreakGen = false,
          wfiGenAsWait = false,
          wfiGenAsNop = true,
          ucycleAccess = CsrAccess.NONE
        )),
        new YamlPlugin("cpu0.yaml")
      )
    )

    val pluginCfuA = new CfuPlugin(
      stageCount = 0,
      allowZeroLatency = true,
      encoding =  M"0000000------------------0001011",
      busParameter = CfuBusParameter(
        CFU_VERSION = 0,
        CFU_INTERFACE_ID_W = 0,
        CFU_FUNCTION_ID_W = 3,
        CFU_REORDER_ID_W = 0,
        CFU_REQ_RESP_ID_W = 0,
        CFU_INPUTS = 2,
        CFU_INPUT_DATA_W = 32,
        CFU_OUTPUTS = 1,
        CFU_OUTPUT_DATA_W = 32,
        CFU_FLOW_REQ_READY_ALWAYS = false,
        CFU_FLOW_RESP_READY_ALWAYS = false
      )
    ).setName("pluginCfuA")
    cpuConfig.plugins += pluginCfuA

    val pluginCfuB = new CfuPlugin(
      stageCount = 0,
      allowZeroLatency = true,
      encoding =  M"0000001------------------0001011",
      busParameter = CfuBusParameter(
        CFU_VERSION = 0,
        CFU_INTERFACE_ID_W = 0,
        CFU_FUNCTION_ID_W = 3,
        CFU_REORDER_ID_W = 0,
        CFU_REQ_RESP_ID_W = 0,
        CFU_INPUTS = 2,
        CFU_INPUT_DATA_W = 32,
        CFU_OUTPUTS = 1,
        CFU_OUTPUT_DATA_W = 32,
        CFU_FLOW_REQ_READY_ALWAYS = false,
        CFU_FLOW_RESP_READY_ALWAYS = false
      )
    ).setName("pluginCfuB")
    cpuConfig.plugins += pluginCfuB

    val pluginCfuC = new CfuPlugin(
      stageCount = 1,
      allowZeroLatency = true,
      encoding =  M"0000010------------------0001011",
      busParameter = CfuBusParameter(
        CFU_VERSION = 0,
        CFU_INTERFACE_ID_W = 0,
        CFU_FUNCTION_ID_W = 3,
        CFU_REORDER_ID_W = 0,
        CFU_REQ_RESP_ID_W = 0,
        CFU_INPUTS = 2,
        CFU_INPUT_DATA_W = 32,
        CFU_OUTPUTS = 1,
        CFU_OUTPUT_DATA_W = 32,
        CFU_FLOW_REQ_READY_ALWAYS = true,
        CFU_FLOW_RESP_READY_ALWAYS = true
      )
    ).setName("pluginCfuC")
    cpuConfig.plugins += pluginCfuC

    cpu produce new Area{
      val cfuA = CfuTest()
      cfuA.io.bus << pluginCfuA.bus

      val cfuB = CfuBb(pluginCfuB.busParameter).setDefinitionName("CfuB")
      cfuB.io.bus << pluginCfuB.bus

      val cfuC = CfuBb(pluginCfuB.busParameter).setDefinitionName("CfuC")
      cfuC.io.bus << pluginCfuC.bus
    }

    cpu.config.load(cpuConfig)

    cpu.enableJtag(clockCtrl)

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

    g
  }
}




object Ice40up5kbevnNoXip {
  //Function used to configure the SoC
  def default(g : Ice40up5kbevnNoXip) = g{
    import g._
    Ice40up5kbevnNoXipSystem.default(system, clockCtrl)
    clockCtrl.resetSensitivity.load(ResetSensitivity.NONE)
    g
  }

  //Generate the SoC
  def main(args: Array[String]): Unit = {
    val report = SpinalRtlConfig.generateVerilog(IceStormInOutWrapper(default(new Ice40up5kbevnNoXip()).toComponent()))
    BspGenerator("Ice40up5kbevnNoXip", report.toplevel.generator, report.toplevel.generator.system.cpu.dBus)
  }
}




object Ice40up5kbevnNoXipSystemSim {
  import spinal.core.sim._

  def main(args: Array[String]): Unit = {

    val simConfig = SimConfig
    simConfig.allOptimisation
    simConfig.withWave
    simConfig.addRtl("hardware/netlist/up5k_cells_sim.v")
    simConfig.addRtl("hardware/netlist/cfuBb.v")
    simConfig.addRtl("hardware/netlist/CFU/cfu.v")
    simConfig.addSimulatorFlag("-I/home/miaou/pro/riscv/SaxonSoc.git/hardware/netlist/CFU")
    simConfig.addSimulatorFlag("-Wno-PINMISSING")
    simConfig.addSimulatorFlag("-Wno-LITENDIAN")

    simConfig.compile(new Ice40up5kbevnNoXipSystem(){
      val clockCtrl = ClockDomainGenerator()
      this.onClockDomain(clockCtrl.clockDomain)
      clockCtrl.makeExternal(ResetSensitivity.HIGH)
      clockCtrl.powerOnReset.load(true)
      clockCtrl.clkFrequency.load(12 MHz)
      clockCtrl.resetHoldDuration.load(16)

      val ramA = BmbOnChipRamGenerator(0x80000000l)(interconnect)
      ramA.dataWidth.load(32)
      ramA.size.load(128 KiB)
      ramA.hexInit.load("software/standalone/cfu/build/cfu.hex")
      interconnect.addConnection(
        bridge.bmb -> List(ramA.bmb)
      )

      Ice40up5kbevnNoXipSystem.default(this, clockCtrl)
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
    }
  }
}
