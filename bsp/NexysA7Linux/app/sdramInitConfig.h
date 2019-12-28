#pragma once

#include "saxon.h"
#include "sdram.h"


#define RL 5
#define CTRL_BURST_LENGHT 2
#define PHY_CLK_RATIO 2

void bspMain() {
	sdram_init(
		SYSTEM_SDRAM_A_APB,
		RL,
		RL-1,
		MT47H64M16HR_25_ps,
		CTRL_BURST_LENGHT,
		PHY_CLK_RATIO,
		3300
	);

	sdram_ddr2_init(
		SYSTEM_SDRAM_A_APB,
		RL,
		MT47H64M16HR_25_ps,
		CTRL_BURST_LENGHT,
		PHY_CLK_RATIO,
		3300
	);

	sdram_phy_s7(
		SYSTEM_SDRAM_A_APB,
		SDRAM_DOMAIN_PHY_A_APB
	);
}
