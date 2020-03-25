package saxon.board.ulx3s.peripheral

import spinal.core._
import spinal.lib._

class UsbhSie extends Component {
  val io = new Bundle {
    val startI = in Bool
    val inTransferI = in Bool
    val sofTransferI = in Bool
    val respExpectedI = in Bool
    val tokenPidI = in Bits(8 bits)
    val tokenDevI = in Bits(7 bits)
    val tokenEpI = in Bits(4 bits)
    val dataLenI = in UInt(16 bits)
    val dataIdxI = in Bool
    val txDataI = in Bits(8 bits)
    val utmiTxReadyI = in Bool
    val utmiDataI = in Bits (8 bits)
    val utmiRxValidI = in Bool
    val utmiRxActiveI = in Bool

    val ackO = out Bool
    val txPopO = out Bool
    val rxDataO = out Bits(8 bits)
    val rxPushO = out Bool
    val txDoneO = out Bool
    val rxDoneO = out Bool
    val crcErrO = out Bool
    val timeoutO = out Bool
    val responseO = out Bits(8 bits)
    val rxCountO = out UInt(16 bits)
    val idleO = out Bool
    val utmiLineCtrlO = out Bool
    val utmiDataO = out Bits(8 bits)
    val utmiTxValidO = out Bool
  }

  val RX_TIMEOUT       = 511 // 10.6uS @ 48MHz, 85us @ 6MHz
  val TX_IFS           = 7   // 2 FS bit times (x5 CLKs @ 60MHz, x4 CLKs @ 48MHz)

  val PID_DATA0        = B(0xC3, 8 bits)
  val PID_DATA1        = B(0x4B, 8 bits)

  val PID_ACK          = B(0xD2, 8 bits)
  val PID_NAK          = B(0x5A, 8 bits)
  val PID_STALL        = B(0x1E, 8 bits)

  // States
  val STATE_IDLE       = B"0000"
  val STATE_RX_DATA    = B"0001"
  val STATE_TX_PID     = B"0010"
  val STATE_TX_DATA    = B"0011"
  val STATE_TX_CRC1    = B"0100"
  val STATE_TX_CRC2    = B"0101"
  val STATE_TX_TOKEN1  = B"0110"
  val STATE_TX_TOKEN2  = B"0111"
  val STATE_TX_TOKEN3  = B"1000"
  val STATE_TX_ACKNAK  = B"1001"
  val STATE_TX_WAIT    = B"1010"
  val STATE_RX_WAIT    = B"1011"
  val STATE_TX_IFS     = B"1100"

  val RX_TIME_ZERO     = U"000"
  val RX_TIME_INC      = U"001"
  val RX_TIME_READY    = U"111"  // 2-bit times (x5 CLKs @ 60MHz, x4 CLKs @ 48MHz)

  // Registers
  val rStartAckQ = Reg(Bool) init False
  val rStatusTxDoneQ = Reg(Bool) init False
  val rStatusRxDoneQ = Reg(Bool) init False
  val rStatusCrcErrQ = Reg(Bool) init False
  val rStatusTimeoutQ = Reg(Bool) init False
  val rStatusResponseQ = Reg(Bits(8 bits)) init 0
  val rByteCountQ = Reg(UInt(16 bits)) init 0
  val rInTransferQ = Reg(Bool) init False
  val rRxTimeQ = Reg(UInt(3 bits)) init RX_TIME_ZERO
  val rRxTimeEnQ = Reg(Bool) init False
  val rLastTxTimeQ = Reg(UInt(9 bits)) init 0
  val rSendData1Q = Reg(Bool) init False
  val rSendSofQ = Reg(Bool) init False
  val rSendAckQ = Reg(Bool) init False
  val rCrcSumQ = Reg(Bits(16 bits)) init 0xffff
  val rTokenQ = Reg(Bits(16 bits)) init 0
  val rWaitRespQ = Reg(Bool) init False
  val rStateQ = Reg(Bits(4 bits)) init STATE_IDLE
  val rUtmiLineCtrl = Reg(Bool) init False
  val rDataBufferQ = Reg(Bits(32 bits)) init 0
  val rDataValidQ = Reg(Bits(4 bits)) init 0
  val rRxActiveQ = Reg(Bits(4 bits)) init 0
  val rDataCrcQ = Reg(Bits(2 bits)) init 0
  val rUtmiTxValidR = Reg(Bool) init False
  val rUtmiDataR = Reg(Bits(8 bits)) init 0

