#include <stdint.h>

#include "saxon.h"
#include "sdram.h"

void main() {
//	uart_writeStr(UART_A, "Hello world\n");
//	static void sdram_init(uint32_t core, uint32_t phy, uint32_t rl, uint32_t wl, uint32_t bl, SdramTiming t, uint32_t phyClkRatio, uint32_t sdramPeriod){
	sdram_init(
		SYSTEM_SDRAM_A_APB,
		SDRAM_DOMAIN_PHY_A_APB,
		5,
		5,
		8,
		MT41K128M16JT_125_ps,
		2,
		3300
	);
}

