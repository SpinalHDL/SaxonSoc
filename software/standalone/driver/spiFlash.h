#pragma once


#include "type.h"
#include "spi.h"
#include "gpio.h"
#include "io.h"


static void spiFlash_select(Gpio_Reg *gpio, u32 cs){
	gpio->OUTPUT &= ~(1 << cs);
	io_udelay(1);
}

static void spiFlash_deselect(Gpio_Reg *gpio, u32 cs){
	gpio->OUTPUT |= (1 << cs);
	io_udelay(1);
}

static void spiFlash_init(Spi_Reg * spi, Gpio_Reg *gpio, u32 cs){
    //SPI init
    Spi_Config spiCfg;
    spiCfg.cpol = 0;
    spiCfg.cpha = 0;
    spiCfg.mode = 0;
    spiCfg.clkDivider = 2;
    spiCfg.ssSetup = 2;
    spiCfg.ssHold = 2;
    spiCfg.ssDisable = 2;
    spi_applyConfig(spi, &spiCfg);

    gpio->OUTPUT_ENABLE |= (1 << cs);
    spiFlash_deselect(gpio,cs);
}

static void spiFlash_wake(Spi_Reg * spi, Gpio_Reg *gpio, u32 cs){
	spiFlash_select(gpio,cs);
	spi_write(spi, 0xAB);
	spi_write(spi, 0x00);
	spi_write(spi, 0x00);
	spi_write(spi, 0x00);
	spi_write(spi, 0x00);
	spiFlash_deselect(gpio,cs);
}


static void spiFlash_f2m(Spi_Reg * spi,  Gpio_Reg *gpio, u32 cs, u32 flashAddress, u32 memoryAddress, u32 size){
	spiFlash_select(gpio,cs);
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
	spiFlash_deselect(gpio,cs);
}
