package saxon

import spinal.core._
import spinal.lib._
import spinal.lib.bus.bmb._
import spinal.lib.bus.misc.SizeMapping
import spinal.lib.com.jtag.JtagTapDebuggerGenerator
import spinal.lib.com.jtag.xilinx.Bscane2BmbMasterGenerator
import spinal.lib.generator.{ClockDomainResetGenerator, Generator, Handle}
import spinal.lib.misc.plic.PlicMapping
import vexriscv.VexRiscvBmbGenerator
import vexriscv.plugin.CsrPlugin

class VexRiscvClusterGenerator  extends Generator {
  val cpuCount = export(Handle(2))

  // Define the BMB interconnect utilities
  implicit val interconnect = BmbInterconnectGenerator()
  val bmbPeripheral = BmbBridgeGenerator(mapping = SizeMapping(0x10000000, 16 MiB)).peripheral(dataWidth = 32)
  implicit val peripheralDecoder = bmbPeripheral.asPeripheralDecoder()

  // Define the main interrupt controllers
  val plic = BmbPlicGenerator(0xC00000)
  plic.priorityWidth.load(2)
  plic.mapping.load(PlicMapping.sifive)

  val clint = BmbClintGenerator(0xB00000)
  cpuCount.derivate(clint.cpuCount.load)

  // Defines the VexRiscv cores with their connections to the PLIC and CLINT
  val cores = for(cpuId <- 0 until cpuCount) yield new Area{
    val cpu = VexRiscvBmbGenerator()
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

  // Can be use to define a SMP memory fabric with mainly 3 attatchement points (iBus, dBusCoherent, dBusIncoherent)
  def withDefaultFabric() = new Area{
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

    for(core <- cores) {
      interconnect.addConnection(
        core.cpu.iBus -> List(iBus.bmb),
        core.cpu.dBus -> List(dBusCoherent.bmb)
      )
    }

    interconnect.masters(dBus.bmb).withOutOfOrderDecoder()
  }

  // Utility to create the debug fabric usable by JTAG
  def withDebugBus(debugCd : ClockDomainResetGenerator, systemCd : ClockDomainResetGenerator, address : Long) = new Area{
    val ctrl = BmbBridgeGenerator() onClockDomain(debugCd.outputClockDomain)
    for(i <- 0 until cpuCount) {
      cores(i).cpu.enableDebugBmb(debugCd, systemCd, SizeMapping(address + i*0x1000, 0x1000))
      interconnect.addConnection(ctrl.bmb, cores(i).cpu.debugBmb)
    }

    def withJtag() = {
      val tap = JtagTapDebuggerGenerator() onClockDomain(debugCd.outputClockDomain)
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
}