#pragma once

#include "soc.h"
#include "uart.h"
#include "machineTimer.h"

#define BSP_PLIC SYSTEM_PLIC_APB
#define BSP_PLIC_CPU_0 SYSTEM_PLIC_SYSTEM_CPU_EXTERNAL_INTERRUPT
#define BSP_MACHINE_TIMER SYSTEM_MACHINE_TIMER_APB
#define BSP_MACHINE_TIMER_HZ SYSTEM_MACHINE_TIMER_HZ
#define BSP_MACHINE_TIMER_PLIC_ID SYSTEM_PLIC_SYSTEM_MACHINE_TIMER_INTERRUPT

#define BSP_UART_TERMINAL SYSTEM_UART_A_APB
#define BSP_LED_GPIO SYSTEM_GPIO_A_APB
#define BSP_LED_MASK 0x01

#define bsp_init() {}
#define bsp_putChar(c) uart_write(BSP_UART_TERMINAL, c);
#define bsp_uDelay(usec) machineTimer_uDelay(usec, SYSTEM_MACHINE_TIMER_HZ, SYSTEM_MACHINE_TIMER_APB);
#define bsp_putString(s) uart_writeStr(BSP_UART_TERMINAL, s);