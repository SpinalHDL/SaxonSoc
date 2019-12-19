#pragma once

#include "saxon.h"
#include "sdram.h"


#define RL 5
#define WL 5
#define BL 8
void bspMain() {
	sdram_init(
		SYSTEM_SDRAM_A_APB,
		SDRAM_DOMAIN_PHY_A_APB,
		RL,
		WL,
		BL,
		MT41K128M16JT_125_ps,
		2,
		3300
	);

    sdram_ddr3_init(
        SYSTEM_SDRAM_A_APB,
        RL,
        WL,
        BL
    );

	sdram_phy_s7(
		SYSTEM_SDRAM_A_APB,
		SDRAM_DOMAIN_PHY_A_APB
	);
}


