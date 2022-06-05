package saxon.board.ulx3s.peripheral

import spinal.core._
import spinal.lib._

class UsbRxPhy(inputHz : Int = 6000000, 
                bitHz : Int = 1500000,
                paBits : Int = 8) extends Component {
  val io = new Bundle() {
    val usbDif = in Bool()
    val usbDp = in Bool()
    val usbDn = in Bool()
    val lineState = out Bits(2 bits)
    val clkRecovered = out Bool()
    val clkRecoveredEdge = out Bool()
    val rawData = out Bool()
    val rxEn = in Bool()
    val rxActive = out Bool()
    val rxError = out Bool()
    val valid = out Bool()
    val data = out Bits(8 bits)
  }

  val paInc = U(((1 << (paBits - 1)) * (bitHz.toFloat / inputHz)).toInt, paBits bits)
  val paCompensate = paInc(paBits-2 downto 0) + paInc(paBits-2 downto 0) + paInc(paBits-2 downto 0)
  val paInit = paCompensate
  val validInit = 0x80
  val idleCntInit = B"1000000"

  val rPa = Reg(UInt(paBits bits)) init 0
  val rDifShift = Reg(Bits(2 bits)) init 0
  val rClkRecoveredShift = Reg(Bits(2 bits)) init 0
  val rLineBitPrev = Reg(Bool) init False
  val rFrame = Reg(Bool) init False
  val rData = Reg(Bits(8 bits)) init 0
  val rDataLatch = Reg(Bits(8 bits)) init 0
  val rValid = Reg(Bits(8 bits)) init validInit
  val rLineState = Reg(Bits(2 bits)) init 0
  val rLineStateSync = Reg(Bits(2 bits)) init 0
  val rLineStatePrev = Reg(Bits(2 bits)) init 0
  val rIdleCnt = Reg(Bits(7 bits)) init idleCntInit
  val rPreamble = Reg(Bool) init False
  val rRxActive = Reg(Bool) init False
  val rRxEn = Reg(Bool) init False
  val rValidPrev = Reg(Bool) init False

  val sClkRecovered = rPa.msb
  val sLineBit = rDifShift(0)
  val sBit = !(sLineBit ^ rLineBitPrev)

  rClkRecoveredShift := sClkRecovered.asBits ## rClkRecoveredShift.msb.asBits
  rLineState := io.usbDn.asBits ## io.usbDp.asBits
  rLineStatePrev := rLineState
  rRxEn := io.rxEn
  rValidPrev := rValid(0)
  
  when ((io.usbDn || io.usbDp) && io.rxEn) {
    rDifShift := io.usbDif.asBits ## rDifShift(1).asBits
  }

  when (rDifShift(1) =/= rDifShift(0)) {
    rPa(paBits - 2 downto 0) := paInit(paBits - 2 downto 0)
  } otherwise {
    rPa := rPa + paInc
  }

  when (rRxEn) {
    // Synchronous with recovered clock
    when (rClkRecoveredShift(1) =/= sClkRecovered) {
      when (rLineBitPrev === sLineBit) {
        rIdleCnt := rIdleCnt(0).asBits ## rIdleCnt(6 downto 1)
      } otherwise {
        rIdleCnt := idleCntInit
      }
      rLineBitPrev := sLineBit

      when ((!rIdleCnt(0) && rFrame) || !rFrame) {
        when (rLineStateSync === B"00") {
          rData := 0
        } otherwise {
          rData := sBit.asBits ## rData(7 downto 1)
        }
      }

      when (rFrame && rValid(1)) {
        rDataLatch := rData
      }

      when (rLineStateSync === B"00") {
        rFrame := False
        rValid := 0
        rPreamble := False
        rRxActive := False
      } otherwise {
        when (rFrame) {
          when (rPreamble) {
            when (rData(6 downto 1) === B"100000") {
              rPreamble := False
              rValid := validInit
              rRxActive := True
            } 
          } otherwise {
            when (!rIdleCnt(0)) {
              rValid := rValid(0).asBits ## rValid(7 downto 1)
            } otherwise {
              when (sBit) {
                rValid := 0
                rFrame := False
                rRxActive := False
              }  
            }
          } 
        } otherwise { // !rFrame
          when (rData(7 downto 2) === B"000111") {
            rFrame := True
            rPreamble := True
            rValid := 0
            rRxActive := False
          }
        }
      }
      rLineStateSync := rLineState
    }
    // synchronous with recovered clock
  } otherwise { // !rxEn
    rValid := 0
    rFrame := False
    rRxActive := False
  }
  
  io.data := rDataLatch
  io.rawData := rLineBitPrev
  io.lineState := rLineState
  io.rxActive := rFrame
  io.valid := rValid(0) & !rValidPrev
  io.rxError := False
  io.clkRecovered := sClkRecovered
  io.clkRecoveredEdge := (rClkRecoveredShift(1) =/= sClkRecovered)
}
