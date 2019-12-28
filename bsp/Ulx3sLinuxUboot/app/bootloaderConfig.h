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
#define CTRL_BURST_LENGHT 1
#define PHY_CLK_RATIO 2

void bspMain() {
	putString("Starting bootloader\n");

	sdram_init(
		SYSTEM_SDRAM_A_APB,
		RL,
		WL,
		MT48LC16M16A2_6A_ps,
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
