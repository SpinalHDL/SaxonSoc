#ifndef GPIO_H_
#define GPIO_H_

#include "type.h"

typedef struct
{
  volatile u32 INPUT;
  volatile u32 OUTPUT;
  volatile u32 OUTPUT_ENABLE;

  volatile u32 dummy1[5];

  volatile u32 INTERRUPT_RISE_ENABLE;
  volatile u32 INTERRUPT_FALL_ENABLE;
  volatile u32 INTERRUPT_HIGH_ENABLE;
  volatile u32 INTERRUPT_LOW_ENABLE;
} Gpio_Reg;


#endif /* GPIO_H_ */


