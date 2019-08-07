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

void flash_write_enable() {
  flash_begin();
  flash_xfer(0x06);
  flash_end();
}

void flash_bulk_erase() {
  flash_begin();
  flash_xfer(0xc7);
  flash_end();
}

void flash_erase_64kB(uint32_t addr) {
  flash_begin();
  flash_xfer(0xd8);
  flash_xfer(addr >> 16);
  flash_xfer(addr >> 8);
  flash_xfer(addr);
  flash_end();
}

void flash_erase_32kB(uint32_t addr) {
  flash_begin();
  flash_xfer(0x52);
  flash_xfer(addr >> 16);
  flash_xfer(addr >> 8);
  flash_xfer(addr);
  flash_end();
}

void flash_write(uint32_t addr, uint8_t *data, int n) {
  flash_begin();
  flash_xfer(0x02);
  flash_xfer(addr >> 16);
  flash_xfer(addr >> 8);
  flash_xfer(addr);
  while (n--)
    flash_xfer(*(data++));
  flash_end();
}

void flash_wait() {
  while (1) {
    flash_begin();
    flash_xfer(0x05);
    int status = flash_xfer(0);
    flash_end();

    if ((status & 0x01) == 0) break;
  }
}

void flash_wake() {
  flash_begin();
  flash_xfer(0xab);
  flash_end();
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

void print(uint8_t * data) {
  uart_writeStr(UART_A, data);
}

//uint8_t data[1024];
#define data ((uint8_t *) 0x90000000)

void main() {
    GPIO_A->OUTPUT_ENABLE = 0x000000FF;
    GPIO_A->OUTPUT = 0x00000000;

    uint32_t addr = readUInt();
    uint32_t len = readUInt();
    
    print("\nProgram Flash memory\n\n");

    print("Address: ");
    print_hex(addr,8);

    print("\nLength:");
    print_hex(len, 8);
    print("\n");
   
    for(int i=0;i<len;i++ ) {
      while(!(UART_A->STATUS >> 24)) {}
      data[i] = UART_A->DATA;
    }

    print("\nData:\n\n");

    for (int i=0; i<(len/4);i++) {
      if ((i % 8) == 0) {
        print_hex(i << 2, 6);
        print(" ");
      } 

      print_hex(((uint32_t *) data)[i], 8);

      if ((i % 8) == 7) {
        print("\n");  
      } else {
        print(" ");
      }
    }

    flash_init();
    flash_divider(1);
    flash_mode(0);
    flash_wake();
    flash_wait();

    // read flash id
    flash_begin();
    flash_xfer(0x9f);

    print("\n\nFlash id: ");

    for (int i = 0; i < 3; i++) {
        uint8_t c = flash_xfer(0x00);
        if (c == 0xFF) {
          print("Cannot access flash\n");
          while(1);
        }
        print_hex(c, 2);
    }
	
    print("\n");

    flash_end();
   
    print("\nWriting flash memory at 0x");
    print_hex(addr, 8);
    print("\n");
    
    uint32_t rem = len;
    uint32_t written = 0;

    for(int i=0; i < ((len + 255) / 256); i++) {
      if ((written & 0x7fff) == 0) {
        print("\nErasing 32kb at ");
        print_hex(addr + (i * 256), 8);
        print("\n\n");
 
        flash_write_enable();
        flash_erase_32kB(addr + (i * 256));
        flash_wait();
      }

      print("Writing flash at ");
      print_hex(addr + (i*256), 8);
      print("\n");;
      flash_write_enable();
      flash_write(addr + (i*256), data + (i*256), rem < 256 ? rem : 256);
      flash_wait();
      rem -= 256;
      written += 256;
    }
    
    print("\nFlash memory read:\n\n");

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

    print("\n");

    // Boot to the user configuration
    (*(volatile uint32_t *) SYSTEM_WARM_BOOT_APB) = 1;
    
    uint32_t counter = 0;
    while(1){
        if(counter++ == 10000){
            GPIO_A->OUTPUT = GPIO_A->OUTPUT + 1;
            counter = 0;
        }
        while(UART_A->STATUS >> 24){ //UART RX interrupt
            UART_A->DATA = (UART_A->DATA) & 0xFF;
        }
    }
}

