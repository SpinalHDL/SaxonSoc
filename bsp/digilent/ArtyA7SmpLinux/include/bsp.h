#pragma once

#include "soc.h"
#include "uart.h"
#include "clint.h"

#define BSP_HART_COUNT SYSTEM_CPU_COUNT

#define BSP_PLIC SYSTEM_PLIC_CTRL
#define BSP_PLIC_CPU_0 SYSTEM_PLIC_SYSTEM_CORES_0_EXTERNAL_INTERRUPT
#define BSP_CLINT SYSTEM_CLINT_CTRL
#define BSP_CLINT_HZ SYSTEM_CLINT_HZ

#define BSP_UART_TERMINAL SYSTEM_UART_A_CTRL
#define BSP_LED_GPIO SYSTEM_GPIO_A_CTRL
#define BSP_LED_MASK 0x01

#define bsp_init() {}
#define bsp_putChar(c) uart_write(BSP_UART_TERMINAL, c);
#define bsp_uDelay(usec) clint_uDelay(usec, SYSTEM_CLINT_HZ, SYSTEM_CLINT_CTRL);
#define bsp_putString(s) uart_writeStr(BSP_UART_TERMINAL, s);

// Freertos specifics
#define configMTIME_BASE_ADDRESS        (BSP_CLINT + 0xBFF8)
#define configMTIMECMP_BASE_ADDRESS     (BSP_CLINT + 0x4000)
#define configCPU_CLOCK_HZ              ( ( uint32_t ) ( BSP_CLINT_HZ ) )