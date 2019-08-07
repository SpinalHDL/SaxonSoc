#ifndef sdcard_h
#define sdcard_h
#include <soc.h>

#define SPI_SDCARD SYSTEM_SPI_B_APB

#define SPI_SDCARD_XFER SPI_SDCARD
#define SPI_SDCARD_DATAMODE (SPI_SDCARD + 0x8)
#define SPI_SDCARD_DIVIDER (SPI_SDCARD + 0xc)
#define SPI_SDCARD_SETUP (SPI_SDCARD + 0x10)
#define SPI_SDCARD_HOLD (SPI_SDCARD + 0x14)
#define SPI_SDCARD_DISABLE (SPI_SDCARD + 0x18)

#define SPI_SDCARD_RX_VALID (1 << 31)

#endif
