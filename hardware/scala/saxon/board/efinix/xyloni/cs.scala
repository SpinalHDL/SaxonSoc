package saxon.board.efinix.xyloni_demoEx.cs

import spinal.core._
import spinal.lib._


class CsCls ()  extends Component {
  val Bus16A   = in Bits (12 bits)
  val Bus16Wr  = in Bool()
  val Bus32A   = in Bits (12 bits)
  val Bus32Wr  = in Bool()
  val Bus32Rd  = in Bool()


  val FifoWrCs, DebugValCs, DacClkPrdCs,
    DacOffsetA_Cs, ToggleRegCs, MonoShotRegCs,
    DacDprCs    = out Bool()

	FifoWrCs		    := Bus16Wr & (Bus16A  === 0x0)
	DebugValCs		  := Bus16Wr & (Bus16A  === 0x2)
	DacClkPrdCs     := Bus16Wr & (Bus16A  === 0x10)
	DacOffsetA_Cs   := Bus16Wr & (Bus16A  === 0x12)
  ToggleRegCs     := Bus16Wr & (Bus16A  === 0x60)
  MonoShotRegCs   := Bus16Wr & (Bus16A  === 0x62)

  DacDprCs        := Bus16Wr & (Bus16A (11 downto  9) === 0x1)  // 001  0x200 to 0x3FF

  // Bus32
  val FifoRdCs    = out (Bus32Rd & (Bus32A  === 64))
  val SysCtrlCs   = out (Bus32Wr & (Bus32A  === 68))
  val GpioCs      = out (Bus32Wr & (Bus32A  === 76))
  val GpioRCs     = out (Bus32Rd & (Bus32A  === 76))
  val GpioSetCs   = out (Bus32Wr & (Bus32A  === 80))
  val GpioClrCs   = out (Bus32Wr & (Bus32A  === 84))
}
