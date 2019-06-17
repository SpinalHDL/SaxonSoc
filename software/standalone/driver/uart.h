#ifndef UART_H_
#define UART_H_


typedef struct
{
  volatile uint32_t DATA;
  volatile uint32_t STATUS;
  volatile uint32_t CLOCK_DIVIDER;
  volatile uint32_t FRAME_CONFIG;
} Uart_Reg;

enum UartDataLength {BITS_8 = 8};
enum UartParity {NONE = 0,EVEN = 1,ODD = 2};
enum UartStop {ONE = 0,TWO = 1};

typedef struct {
	enum UartDataLength dataLength;
	enum UartParity parity;
	enum UartStop stop;
	uint32_t clockDivider;
} Uart_Config;

static uint32_t uart_writeAvailability(Uart_Reg *reg){
	return (reg->STATUS >> 16) & 0xFF;
}
static uint32_t uart_readOccupancy(Uart_Reg *reg){
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


