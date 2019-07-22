#ifndef flash_h
#define flash_h
#include <soc.h>

#define SPI_FLASH SYSTEM_SPI_A_APB

#define SPI_FLASH_XFER SPI_FLASH
#define SPI_FLASH_DATAMODE (SPI_FLASH + 0x8)
#define SPI_FLASH_DIVIDER (SPI_FLASH + 0xc)
#define SPI_FLASH_SETUP (SPI_FLASH + 0x10)
#define SPI_FLASH_HOLD (SPI_FLASH + 0x14)
#define SPI_FLASH_DISABLE (SPI_FLASH + 0x18)

#define SPI_FLASH_RX_VALID (1 << 31)

#endif