  // Wires
  val autorespThreshW = rSendAckQ && rRxTimeEnQ && (rRxTimeQ === RX_TIME_READY)
  val rxRespTimeoutW =  (rLastTxTimeQ >= RX_TIMEOUT) && rWaitRespQ
  val txIfsReadyW = rLastTxTimeQ > TX_IFS
  val statusResponseAllowW = (rStatusResponseQ =/= PID_NAK)
  val crcErrorW = rCrcSumQ =/= 0xb001

  val rxDataW = rDataBufferQ(7 downto 0)
  val dataReadyW = rDataValidQ(0)
  val crcByteW = rDataCrcQ(0)
  val rxActiveW = rRxActiveQ(0)

  val crcDataInW = (rStateQ === STATE_RX_DATA) ? rxDataW | io.txDataI
  val crc5OutW = Bits(5 bits)
  val crc5NextW = crc5OutW ^ 0x1f
  val shiftEnW = (io.utmiRxValidI && io.utmiRxActiveI) || !io.utmiRxActiveI
 
  // Complex case:
  //val crcErrorW = rStateQ === STATE_RX_DATA &&
  //                !rxActiveW &&
  //                !rInTransferQ &&
  //                rStatusResponseAllowW &&
  //                rCrcSumQ =/= 0xb001

  val nextStateR = Bits(4 bits)

  nextStateR := rStateQ

  // State machine
  switch(rStateQ) {
    is(STATE_TX_TOKEN1) {
      when(io.utmiTxReadyI) {
        when(rUtmiLineCtrl) {
          nextStateR := STATE_TX_IFS
        } otherwise {
          nextStateR := STATE_TX_TOKEN2
        }
      }
    }
    is(STATE_TX_TOKEN2) {
      when (io.utmiTxReadyI) {
        nextStateR := STATE_TX_TOKEN3
      }
    }
    is(STATE_TX_TOKEN3) {
      when (io.utmiTxReadyI) {
        when (rSendSofQ) {
          nextStateR := STATE_TX_IFS
        } elsewhen (rInTransferQ) {
          nextStateR := STATE_RX_WAIT
        } otherwise {
          nextStateR := STATE_TX_IFS
        }
      }
    }
    is(STATE_TX_IFS) {
      when (txIfsReadyW) {
        when (rSendSofQ) {
          nextStateR := STATE_IDLE
        } otherwise {
          nextStateR := STATE_TX_PID
        }
      }
    }
    is(STATE_TX_PID) {
      when (io.utmiTxReadyI && rByteCountQ === 0) {
        nextStateR := STATE_TX_CRC1
      } elsewhen (io.utmiTxReadyI) {
        nextStateR := STATE_TX_DATA
      }
    }
    is(STATE_TX_DATA) {
      when(io.utmiTxReadyI && rByteCountQ === 0) {
        nextStateR := STATE_TX_CRC1
      }
    }
    is(STATE_TX_CRC1) {
      when (io.utmiTxReadyI) {
        nextStateR := STATE_TX_CRC2
      }
    }
    is(STATE_TX_CRC2) {
      when (io.utmiTxReadyI) {
        when (rWaitRespQ) {
          nextStateR := STATE_RX_WAIT
        } otherwise {
          nextStateR := STATE_IDLE
        }
      }
    }
    is(STATE_TX_WAIT) {
      when (autorespThreshW) {
        nextStateR := STATE_TX_ACKNAK
      }
    }
    is(STATE_TX_ACKNAK) {
      when (io.utmiTxReadyI) {
        nextStateR := STATE_IDLE
      }
    }
    is(STATE_RX_WAIT) {
      when (dataReadyW) {
        nextStateR := STATE_RX_DATA
      } elsewhen (rxRespTimeoutW) {
        nextStateR := STATE_IDLE
      } 
    }
    is(STATE_RX_DATA) {
      when (!rxActiveW) {
        // Send ACK if CRC OK
        when (rSendAckQ && statusResponseAllowW && !crcErrorW) {
          nextStateR := STATE_TX_WAIT
        } otherwise {
          nextStateR := STATE_IDLE
        }
      }
    }
    is(STATE_IDLE) {
      when (io.startI) {
        nextStateR := STATE_TX_TOKEN1
      }
    }
  }

