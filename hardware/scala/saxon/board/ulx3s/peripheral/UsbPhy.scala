package saxon.board.ulx3s.peripheral

import spinal.core._
import spinal.lib._

class UsbPhy extends Component {
  val io = new Bundle {
    val phyTxMode = in Bool
    val usbRst = out Bool
    val rxd = in Bool
    val rxdp = in Bool
    val rxdn =in Bool
    val txdp = out Bool
    val txdn = out Bool
    val txoe = out Bool
    val ceO = out Bool
    val lineCtrlI = in Bool
    val dataOutI = in Bits(8 bits)
    val txValidI = in Bool
    val txReadyO = out Bool
    val dataInO = out Bits(8 bits)
    val rxValidO = out Bool
    val rxActiveO = out Bool
    val rxErrorO = out Bool
    val lineStateO = out Bits(2 bits)
  }

  val rRstCnt = Reg(UInt(5 bits)) init 0
  val rUsbRstOut = Reg(Bool) init False

  val sTxoe = Bool

  val usbRxPhy = new UsbRxPhy
  usbRxPhy.io.usbDif := io.rxd
  usbRxPhy.io.usbDp := io.rxdp
  usbRxPhy.io.usbDn := io.rxdn
  usbRxPhy.io.rxEn := sTxoe

  val usbTxPhy = new UsbTxPhy
  usbTxPhy.io.phyMode := io.phyTxMode
  usbTxPhy.io.lineCtrlI := io.lineCtrlI
  usbTxPhy.io.dataOutI := io.dataOutI
  usbTxPhy.io.txValidI := io.txValidI
  usbTxPhy.io.fsCe := usbRxPhy.io.clkRecoveredEdge

  sTxoe := usbTxPhy.io.txoe

  when (usbRxPhy.io.lineState =/= B"00") {
    rRstCnt := 0
  } otherwise {
    rRstCnt := rRstCnt + 1
  }

  io.usbRst := (rRstCnt === U"11111")
  io.rxErrorO := False
  io.ceO := usbTxPhy.io.fsCe
  io.txdp := usbTxPhy.io.txdp
  io.txdn := usbTxPhy.io.txdn
  io.txoe := usbTxPhy.io.txoe
  io.txReadyO := usbTxPhy.io.txReadyO
  io.dataInO := usbRxPhy.io.data
  io.lineStateO := usbRxPhy.io.lineState
  io.rxValidO := usbRxPhy.io.valid
  io.rxActiveO := usbRxPhy.io.rxActive
}

