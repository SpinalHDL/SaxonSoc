#include <stdint.h>

#include "saxon.h"

#define mem ((volatile uint32_t*)0x90000000)
#define MAX_WORDS (512 * 1024)

void main() {

    GPIO_A->OUTPUT_ENABLE = 0x000000FF;

    // Set the Blue led to indicate test in progress
    GPIO_A->OUTPUT = 0x000000E0;
    
    for(int i=0;i<MAX_WORDS;i++) mem[i] = i;

    for(int i=0;i<MAX_WORDS;i++) {
      if (mem[i] != i) {
        // Set the Red led for failure
        GPIO_A->OUTPUT = 0x00000070;
        while(1){}
      }
    }

    // Set the Green led for success
    GPIO_A->OUTPUT = 0x000000D0;
    while(1){}
}

