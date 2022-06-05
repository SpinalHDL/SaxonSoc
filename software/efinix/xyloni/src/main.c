#include <stdint.h>

#include "bsp.h"
#include "gpio.h"
#include "uart.h"

#ifdef SPINAL_SIM
#define LOOP_UDELAY 100
#else
#define LOOP_UDELAY 100000
#endif

void main() {
    bsp_init();

    gpio_setOutputEnable(BSP_LED_GPIO, BSP_LED_MASK);
    gpio_setOutput(BSP_LED_GPIO, 0x00000000);

    uart_write(BSP_UART_TERMINAL, '!');
    while(1){
        gpio_setOutput(BSP_LED_GPIO, gpio_getOutput(BSP_LED_GPIO) ^ BSP_LED_MASK);

        while(uart_readOccupancy(BSP_UART_TERMINAL)){
            uart_write(BSP_UART_TERMINAL, uart_read(BSP_UART_TERMINAL));
        }

        bsp_uDelay(LOOP_UDELAY);
    }
}

