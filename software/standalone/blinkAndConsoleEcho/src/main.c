#include <stdint.h>

#include "saxon.h"
#include "io.h"

volatile char globalC = 'b';

void main() {
	volatile uint32_t a = 1, b = 2, c = 3;
	uint32_t result = 0;
    GPIO_A->OUTPUT_ENABLE = 0x000000FF;
    GPIO_A->OUTPUT = 0x00000000;

    globalC+=1;
    UART_A->DATA = globalC;

    uint32_t counter = 0;
    while(1){
        if(counter++ == 1000){
            GPIO_A->OUTPUT = GPIO_A->OUTPUT + 1;
            counter = 0;
        }
        while(1){
	    uint32_t key = read_u32(SYSTEM_PS2_KEYBOARD_A_APB);
	    if (UART_A->STATUS >> 24 || key >> 8) { //UART RX interrupt or key valid
	        uint32_t ch = (key >> 8) ? key & 0xFF : (UART_A->DATA) & 0xFF;
                UART_A->DATA = ch;
	        write_u32(ch, SYSTEM_HDMI_CONSOLE_A_APB);
	    }
        }
    }
}

