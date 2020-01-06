#ifndef UART_H_
#define UART_H_

#include "type.h"

typedef struct
{
  volatile u32 DATA;
  volatile u32 STATUS;
  volatile u32 CLOCK_DIVIDER;
  volatile u32 FRAME_CONFIG;
} Uart_Reg;

enum UartDataLength {BITS_8 = 8};
enum UartParity {NONE = 0,EVEN = 1,ODD = 2};
enum UartStop {ONE = 0,TWO = 1};

typedef struct {
	enum UartDataLength dataLength;
	enum UartParity parity;
	enum UartStop stop;
	u32 clockDivider;
} Uart_Config;

static u32 uart_writeAvailability(Uart_Reg *reg){
	return (reg->STATUS >> 16) & 0xFF;
}
static u32 uart_readOccupancy(Uart_Reg *reg){
	return reg->STATUS >> 24;
}

static void uart_write(Uart_Reg *reg, char data){
	while(uart_writeAvailability(reg) == 0);
	reg->DATA = data;
}

static void uart_writeStr(Uart_Reg *reg, char* str){
	while(*str) uart_write(reg, *str++);
}

static char uart_read(Uart_Reg *reg){
	while(uart_readOccupancy(reg) == 0);
	return reg->DATA;
}

static void uart_applyConfig(Uart_Reg *reg, Uart_Config *config){
	reg->CLOCK_DIVIDER = config->clockDivider;
	reg->FRAME_CONFIG = ((config->dataLength-1) << 0) | (config->parity << 8) | (config->stop << 16);
}

#endif /* UART_H_ */


