`timescale 1ns / 1ps
////////////////////////////////////////////////////////////////////////
// Company: 
// Engineer: roman3017
// 
// Create Date:    2020
// Design Name: 
// Module Name:    top 
// Project Name: 
// Target Devices: 
// Tool versions: 
// Description: 
//
// Dependencies: 
//
// Revision: 
// Revision 0.01 - File Created
// Additional Comments: 
//
////////////////////////////////////////////////////////////////////////
module top (
  input wire CLK_24,
  input wire RST_N,
  input wire UART1_RX,
  output wire UART1_TX,
  input wire TMS,
  input wire TCK,
  input wire TDI,
  output wire TDO,
  output wire SD_CLK,
  output wire SD_D3,
  inout wire SD_D0,
  inout wire SD_CMD,
  output wire NOR_CLK,
  output wire NOR_CS,
  inout wire[3:0] NOR_IO,
  output wire[2:0] RGB_LED
);

wire user_clk;
wire[1:0] user_io;

wire sdram_clk;
wire sdram_ras_n;
wire sdram_cas_n;
wire sdram_we_n;
wire [10:0] sdram_addr;
wire [1:0] sdram_ba;
wire [31:0] sdram_dq;
wire sdram_cs_n;
wire [3:0] sdram_dm;
wire sdram_cke;

EG_PHY_SDRAM_2M_32 U_EG_PHY_SDRAM_2M_32(
  .clk(sdram_clk),
  .ras_n(sdram_ras_n),
  .cas_n(sdram_cas_n),
  .we_n(sdram_we_n),
  .addr(sdram_addr),
  .ba(sdram_ba),
  .dq(sdram_dq),
  .cs_n(sdram_cs_n),
  .dm0(sdram_dm[0]),
  .dm1(sdram_dm[1]),
  .dm2(sdram_dm[2]),
  .dm3(sdram_dm[3]),
  .cke(sdram_cke)
);

TangLinux SoC(
  .clocking_CLOCK_24(CLK_24),
  .clocking_resetN(RST_N),

  .system_uartA_uart_txd(UART1_TX),
  .system_uartA_uart_rxd(UART1_RX),

  .system_cpu_jtag_tms(TMS),
  .system_cpu_jtag_tdi(TDI),
  .system_cpu_jtag_tdo(TDO),
  .system_cpu_jtag_tck(TCK),

  .clocking_sdramClk(sdram_clk),
  .system_sdramA_sdram_ADDR(sdram_addr),
  .system_sdramA_sdram_BA(sdram_ba),
  .system_sdramA_sdram_DQ(sdram_dq),
  .system_sdramA_sdram_DQM(sdram_dm),
  .system_sdramA_sdram_CASn(sdram_cas_n),
  .system_sdramA_sdram_CKE(sdram_cke),
  .system_sdramA_sdram_CSn(sdram_cs_n),
  .system_sdramA_sdram_RASn(sdram_ras_n),
  .system_sdramA_sdram_WEn(sdram_we_n),

  .system_spiA_user_sclk(user_clk),
  .system_spiA_user_data(user_io),

  .system_spiA_sdcard_ss({SD_D3}),
  .system_spiA_sdcard_sclk(SD_CLK),
  .system_spiA_sdcard_data({SD_D0, SD_CMD}),

  .system_spiA_flash_ss({NOR_CS}),
  .system_spiA_flash_sclk(NOR_SCK),
  .system_spiA_flash_data(NOR_IO),

  .system_gpioA_gpio(RGB_LED)
);

endmodule
