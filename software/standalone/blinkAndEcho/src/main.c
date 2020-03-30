#include <stdint.h>

#include "bsp.h"
#include "gpio.h"
#include "uart.h"

void main() {
    bsp_init();

    gpio_setOutputEnable(BSP_LED_GPIO, BSP_LED_MASK);
    gpio_setOutput(BSP_LED_GPIO, 0x00000000);

    uart_write(BSP_UART_TERMINAL, '!');

    uint32_t counter = 0;
    while(1){
        if(counter++ == 1000){
            gpio_setOutput(BSP_LED_GPIO, gpio_getInput(BSP_LED_GPIO) ^ BSP_LED_MASK);
            counter = 0;
        }

        while(uart_readOccupancy(BSP_UART_TERMINAL)){
            uart_write(BSP_UART_TERMINAL, uart_read(BSP_UART_TERMINAL));
        }

        bsp_uDelay(100000);
    }
}

