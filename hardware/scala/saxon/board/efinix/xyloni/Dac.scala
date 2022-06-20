package saxon.board.efinix.xyloni_demoEx.dac

import spinal.core._
import spinal.lib._
import spinal.core.fiber._
import saxon.board.efinix.xyloni_demoEx.bb._

// For use with Analog Devices LTC2634 Dac. It generates 2 curves Gain and Vca. Gain = DAC A, Vca = DAC C

object GainVcaStateEnum extends SpinalEnum {
    val IDLE, DATA, RTZ, STOP = newElement()
}


class DacCls () extends Component {

  //Clk80MHz			    : Input;
  val D             = in Bits(16 bits)
  val A             = in Bits( 9 bits)
  val Draw          = in Bool()       // Starts DAC curves
  val DacClkPrdCs,                    // DAC update Period. Max 20MHz/24 = 833 KHz. Please set as appropriate from app.
    OffsetA_Cs, 
    DacDprCs        = in Bool()
  val AnBcurve      = in Bool()       // to choose Gain or Vca

  val Sclk, MoSi, nSS = out Bool()

  // ------ Variable
  val DacClkPrdVal, OffsetA_Val = Reg(UInt(8 bits))
  val addrCnt 			=	Reg(UInt(8 bits))

  val dpr           = new mixed_width_ram()
  val dprRdQ        = UInt (8 bits)
  val GainVca       = UInt(8 bits)

  val Run           = Reg (False)

  // ------ Begin
  val DacClkPrd  = new Area {
    val counter = Reg(UInt(9 bits))  init(0)  // freq. determined by DacClkPrdVal
    val tick = False

    when (Run){
      counter := counter - 1
      when (counter(8)) {
        counter := DacClkPrdVal.resized    // zero extended
        tick := True
      }
    }.otherwise {
      counter := 0
    }
  }

  //---- gain Vca
  val gainVcaState = new Area {
    import GainVcaStateEnum._
    val state = RegInit(IDLE)
    val CurveRstP = False

    switch (state)  {
      is (IDLE) {
        Handle (when (Draw & Spi.counter.willOverflow) {
        state := DATA
        Run := True
        })
      }

      is (DATA) {
        when (DacClkPrd.tick) {
          when (!Draw) {
            state := RTZ
            addrCnt := 0
          }.otherwise {
          addrCnt := addrCnt + 1
          }
        }
      }

      is (RTZ) {
        CurveRstP := False
        when (DacClkPrd.tick) {
          state := STOP
          Run := False
          CurveRstP := True
        }
      }

      is (STOP) {
        state := IDLE
      }
    }
  }


  //-- Dac (23..16) Value 
  val Dac23_16Val = UInt(8 bits)
  when (AnBcurve) {
    Dac23_16Val := U"8'h31"
  } otherwise {
    Dac23_16Val := U"8'h33"
  }

  //-- Dac (15..8) Value 
  val Dac15_8Val = UInt(8 bits)
  Dac15_8Val  := GainVca    


  //  -- 5. DPRAM
  dpr.waddr   <> A(8 downto 1)
  dpr.wdata   <> D
  dpr.we      <> DacDprCs
  dpr.clk     := ClockDomain.current.readClockWire
  dpr.raddr   := (AnBcurve, addrCnt(7 downto 0)).asBits
  dprRdQ      := dpr.q.asUInt


  val vca       = UInt(8 bits)
  val gain      = SInt(10 bits)
  val gainH     = SInt(10 bits)
  val gainL     = SInt(10 bits)

  gain  := dprRdQ.resize(10).asSInt
  gainH := (gain > S"9'd200") ? S"9'd200" | gain      //(gain > 200)
  gainL := (gainH < S"9'd0") ? S"9'd0"    | gainH     //(gain > 0)

  vca   := dprRdQ + OffsetA_Val

  GainVca := Mux(AnBcurve, vca, gainL(7 downto 0).asUInt)

  // --7. RiscV Iface
  when (DacClkPrdCs) (DacClkPrdVal := D(7 downto 0).asUInt)
  when (OffsetA_Cs) (OffsetA_Val := D(7 downto 0).asUInt)


  // -- SPI
  val clockDivider = new Area {
    val counter = Counter(2)      // 20MHz spiClk
    val enable = Reg(False)
    val tick = counter.willOverflow

    when (enable) (counter.increment)
    .otherwise (counter.clear)
  }  

  val Spi = new Area {
    val sftReg  = Reg(UInt(24 bits)) 
    val counter = Counter(24*2)     // LTC2634 = 24 bit shiftReg. Rising and falling edge
    val ss      = Reg(True)
    val st      = Reg(False)

    st  := RegNext(DacClkPrd.tick | Draw.rise())

    when (st) {
      sftReg  := (Dac23_16Val ## Dac15_8Val ## U"8'h55").asUInt
      clockDivider.enable   := True
      ss  := False
    }         


    when(clockDivider.tick) {
      counter.increment()
      when (counter.willOverflow) {
        clockDivider.enable  := False
        counter.clear
        ss  := True
      }
    }

    Sclk  := RegNext(counter.lsb)
    MoSi  := RegNext(sftReg(23 - (counter>>1)))
    nSS   := ss
  }


}

/*
// Verify
object DacCls {
  def main(args: Array[String]) {
    SpinalVerilog(new DacCls())
  }
}
*/

