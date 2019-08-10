#include <stdint.h>

#include "saxon.h"
#include "lcd.h"
#include "io.h"

void print_hex(uint32_t val, uint32_t digits)
{
        for (int i = (4*digits)-4; i >= 0; i -= 4)
                uart_write(UART_A, "0123456789ABCDEF"[(val >> i) % 16]);
}

void print(uint8_t * data) {
  uart_writeStr(UART_A, data);
}

void set_tile(uint32_t x, uint32_t y, uint32_t v) {
  write_u32((y * 40) + x, LCD_OFFSET);
  write_u32(v, LCD_VALUE);
  write_u32(0, LCD_TEXTURE);
}

void set_texture(uint32_t t, uint32_t x, uint32_t y, uint32_t v) {
  write_u32((t << 6) + (y << 3) + x, LCD_OFFSET);
  write_u32(v, LCD_VALUE);
  write_u32(1, LCD_TEXTURE);
}

void main() {
    GPIO_A->OUTPUT_ENABLE = 0x00000001;
    GPIO_A->OUTPUT = 0x00000000;

    print("\nSetting up tiles\n");

    for(int x=0;x<40;x++) for(int y=0;y<30;y++) set_tile(x, y, 0);

    for(int t=0;t<64;t++) for(int x=0;x<8;x++) for(int y=0;y<8;y++) set_texture(t, x, y, 5);

    uint32_t counter = 0;
    while(1){
        if(counter++ == 100000){
            GPIO_A->OUTPUT = GPIO_A->OUTPUT + 1;
            counter = 0;
        }
    }
}

