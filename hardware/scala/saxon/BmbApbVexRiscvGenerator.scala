package saxon

import spinal.lib.generator._
import spinal.lib.io.Gpio
import spinal.lib.memory.sdram.IS42x320D

class BmbApbVexRiscvGenerator extends Generator {
  implicit val interconnect = BmbInterconnectGenerator()
  implicit val apbDecoder = Apb3DecoderGenerator()
  implicit val peripheralBridge = BmbToApb3Decoder(address = 0x10000000)
  implicit val cpu = VexRiscvBmbGenerator()

  interconnect.setDefaultArbitration(BmbInterconnectGenerator.STATIC_PRIORITY)
  interconnect.setPriority(cpu.iBus, 1)
  interconnect.setPriority(cpu.dBus, 2)
  interconnect.addConnection(cpu.dBus, peripheralBridge.input)
}

class SaxonSocLinux extends BmbApbVexRiscvGenerator{
  val plic = Apb3PlicGenerator(0xF0000)
  cpu.setExternalInterrupt(plic.interrupt)

  val machineTimer = Apb3MachineTimerGenerator(0x08000)
  cpu.setTimerInterrupt(machineTimer.interrupt)

  val uartA = Apb3UartGenerator(0x10000)
  plic.addInterrupt(source = uartA.interrupt, id = 1)
}
