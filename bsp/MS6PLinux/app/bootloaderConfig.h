#pragma once

#include "saxon.h"
#include "sdram.h"

#define MACHINE_MODE_SBI_MEMORY 0x80000000
#define SDRAM_TIMING MT48LC16M16A2_6A_ps

#if 1
void initUart(void){
	UART_A->CLOCK_DIVIDER = (SYSTEM_MACHINE_TIMER_HZ/SYSTEM_UART_A_PARAMETER_INIT_CONFIG_BAUDRATE)*2;
	UART_A->FRAME_CONFIG = 7;
}
void putString(char* s){
		while(*s){
				UART_A->DATA = *s;
				s++;
		}
}
void putHex(int value){
		for(int i = 7; i >=	  0;i--){
				int hex = (value >> i*4) & 0xF;
				UART_A->DATA = (hex > 9 ? 'A' + hex - 10 : '0' + hex);
		}
}
#else
#define initUart(x)
#define putString(x)
#define putHex(x)
#endif

void bspMain() {
	initUart();
	putString("Starting bootloader\n");
/*
#define RL 3
#define WL 0
#define CTRL_BURST_LENGHT 1
#define PHY_CLK_RATIO 2
#define SYSTEM_SDRAM_A_APB 0x10100000
	sdram_init(
		SYSTEM_SDRAM_A_APB,
		RL,
		WL,
		SDRAM_TIMING,
		CTRL_BURST_LENGHT,
		PHY_CLK_RATIO,
		20000
	);
	sdram_sdr_init(
		SYSTEM_SDRAM_A_APB,
		RL,
		CTRL_BURST_LENGHT,
		PHY_CLK_RATIO
	);
*/
	putString("Calling userMain\n");

	void (*userMain)() = (void (*)())MACHINE_MODE_SBI_MEMORY;
	userMain();
}
