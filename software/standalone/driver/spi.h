#ifndef SPI_H_
#define SPI_H_

#include <stdint.h>

typedef struct
{
  volatile uint32_t DATA;
  volatile uint32_t BUFFER;
  volatile uint32_t CONFIG;
  volatile uint32_t INTERRUPT;

  volatile uint32_t _a[4];

  volatile uint32_t CLK_DIVIDER;
  volatile uint32_t SS_SETUP;
  volatile uint32_t SS_HOLD;
  volatile uint32_t SS_DISABLE;
} Spi_Reg;




typedef struct {
	int cpol;
	int cpha;
	int mode;
	uint32_t clkDivider;
	uint32_t ssSetup;
	uint32_t ssHold;
	uint32_t ssDisable;
} Spi_Config;

#define SPI_CMD_WRITE (1 << 8)
#define SPI_CMD_READ (1 << 9)
#define SPI_CMD_SS (1 << 11)

#define SPI_RSP_VALID (1 << 31)

#define SPI_STATUS_CMD_INT_ENABLE = (1 << 0)
#define SPI_STATUS_RSP_INT_ENABLE = (1 << 1)
#define SPI_STATUS_CMD_INT_FLAG = (1 << 8)
#define SPI_STATUS_RSP_INT_FLAG = (1 << 9)


#define SPI_MODE_CPOL (1 << 0)
#define SPI_MODE_CPHA (1 << 1)


static uint32_t spi_cmdAvailability(Spi_Reg *reg){
	return reg->BUFFER & 0xFFFF;
}
static uint32_t spi_rspOccupancy(Spi_Reg *reg){
	return reg->BUFFER >> 16;
}

static void spi_write(Spi_Reg *reg, uint8_t data){
	while(spi_cmdAvailability(reg) == 0);
	reg->DATA = data | SPI_CMD_WRITE;
}

static uint8_t spi_read(Spi_Reg *reg){
	while(spi_cmdAvailability(reg) == 0);
	reg->DATA = SPI_CMD_READ;
	while(spi_rspOccupancy(reg) == 0);
	return reg->DATA;
}

static void spi_select(Spi_Reg *reg, uint32_t slaveId){
	while(spi_cmdAvailability(reg) == 0);
	reg->DATA = slaveId | 0x80 | SPI_CMD_SS;
}

static void spi_diselect(Spi_Reg *reg, uint32_t slaveId){
	while(spi_cmdAvailability(reg) == 0);
	reg->DATA = slaveId | 0x00 | SPI_CMD_SS;
}

static void spi_applyConfig(Spi_Reg *reg, Spi_Config *config){
	reg->CONFIG = (config->cpol << 0) | (config->cpha << 1) | (config->mode << 4);
	reg->CLK_DIVIDER = config->clkDivider;
	reg->SS_SETUP = config->ssSetup;
	reg->SS_HOLD = config->ssHold;
	reg->SS_DISABLE = config->ssDisable;
}

#endif /* SPI_H_ */


