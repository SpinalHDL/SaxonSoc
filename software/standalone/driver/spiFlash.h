#pragma once


#include "type.h"
#include "spi.h"
#include "gpio.h"
#include "io.h"


static void spiFlash_select_withGpioCs(Gpio_Reg *gpio, u32 cs){
	gpio->OUTPUT &= ~(1 << cs);
	io_udelay(1);
}

static void spiFlash_diselect_withGpioCs(Gpio_Reg *gpio, u32 cs){
	gpio->OUTPUT |= (1 << cs);
	io_udelay(1);
}

static void spiFlash_select(Spi_Reg *spi, u32 cs){
	spi_select(spi,cs);
}

static void spiFlash_diselect(Spi_Reg *spi, u32 cs){
	spi_diselect(spi,cs);
}

static void spiFlash_init_(Spi_Reg * spi){
    Spi_Config spiCfg;
    spiCfg.cpol = 0;
    spiCfg.cpha = 0;
    spiCfg.mode = 0;
    spiCfg.clkDivider = 2;
    spiCfg.ssSetup = 2;
    spiCfg.ssHold = 2;
    spiCfg.ssDisable = 2;
    spi_applyConfig(spi, &spiCfg);
}

static void spiFlash_init_withGpioCs(Spi_Reg * spi, Gpio_Reg *gpio, u32 cs){
    spiFlash_init_(spi);
    gpio->OUTPUT_ENABLE |= (1 << cs);
    spiFlash_diselect_withGpioCs(gpio,cs);
}

static void spiFlash_init(Spi_Reg * spi, u32 cs){
    spiFlash_init_(spi);
    spiFlash_diselect(spi, cs);
}


static void spiFlash_wake_(Spi_Reg * spi){
	spi_write(spi, 0xAB);
	spi_write(spi, 0x00);
	spi_write(spi, 0x00);
	spi_write(spi, 0x00);
	spi_write(spi, 0x00);
}

static void spiFlash_wake_withGpioCs(Spi_Reg * spi, Gpio_Reg *gpio, u32 cs){
	spiFlash_select_withGpioCs(gpio,cs);
	spiFlash_wake_(spi);
	spiFlash_diselect_withGpioCs(gpio,cs);
}

static void spiFlash_wake(Spi_Reg * spi, u32 cs){
	spiFlash_select(spi,cs);
	spiFlash_wake_(spi);
	spiFlash_diselect(spi,cs);
}

static void spiFlash_f2m_(Spi_Reg * spi, u32 flashAddress, u32 memoryAddress, u32 size){
	spi_write(spi, 0x0B);
	spi_write(spi, flashAddress >> 16);
	spi_write(spi, flashAddress >>  8);
	spi_write(spi, flashAddress >>  0);
	spi_write(spi, 0);
	uint8_t *ram = (uint8_t *) memoryAddress;
	for(u32 idx = 0;idx < size;idx++){
		u8 value = spi_read(spi);
		*ram++ = value;
	}
}


static void spiFlash_f2m_withGpioCs(Spi_Reg * spi,  Gpio_Reg *gpio, u32 cs, u32 flashAddress, u32 memoryAddress, u32 size){
	spiFlash_select_withGpioCs(gpio,cs);
	spiFlash_f2m_(spi, flashAddress, memoryAddress, size);
	spiFlash_diselect_withGpioCs(gpio,cs);
}


static void spiFlash_f2m(Spi_Reg * spi, u32 cs, u32 flashAddress, u32 memoryAddress, u32 size){
	spiFlash_select(spi,cs);
	spiFlash_f2m_(spi, flashAddress, memoryAddress, size);
	spiFlash_diselect(spi,cs);
}
