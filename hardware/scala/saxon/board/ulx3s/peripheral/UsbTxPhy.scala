package saxon.board.ulx3s.peripheral

import spinal.core._
import spinal.lib._

class UsbTxPhy extends Component {
  val io = new Bundle {
    val fsCe = in Bool
    val phyMode = in Bool
    val txdp = out Bool
    val txdn = out Bool
    val txoe = out Bool
    val lineCtrlI = in Bool
    val dataOutI = in Bits(8 bits)
    val txValidI = in Bool
    val txReadyO = out Bool
  }

  val IDLE_STATE = B"0000"
  val SOP_STATE  = B"0001"
  val DATA_STATE = B"0010"
  val WAIT_STATE = B"0011"
  val EOP0_STATE = B"1000"
  val EOP1_STATE = B"1001"
  val EOP2_STATE = B"1010"
  val EOP3_STATE = B"1011"
  val EOP4_STATE = B"1100"
  val EOP5_STATE = B"1101"

  val rHold = Reg(Bits(8 bits)) init 0x00
  val rLdData = Reg(Bool) init False
  val rLineCtrlI = Reg(Bool) init False
  val rLongI = Reg(Bool) init False
  val rBusResetI = Reg(Bool) init False
  val rBitCnt = Reg(UInt(16 bits)) init 0
  val rDataXmit = Reg(Bool) init False
  val rHoldD = Reg(Bits(8 bits)) init 0x00
  val rOneCnt = Reg(UInt(3 bits)) init 0
  val rSdBsO = Reg(Bool) init False
  val rSdNrziO = Reg(Bool) init True
  val rSdRawO = Reg(Bool) init False
  val rSftDone = Reg(Bool) init False
  val rSftDoneR = Reg(Bool) init False
  val rState = Reg(Bits(4 bits)) init IDLE_STATE
  val rTxIp = Reg(Bool) init False
  val rTxIpSync = Reg(Bool) init False
  val rTxoeR1 = Reg(Bool) init False
  val rTxoeR2 = Reg(Bool) init False
  
  val rTxDp = Reg(Bool) init True
  val rTxDn = Reg(Bool) init False
  val rTxoe = Reg(Bool) init True
  val rTxReady = Reg(Bool) init False

  val anyEopState = rState(3)
  val appendEop = (rState(3 downto 2) === B"11")
  val stuff = (rOneCnt === U"110")

  val sftDoneE = rSftDone && !rSftDoneR
  val ldDataD = (rState === SOP_STATE || (rState === DATA_STATE && rDataXmit)) ? sftDoneE | False
  val ldSopD = (rState === IDLE_STATE) ? io.txValidI | False
  val seState = appendEop || (rState =/= WAIT_STATE && rLineCtrlI && rLongI && rBusResetI)
  val sLong = rState === WAIT_STATE || !rLongI

  // Misc logic
  rTxReady := (ldDataD || (rLineCtrlI && anyEopState)) && io.txValidI
  rLdData := ldDataD

  // trasmit in progess
  when (ldSopD) {
    rTxIp := True
  } elsewhen (appendEop) {
    rTxIp := False
  }

  when (io.fsCe) {
    rTxIpSync := rTxIp
  }

  when (io.txValidI && !rTxIp) {
    rDataXmit := True
  } elsewhen (!io.txValidI) {
    rDataXmit := False
  }

  // Shift register
  when (!rTxIpSync) {
    rBitCnt := 0
  } elsewhen (io.fsCe && !stuff) {
    rBitCnt := rBitCnt + 1
  }

  when (!rTxIpSync) {
    rSdRawO := False
  } otherwise {
    rSdRawO := rHoldD(rBitCnt(2 downto 0))
  }

  when ((rBitCnt.msb === (rLineCtrlI & rLongI)) && rBitCnt(2 downto 0) === U"111") {
    rSftDone := !stuff
  } otherwise {
    rSftDone := False
  }

  rSftDoneR := rSftDone

  // Output data hold register
  when (ldSopD) {
    rHold := 0x80
  } elsewhen (rLdData) {
    rHold := io.dataOutI
  }

  rHoldD := rHold

  // Bit stuffer
  when (!rTxIpSync) {
    rOneCnt := 0
  }  elsewhen (io.fsCe) {
    when (!rSdRawO || stuff) {
      rOneCnt := 0
    } otherwise {
      rOneCnt := rOneCnt + 1
    }
  }

  when (io.fsCe) {
    when (!rTxIpSync) {
      rSdBsO := False
    } otherwise {
      when (stuff) {
        rSdBsO := False
      } otherwise {
        rSdBsO := rSdRawO
      }
    }
  }

  // NRZI encoder
  when (!rTxIpSync || !rTxoeR1 || rLineCtrlI) {
    when (rLineCtrlI) {
      rSdNrziO := sLong
    } otherwise {
      rSdNrziO := True
    }
  } elsewhen (io.fsCe) {
    when (rSdBsO) {
      rSdNrziO := rSdNrziO // Why?
    } otherwise {
      rSdNrziO := !rSdNrziO
    }
  }

  // Output enable logic
  when (io.fsCe) {
    rTxoeR1 := rTxIpSync
    rTxoeR2 := rTxoeR1
    rTxoe := !(rTxoeR1 || rTxoeR2)
  }

  // Ouput registers
  when (io.fsCe) {
    when (io.phyMode) {
      rTxDp := !seState && rSdNrziO
      rTxDn := !seState && !rSdNrziO
    } otherwise {
      rTxDp := rSdNrziO
      rTxDn := seState
    }
  }

  // Tx state machine
  when (!anyEopState) {
    switch (rState) {
      is(IDLE_STATE) {
        when (io.txValidI) {
          rLineCtrlI := io.lineCtrlI
          rLongI := io.dataOutI(0)
          rBusResetI := io.dataOutI(1)
          rState := SOP_STATE
        }
      }
      is(SOP_STATE) {
        when (sftDoneE) {
          rState := DATA_STATE
        }
      }
      is(DATA_STATE) {
        when (!rDataXmit && sftDoneE) {
          when (rOneCnt === U"101" && rHoldD(7)) {
            rState := EOP0_STATE
          } otherwise {
            rState := EOP1_STATE
          }
        }
      }
      is(WAIT_STATE) {
        when (io.fsCe) {
          rState := IDLE_STATE
        }
      }
      default (rState := IDLE_STATE)
    }
  } otherwise {
    when (io.fsCe) {
      when (rState === EOP5_STATE) {
        rState := WAIT_STATE
      } otherwise {
        rState := (rState.asUInt + 1).asBits
      }
    }
  }

  // Output assignments
  io.txReadyO := rTxReady
  io.txdp := rTxDp
  io.txdn := rTxDn
  io.txoe := rTxoe
}

