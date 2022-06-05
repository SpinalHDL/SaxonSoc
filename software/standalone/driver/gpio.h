#pragma once

#include "type.h"
#include "io.h"

#define GPIO_INPUT 0x00
#define GPIO_OUTPUT 0x04
#define GPIO_OUTPUT_ENABLE 0x08

#define GPIO_INTERRUPT_RISE_ENABLE 0x20
#define GPIO_INTERRUPT_FALL_ENABLE 0x24
#define GPIO_INTERRUPT_HIGH_ENABLE 0x28
#define GPIO_INTERRUPT_LOW_ENABLE  0x2c

readReg_u32 (gpio_getInput               , GPIO_INPUT)
readReg_u32 (gpio_getOutput              , GPIO_OUTPUT)
writeReg_u32(gpio_setOutput              , GPIO_OUTPUT)
readReg_u32 (gpio_getOutputEnable        , GPIO_OUTPUT_ENABLE)
writeReg_u32(gpio_setOutputEnable        , GPIO_OUTPUT_ENABLE)

writeReg_u32(gpio_setInterruptRiseEnable , GPIO_INTERRUPT_RISE_ENABLE)
writeReg_u32(gpio_setInterruptFallEnable , GPIO_INTERRUPT_FALL_ENABLE)
writeReg_u32(gpio_setInterruptHighEnable , GPIO_INTERRUPT_HIGH_ENABLE)
writeReg_u32(gpio_setInterruptLowEnable  , GPIO_INTERRUPT_LOW_ENABLE)
