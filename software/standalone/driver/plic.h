#ifndef PLIC_H_
#define PLIC_H_

#include <stdint.h>
#include "io.h"

#define PLIC_PRIORITY_BASE         0x0000
#define PLIC_PENDING_BASE          0x1000
#define PLIC_ENABLE_BASE           0x2000
#define PLIC_THRESHOLD_BASE 	   0x200000
#define PLIC_CLAIM_BASE            0x200004

#define PLIC_ENABLE_PER_HART	   0x80
#define PLIC_CONTEXT_PER_HART	   0x1000



void plic_set_priority(uint32_t plic, uint32_t gateway, uint32_t priority){
	write_u32(priority, plic + PLIC_PRIORITY_BASE + gateway*4);
}

void plic_set_enable(uint32_t plic, uint32_t target,uint32_t gateway, uint32_t enable){
	uint32_t word = plic + PLIC_ENABLE_BASE + target * PLIC_ENABLE_PER_HART + (gateway / 32);
	uint32_t mask = 1 << (gateway % 32);
	if (enable)
		write_u32(read_u32(word) | mask, word);
	else
		write_u32(read_u32(word) & ~mask, word);
}

void plic_set_threshold(uint32_t plic, uint32_t target, uint32_t threshold){
	write_u32(threshold, plic + PLIC_THRESHOLD_BASE + target*PLIC_CONTEXT_PER_HART);
}

uint32_t plic_claim(uint32_t plic, uint32_t target){
	return read_u32(plic + PLIC_CLAIM_BASE + target*PLIC_CONTEXT_PER_HART);
}

void plic_release(uint32_t plic, uint32_t target, uint32_t gateway){
	write_u32(gateway,plic + PLIC_CLAIM_BASE + target*PLIC_CONTEXT_PER_HART);
}



#endif /* PLIC_H_ */


