#ifndef MACHINE_TIMER_H_
#define MACHINE_TIMER_H_

#include <stdint.h>

void machineTimer_setCmp(void *p, uint64_t cmp){
	uint32_t *r = p;
	r[3] = 0xFFFFFFFF;
	r[2] = cmp;
	r[3] = cmp >> 32;
}

uint64_t machineTimer_getTime(void *p){
	uint32_t *r = p;
	uint32_t lo, hi;

	/* Likewise, must guard against rollover when reading */
	do {
		hi = r[1];
		lo = r[0];
	} while (r[1] != hi);

	return (((uint64_t)hi) << 32) | lo;
}


#endif /* MACHINE_TIMER_H_ */


