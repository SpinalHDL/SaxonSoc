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



void bspMain() {
	sdram_init(
		SYSTEM_SDRAM_A_APB,
		SDRAM_DOMAIN_PHY_A_APB,
		5,
		5,
		8,
		MT41K128M16JT_125_ps,
		2,
		3300
	);

#ifndef SPINAL_SIM
	sdram_phy_s7(
		SYSTEM_SDRAM_A_APB,
		SDRAM_DOMAIN_PHY_A_APB
	);

	spiFlash_init(SPI, SPI_CS);
	spiFlash_wake(SPI, SPI_CS);
	spiFlash_f2m(SPI, SPI_CS, MACHINE_MODE_SBI_FLASH, MACHINE_MODE_SBI_MEMORY, MACHINE_MODE_SBI_SIZE);
	spiFlash_f2m(SPI, SPI_CS, UBOOT_SBI_FLASH, UBOOT_MEMORY, UBOOT_SIZE);
#endif

	void (*userMain)() = (void (*)())MACHINE_MODE_SBI_MEMORY;
	userMain();
}


