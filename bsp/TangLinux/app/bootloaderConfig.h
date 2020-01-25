#pragma once

#include "saxon.h"
#include "sdram.h"

#define MACHINE_MODE_SBI_MEMORY 0x80000000

#if 1
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
#define putString(x)
#define putHex(x)
#endif

void bspMain() {
	putString("Starting bootloader\n");
	putHex(MACHINE_MODE_SBI_MEMORY);
	putString("\nCalling userMain\n");
	void (*userMain)() = (void (*)())MACHINE_MODE_SBI_MEMORY;
	userMain();
}
