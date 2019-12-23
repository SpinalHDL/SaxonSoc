#pragma once

#include "saxon.h"
#include "io.h"
#include "sdram.h"
#include "spiFlash.h"

#define SPI SYSTEM_SPI_C_APB
#define GPIO SYSTEM_GPIO_A_APB
#define GPIO_CS_PIN 14

#define MACHINE_MODE_SBI_MEMORY 0x80800000
#define MACHINE_MODE_SBI_FLASH  0x00300000
#define MACHINE_MODE_SBI_SIZE	  0x10000

#define UBOOT_MEMORY	 0x81F00000
#define UBOOT_SBI_FLASH  0x00310000
#define UBOOT_SIZE		  0x70000

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


#define RL 3
#define WL 0
#define BL 1

void bspMain() {
	putString("Starting bootloader\n");

	sdram_init(
		SYSTEM_SDRAM_A_APB,
		RL,
		WL,
		BL,
		MT48LC16M16A2_6A_ps,
		1,
		20000
	);

	sdram_sdr_init(
		SYSTEM_SDRAM_A_APB,
		RL
	);

//	write_u32(0x11223344,0x80000000);
//	write_u16(    0x5566,0x80000006);
//	write_u16(    0x7788,0x80000004);
//	write_u8(       0xDD,0x80000008);
//	write_u8(       0xCC,0x80000009);
//	write_u8(       0xBB,0x8000000A);
//	write_u8(       0xAA,0x8000000B);
//	asm(".word(0x500F)"); //Flush data cache
//	u32 a = read_u32(0x80000000);
//	u32 b = read_u32(0x80000004);
//	u32 c = read_u32(0x80000008);

#ifndef SPINAL_SIM
	spiFlash_init_withGpioCs(SPI, GPIO, GPIO_CS_PIN);
	spiFlash_wake_withGpioCs(SPI, GPIO, GPIO_CS_PIN);
	spiFlash_f2m_withGpioCs(SPI, GPIO, GPIO_CS_PIN, MACHINE_MODE_SBI_FLASH, MACHINE_MODE_SBI_MEMORY, MACHINE_MODE_SBI_SIZE);
	spiFlash_f2m_withGpioCs(SPI, GPIO, GPIO_CS_PIN, UBOOT_SBI_FLASH, UBOOT_MEMORY, UBOOT_SIZE);
#endif

	putString("Calling userMain\n");

	void (*userMain)() = (void (*)())MACHINE_MODE_SBI_MEMORY;
	userMain();
}
