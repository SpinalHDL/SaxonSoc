#include <stdint.h>

#include "saxon.h"
#include "i2c.h"
#include "i2cMcp4725.h" //BSP


/*
 * This demo use the I2C peripheral to communicate with a MCP4725 (DAC)
 * It assume it is the single master on the bus, and send frame in a blocking manner.
 */

void init();
void main();
void trap();
void crash();
void trap_entry();
void externalInterrupt();
void externalInterrupt_i2cA();


void init(){
    //I2C init
    I2c_Config i2c;
    i2c.samplingClockDivider = 3;
    i2c.timeout = CORE_HZ/1000;    //1 ms;
    i2c.tsuDat  = CORE_HZ/2000000; //500 ns

    i2c.tLow  = CORE_HZ/800000;  //1.25 us
    i2c.tHigh = CORE_HZ/800000; //1.25 us
    i2c.tBuf  = CORE_HZ/400000;  //2.5 us

    i2c_applyConfig(I2C_A, &i2c);
}

void print_hex_digit(uint8_t digit){
    uart_write(UART_A, digit < 10 ? '0' + digit : 'A' + digit - 10);
}


void print_hex_byte(uint8_t byte){
    print_hex_digit(byte >> 4);
    print_hex_digit(byte & 0x0F);
}

void assert(int cond){
    if(!cond) {
        uart_writeStr(UART_A, "Assert failure\n");
        while(1);
    }
}

uint32_t phase = 0;
void main() {
    init();

    uart_writeStr(UART_A, "Start\n");


    while(1){
        uint32_t ready;
        uint32_t dacValue = 0;

        //Read the status of the DAC
        i2c_masterStartBlocking(I2C_A);
        i2c_txByte(I2C_A, 0xC1); i2c_txNackBlocking(I2C_A);
        i2c_txByte(I2C_A, 0xFF); i2c_txAckBlocking(I2C_A);
        ready = (i2c_rxData(I2C_A) & 0x80) != 0;
        i2c_txByte(I2C_A, 0xFF); i2c_txAckBlocking(I2C_A);
        dacValue |= i2c_rxData(I2C_A) << 4;
        i2c_txByte(I2C_A, 0xFF); i2c_txNackBlocking(I2C_A);
        dacValue |= i2c_rxData(I2C_A) >> 4;
        i2c_masterStopBlocking(I2C_A);

        //If not busy, write a new DAC value
        if(ready){
            dacValue += 1;
            dacValue &= 0xFFF;
            i2c_masterStartBlocking(I2C_A);
            i2c_txByte(I2C_A, 0xC0); i2c_txNackBlocking(I2C_A);
            i2c_txByte(I2C_A, 0x00 | ((dacValue >> 8) & 0x0F)); i2c_txNackBlocking(I2C_A);
            i2c_txByte(I2C_A, 0x00 | ((dacValue >> 0) & 0xFF)); i2c_txNackBlocking(I2C_A);
            i2c_masterStopBlocking(I2C_A);
            for(uint32_t i = 0;i < 1000;i++)  asm("nop");
        }
    }
}




