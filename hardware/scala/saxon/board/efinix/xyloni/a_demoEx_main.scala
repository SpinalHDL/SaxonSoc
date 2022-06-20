package saxon.board.efinix.xyloni_demoEx


import spinal.core._
import spinal.lib._
import spinal.core.fiber._
import spinal.lib.generator._


import saxon.board.efinix.xyloni_demoEx.cs._
import saxon.board.efinix.xyloni_demoEx.dac._
import saxon.board.efinix.xyloni_demoEx.bb._


class DemoEx extends Component {

  val io = new Bundle {

    val  ClkCore				= in Bool() 
    val  nReset				  = in Bool()
    
    val  jtag_inst1_tck		  = in Bool()  
    val  jtag_inst1_tdi		  = in Bool()  
    val  jtag_inst1_enable	= in Bool()         
    val  jtag_inst1_capture	= in Bool()         
    val  jtag_inst1_shift		= in Bool()        
    val  jtag_inst1_update	= in Bool()        
    val  jtag_inst1_reset		= in Bool()        
    val  jtag_inst1_tdo		  = out Bool()        

    val system_gpioA_gpio_read        = in Bits(8 bits)
    val system_gpioA_gpio_write       = out Bits(8 bits)
    val system_gpioA_gpio_writeEnable = out Bits(8 bits)

    val system_uartA_uart_txd         = out Bool()
    val system_uartA_uart_rxd         = in Bool()

    val  Clk80MHz           = in Bool()

	  // -- A port to connect something like EzUsbFx2
    val UsbA        = in  Bits (8 bits)
    val UsbD_rd     = in  Bits (16 bits)    // UsbD
    val UsbD_wr     = out Bits (16 bits)
    val UsbD_we     = out Bits (16 bits)

    val StrobeUsb 	= in  Bool()
    val Dpr_nFifo		= in  Bool()	
    val AfmFifoFlag	= out Bool()	
    val IfClk				= in  Bool()	
    val Rd_nWr_Usb	= in  Bool()	

	  // -- DAC
    val Sclk_Dac		= out Bool()	  // J1.18
    val MoSi_Dac 	  = out Bool()    // J1.17
    val nSS_Dac	    = out Bool()	  // J1.16

    // -- Misc
    val XF          = out Bool()    // J2.3
    val Toggle      = out Bool()    // J2.4
    val Pulse       = out Bool()    // J2.8
    val UserOut1    = out Bool()    // J2.9
    val UserOut2    = out Bool()    // J2.10
    val BTN2        = in  Bool()    
  } // io

  //---- downstream variables
  val Cs        = new CsCls
  val FifoA     = new DcFifo
  val Dac       = Handle (Clk80MHzCd on new DacCls)
  
  //---- wires
  val UsbDpr_b_clk  =  Bool()
  val UsbDpr_b_wr   =  Bool()
  val UsbDpr_b_addr =  Bits(9 bits)
  val UsbDpr_b_din  =  Bits(16 bits)
  val UsbDpr_b_dout =  Bits(16 bits)
  val Bus16_wr      =  Bool()
  val Bus16_A       =  Bits(12 bits)
  val Bus16_Din     =  Bits(16 bits)
  val Bus16_Dout    =  Bits(16 bits)
  val Bus32_wr      =  Bool()
  val Bus32_rd      =  Bool()
  val Bus32_A       =  Bits(12 bits)
  val Bus32_Din     =  Bits(32 bits)
  val Bus32_Dout    =  Bits(32 bits)

  val FifoA_clr, DacDraw, AnBcurve  = Bool()

  //---- 0. Reset
  val rst1 = new ClockingArea (ClockDomain (clock = io.ClkCore, reset = False)) {
    val rst = Reg (False)
    val tout = Reg (UInt (4 bits)) init (0)
    when (tout(3)) {
      rst := True
    } otherwise {
      tout := tout + 1
    }
  }
  val Reset   = !rst1.rst


	//---- 0. Clock
  // assigned in interface editor in pll

