package saxon.board.digilent

import saxon.{ResetSensitivity, _}
import spinal.core._
import spinal.lib.bus.misc.{AddressMapping, SizeMapping}
import spinal.lib.com.eth.{MacEthParameter, Mii, MiiParameter, MiiRxParameter, MiiTxParameter, PhyParameter}
import spinal.lib.com.jtag.sim.JtagTcp
import spinal.lib.com.uart.sim.{UartDecoder, UartEncoder}
import spinal.lib.io.{Gpio, InOutWrapper}
import spinal.lib.memory.sdram.sdr._
import spinal.lib.memory.sdram.xdr.CoreParameter
import spinal.lib.memory.sdram.xdr.phy.XilinxS7Phy

object NexysA7SmpLinux {
  def default(g : ArtyA7SmpLinux) = g{
    import g._

    sdramDomain.phyA.sdramLayout.load(MT47H64M16HR.layout)
    ArtyA7SmpLinuxSystem.default(system, debugCd, sdramCd)

    system.sdramA.coreParameter.load(CoreParameter(
      portTockenMin = 4,
      portTockenMax = 8,
      timingWidth = 4,
      refWidth = 16,
      stationCount  = 2,
      bytePerTaskMax = 64,
      writeLatencies = List(2),
      readLatencies = List(5+3, 5+4)
    ))

    system.mac.parameter load MacEthParameter(
      phy = PhyParameter(
        txDataWidth = 2,
        rxDataWidth = 2
      ),
      rxDataWidth = 32,
      rxBufferByteSize = 4096,
      txDataWidth = 32,
      txBufferByteSize = 4096
    )

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
