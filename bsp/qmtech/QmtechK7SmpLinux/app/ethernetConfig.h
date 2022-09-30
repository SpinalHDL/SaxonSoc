#pragma once

#include "bsp.h"

#define SPI SYSTEM_SPI_A_CTRL
#define SPI_CS 2

#define RESETN_GPIO SYSTEM_GPIO_A_BUS
#define RESETN_PIN (1 << 14)

#define MAC SYSTEM_MAC_BUS