  val ClkCoreCd = ClockDomain(
    clock = io.ClkCore,
    reset = Reset
  )
  val Clk80MHzCd = ClockDomain(
    clock = io.Clk80MHz,
    reset = Reset
  )
  val IfClkCd = ClockDomain (
    clock = io.IfClk,
    reset = Reset
  )


  //---- 2. UsbDpr
  UsbDpr_b_clk          <> io.IfClk
  UsbDpr_b_wr           := (io.Dpr_nFifo & !io.Rd_nWr_Usb & io.StrobeUsb)
  UsbDpr_b_addr         := io.UsbA (7 downto 0).resized   // high 512B has no access from USB!
  UsbDpr_b_din          := io.UsbD_rd


  io.UsbD_we.setAllTo (io.Rd_nWr_Usb | !io.Dpr_nFifo)
  when (io.Dpr_nFifo) {
    io.UsbD_wr := UsbDpr_b_dout
  } .otherwise {
    io.UsbD_wr := FifoA.rdata
  }

  
  //---- 3. FifoA
  val FifoRdArea = new ClockingArea(ClkCoreCd) {
    val q = Bits (16 bits)
    val firstVal = RegNextWhen(Bus16_Dout(7 downto 0) , Cs.FifoWrCs)
    when (RegNext(Cs.FifoRdCs)) (q := FifoA.rdata) .otherwise (q := 0)   // this works

  }

  val FifoWrArea = new ClockingArea(Clk80MHzCd) {
    val p1, go  = Reg (False)
    val cnt     = Reg (UInt(3 bits))
    val q       = UInt (8 bits)
    p1          := Cs.FifoWrCs.fall()
    when (p1) {
      go.set()
    } elsewhen (go) {
      cnt   := cnt + 1
      when (cnt > 4) {
        go.clear()
        cnt := 0
      }
    }
    q  := cnt +  FifoRdArea.firstVal.asUInt.addTag(crossClockDomain)
  }

  FifoA.wr_clk_i     <> io.Clk80MHz 
  FifoA.rd_clk_i     <> io.ClkCore      // io.IfClk
  FifoA.wr_en_i      := FifoWrArea.go 
  FifoA.rd_en_i      := Cs.FifoRdCs     // !io.Dpr_nFifo & io.StrobeUsb
  FifoA.wdata        := FifoWrArea.q.asBits.resized
  FifoA.a_rst_i      := FifoA_clr

  io.AfmFifoFlag     := FifoA.prog_empty_o


  //---- 5. DAC
  Dac.D               <> Bus16_Dout
  Dac.A               <> Bus16_A (8 downto 0)
  Dac.Draw            <> DacDraw
  Dac.DacClkPrdCs     <> Cs.DacClkPrdCs
  Dac.OffsetA_Cs      <> Cs.DacOffsetA_Cs
  Dac.DacDprCs        <> Cs.DacDprCs
  Dac.AnBcurve        <> AnBcurve

  io.Sclk_Dac         <> Dac.Sclk
  io.MoSi_Dac         <> Dac.MoSi
  io.nSS_Dac          <> Dac.nSS



  val ClkCoreArea = new ClockingArea(ClkCoreCd) {

    //-- ToggleReg
    val ToggleReg = Reg(False)
    when(Cs.ToggleRegCs & Bus16_Dout(0)) {  
      ToggleReg     := !ToggleReg
    }
    io.Toggle := ToggleReg

    //-- MonoShotReg
    FifoA_clr     := RegNext (Cs.MonoShotRegCs & Bus16_Dout(0))
    io.Pulse      := RegNext (Cs.MonoShotRegCs & Bus16_Dout(1))

    //-- SysCtrl Reg
    io.UserOut1         := RegNextWhen (Bus32_Dout(0), Cs.SysCtrlCs)
    io.UserOut2         := RegNextWhen (Bus32_Dout(1), Cs.SysCtrlCs)

    //-- My Gpio
    val Gpio      = RegNextWhen(Bus32_Dout(2 downto 0) , Cs.GpioCs)

    when(Cs.GpioSetCs) (Gpio  := (Gpio |   Bus32_Dout (2 downto 0)) )
    when(Cs.GpioClrCs) (Gpio  := (Gpio &  ~Bus32_Dout (2 downto 0)) )

    (DacDraw, AnBcurve)   := Gpio(1 downto 0).addTag(crossClockDomain)

    //-- GpioR
    val GpioR = Reg(Bits(8 bits))
    when (Cs.GpioRCs) {      
      GpioR(0)  := io.BTN2
      GpioR(1)  := FifoA.prog_full_o
      GpioR(2)  := True
      GpioR(3)  := False
      GpioR(4)  := True
      GpioR(5)  := False
      GpioR(6)  := False
      GpioR(7)  := False
    } otherwise {
      GpioR   := 0  
    }

    Bus16_Din  := B"0000_0000_0000_0000"
  }

