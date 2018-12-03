#ifndef SAXON_H_
#define SAXON_H_

#include "gpio.h"
#include "uart.h"

#define CORE_HZ 12000000

#define GPIO_A    ((Gpio_Reg*)(0xF0000000))
#define UART_A      ((Uart_Reg*)(0xF0010000))


#endif 
