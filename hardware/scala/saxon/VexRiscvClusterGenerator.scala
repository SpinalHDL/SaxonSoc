package saxon

import spinal.core._
import spinal.core.fiber._
import spinal.lib._
import spinal.lib.bus.bmb._
import spinal.lib.bus.misc.SizeMapping
import spinal.lib.com.jtag.{JtagInstructionDebuggerGenerator, JtagTapDebuggerGenerator}
import spinal.lib.com.jtag.xilinx.Bscane2BmbMasterGenerator
import spinal.lib.com.jtag.altera.VJtag2BmbMasterGenerator
import spinal.lib.cpu.riscv.debug.{DebugModule, DebugModuleCpuConfig, DebugModuleParameter, DebugTransportModuleJtagTap, DebugTransportModuleParameter, DebugTransportModuleTunneled}
import spinal.lib.generator._
import spinal.lib.misc.plic.PlicMapping
import vexriscv.VexRiscvBmbGenerator
import vexriscv.ip.fpu.{FpuCore, FpuParameter, FpuPort}
import vexriscv.plugin.{CsrPlugin, FpuPlugin}

class VexRiscvClusterGenerator(cpuCount : Int, withSupervisor : Boolean = true, peripheralCd : Handle[ClockDomain] = ClockDomain.currentHandle) extends Area {
  // Define the BMB interconnect utilities
  implicit val interconnect = BmbInterconnectGenerator()
  val bmbPeripheral = peripheralCd on BmbBridgeGenerator(mapping = SizeMapping(0x10000000, 16 MiB)).peripheral(dataWidth = 32)
  implicit val peripheralDecoder = peripheralCd on bmbPeripheral.asPeripheralDecoder()

  // Define the main interrupt controllers
  val plic = peripheralCd on BmbPlicGenerator(0xC00000)
  plic.priorityWidth.load(2)
  plic.mapping.load(PlicMapping.sifive)

  val clint = peripheralCd on BmbClintGenerator(0xB00000)
  clint.cpuCount.load(cpuCount)

  // Defines the VexRiscv cores with their connections to the PLIC and CLINT
  val cores = for(cpuId <- 0 until cpuCount) yield {
    def bufferize[T <: Data](that : T) : T = if(peripheralCd != ClockDomain.currentHandle) BufferCC[T](that, init = null.asInstanceOf[T]) else RegNext[T](that)
    val vex = VexRiscvBmbGenerator()
    vex.setTimerInterrupt(clint.timerInterrupt(cpuId).derivate(bufferize))
    vex.setSoftwareInterrupt(clint.softwareInterrupt(cpuId).derivate(bufferize))
    plic.addTarget(vex.externalInterrupt)
    if(withSupervisor) plic.addTarget(vex.externalSupervisorInterrupt)
    List(clint.logic, vex.logic).produce{
      for (plugin <- vex.config.plugins) plugin match {
        case plugin : CsrPlugin if plugin.utime != null => plugin.utime := bufferize(clint.logic.io.time)
        case _ =>
      }
    }
    vex
  }

  // Can be use to define a SMP memory fabric with mainly 3 attatchement points (iBus, dBusCoherent, dBusIncoherent)
  def withDefaultFabric(withOutOfOrderDecoder : Boolean = true, withInvalidation : Boolean = true) = new Area{
    val iBus = BmbBridgeGenerator()
    val dBusCoherent = BmbBridgeGenerator()
    val dBus = BmbBridgeGenerator()

    val exclusiveMonitor = BmbExclusiveMonitorGenerator()
    val invalidationMonitor = withInvalidation generate BmbInvalidateMonitorGenerator()



    if(withInvalidation)     interconnect.addConnection(
      dBusCoherent.bmb           -> List(exclusiveMonitor.input),
      exclusiveMonitor.output    -> List(invalidationMonitor.input),
      invalidationMonitor.output -> List(dBus.bmb)
    ) else interconnect.addConnection(
      dBusCoherent.bmb           -> List(exclusiveMonitor.input),
      exclusiveMonitor.output    -> List(dBus.bmb)
    )

    for(cpu <- cores) {
      interconnect.addConnection(
        cpu.iBus -> List(iBus.bmb),
        cpu.dBus -> List(dBusCoherent.bmb)
      )
    }

    if(withOutOfOrderDecoder) interconnect.masters(dBus.bmb).withOutOfOrderDecoder()
  }

