package saxon.board.efinix.xyloni_soc


import spinal.core._
import spinal.core.fiber._
import spinal.lib._
import spinal.lib.bus.amba3.apb.{Apb3, Apb3CC, Apb3Config, Apb3SlaveFactory}
import spinal.lib.bus.bmb.{Bmb, BmbAccessCapabilities, BmbAccessParameter, BmbArbiter, BmbEg4S20Bram32K, BmbExclusiveMonitor, BmbIce40Spram, BmbImplicitPeripheralDecoder, BmbInterconnectGenerator, BmbInvalidateMonitor, BmbInvalidationParameter, BmbOnChipRam, BmbOnChipRamMultiPort, BmbParameter, BmbToApb3Bridge}
import spinal.lib.bus.misc.{AddressMapping, DefaultMapping, SizeMapping}
import spinal.lib.generator.{Dependable, Export, Generator, MemoryConnection}
import spinal.lib.slave

import saxon.board.efinix.xyloni_demoEx.bb._


case class BmbOnChipDpRamGenerator(val address: Handle[BigInt] = Unset)
                                (implicit interconnect: BmbInterconnectGenerator) extends Area {
  val size          = Handle[BigInt]
  val source        = Handle[BmbAccessCapabilities]
  val requirements  = Handle[BmbAccessParameter]
  val ctrl          = Handle(logic.io.bus)

  interconnect.addSlave(
    accessSource       = source,
    accessCapabilities = Handle(BmbOnChipRam.busCapabilities(size, source.dataWidth)),
    accessRequirements = requirements,
    bus = ctrl,
    mapping = Handle(SizeMapping(address, BigInt(1) << log2Up(size)))
  )


  val logic = Handle(BmbOnChipDpRam(
    p = requirements.toBmbParameter()
  ))

  sexport[BigInt](size, size.toInt)
}


case class BmbOnChipDpRam (p: BmbParameter) extends Component {
  val io = new Bundle {
    val bus = slave(Bmb(p))
    val portB = new Bundle {
      val b_clk       = in  Bool()
      val b_wr        = in  Bool()
      val b_addr      = in  Bits( (p.access.addressWidth - p.access.wordRangeLength) bits)
      val b_din       = in  Bits(p.access.dataWidth bits)
      val b_dout      = out Bits(p.access.dataWidth bits)
    }
  }

  val UsbDpr = new DcDpr ((p.access.addressWidth - p.access.wordRangeLength))


//  val ram = Mem(Bits(p.access.dataWidth bits), size / p.access.byteCount)
  io.bus.cmd.ready   := !io.bus.rsp.isStall
  io.bus.rsp.valid   := RegNextWhen(io.bus.cmd.valid,   io.bus.cmd.ready) init(False)
  io.bus.rsp.source  := RegNextWhen(io.bus.cmd.source,  io.bus.cmd.ready)
  io.bus.rsp.context := RegNextWhen(io.bus.cmd.context, io.bus.cmd.ready)

  io.bus.rsp.data       := UsbDpr.a_dout
  UsbDpr.a_clk          := ClockDomain.current.readClockWire
  UsbDpr.a_addr         := (io.bus.cmd.address >> p.access.wordRangeLength).asBits   //.resized.asBits
  UsbDpr.a_din          := io.bus.cmd.data
  UsbDpr.a_wr           := io.bus.cmd.isWrite & io.bus.cmd.fire

  io.bus.rsp.setSuccess()
  io.bus.rsp.last := True

  UsbDpr.b_clk          := io.portB.b_clk
  UsbDpr.b_wr           := io.portB.b_wr
  UsbDpr.b_addr         := io.portB.b_addr
  UsbDpr.b_din          := io.portB.b_din

  io.portB.b_dout       := UsbDpr.b_dout
}


case class BmbBusExportGenerator(val address: Handle[BigInt] = Unset)
                                (implicit interconnect: BmbInterconnectGenerator) extends Area {
  val size          = Handle[BigInt]
  val source        = Handle[BmbAccessCapabilities]
  val requirements  = Handle[BmbAccessParameter]
  val ctrl          = Handle(logic.io.bus)

  interconnect.addSlave(
    accessSource       = source,
    accessCapabilities = Handle(BmbOnChipRam.busCapabilities(size, source.dataWidth)),
    accessRequirements = requirements,
    bus = ctrl,
    mapping = Handle(SizeMapping(address, BigInt(1) << log2Up(size)))
  )


  val logic = Handle(BmbBusExporter(
    p = requirements.toBmbParameter()
  ))

  sexport[BigInt](size, size.toInt)
}


case class BmbBusExporter (p: BmbParameter) extends Component {
  val io = new Bundle {
    val bus = slave(Bmb(p))
    val wr        = out Bool()
    var rd        = out Bool()
    val addr      = out Bits(p.access.addressWidth bits)
    val din       = in  Bits(p.access.dataWidth bits)
    val dout      = out Bits(p.access.dataWidth bits)
  }

  io.bus.cmd.ready   := !io.bus.rsp.isStall
  io.bus.rsp.valid   := RegNextWhen(io.bus.cmd.valid,   io.bus.cmd.ready) init(False)
  io.bus.rsp.source  := RegNextWhen(io.bus.cmd.source,  io.bus.cmd.ready)
  io.bus.rsp.context := RegNextWhen(io.bus.cmd.context, io.bus.cmd.ready)

  io.bus.rsp.data := io.din
  io.addr         := io.bus.cmd.address.asBits
  io.dout         := io.bus.cmd.data
  io.wr           := io.bus.cmd.isWrite & io.bus.cmd.fire
  io.rd           := io.bus.cmd.isRead  & io.bus.cmd.fire

  io.bus.rsp.setSuccess()
  io.bus.rsp.last := True
}
