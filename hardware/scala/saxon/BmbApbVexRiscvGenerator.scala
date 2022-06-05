package saxon

import spinal.lib.bus.bmb._
import spinal.lib.generator._
import spinal.core._
import spinal.lib.io.Gpio
import spinal.lib.memory.sdram.sdr.IS42x320D
import spinal.lib.misc.plic.PlicMapping
import vexriscv.VexRiscvBmbGenerator

class BmbApbVexRiscvGenerator  extends Area {
  implicit val interconnect = BmbInterconnectGenerator()
  implicit val apbDecoder = Apb3DecoderGenerator()
  implicit val peripheralBridge = BmbToApb3Decoder(address = 0x10000000)
  implicit val cpu = VexRiscvBmbGenerator()

  interconnect.setDefaultArbitration(BmbInterconnectGenerator.STATIC_PRIORITY)
  interconnect.setPriority(cpu.iBus, 1)
  interconnect.setPriority(cpu.dBus, 2)
}

class SaxonSocLinux extends BmbApbVexRiscvGenerator{
  val plic = Apb3PlicGenerator(0xC00000)
  plic.priorityWidth.load(2)
  plic.mapping.load(PlicMapping.sifive)
  plic.addTarget(cpu.externalInterrupt)
  plic.addTarget(cpu.externalSupervisorInterrupt)

  val machineTimer = Apb3MachineTimerGenerator(0x08000)
  cpu.setTimerInterrupt(machineTimer.interrupt)

  val uartA = Apb3UartGenerator(0x10000)
  uartA.connectInterrupt(plic, 1)
}