  /*Example of usage
    val riscvJtag = new Area {
      val debug = withRiscvDebug(debugCd.outputClockDomain, debugResetCd)
      val soft = withSoftJtag generate new Area{
        val tap = debug.dmiDirect()
        val io = Handle(tap.tap.io.jtag.toIo)
      }
      val hard = !withSoftJtag generate new Area{
        val jtagCd = Handle[ClockDomain]
        val noTap = debug.noTap(jtagCd)
        val jtagCtrl = Handle(noTap.tunnel.io.instruction.toIo)
        val jtagCtrl_tck = Handle(in(Bool()))
        jtagCd.loadAsync(ClockDomain(jtagCtrl_tck))
      }
    }
   */
  def withRiscvDebug(debugCd : Handle[ClockDomain], systemCd : ClockDomainResetGenerator) = new Area{
    for ((cpu,i) <- cores.zipWithIndex) {
      cores(i).enableRiscvDebug(debugCd, systemCd)
    }

    val systemReset = Handle(Bool())
    systemCd.asyncReset(systemReset, ResetSensitivity.HIGH)

    val p = DebugTransportModuleParameter(
      addressWidth = 7,
      version      = 1,
      idle         = 7
    )

    val logic = hardFork(debugCd on new Area{
      val XLEN = 32

      val dm = DebugModule(
        DebugModuleParameter(
          version = p.version + 1,
          harts = cpuCount,
          progBufSize = 2,
          datacount   = XLEN/32,
          hartsConfig = cores.map(c => DebugModuleCpuConfig(
            xlen = XLEN,
            flen = c.config.get.FLEN,
            withFpuRegAccess = c.config.get.FLEN == 64
          ))
        )
      )
      systemReset := dm.io.ndmreset
      for ((cpu,i) <- cores.zipWithIndex) {
        val privBus = cpu.debugRiscv
        privBus <> dm.io.harts(i)
        privBus.dmToHart.removeAssignments() <-< dm.io.harts(i).dmToHart
      }
    })

    def dmiDirect() = hardFork(debugCd on new Area{
      val tap = DebugTransportModuleJtagTap(
        p.copy(addressWidth = 7),
        debugCd = ClockDomain.current
      )
      logic.dm.io.ctrl <> tap.io.bus
    })

    def noTap(noTapCd : Handle[ClockDomain]) = hardFork(debugCd on new Area{
      val tunnel = DebugTransportModuleTunneled(
        p       = p,
        jtagCd  = noTapCd,
        debugCd = ClockDomain.current
      )
      logic.dm.io.ctrl <> tunnel.io.bus
    })
  }

  // Utility to create the debug fabric usable by JTAG
  def withDebugBus(debugCd : Handle[ClockDomain], systemCd : ClockDomainResetGenerator, address : Long) = new Area{
    val ctrl = debugCd on BmbBridgeGenerator()

    for ((cpu,i) <- cores.zipWithIndex) {
      cores(i).enableDebugBmb(debugCd, systemCd, SizeMapping(address + i * 0x1000, 0x1000))
      interconnect.addConnection(ctrl.bmb, cpu.debugBmb)
    }

    def withJtag() = {
      val tap = debugCd on JtagTapDebuggerGenerator()
      interconnect.addConnection(tap.bmb, ctrl.bmb)
      tap
    }

    def withJtagInstruction() = {
      val tap = debugCd on JtagInstructionDebuggerGenerator(ignoreWidth=0)
      interconnect.addConnection(tap.bmb, ctrl.bmb)
      tap
    }

    // For Xilinx series 7 FPGA
    def withBscane2(userId : Int) = {
      val tap = debugCd on Bscane2BmbMasterGenerator(userId, ignoreWidth=0)
      interconnect.addConnection(tap.bmb, ctrl.bmb)
      tap
    }

    // For Altera FPGAs
    def withVJtag() = {
      val tap = debugCd on VJtag2BmbMasterGenerator(ignoreWidth=0)
      interconnect.addConnection(tap.bmb, ctrl.bmb)
      tap
    }
  }

  def withoutDebug(): Unit ={
    for ((cpu,i) <- cores.zipWithIndex) {
      cores(i).disableDebug()
    }
  }

  class FpuIntegration extends Area{
    val parameter = Handle[FpuParameter]
    val connect = Handle[(FpuPort,FpuPort) => Unit]

    def setParameters(extraStage : Boolean): this.type ={
      connect.load{(m : FpuPort, s : FpuPort) =>
        m.cmd >> s.cmd
        m.commit.pipelined(m2s = extraStage) >> s.commit
        m.completion := s.completion.stage()
        m.rsp << s.rsp.pipelined(s2m = extraStage)
        : Unit
      }
      parameter.load(
        FpuParameter(
          withDouble = true,
          asyncRegFile = false,
          schedulerM2sPipe = extraStage
        )
      )
      this
    }


    val logic = Handle{
      new FpuCore(
        portCount = cpuCount,
        p =  FpuParameter(
          withDouble = true,
          asyncRegFile = false
        )
      )
    }

    val doConnect = Handle{
      for(i <- 0 until cpuCount;
          vex = cores(i).logic.cpu;
          port = logic.io.port(i)) {
        val plugin = vex.service(classOf[FpuPlugin])
        connect(plugin.port, port)

        if (i == 0) {
          println("cpuDecode to fpuDispatch " + LatencyAnalysis(vex.decode.arbitration.isValid, logic.decode.input.valid))
          println("fpuDispatch to cpuRsp    " + LatencyAnalysis(logic.decode.input.valid, plugin.port.rsp.valid))

          println("cpuWriteback to fpuAdd   " + LatencyAnalysis(vex.writeBack.input(plugin.FPU_COMMIT), logic.commitLogic(0).add.counter))

          println("add                      " + LatencyAnalysis(logic.decode.add.rs1.mantissa, logic.get.merge.arbitrated.value.mantissa))
          println("mul                      " + LatencyAnalysis(logic.decode.mul.rs1.mantissa, logic.get.merge.arbitrated.value.mantissa))
          println("fma                      " + LatencyAnalysis(logic.decode.mul.rs1.mantissa, logic.get.decode.add.rs1.mantissa, logic.get.merge.arbitrated.value.mantissa))
          println("short                    " + LatencyAnalysis(logic.decode.shortPip.rs1.mantissa, logic.get.merge.arbitrated.value.mantissa))
        }
      }
    }
  }
}