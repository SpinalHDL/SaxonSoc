#pragma once

#include "soc.h"
#include "uart.h"
#include "clint.h"

#define BSP_HART_COUNT SYSTEM_CPU_COUNT

#define BSP_PLIC SYSTEM_PLIC_BUS
#define BSP_PLIC_CPU_0 SYSTEM_PLIC_SYSTEM_CPU_EXTERNAL_INTERRUPT
#define BSP_CLINT SYSTEM_CLINT_BUS
#define BSP_CLINT_HZ SYSTEM_CLINT_HZ
#define BSP_CLINT_PLIC_ID SYSTEM_PLIC_SYSTEM_CLINT_INTERRUPT

#define BSP_UART_TERMINAL SYSTEM_UART_A_BUS
#define BSP_LED_GPIO SYSTEM_GPIO_A_BUS
#define BSP_LED_MASK 0x01

#define bsp_init() {}
#define bsp_putChar(c) uart_write(BSP_UART_TERMINAL, c);
#define bsp_uDelay(usec) clint_uDelay(usec, SYSTEM_CLINT_HZ, SYSTEM_CLINT_BUS);
#define bsp_putString(s) uart_writeStr(BSP_UART_TERMINAL, s);
#define bsp_putHex(s) uart_writeHex(BSP_UART_TERMINAL, s);