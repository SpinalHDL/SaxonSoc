#include <stdint.h>

#include "saxon.h"
#include "soc.h"
#include "flash.h"
#include "io.h"

void print_hex(uint32_t val, uint32_t digits)
{
	for (int i = (4*digits)-4; i >= 0; i -= 4)
		uart_write(UART_A, "0123456789ABCDEF"[(val >> i) % 16]);
}

void print(uint8_t * data) {
  uart_writeStr(UART_A, data);
}

uint32_t readUInt() {
  uint32_t r = 0;

  for(int i=0;i<4;i++) {
    while(!(UART_A->STATUS >> 24));
    uint8_t c = UART_A->DATA;
    r <<= 8;
    r |= c;
  }

  return r;
}

uint8_t readChar() {
    while(!(UART_A->STATUS >> 24));
    return UART_A->DATA;
}

void flash_init () {
  write_u32(500, SPI_FLASH_SETUP);
  write_u32(500, SPI_FLASH_HOLD);
  write_u32(500, SPI_FLASH_DISABLE);
}

void flash_divider(uint32_t divider) {
  write_u32(divider, SPI_FLASH_DIVIDER);
}

void flash_mode(uint32_t mode) {
  write_u32(mode, SPI_FLASH_DATAMODE);
}

void flash_begin() {
  write_u32(0x11000000, SPI_FLASH_XFER);
}

void flash_end() {
 write_u32(0x10000000, SPI_FLASH_XFER);
}

uint8_t flash_xfer(uint8_t data) {
  write_u32(data | 0x01000000, SPI_FLASH_XFER);

  for(int i=0;i<1000;i++) {
    uint32_t r = read_u32(SPI_FLASH_XFER);
    if ((r & SPI_FLASH_RX_VALID)) return r & 0xff;
  }
  return 0xFF;
}

void flash_read(uint32_t addr, uint8_t *data, int n) {
        flash_begin();
        flash_xfer(0x03);
        flash_xfer(addr >> 16);
        flash_xfer(addr >> 8);
        flash_xfer(addr);
        while (n--)
                *(data++) = flash_xfer(0);
        flash_end();
}

void flash_wake() {
  flash_begin();
  flash_xfer(0xab);
  flash_end();
}

uint8_t data[4];

void main() {
    GPIO_A->OUTPUT_ENABLE = 0x000000FF;
    GPIO_A->OUTPUT = 0x00000000;

    uint8_t cmd = readChar();
    uint32_t addr = readUInt();
    uint32_t len = readUInt();

    flash_init();
    flash_divider(1);
    flash_mode(0);
    flash_wake();
    
    if (cmd == 'd') { // Hex dump of flash
      print("\nFlash memory at 0x");
      print_hex(addr, 6);
      print("\n\n");

      for (int i=0; i<((len + 3)/4);i++) {
        flash_read(addr + (i*4) , data, 4);

        if ((i % 8) == 0) {
          print_hex(i << 2, 6);
          print(" ");
        }

        print_hex(*((uint32_t *) data), 8);

        if ((i % 8) == 7) {
          print("\n");
        } else {
          print(" ");
        }
      }
    } else if (cmd == 'r') { // Copy flash to uart
      for(int i=0;i<len;i++) {
        flash_read(addr + i , data, 1);
        uart_write(UART_A, data[0]);
      }
    }

    uint32_t counter = 0;
    while(1){
        if(counter++ == 10000){
            GPIO_A->OUTPUT = GPIO_A->OUTPUT + 1;
            counter = 0;
        }
    }
}

