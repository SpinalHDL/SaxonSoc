#ifndef SAXON_H_
#define SAXON_H_

#include <soc.h>
#include "riscv.h"
#include "gpio.h"
#include "uart.h"


#define GPIO_A    ((Gpio_Reg*)(SYSTEM_GPIO_A))
#define UART_A      ((Uart_Reg*)(SYSTEM_UART_A))
#define MACHINE_TIMER   SYSTEM_MACHINE_TIMER_APB
#define MACHINE_TIMER_HZ   SYSTEM_MACHINE_TIMER_HZ
#define PLIC      SYSTEM_PLIC_APB
#define PLIC_CPU_0 0
#define PLIC_GPIO_A_0  4

#endif 
