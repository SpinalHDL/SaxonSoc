#pragma once

#include "bsp.h"
#include "sdram.h"

#define RL 5
#define WL 5
#define CTRL_BURST_LENGHT 2
#define PHY_CLK_RATIO 2

void bspMain() {
    sdram_init(
        SYSTEM_SDRAM_A_APB,
        RL,
        WL,
        MT41K128M16JT_125_ps,
        CTRL_BURST_LENGHT,
        PHY_CLK_RATIO,
        3300
    );

    sdram_ddr3_init(
        SYSTEM_SDRAM_A_APB,
        RL,
        WL,
        CTRL_BURST_LENGHT,
        PHY_CLK_RATIO
    );

    sdram_phy_s7(
        SYSTEM_SDRAM_A_APB,
        SDRAM_DOMAIN_PHY_A_APB,
        SYSTEM_SDRAM_A0_BMB
    );
}


