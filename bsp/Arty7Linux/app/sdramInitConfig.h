#pragma once

#include "saxon.h"
#include "sdram.h"

void bspMain() {
	sdram_init(
		SYSTEM_SDRAM_A_APB,
		SDRAM_DOMAIN_PHY_A_APB,
		5,
		5,
		8,
		MT41K128M16JT_125_ps,
		2,
		3300,
        1,
        0
	);
}


