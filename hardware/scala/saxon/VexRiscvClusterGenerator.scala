package saxon

import spinal.core._
import spinal.lib._
import spinal.lib.bus.bmb._
import spinal.lib.bus.misc.SizeMapping
import spinal.lib.com.jtag.{JtagInstructionDebuggerGenerator, JtagTapDebuggerGenerator}
import spinal.lib.com.jtag.xilinx.Bscane2BmbMasterGenerator
import spinal.lib.generator.{ClockDomainResetGenerator, Generator, Handle}
import spinal.lib.misc.plic.PlicMapping
import vexriscv.VexRiscvBmbGenerator
import vexriscv.plugin.CsrPlugin

class VexRiscvClusterGenerator(cpuCount : Int) extends Generator {
  // Define the BMB interconnect utilities
  implicit val interconnect = BmbInterconnectGenerator()
  val bmbPeripheral = BmbBridgeGenerator(mapping = SizeMapping(0x10000000, 16 MiB)).peripheral(dataWidth = 32)
  implicit val peripheralDecoder = bmbPeripheral.asPeripheralDecoder()

  // Define the main interrupt controllers
  val plic = BmbPlicGenerator(0xC00000)
  plic.priorityWidth.load(2)
  plic.mapping.load(PlicMapping.sifive)

  val clint = BmbClintGenerator(0xB00000)
  clint.cpuCount.load(cpuCount)

  // Defines the VexRiscv cores with their connections to the PLIC and CLINT
  val cores = for(cpuId <- 0 until cpuCount) yield {
    val vex = VexRiscvBmbGenerator()
    vex.setTimerInterrupt(clint.timerInterrupt(cpuId))
    vex.setSoftwareInterrupt(clint.softwareInterrupt(cpuId))
    plic.addTarget(vex.externalInterrupt)
    plic.addTarget(vex.externalSupervisorInterrupt)
    List(clint.logic, vex.logic).produce{
      for (plugin <- vex.config.plugins) plugin match {
        case plugin : CsrPlugin if plugin.utime != null => plugin.utime := RegNext(clint.logic.io.time)
        case _ =>
      }
    }
    vex
  }

  // Can be use to define a SMP memory fabric with mainly 3 attatchement points (iBus, dBusCoherent, dBusIncoherent)
  def withDefaultFabric(withOutOfOrderDecoder : Boolean = true) = new Area{
    val iBus = BmbBridgeGenerator()
    val dBusCoherent = BmbBridgeGenerator()
    val dBus = BmbBridgeGenerator()

    val exclusiveMonitor = BmbExclusiveMonitorGenerator()
    val invalidationMonitor = BmbInvalidateMonitorGenerator()

    interconnect.addConnection(
      dBusCoherent.bmb           -> List(exclusiveMonitor.input),
      exclusiveMonitor.output    -> List(invalidationMonitor.input),
      invalidationMonitor.output -> List(dBus.bmb)
    )

   for(cpu <- cores) {
      interconnect.addConnection(
        cpu.iBus -> List(iBus.bmb),
        cpu.dBus -> List(dBusCoherent.bmb)
      )
    }

    if(withOutOfOrderDecoder) interconnect.masters(dBus.bmb).withOutOfOrderDecoder()
  }

  // Utility to create the debug fabric usable by JTAG
  def withDebugBus(debugCd : ClockDomainResetGenerator, systemCd : ClockDomainResetGenerator, address : Long) = new Area{
    val ctrl = BmbBridgeGenerator() onClockDomain(debugCd.outputClockDomain)

    for ((cpu,i) <- cores.zipWithIndex) {
      cores(i).enableDebugBmb(debugCd, systemCd, SizeMapping(address + i * 0x1000, 0x1000))
      interconnect.addConnection(ctrl.bmb, cpu.debugBmb)
    }

    def withJtag() = {
      val tap = JtagTapDebuggerGenerator() onClockDomain(debugCd.outputClockDomain)
      interconnect.addConnection(tap.bmb, ctrl.bmb)
      tap
    }


    def withJtagInstruction() = {
      val tap = JtagInstructionDebuggerGenerator() onClockDomain(debugCd.outputClockDomain)
      interconnect.addConnection(tap.bmb, ctrl.bmb)
      tap
    }

    // For Xilinx series 7 FPGA
    def withBscane2(userId : Int) = {
      val tap = Bscane2BmbMasterGenerator(userId) onClockDomain(debugCd.outputClockDomain)
      interconnect.addConnection(tap.bmb, ctrl.bmb)
      tap
    }
  }

  def withoutDebug(): Unit ={
    for ((cpu,i) <- cores.zipWithIndex) {
      cores(i).disableDebug()
    }
  }
}