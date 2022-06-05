#include <stdint.h>

#include "bsp.h"
#include "spi.h"
#include "spiDemo.h"

void init(){
    //SPI init
    Spi_Config spiA;
    spiA.cpol = 1;
    spiA.cpha = 1;
    spiA.mode = 0; //Assume full duplex (standard SPI)
    spiA.clkDivider = 10;
    spiA.ssSetup = 5;
    spiA.ssHold = 5;
    spiA.ssDisable = 5;
    spi_applyConfig(SPI, &spiA);
}

void print_hex_digit(uint8_t digit){
    bsp_putChar(digit < 10 ? '0' + digit : 'A' + digit - 10);
}


void print_hex_byte(uint8_t byte){
    print_hex_digit(byte >> 4);
    print_hex_digit(byte & 0x0F);
}


void main() {
    init();

    bsp_putString("Hello world\n");

    spi_select(SPI, 0);
    spi_write(SPI, 0xAB);
    spi_write(SPI, 0x00);
    spi_write(SPI, 0x00);
    spi_write(SPI, 0x00);
    uint8_t id = spi_read(SPI);
    spi_diselect(SPI, 0);


    bsp_putString("Device ID : ");
    print_hex_byte(id);
    bsp_putString("\n");

    while(1){
        uint8_t data[3];
        spi_select(SPI, 0);
        spi_write(SPI, 0x9F);
        data[0] = spi_read(SPI);
        data[1] = spi_read(SPI);
        data[2] = spi_read(SPI);
        spi_diselect(SPI, 0);

        bsp_putString("CMD 0x9F : ");
        print_hex_byte(data[0]);
        print_hex_byte(data[1]);
        print_hex_byte(data[2]);
        bsp_putString("\n");
    }
}

