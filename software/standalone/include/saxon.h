#ifndef SAXON_H_
#define SAXON_H_

#include <soc.h>
#include "gpio.h"
#include "uart.h"

#define CORE_HZ 25000000

#define GPIO_A    ((Gpio_Reg*)(SYSTEM_GPIO_A))
#define UART_A      ((Uart_Reg*)(SYSTEM_UART_A))


#endif 
