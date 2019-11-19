#include <stdint.h>

#include "saxon.h"
#include "io.h"

void print_hex(uint32_t val, uint32_t digits)
{
	for (int i = (4*digits)-4; i >= 0; i -= 4)
		uart_write(UART_A, "0123456789ABCDEF"[(val >> i) % 16]);
}


void print(uint8_t * data) {
  uart_writeStr(UART_A, data);
}

#define mem ((volatile uint32_t*)0x90000000)
#define MAX_WORDS (8 * 1024 * 1024)

void main() {

    print("Memory test\n");

    GPIO_A->OUTPUT_ENABLE = 0x000000FF;

    // Set the Blue led to indicate test in progress
    GPIO_A->OUTPUT = 0x0000000E;
    
    for(int i=0;i<MAX_WORDS;i++) mem[i] = i;

    for(int i=0;i<MAX_WORDS;i++) {
      if (mem[i] != i) {
        // Set the Red led for failure
        GPIO_A->OUTPUT = 0x00000007;
	print("Failed at address 0x");
	print_hex(i, 8);
	print(" with value 0x");
	print_hex(mem[i], 8);
	print("\n");

        while(1){}
      }
    }

    // Set the Green led for success
    GPIO_A->OUTPUT = 0x0000000D;
    print("Success\n");

    while(1){}
}

