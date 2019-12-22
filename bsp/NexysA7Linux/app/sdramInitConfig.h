#pragma once

#include "saxon.h"
#include "sdram.h"


#define AL 0
#define BL 8 //4,8
#define CL 6 //2.5ns@CL=5 p.1,34
#define RL (AL+CL)
#define WL (AL+CL-1)

void bspMain() {
	sdram_init(
		SYSTEM_SDRAM_A_APB,
		SDRAM_DOMAIN_PHY_A_APB,
		RL,
		WL,
		BL,
		MT47H64M16HR_25_ps,
		2,
		3300
	);

	sdram_ddr2_init(
		SYSTEM_SDRAM_A_APB,
		RL,
		WL,
		BL,
		AL
	);

	sdram_phy_s7(
		SYSTEM_SDRAM_A_APB,
		SDRAM_DOMAIN_PHY_A_APB
	);
}
