#ifndef MACHINE_TIMER_H_
#define MACHINE_TIMER_H_

#include <stdint.h>
#include "io.h"

void machineTimer_setCmp(uint32_t p, uint64_t cmp){
    write_u32(0xFFFFFFFF, p + 0xC);
    write_u32(cmp, p + 0x8);
    write_u32(cmp >> 32, p + 0xC);
}

uint64_t machineTimer_getTime(uint32_t p){
	uint32_t lo, hi;

	/* Likewise, must guard against rollover when reading */
	do {
		hi = read_u32(p + 0x4);
		lo = read_u32(p + 0x0);
	} while (read_u32(p + 0x4) != hi);

	return (((uint64_t)hi) << 32) | lo;
}


#endif /* MACHINE_TIMER_H_ */