  // Update state
  rStateQ := nextStateR

  // Tx token
  when (rStateQ === STATE_IDLE) {
    rTokenQ := io.tokenDevI ## io.tokenEpI ## B"00000"
  } elsewhen (rStateQ === STATE_TX_TOKEN1 && io.utmiTxReadyI) {
    rTokenQ(4 downto 0) := crc5NextW
  }

  // Tx timer
  when (rStateQ === STATE_IDLE || (io.utmiTxValidO && io.utmiTxReadyI)) {
    rLastTxTimeQ := 0
  } elsewhen (rLastTxTimeQ =/= RX_TIMEOUT) {
    rLastTxTimeQ := rLastTxTimeQ + 1
  }

  // Trasmit / Receive counter
  when (rStateQ === STATE_IDLE && io.startI && !io.sofTransferI) {
    rByteCountQ := io.dataLenI
  } elsewhen (rStateQ === STATE_RX_WAIT) {
    rByteCountQ := 0
  } elsewhen ((rStateQ === STATE_TX_PID || rStateQ === STATE_TX_DATA) && io.utmiTxReadyI) {
    when (rByteCountQ =/= 0) {
      rByteCountQ := rByteCountQ - 1
    }
  } elsewhen (rStateQ === STATE_RX_DATA && dataReadyW && !crcByteW) {
    rByteCountQ := rByteCountQ + 1
  }

  // Transfer start ack
  rStartAckQ := (rStateQ === STATE_TX_TOKEN1 && io.utmiTxReadyI)

  // Record request details
  when (rStateQ === STATE_IDLE && io.startI) {
    rInTransferQ := io.inTransferI
    rSendAckQ := io.inTransferI && io.respExpectedI
    rSendData1Q := io.dataIdxI
    rUtmiLineCtrl := io.sofTransferI && io.inTransferI
    rSendSofQ := io.sofTransferI
  }

  // Response delay timer
  when (rStateQ === STATE_IDLE) {
    rRxTimeQ := RX_TIME_ZERO
    rRxTimeEnQ := False
  } elsewhen (rStateQ === STATE_RX_DATA && !io.utmiRxActiveI) {
    rRxTimeQ := RX_TIME_ZERO
    rRxTimeEnQ := True
  } elsewhen (rRxTimeEnQ && rRxTimeQ =/= RX_TIME_READY) {
    rRxTimeQ := rRxTimeQ + RX_TIME_INC
  }

  // Response expected
  when (rStateQ === STATE_RX_WAIT && dataReadyW) {
    rWaitRespQ := False
  } elsewhen (rStateQ === STATE_IDLE && io.startI) {
    rWaitRespQ := io.respExpectedI
  }

  // Status
  switch (rStateQ) {
    is(STATE_RX_WAIT) {
      when (dataReadyW) (rStatusResponseQ := rxDataW)
      when (rxRespTimeoutW) (rStatusTimeoutQ := True)
      rStatusTxDoneQ := False
    }
    is(STATE_RX_DATA) {
      rStatusRxDoneQ := !io.utmiRxActiveI
    }
    is(STATE_TX_CRC2) {
      when (io.utmiTxReadyI && !rWaitRespQ) {
        rStatusTxDoneQ := True
      }
    }
    is(STATE_IDLE) {
      when (io.startI && !io.sofTransferI) {
        rStatusResponseQ := 0
        rStatusTimeoutQ := False
      }
      rStatusRxDoneQ := False
      rStatusTxDoneQ := False
    }
    default {
      rStatusRxDoneQ := False
      rStatusTxDoneQ := False
    }
  }

