#pragma once

#include "bsp.h"
#include "sdram.h"
#include "vgaInit.h"

#define SDRAM_CTRL SYSTEM_SDRAM_A_CTRL
#define SDRAM_PHY  SDRAM_DOMAIN_PHY_A_CTRL
#define SDRAM_BASE SYSTEM_SDRAM_A0_BMB

#define RL 5
#define WL (RL-1)
#define CTRL_BURST_LENGHT 2
#define PHY_CLK_RATIO 2

void bspMain() {
    sdram_init(
        SDRAM_CTRL,
        RL,
        WL,
        MT47H64M16HR_25_ps,
        CTRL_BURST_LENGHT,
        PHY_CLK_RATIO,
        3300
    );
    sdram_ddr2_init(
        SDRAM_CTRL,
        RL,
        MT47H64M16HR_25_ps,
        CTRL_BURST_LENGHT,
        PHY_CLK_RATIO,
        3300
    );
#ifndef SPINAL_SIM
    sdram_phy_s7(
        SDRAM_CTRL,
        SDRAM_PHY,
        SDRAM_BASE
    );
    vgaInit();
#endif
    asm("ebreak");
}
