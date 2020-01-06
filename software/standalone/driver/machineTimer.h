#ifndef MACHINE_TIMER_H_
#define MACHINE_TIMER_H_

#include "type.h"
#include "io.h"

static void machineTimer_setCmp(u32 p, u64 cmp){
    write_u32(0xFFFFFFFF, p + 0xC);
    write_u32(cmp, p + 0x8);
    write_u32(cmp >> 32, p + 0xC);
}

static u64 machineTimer_getTime(u32 p){
	u32 lo, hi;

	/* Likewise, must guard against rollover when reading */
	do {
		hi = read_u32(p + 0x4);
		lo = read_u32(p + 0x0);
	} while (read_u32(p + 0x4) != hi);

	return (((u64)hi) << 32) | lo;
}


#endif /* MACHINE_TIMER_H_ */


