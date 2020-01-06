#ifndef SAXON_H_
#define SAXON_H_

#include <soc.h>
#include "riscv.h"
#include "gpio.h"
#include "uart.h"
#include "i2c.h"


#define GPIO_A    ((Gpio_Reg*)(SYSTEM_GPIO_A_APB))
#define UART_A      ((Uart_Reg*)(SYSTEM_UART_A_APB))
#define UART_CONSOLE  UART_A
#define I2C_A ((I2c_Reg*)(SYSTEM_I2C_A_APB))

#ifdef SYSTEM_MACHINE_TIMER_APB
#define MACHINE_TIMER   SYSTEM_MACHINE_TIMER_APB
#define MACHINE_TIMER_HZ   SYSTEM_MACHINE_TIMER_HZ
#endif

#ifdef SYSTEM_PLIC_APB
#define PLIC SYSTEM_PLIC_APB
#define PLIC_CPU_0 SYSTEM_PLIC_SYSTEM_CPU_EXTERNAL_INTERRUPT
#endif


#endif 
