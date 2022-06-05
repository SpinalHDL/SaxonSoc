`timescale 1ns / 1ps
////////////////////////////////////////////////////////////////////////
// Company: 
// Engineer: roman3017
// 
// Create Date:    2019
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
module pll_0002 ( 
	input refclk,
	input rst,
	output outclk_0,
	output outclk_1,
	output locked
);

	BUFG BufClock(
	.O(outclk_0),
	.I(refclk));

	ODDR2 ExportClock(
	.D0(1'b1),
	.D1(1'b0),
	.C0(outclk_0),
	.C1(!outclk_0),
	.Q(outclk_1),
	.S(1'b0),
	.R(1'b0),
	.CE(1'b1));

endmodule

module top
(
	input CLK50,

	input  UART0_RXD,
	output UART0_TXD,

	input  TCK,
	input  TDI,
	output TDO,
	input  TMS,

	output [7:0] LEDS,

	output [12:0] SDRAM_ADDR,
	inout  [15:0] SDRAM_DATA,
	output [1:0]  SDRAM_BA,
	output [1:0]  SDRAM_DQM,
	output SDRAM_CLK,
	output SDRAM_CKE,
	output SDRAM_CSn,
	output SDRAM_RASn,
	output SDRAM_CASn,
	output SDRAM_WEn,

	output SD_CLK,
	output SD_DAT3,
	inout SD_DAT0,
	inout SD_CMD,

	output FLASH_CS,
	output FLASH_CCLK,
	inout FLASH_MOSI,
	inout FLASH_MISO
);

	MS6PLinux SoC(
	.resetN(1'b1),
	.CLOCK_50(CLK50),
	.sdramClk(SDRAM_CLK),
	.system_cpu_tms(TMS),
	.system_cpu_tdi(TDI),
	.system_cpu_tdo(TDO),
	.system_cpu_tck(TCK),
	.system_sdramA_sdram_ADDR(SDRAM_ADDR),
	.system_sdramA_sdram_BA(SDRAM_BA),
	.system_sdramA_sdram_DQ(SDRAM_DATA),
	.system_sdramA_sdram_DQM(SDRAM_DQM),
	.system_sdramA_sdram_CASn(SDRAM_CASn),
	.system_sdramA_sdram_CKE(SDRAM_CKE),
	.system_sdramA_sdram_CSn(SDRAM_CSn),
	.system_sdramA_sdram_RASn(SDRAM_RASn),
	.system_sdramA_sdram_WEn(SDRAM_WEn),
	.system_uartA_uart_txd(UART0_TXD),
	.system_uartA_uart_rxd(UART0_RXD),
	.system_gpioA_gpio(LEDS),
	.system_spiA_spi_ss({SD_DAT3}),
	.system_spiA_spi_sclk(SD_CLK),
	.system_spiA_spi_data({SD_DAT0, SD_CMD}),
	.system_spiB_spi_ss(FLASH_CS),
	.system_spiB_spi_sclk(FLASH_CCLK),
	.system_spiB_spi_data({FLASH_MISO, FLASH_MOSI})
	);

endmodule
