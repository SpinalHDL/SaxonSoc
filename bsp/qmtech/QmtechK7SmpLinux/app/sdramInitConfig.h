#pragma once

#include "bsp.h"
#include "sdram.h"
#include "vgaInit.h"
#include "spiFlash.h"

#define SDRAM_CTRL SYSTEM_SDRAM_A_CTRL
#define SDRAM_PHY  SDRAM_DOMAIN_PHY_A_CTRL
#define SDRAM_BASE SYSTEM_SDRAM_A0_BMB

#define RL 5
#define WL 5
#define CTRL_BURST_LENGHT 2
#define PHY_CLK_RATIO 2

#define SPI SYSTEM_SPI_A_CTRL
#define SPI_CS 0

void bspMain() {
    sdram_init(
        SDRAM_CTRL,
        RL,
        WL,
        MT41K128M16JT_125_ps,
        CTRL_BURST_LENGHT,
        PHY_CLK_RATIO,
        3300
    );

    sdram_ddr3_init(
        SDRAM_CTRL,
        RL,
        WL,
        CTRL_BURST_LENGHT,
        PHY_CLK_RATIO
    );

#ifndef SPINAL_SIM
    sdram_phy_s7(
        SDRAM_CTRL,
        SDRAM_PHY,
        SDRAM_BASE
    );
#endif

    vgaInit();

    spiFlash_init(SPI, SPI_CS);
    spiFlash_wake(SPI, SPI_CS);

    asm("ebreak");
}
