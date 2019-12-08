#pragma once

#include "saxon.h"
#include "sdram.h"
#include "spiFlash.h"

#define CS_PIN 0

#define MACHINE_MODE_SBI_FLASH  0x00300000
#define MACHINE_MODE_SBI_MEMORY 0x80000000
#define MACHINE_MODE_SBI_SIZE      0x10000


#define UBOOT_SBI_FLASH  0x00310000
#define UBOOT_MEMORY     0x80010000
#define UBOOT_SIZE          0x40000


void bspMain() {
	sdram_init(
		SYSTEM_SDRAM_A_APB,
		SDRAM_DOMAIN_PHY_A_APB,
		5,
		5,
		8,
		MT41K128M16JT_125_ps,
		2,
		3300,
		0,
		1
	);


//	spiFlash_init(spiA, gpioA, CS_PIN);
//	spiFlash_wake(spiA, gpioA, CS_PIN);
//	spiFlash_f2m(spiA, gpioA, CS_PIN, MACHINE_MODE_SBI_FLASH, MACHINE_MODE_SBI_MEMORY, MACHINE_MODE_SBI_SIZE);
//	spiFlash_f2m(spiA, gpioA, CS_PIN, UBOOT_SBI_FLASH, UBOOT_MEMORY, UBOOT_SIZE);

	void (*userMain)() = MACHINE_MODE_SBI_MEMORY;
	userMain();
}


