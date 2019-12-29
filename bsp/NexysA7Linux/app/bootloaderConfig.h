#pragma once

#include "saxon.h"
#include "sdram.h"
#include "spiFlash.h"

#define SPI SYSTEM_SPI_A_APB
#define SPI_CS 0

#define GPIO SYSTEM_GPIO_A_APB

#define MACHINE_MODE_SBI_MEMORY 0x81FF0000
#define MACHINE_MODE_SBI_FLASH  0x00400000
#define MACHINE_MODE_SBI_SIZE      0x10000

#define UBOOT_MEMORY     0x81F00000
#define UBOOT_SBI_FLASH  0x00410000
#define UBOOT_SIZE          0x70000

#if 0
#include "uart.h"
#define UART_A ((Uart_Reg*)(0x10010000))
#define MACHINE_TIMER ((volatile uint32_t*)(0x10008000))
void putC(char c){
	uart_write(UART_A, c);
}
void putString(char* s){
	while(*s){
		putC(*s);
		s++;
	}
}
void putHex(int value){
	for(int i = 7; i >=	 0;i--){
		int hex = (value >> i*4) & 0xF;
		putC(hex > 9 ? 'A' + hex - 10 : '0' + hex);
	}
}
#else
#define putString(x)
#define putHex(x)
#endif

#define RL 5
#define CTRL_BURST_LENGHT 2
#define PHY_CLK_RATIO 2

void bspMain() {
	putString("sdram_init\n");

	sdram_init(
		SYSTEM_SDRAM_A_APB,
		RL,
		RL-1,
		MT47H64M16HR_25_ps,
		CTRL_BURST_LENGHT,
		PHY_CLK_RATIO,
		3300
	);

	sdram_ddr2_init(
		SYSTEM_SDRAM_A_APB,
		RL,
		MT47H64M16HR_25_ps,
		CTRL_BURST_LENGHT,
		PHY_CLK_RATIO,
		3300
	);

#ifndef SPINAL_SIM
	putString("sdram_phy_s7\n");
	sdram_phy_s7(
		SYSTEM_SDRAM_A_APB,
		SDRAM_DOMAIN_PHY_A_APB
	);

	putString("spiFlash_init\n");
	spiFlash_init(SPI, SPI_CS);
	spiFlash_wake(SPI, SPI_CS);
	spiFlash_f2m(SPI, SPI_CS, MACHINE_MODE_SBI_FLASH, MACHINE_MODE_SBI_MEMORY, MACHINE_MODE_SBI_SIZE);
	spiFlash_f2m(SPI, SPI_CS, UBOOT_SBI_FLASH, UBOOT_MEMORY, UBOOT_SIZE);

	putHex(read_u32(UBOOT_MEMORY+0)); putString(" ddr\n");
	putHex(read_u32(UBOOT_MEMORY+4)); putString(" ddr\n");
	putHex(read_u32(UBOOT_MEMORY+8)); putString(" ddr\n");
	putHex(read_u32(UBOOT_MEMORY+12)); putString(" ddr\n");
	spiFlash_f2m(SPI, SPI_CS, UBOOT_SBI_FLASH, 0x20001000, 16);
	putHex(read_u32(0x20001000)); putString(" ram\n");
	putHex(read_u32(0x20001004)); putString(" ram\n");
	putHex(read_u32(0x20001008)); putString(" ram\n");
	putHex(read_u32(0x2000100C)); putString(" ram\n");
#endif

	putString("userMain\n");
	putHex(MACHINE_MODE_SBI_MEMORY); putString("\n");
	void (*userMain)() = (void (*)())MACHINE_MODE_SBI_MEMORY;
	userMain();
}


