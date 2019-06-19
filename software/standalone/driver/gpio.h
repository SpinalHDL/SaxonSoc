#ifndef GPIO_H_
#define GPIO_H_

#include "stdint.h"

typedef struct
{
  volatile uint32_t INPUT;
  volatile uint32_t OUTPUT;
  volatile uint32_t OUTPUT_ENABLE;

  volatile uint32_t dummy1[5];

  volatile uint32_t INTERRUPT_RISE_ENABLE;
  volatile uint32_t INTERRUPT_FALL_ENABLE;
  volatile uint32_t INTERRUPT_HIGH_ENABLE;
  volatile uint32_t INTERRUPT_LOW_ENABLE;
} Gpio_Reg;


#endif /* GPIO_H_ */


