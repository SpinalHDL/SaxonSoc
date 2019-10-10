#include <stdint.h>

#include "saxon.h"
#include "sdram.h"

volatile char globalC = 'b';

void main() {
//	uart_writeStr(UART_A, "Hello world\n");
	sdram_init(SYSTEM_SDRAM_A_APB, PHY_A_APB, 5, 5,sdram_timingsFrom(MT41K128M16JT_125_ps, MT41K128M16JT_125_cycle, 6667, 2), 2);
}