  //---- Bus32 Din Collection
  Bus32_Din   := FifoRdArea.q.resize(32) | ClkCoreArea.GpioR.resize(32)  
                  

  //---- Chip Selects
  Cs.Bus16Wr  := Bus16_wr
  Cs.Bus16A   := Bus16_A
  Cs.Bus32A   := Bus32_A
  Cs.Bus32Wr  := Bus32_wr
  Cs.Bus32Rd  := Bus32_rd

  //---- 10. SoC
  val SoC = new saxon.board.efinix.xyloni_soc.bb.SoC()
    
  SoC.ClkCore               :=    io.ClkCore 
  SoC.nReset                :=    io.nReset

  io.jtag_inst1_tck         <>    SoC.jtag_inst1_tck
  io.jtag_inst1_tdi         <>    SoC.jtag_inst1_tdi
  io.jtag_inst1_enable      <>    SoC.jtag_inst1_enable
  io.jtag_inst1_capture     <>    SoC.jtag_inst1_capture
  io.jtag_inst1_shift       <>    SoC.jtag_inst1_shift
  io.jtag_inst1_update      <>    SoC.jtag_inst1_update
  io.jtag_inst1_reset       <>    SoC.jtag_inst1_reset
  io.jtag_inst1_tdo         <>    SoC.jtag_inst1_tdo

  Bus32_wr        <>    SoC.Bus32_wr
  Bus32_rd        <>    SoC.Bus32_rd
  Bus32_A         <>    SoC.Bus32_A
  Bus32_Din       <>    SoC.Bus32_Din
  Bus32_Dout      <>    SoC.Bus32_Dout

  io.system_gpioA_gpio_read           <>    SoC.system_gpioA_gpio_read
  io.system_gpioA_gpio_write          <>    SoC.system_gpioA_gpio_write
  io.system_gpioA_gpio_writeEnable    <>    SoC.system_gpioA_gpio_writeEnable
  io.system_uartA_uart_txd            <>    SoC.system_uartA_uart_txd
  io.system_uartA_uart_rxd            <>    SoC.system_uartA_uart_rxd


  UsbDpr_b_clk    <>    SoC.UsbDpr_b_clk
  UsbDpr_b_wr     <>    SoC.UsbDpr_b_wr
  UsbDpr_b_addr   <>    SoC.UsbDpr_b_addr
  UsbDpr_b_din    <>    SoC.UsbDpr_b_din
  UsbDpr_b_dout   <>    SoC.UsbDpr_b_dout

  Bus16_wr        <>    SoC.Bus16_wr
  Bus16_A         <>    SoC.Bus16_A
  Bus16_Din       <>    SoC.Bus16_Din
  Bus16_Dout      <>    SoC.Bus16_Dout

  
  // ---- Debug
  io.XF     := Cs.FifoRdCs


  // ---- compile flags
  noIoPrefix()

}



//Generate the MyTopLevel's Verilog
object xyloni_demoEx {
  def main(args: Array[String]) {

    def mySpinalConfig = SpinalConfig(targetDirectory = saxon.board.efinix.xyloni_soc.Const.GENERATED_VERILOG_DIR)

    mySpinalConfig.generateVerilog (new DemoEx())
  }
}



