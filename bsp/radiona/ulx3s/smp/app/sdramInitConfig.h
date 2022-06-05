#pragma once

#include "bsp.h"
#include "start.h"
#include "sdram.h"
#include "spiFlash.h"
#include "vgaInit.h"

#define SDRAM_CTRL SYSTEM_SDRAM_A_CTRL
#define SDRAM_PHY  SDRAM_DOMAIN_PHY_A_CTRL
#define SDRAM_BASE SYSTEM_SDRAM_A0_BMB

#define SPI SYSTEM_SPI_A_CTRL
#define SPI_CS 0

#define RL 3
#define WL 0
#define CTRL_BURST_LENGHT 1
#define PHY_CLK_RATIO 2

#ifndef SDRAM_TIMING
#error "You need to define the SDRAM_TIMING via the makefile CFLAGS_ARGS"
//Ex : make clean all BSP=Ulx3sLinuxUboot CFLAGS_ARGS="-DSDRAM_TIMING=MT48LC16M16A2_6A_ps"
//Ex : make clean all BSP=Ulx3sLinuxUboot CFLAGS_ARGS="-DSDRAM_TIMING=AS4C32M16SB_7TCN_ps"
#endif

void bspMain() {
    sdram_init(
        SDRAM_CTRL,
        RL,
        WL,
        SDRAM_TIMING,
        CTRL_BURST_LENGHT,
        PHY_CLK_RATIO,
        20000
    );

    sdram_sdr_init(
        SDRAM_CTRL,
        RL,
        CTRL_BURST_LENGHT,
        PHY_CLK_RATIO
    );

    vgaInit();
    asm("ebreak");
}


