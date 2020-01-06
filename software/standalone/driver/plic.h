#ifndef PLIC_H_
#define PLIC_H_

#include "type.h"
#include "io.h"

#define PLIC_PRIORITY_BASE         0x0000
#define PLIC_PENDING_BASE          0x1000
#define PLIC_ENABLE_BASE           0x2000
#define PLIC_THRESHOLD_BASE 	   0x200000
#define PLIC_CLAIM_BASE            0x200004

#define PLIC_ENABLE_PER_HART	   0x80
#define PLIC_CONTEXT_PER_HART	   0x1000



static void plic_set_priority(u32 plic, u32 gateway, u32 priority){
	write_u32(priority, plic + PLIC_PRIORITY_BASE + gateway*4);
}

static void plic_set_enable(u32 plic, u32 target,u32 gateway, u32 enable){
	u32 word = plic + PLIC_ENABLE_BASE + target * PLIC_ENABLE_PER_HART + (gateway / 32);
	u32 mask = 1 << (gateway % 32);
	if (enable)
		write_u32(read_u32(word) | mask, word);
	else
		write_u32(read_u32(word) & ~mask, word);
}

static void plic_set_threshold(u32 plic, u32 target, u32 threshold){
	write_u32(threshold, plic + PLIC_THRESHOLD_BASE + target*PLIC_CONTEXT_PER_HART);
}

static u32 plic_claim(u32 plic, u32 target){
	return read_u32(plic + PLIC_CLAIM_BASE + target*PLIC_CONTEXT_PER_HART);
}

static void plic_release(u32 plic, u32 target, u32 gateway){
	write_u32(gateway,plic + PLIC_CLAIM_BASE + target*PLIC_CONTEXT_PER_HART);
}



#endif /* PLIC_H_ */


