#pragma once

#include "soc.h"
#include "uart.h"
#include "machineTimer.h"

#define BSP_CORE_HZ SYSTEM_MACHINE_TIMER_HZ
#define BSP_PLIC SYSTEM_PLIC_APB
#define BSP_PLIC_CPU_0 SYSTEM_PLIC_SYSTEM_CPU_EXTERNAL_INTERRUPT
#define BSP_LED_GPIO SYSTEM_GPIO_A_APB
#define BSP_LED_MASK 0x00
#define BSP_UART_TERMINAL SYSTEM_UART_A_APB

#define bsp_init() {}
#define bsp_uDelay(usec) machineTimer_uDelay(usec, SYSTEM_MACHINE_TIMER_HZ, SYSTEM_MACHINE_TIMER_APB);
#define bsp_putChar(c) uart_write(BSP_UART_TERMINAL, c);

static void bsp_putString(char *s){
    char c;
    while(c = *s++){
        bsp_putChar(c);
    }
}