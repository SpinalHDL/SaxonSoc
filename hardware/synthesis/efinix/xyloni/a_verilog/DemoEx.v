// Generator : SpinalHDL v1.7.1-SNAPSHOT    git head : ???
// Component : DemoEx

`timescale 1ns/1ps

module DemoEx (
  input               ClkCore,
  input               nReset,
  input               jtag_inst1_tck,
  input               jtag_inst1_tdi,
  input               jtag_inst1_enable,
  input               jtag_inst1_capture,
  input               jtag_inst1_shift,
  input               jtag_inst1_update,
  input               jtag_inst1_reset,
  output              jtag_inst1_tdo,
  input      [7:0]    system_gpioA_gpio_read,
  output     [7:0]    system_gpioA_gpio_write,
  output     [7:0]    system_gpioA_gpio_writeEnable,
  output              system_uartA_uart_txd,
  input               system_uartA_uart_rxd,
  input               Clk80MHz,
  input      [7:0]    UsbA,
  input      [15:0]   UsbD_rd,
  output reg [15:0]   UsbD_wr,
  output     [15:0]   UsbD_we,
  input               StrobeUsb,
  input               Dpr_nFifo,
  output              AfmFifoFlag,
  input               IfClk,
  input               Rd_nWr_Usb,
  output              Sclk_Dac,
  output              MoSi_Dac,
  output              nSS_Dac,
  output              XF,
  output              Toggle,
  output              Pulse,
  output              UserOut1,
  output              UserOut2,
  input               BTN2
);

  wire       [15:0]   FifoA_wdata;
  wire       [8:0]    Dac_A;
  wire                Cs_FifoWrCs;
  wire                Cs_DebugValCs;
  wire                Cs_DacClkPrdCs;
  wire                Cs_DacOffsetA_Cs;
  wire                Cs_ToggleRegCs;
  wire                Cs_MonoShotRegCs;
  wire                Cs_DacDprCs;
  wire                Cs_FifoRdCs;
  wire                Cs_SysCtrlCs;
  wire                Cs_GpioCs;
  wire                Cs_GpioRCs;
  wire                Cs_GpioSetCs;
  wire                Cs_GpioClrCs;
  wire       [15:0]   FifoA_rdata;
  wire                FifoA_prog_full_o;
  wire                FifoA_prog_empty_o;
  wire                FifoA_full_o;
  wire                FifoA_empty_o;
  wire       [9:0]    FifoA_wr_datacount_o;
  wire       [9:0]    FifoA_rd_datacount_o;
  wire                FifoA_rst_busy;
  wire                Dac_Sclk;
  wire                Dac_MoSi;
  wire                Dac_nSS;
  wire                SoC_1_jtag_inst1_tdo;
  wire       [7:0]    SoC_1_system_gpioA_gpio_write;
  wire       [7:0]    SoC_1_system_gpioA_gpio_writeEnable;
  wire                SoC_1_system_uartA_uart_txd;
  wire                SoC_1_Bus32_wr;
  wire                SoC_1_Bus32_rd;
  wire       [11:0]   SoC_1_Bus32_A;
  wire       [31:0]   SoC_1_Bus32_Dout;
  wire       [15:0]   SoC_1_UsbDpr_b_dout;
  wire                SoC_1_Bus16_wr;
  wire       [11:0]   SoC_1_Bus16_A;
  wire       [15:0]   SoC_1_Bus16_Dout;
  wire       [7:0]    _zz_UsbDpr_b_addr;
  wire       [7:0]    _zz_FifoWrArea_q_1;
  wire       [7:0]    _zz_wdata;
  wire       [31:0]   _zz_Bus32_Din;
  wire       [31:0]   _zz_Bus32_Din_1;
  wire                UsbDpr_b_clk;
  wire                UsbDpr_b_wr;
  wire       [8:0]    UsbDpr_b_addr;
  wire       [15:0]   UsbDpr_b_din;
  wire       [15:0]   UsbDpr_b_dout;
  wire                Bus16_wr;
  wire       [11:0]   Bus16_A;
  wire       [15:0]   Bus16_Din;
  wire       [15:0]   Bus16_Dout;
  wire                Bus32_wr;
  wire                Bus32_rd;
  wire       [11:0]   Bus32_A;
  wire       [31:0]   Bus32_Din;
  wire       [31:0]   Bus32_Dout;
  wire                FifoA_clr;
  wire                DacDraw;
  wire                AnBcurve;
  wire                _zz_1;
  reg                 rst1_rst;
  reg        [3:0]    rst1_tout;
  wire                when_a_demoEx_main_l93;
  wire                Reset;
  reg        [15:0]   FifoRdArea_q;
  reg        [7:0]    FifoRdArea_firstVal;
  reg                 Cs_FifoRdCs_regNext;
  reg                 FifoWrArea_p1;
  reg                 FifoWrArea_go;
  reg        [2:0]    FifoWrArea_cnt;
  wire       [7:0]    FifoWrArea_q;
  reg                 Cs_FifoWrCs_regNext;
  wire                when_a_demoEx_main_l151;
  wire       [7:0]    _zz_FifoWrArea_q;
  reg                 ClkCoreArea_ToggleReg;
  wire                when_a_demoEx_main_l188;
  reg                 _zz_FifoA_clr;
  reg                 _zz_Pulse;
  reg                 _zz_UserOut1;
  reg                 _zz_UserOut2;
  reg        [2:0]    ClkCoreArea_Gpio;
  wire       [1:0]    _zz_DacDraw;
  reg        [7:0]    ClkCoreArea_GpioR;

  assign _zz_UsbDpr_b_addr = UsbA[7 : 0];
  assign _zz_FifoWrArea_q_1 = {5'd0, FifoWrArea_cnt};
  assign _zz_wdata = FifoWrArea_q;
  assign _zz_Bus32_Din = {16'd0, FifoRdArea_q};
  assign _zz_Bus32_Din_1 = {24'd0, ClkCoreArea_GpioR};
  CsCls Cs (
    .Bus16A        (Bus16_A[11:0]   ), //i
    .Bus16Wr       (Bus16_wr        ), //i
    .Bus32A        (Bus32_A[11:0]   ), //i
    .Bus32Wr       (Bus32_wr        ), //i
    .Bus32Rd       (Bus32_rd        ), //i
    .FifoWrCs      (Cs_FifoWrCs     ), //o
    .DebugValCs    (Cs_DebugValCs   ), //o
    .DacClkPrdCs   (Cs_DacClkPrdCs  ), //o
    .DacOffsetA_Cs (Cs_DacOffsetA_Cs), //o
    .ToggleRegCs   (Cs_ToggleRegCs  ), //o
    .MonoShotRegCs (Cs_MonoShotRegCs), //o
    .DacDprCs      (Cs_DacDprCs     ), //o
    .FifoRdCs      (Cs_FifoRdCs     ), //o
    .SysCtrlCs     (Cs_SysCtrlCs    ), //o
    .GpioCs        (Cs_GpioCs       ), //o
    .GpioRCs       (Cs_GpioRCs      ), //o
    .GpioSetCs     (Cs_GpioSetCs    ), //o
    .GpioClrCs     (Cs_GpioClrCs    )  //o
  );
  DcFifo FifoA (
    .wr_clk_i       (Clk80MHz                 ), //i
    .rd_clk_i       (ClkCore                  ), //i
    .wr_en_i        (FifoWrArea_go            ), //i
    .rd_en_i        (Cs_FifoRdCs              ), //i
    .rdata          (FifoA_rdata[15:0]        ), //o
    .wdata          (FifoA_wdata[15:0]        ), //i
    .prog_full_o    (FifoA_prog_full_o        ), //o
    .prog_empty_o   (FifoA_prog_empty_o       ), //o
    .a_rst_i        (FifoA_clr                ), //i
    .full_o         (FifoA_full_o             ), //o
    .empty_o        (FifoA_empty_o            ), //o
    .wr_datacount_o (FifoA_wr_datacount_o[9:0]), //o
    .rd_datacount_o (FifoA_rd_datacount_o[9:0]), //o
    .rst_busy       (FifoA_rst_busy           )  //o
  );
  DacCls Dac (
    .D           (Bus16_Dout[15:0]), //i
    .A           (Dac_A[8:0]      ), //i
    .Draw        (DacDraw         ), //i
    .DacClkPrdCs (Cs_DacClkPrdCs  ), //i
    .OffsetA_Cs  (Cs_DacOffsetA_Cs), //i
    .DacDprCs    (Cs_DacDprCs     ), //i
    .AnBcurve    (AnBcurve        ), //i
    .Sclk        (Dac_Sclk        ), //o
    .MoSi        (Dac_MoSi        ), //o
    .nSS         (Dac_nSS         ), //o
    .Clk80MHz    (Clk80MHz        ), //i
    .Reset       (Reset           )  //i
  );
  SoC SoC_1 (
    .ClkCore                       (ClkCore                                 ), //i
    .nReset                        (nReset                                  ), //i
    .jtag_inst1_tck                (jtag_inst1_tck                          ), //i
    .jtag_inst1_tdi                (jtag_inst1_tdi                          ), //i
    .jtag_inst1_enable             (jtag_inst1_enable                       ), //i
    .jtag_inst1_capture            (jtag_inst1_capture                      ), //i
    .jtag_inst1_shift              (jtag_inst1_shift                        ), //i
    .jtag_inst1_update             (jtag_inst1_update                       ), //i
    .jtag_inst1_reset              (jtag_inst1_reset                        ), //i
    .jtag_inst1_tdo                (SoC_1_jtag_inst1_tdo                    ), //o
    .system_gpioA_gpio_read        (system_gpioA_gpio_read[7:0]             ), //i
    .system_gpioA_gpio_write       (SoC_1_system_gpioA_gpio_write[7:0]      ), //o
    .system_gpioA_gpio_writeEnable (SoC_1_system_gpioA_gpio_writeEnable[7:0]), //o
    .system_uartA_uart_txd         (SoC_1_system_uartA_uart_txd             ), //o
    .system_uartA_uart_rxd         (system_uartA_uart_rxd                   ), //i
    .Bus32_wr                      (SoC_1_Bus32_wr                          ), //o
    .Bus32_rd                      (SoC_1_Bus32_rd                          ), //o
    .Bus32_A                       (SoC_1_Bus32_A[11:0]                     ), //o
    .Bus32_Din                     (Bus32_Din[31:0]                         ), //i
    .Bus32_Dout                    (SoC_1_Bus32_Dout[31:0]                  ), //o
    .UsbDpr_b_clk                  (UsbDpr_b_clk                            ), //i
    .UsbDpr_b_wr                   (UsbDpr_b_wr                             ), //i
    .UsbDpr_b_addr                 (UsbDpr_b_addr[8:0]                      ), //i
    .UsbDpr_b_din                  (UsbDpr_b_din[15:0]                      ), //i
    .UsbDpr_b_dout                 (SoC_1_UsbDpr_b_dout[15:0]               ), //o
    .Bus16_wr                      (SoC_1_Bus16_wr                          ), //o
    .Bus16_A                       (SoC_1_Bus16_A[11:0]                     ), //o
    .Bus16_Din                     (Bus16_Din[15:0]                         ), //i
    .Bus16_Dout                    (SoC_1_Bus16_Dout[15:0]                  )  //o
  );
  assign _zz_1 = 1'b0;
  assign when_a_demoEx_main_l93 = rst1_tout[3];
  assign Reset = (! rst1_rst);
  assign UsbDpr_b_clk = IfClk;
  assign UsbDpr_b_wr = ((Dpr_nFifo && (! Rd_nWr_Usb)) && StrobeUsb);
  assign UsbDpr_b_addr = {1'd0, _zz_UsbDpr_b_addr};
  assign UsbDpr_b_din = UsbD_rd;
  assign UsbD_we = ((Rd_nWr_Usb || (! Dpr_nFifo)) ? 16'hffff : 16'h0);
  always @(*) begin
    if(Dpr_nFifo) begin
      UsbD_wr = UsbDpr_b_dout;
    end else begin
      UsbD_wr = FifoA_rdata;
    end
  end

  always @(*) begin
    if(Cs_FifoRdCs_regNext) begin
      FifoRdArea_q = FifoA_rdata;
    end else begin
      FifoRdArea_q = 16'h0;
    end
  end

  assign when_a_demoEx_main_l151 = (3'b100 < FifoWrArea_cnt);
  assign _zz_FifoWrArea_q = FifoRdArea_firstVal;
  assign FifoWrArea_q = (_zz_FifoWrArea_q_1 + _zz_FifoWrArea_q);
  assign FifoA_wdata = {8'd0, _zz_wdata};
  assign AfmFifoFlag = FifoA_prog_empty_o;
  assign Dac_A = Bus16_A[8 : 0];
  assign Sclk_Dac = Dac_Sclk;
  assign MoSi_Dac = Dac_MoSi;
  assign nSS_Dac = Dac_nSS;
  assign when_a_demoEx_main_l188 = (Cs_ToggleRegCs && Bus16_Dout[0]);
  assign Toggle = ClkCoreArea_ToggleReg;
  assign FifoA_clr = _zz_FifoA_clr;
  assign Pulse = _zz_Pulse;
  assign UserOut1 = _zz_UserOut1;
  assign UserOut2 = _zz_UserOut2;
  assign _zz_DacDraw = ClkCoreArea_Gpio[1 : 0];
  assign AnBcurve = _zz_DacDraw[0];
  assign DacDraw = _zz_DacDraw[1];
  assign Bus16_Din = 16'h0;
  assign Bus32_Din = (_zz_Bus32_Din | _zz_Bus32_Din_1);
  assign jtag_inst1_tdo = SoC_1_jtag_inst1_tdo;
  assign Bus32_wr = SoC_1_Bus32_wr;
  assign Bus32_rd = SoC_1_Bus32_rd;
  assign Bus32_A = SoC_1_Bus32_A;
  assign Bus32_Dout = SoC_1_Bus32_Dout;
  assign system_gpioA_gpio_write = SoC_1_system_gpioA_gpio_write;
  assign system_gpioA_gpio_writeEnable = SoC_1_system_gpioA_gpio_writeEnable;
  assign system_uartA_uart_txd = SoC_1_system_uartA_uart_txd;
  assign UsbDpr_b_dout = SoC_1_UsbDpr_b_dout;
  assign Bus16_wr = SoC_1_Bus16_wr;
  assign Bus16_A = SoC_1_Bus16_A;
  assign Bus16_Dout = SoC_1_Bus16_Dout;
  assign XF = Cs_FifoRdCs;
  always @(posedge ClkCore or posedge _zz_1) begin
    if(_zz_1) begin
      rst1_tout <= 4'b0000;
    end else begin
      if(!when_a_demoEx_main_l93) begin
        rst1_tout <= (rst1_tout + 4'b0001);
      end
    end
  end

  always @(posedge ClkCore) begin
    if(when_a_demoEx_main_l93) begin
      rst1_rst <= 1'b1;
    end
  end

  always @(posedge ClkCore) begin
    if(Cs_FifoWrCs) begin
      FifoRdArea_firstVal <= Bus16_Dout[7 : 0];
    end
    Cs_FifoRdCs_regNext <= Cs_FifoRdCs;
    if(when_a_demoEx_main_l188) begin
      ClkCoreArea_ToggleReg <= (! ClkCoreArea_ToggleReg);
    end
    _zz_FifoA_clr <= (Cs_MonoShotRegCs && Bus16_Dout[0]);
    _zz_Pulse <= (Cs_MonoShotRegCs && Bus16_Dout[1]);
    if(Cs_SysCtrlCs) begin
      _zz_UserOut1 <= Bus32_Dout[0];
    end
    if(Cs_SysCtrlCs) begin
      _zz_UserOut2 <= Bus32_Dout[1];
    end
    if(Cs_GpioCs) begin
      ClkCoreArea_Gpio <= Bus32_Dout[2 : 0];
    end
    if(Cs_GpioSetCs) begin
      ClkCoreArea_Gpio <= (ClkCoreArea_Gpio | Bus32_Dout[2 : 0]);
    end
    if(Cs_GpioClrCs) begin
      ClkCoreArea_Gpio <= (ClkCoreArea_Gpio & (~ Bus32_Dout[2 : 0]));
    end
    if(Cs_GpioRCs) begin
      ClkCoreArea_GpioR[0] <= BTN2;
      ClkCoreArea_GpioR[1] <= FifoA_prog_full_o;
      ClkCoreArea_GpioR[2] <= 1'b1;
      ClkCoreArea_GpioR[3] <= 1'b0;
      ClkCoreArea_GpioR[4] <= 1'b1;
      ClkCoreArea_GpioR[5] <= 1'b0;
      ClkCoreArea_GpioR[6] <= 1'b0;
      ClkCoreArea_GpioR[7] <= 1'b0;
    end else begin
      ClkCoreArea_GpioR <= 8'h0;
    end
  end

  always @(posedge Clk80MHz) begin
    Cs_FifoWrCs_regNext <= Cs_FifoWrCs;
    FifoWrArea_p1 <= ((! Cs_FifoWrCs) && Cs_FifoWrCs_regNext);
    if(FifoWrArea_p1) begin
      FifoWrArea_go <= 1'b1;
    end else begin
      if(FifoWrArea_go) begin
        FifoWrArea_cnt <= (FifoWrArea_cnt + 3'b001);
        if(when_a_demoEx_main_l151) begin
          FifoWrArea_go <= 1'b0;
          FifoWrArea_cnt <= 3'b000;
        end
      end
    end
  end


endmodule

module DacCls (
  input      [15:0]   D,
  input      [8:0]    A,
  input               Draw,
  input               DacClkPrdCs,
  input               OffsetA_Cs,
  input               DacDprCs,
  input               AnBcurve,
  output              Sclk,
  output              MoSi,
  output              nSS,
  input               Clk80MHz,
  input               Reset
);
  localparam GainVcaStateEnum_IDLE = 2'd0;
  localparam GainVcaStateEnum_DATA = 2'd1;
  localparam GainVcaStateEnum_RTZ = 2'd2;
  localparam GainVcaStateEnum_STOP = 2'd3;

  wire       [7:0]    dpr_waddr;
  wire       [8:0]    dpr_raddr;
  wire       [7:0]    dpr_q;
  wire       [9:0]    _zz_gain;
  wire       [9:0]    _zz_gainH;
  wire       [8:0]    _zz_gainH_1;
  wire       [9:0]    _zz_gainH_2;
  wire       [8:0]    _zz_gainH_3;
  wire       [9:0]    _zz_gainL;
  wire       [8:0]    _zz_gainL_1;
  wire       [9:0]    _zz_gainL_2;
  wire       [8:0]    _zz_gainL_3;
  wire       [7:0]    _zz_GainVca;
  wire       [5:0]    _zz_Spi_counter_valueNext;
  wire       [0:0]    _zz_Spi_counter_valueNext_1;
  wire       [4:0]    _zz__zz_MoSi;
  wire       [4:0]    _zz__zz_MoSi_1;
  reg        [7:0]    DacClkPrdVal;
  reg        [7:0]    OffsetA_Val;
  reg        [7:0]    addrCnt;
  wire       [7:0]    dprRdQ;
  wire       [7:0]    GainVca;
  reg                 Run;
  reg        [8:0]    DacClkPrd_counter;
  reg                 DacClkPrd_tick;
  wire                when_Dac_l45;
  reg        [1:0]    gainVcaState_state;
  reg                 gainVcaState_CurveRstP;
  wire                when_Dac_l62;
  wire                when_Dac_l70;
  reg        [7:0]    Dac23_16Val;
  wire       [7:0]    Dac15_8Val;
  wire       [7:0]    vca;
  wire       [9:0]    gain;
  wire       [9:0]    gainH;
  wire       [9:0]    gainL;
  reg                 clockDivider_counter_willIncrement;
  reg                 clockDivider_counter_willClear;
  reg        [0:0]    clockDivider_counter_valueNext;
  reg        [0:0]    clockDivider_counter_value;
  wire                clockDivider_counter_willOverflowIfInc;
  wire                clockDivider_counter_willOverflow;
  reg                 clockDivider_enable;
  reg        [23:0]   Spi_sftReg;
  reg                 Spi_counter_willIncrement;
  reg                 Spi_counter_willClear;
  reg        [5:0]    Spi_counter_valueNext;
  reg        [5:0]    Spi_counter_value;
  wire                Spi_counter_willOverflowIfInc;
  wire                Spi_counter_willOverflow;
  reg                 Spi_ss;
  reg                 Spi_st;
  reg                 Draw_regNext;
  reg                 _zz_Spi_st;
  reg                 _zz_Sclk;
  reg                 _zz_MoSi;
  `ifndef SYNTHESIS
  reg [31:0] gainVcaState_state_string;
  `endif


  assign _zz_gain = {2'd0, dprRdQ};
  assign _zz_gainH_1 = 9'h0c8;
  assign _zz_gainH = {{1{_zz_gainH_1[8]}}, _zz_gainH_1};
  assign _zz_gainH_3 = 9'h0c8;
  assign _zz_gainH_2 = {{1{_zz_gainH_3[8]}}, _zz_gainH_3};
  assign _zz_gainL_1 = 9'h0;
  assign _zz_gainL = {{1{_zz_gainL_1[8]}}, _zz_gainL_1};
  assign _zz_gainL_3 = 9'h0;
  assign _zz_gainL_2 = {{1{_zz_gainL_3[8]}}, _zz_gainL_3};
  assign _zz_GainVca = gainL[7 : 0];
  assign _zz_Spi_counter_valueNext_1 = Spi_counter_willIncrement;
  assign _zz_Spi_counter_valueNext = {5'd0, _zz_Spi_counter_valueNext_1};
  assign _zz__zz_MoSi = (5'h17 - _zz__zz_MoSi_1);
  assign _zz__zz_MoSi_1 = (Spi_counter_value >>> 1);
  mixed_width_ram dpr (
    .waddr (dpr_waddr[7:0]), //i
    .wdata (D[15:0]       ), //i
    .we    (DacDprCs      ), //i
    .clk   (Clk80MHz      ), //i
    .raddr (dpr_raddr[8:0]), //i
    .q     (dpr_q[7:0]    )  //o
  );
  `ifndef SYNTHESIS
  always @(*) begin
    case(gainVcaState_state)
      GainVcaStateEnum_IDLE : gainVcaState_state_string = "IDLE";
      GainVcaStateEnum_DATA : gainVcaState_state_string = "DATA";
      GainVcaStateEnum_RTZ : gainVcaState_state_string = "RTZ ";
      GainVcaStateEnum_STOP : gainVcaState_state_string = "STOP";
      default : gainVcaState_state_string = "????";
    endcase
  end
  `endif

  always @(*) begin
    DacClkPrd_tick = 1'b0;
    if(Run) begin
      if(when_Dac_l45) begin
        DacClkPrd_tick = 1'b1;
      end
    end
  end

  assign when_Dac_l45 = DacClkPrd_counter[8];
  always @(*) begin
    gainVcaState_CurveRstP = 1'b0;
    case(gainVcaState_state)
      GainVcaStateEnum_IDLE : begin
      end
      GainVcaStateEnum_DATA : begin
      end
      GainVcaStateEnum_RTZ : begin
        gainVcaState_CurveRstP = 1'b0;
        if(DacClkPrd_tick) begin
          gainVcaState_CurveRstP = 1'b1;
        end
      end
      default : begin
      end
    endcase
  end

  assign when_Dac_l62 = (Draw && Spi_counter_willOverflow);
  assign when_Dac_l70 = (! Draw);
  always @(*) begin
    if(AnBcurve) begin
      Dac23_16Val = 8'h31;
    end else begin
      Dac23_16Val = 8'h33;
    end
  end

  assign Dac15_8Val = GainVca;
  assign dpr_waddr = A[8 : 1];
  assign dpr_raddr = {AnBcurve,addrCnt[7 : 0]};
  assign dprRdQ = dpr_q;
  assign gain = _zz_gain;
  assign gainH = (($signed(_zz_gainH) < $signed(gain)) ? _zz_gainH_2 : gain);
  assign gainL = (($signed(gainH) < $signed(_zz_gainL)) ? _zz_gainL_2 : gainH);
  assign vca = (dprRdQ + OffsetA_Val);
  assign GainVca = (AnBcurve ? vca : _zz_GainVca);
  always @(*) begin
    clockDivider_counter_willIncrement = 1'b0;
    if(clockDivider_enable) begin
      clockDivider_counter_willIncrement = 1'b1;
    end
  end

  always @(*) begin
    clockDivider_counter_willClear = 1'b0;
    if(!clockDivider_enable) begin
      clockDivider_counter_willClear = 1'b1;
    end
  end

  assign clockDivider_counter_willOverflowIfInc = (clockDivider_counter_value == 1'b1);
  assign clockDivider_counter_willOverflow = (clockDivider_counter_willOverflowIfInc && clockDivider_counter_willIncrement);
  always @(*) begin
    clockDivider_counter_valueNext = (clockDivider_counter_value + clockDivider_counter_willIncrement);
    if(clockDivider_counter_willClear) begin
      clockDivider_counter_valueNext = 1'b0;
    end
  end

  always @(*) begin
    Spi_counter_willIncrement = 1'b0;
    if(clockDivider_counter_willOverflow) begin
      Spi_counter_willIncrement = 1'b1;
    end
  end

  always @(*) begin
    Spi_counter_willClear = 1'b0;
    if(clockDivider_counter_willOverflow) begin
      if(Spi_counter_willOverflow) begin
        Spi_counter_willClear = 1'b1;
      end
    end
  end

  assign Spi_counter_willOverflowIfInc = (Spi_counter_value == 6'h2f);
  assign Spi_counter_willOverflow = (Spi_counter_willOverflowIfInc && Spi_counter_willIncrement);
  always @(*) begin
    if(Spi_counter_willOverflow) begin
      Spi_counter_valueNext = 6'h0;
    end else begin
      Spi_counter_valueNext = (Spi_counter_value + _zz_Spi_counter_valueNext);
    end
    if(Spi_counter_willClear) begin
      Spi_counter_valueNext = 6'h0;
    end
  end

  assign Sclk = _zz_Sclk;
  assign MoSi = _zz_MoSi;
  assign nSS = Spi_ss;
  always @(posedge Clk80MHz or posedge Reset) begin
    if(Reset) begin
      DacClkPrd_counter <= 9'h0;
      gainVcaState_state <= GainVcaStateEnum_IDLE;
      clockDivider_counter_value <= 1'b0;
      Spi_counter_value <= 6'h0;
    end else begin
      if(Run) begin
        DacClkPrd_counter <= (DacClkPrd_counter - 9'h001);
        if(when_Dac_l45) begin
          DacClkPrd_counter <= {1'd0, DacClkPrdVal};
        end
      end else begin
        DacClkPrd_counter <= 9'h0;
      end
      case(gainVcaState_state)
        GainVcaStateEnum_IDLE : begin
          if(when_Dac_l62) begin
            gainVcaState_state <= GainVcaStateEnum_DATA;
          end
        end
        GainVcaStateEnum_DATA : begin
          if(DacClkPrd_tick) begin
            if(when_Dac_l70) begin
              gainVcaState_state <= GainVcaStateEnum_RTZ;
            end
          end
        end
        GainVcaStateEnum_RTZ : begin
          if(DacClkPrd_tick) begin
            gainVcaState_state <= GainVcaStateEnum_STOP;
          end
        end
        default : begin
          gainVcaState_state <= GainVcaStateEnum_IDLE;
        end
      endcase
      clockDivider_counter_value <= clockDivider_counter_valueNext;
      Spi_counter_value <= Spi_counter_valueNext;
    end
  end

  always @(posedge Clk80MHz) begin
    case(gainVcaState_state)
      GainVcaStateEnum_IDLE : begin
        if(when_Dac_l62) begin
          Run <= 1'b1;
        end
      end
      GainVcaStateEnum_DATA : begin
        if(DacClkPrd_tick) begin
          if(when_Dac_l70) begin
            addrCnt <= 8'h0;
          end else begin
            addrCnt <= (addrCnt + 8'h01);
          end
        end
      end
      GainVcaStateEnum_RTZ : begin
        if(DacClkPrd_tick) begin
          Run <= 1'b0;
        end
      end
      default : begin
      end
    endcase
    if(DacClkPrdCs) begin
      DacClkPrdVal <= D[7 : 0];
    end
    if(OffsetA_Cs) begin
      OffsetA_Val <= D[7 : 0];
    end
    Draw_regNext <= Draw;
    _zz_Spi_st <= (DacClkPrd_tick || (Draw && (! Draw_regNext)));
    Spi_st <= _zz_Spi_st;
    if(Spi_st) begin
      Spi_sftReg <= {{Dac23_16Val,Dac15_8Val},8'h55};
      clockDivider_enable <= 1'b1;
      Spi_ss <= 1'b0;
    end
    if(clockDivider_counter_willOverflow) begin
      if(Spi_counter_willOverflow) begin
        clockDivider_enable <= 1'b0;
        Spi_ss <= 1'b1;
      end
    end
    _zz_Sclk <= Spi_counter_value[0];
    _zz_MoSi <= Spi_sftReg[_zz__zz_MoSi];
  end


endmodule

module CsCls (
  input      [11:0]   Bus16A,
  input               Bus16Wr,
  input      [11:0]   Bus32A,
  input               Bus32Wr,
  input               Bus32Rd,
  output              FifoWrCs,
  output              DebugValCs,
  output              DacClkPrdCs,
  output              DacOffsetA_Cs,
  output              ToggleRegCs,
  output              MonoShotRegCs,
  output              DacDprCs,
  output              FifoRdCs,
  output              SysCtrlCs,
  output              GpioCs,
  output              GpioRCs,
  output              GpioSetCs,
  output              GpioClrCs
);


  assign FifoWrCs = (Bus16Wr && (Bus16A == 12'h0));
  assign DebugValCs = (Bus16Wr && (Bus16A == 12'h002));
  assign DacClkPrdCs = (Bus16Wr && (Bus16A == 12'h010));
  assign DacOffsetA_Cs = (Bus16Wr && (Bus16A == 12'h012));
  assign ToggleRegCs = (Bus16Wr && (Bus16A == 12'h060));
  assign MonoShotRegCs = (Bus16Wr && (Bus16A == 12'h062));
  assign DacDprCs = (Bus16Wr && (Bus16A[11 : 9] == 3'b001));
  assign FifoRdCs = (Bus32Rd && (Bus32A == 12'h040));
  assign SysCtrlCs = (Bus32Wr && (Bus32A == 12'h044));
  assign GpioCs = (Bus32Wr && (Bus32A == 12'h04c));
  assign GpioRCs = (Bus32Rd && (Bus32A == 12'h04c));
  assign GpioSetCs = (Bus32Wr && (Bus32A == 12'h050));
  assign GpioClrCs = (Bus32Wr && (Bus32A == 12'h054));

endmodule