  // Data delay to strip CRC16 trailing bytes
  when (shiftEnW) {
    rDataBufferQ := io.utmiDataI ## rDataBufferQ(31 downto 8)
    rDataValidQ := (io.utmiRxValidI & io.utmiRxActiveI) ## rDataValidQ(3 downto 1)
    rDataCrcQ := (!io.utmiRxActiveI).asBits ## rDataCrcQ(1).asBits
  } otherwise {
    rDataValidQ := rDataValidQ(3 downto 1) ## B"0"
  }

  rRxActiveQ := io.utmiRxActiveI.asBits ## rRxActiveQ(3 downto 1)

  // CRC
  val usbCrc16 = new UsbCrc16
  usbCrc16.io.crc_i :=  rCrcSumQ
  usbCrc16.io.data_i := crcDataInW

  val crcOutW = usbCrc16.io.crc_o

  val usbCrc5 = new UsbCrc5
  usbCrc5.io.crc_i := 0x1f
  usbCrc5.io.data_i := rTokenQ(15 downto 5)
  crc5OutW := usbCrc5.io.crc_o

  // CRC control / check
  switch (rStateQ) {
    is(STATE_TX_PID) {
      rCrcSumQ := 0xffff
    }
    is(STATE_TX_DATA) {
      when (io.utmiTxReadyI) {
        rCrcSumQ := crcOutW
      }
    }
    is(STATE_RX_WAIT) {
      rCrcSumQ := 0xffff
    }
    is(STATE_RX_DATA) {
      when (dataReadyW) {
        rCrcSumQ := crcOutW
      } elsewhen (!rxActiveW) {
        rStatusCrcErrQ := crcErrorW && statusResponseAllowW
      }
    }
    is(STATE_IDLE) {
      when (io.startI && !io.sofTransferI) {
        rStatusCrcErrQ := False
      }
    }
  }

  val tokenRevW = Bits(16 bits)

  for(i <- 0 to 15) {
    tokenRevW(i) := rTokenQ(15 - i)
  }

  switch (rStateQ) {
    is(STATE_TX_CRC1) {
      rUtmiTxValidR := True
      rUtmiDataR := rCrcSumQ(7 downto 0) ^ 0xff
    }
    is(STATE_TX_CRC2) {
      rUtmiTxValidR := True
      rUtmiDataR := rCrcSumQ(15 downto 8) ^ 0xff
    }
    is(STATE_TX_TOKEN1) {
      rUtmiTxValidR := True
      rUtmiDataR := io.tokenPidI
    }
    is(STATE_TX_TOKEN2) {
      rUtmiTxValidR := True
      rUtmiDataR := tokenRevW(7 downto 0)
    }
    is(STATE_TX_TOKEN3) {
      rUtmiTxValidR := True
      rUtmiDataR := tokenRevW(15 downto 8)
    }
    is(STATE_TX_PID) {
      rUtmiTxValidR := True
      rUtmiDataR := rSendData1Q ? PID_DATA1 | PID_DATA0
    }
    is(STATE_TX_ACKNAK) {
      rUtmiTxValidR := True
      rUtmiDataR := PID_ACK
    }
    is(STATE_TX_DATA) {
      rUtmiTxValidR := True
      rUtmiDataR := io.txDataI
    }
    default {
      rUtmiTxValidR := False
      rUtmiDataR := 0
    }
  }

  io.utmiTxValidO := rUtmiTxValidR
  io.utmiDataO := rUtmiDataR
  io.utmiLineCtrlO := rUtmiLineCtrl

  io.rxDataO := rxDataW
  io.rxPushO := (rStateQ =/= STATE_IDLE && rStateQ =/= STATE_RX_WAIT) && dataReadyW && !crcByteW

  io.rxCountO := rByteCountQ
  io.idleO := (rStateQ === STATE_IDLE)

  io.ackO := rStartAckQ
  io.txPopO := rStateQ === STATE_TX_DATA && io.utmiTxReadyI

  io.txDoneO := rStatusTxDoneQ
  io.rxDoneO := rStatusRxDoneQ

  io.crcErrO := rStatusCrcErrQ
  io.timeoutO := rStatusTimeoutQ
  io.responseO := rStatusResponseQ
}

