#ifndef IO_H
#define IO_H

#include <stdint.h>
#include "soc.h"

static inline uint32_t read_u32(uint32_t address){
	return *((volatile uint32_t*) address);
}

static inline void write_u32(uint32_t data, uint32_t address){
	*((volatile uint32_t*) address) = data;
}

static inline void write_u32_ad(uint32_t address, uint32_t data){
	*((volatile uint32_t*) address) = data;
}

//static void io_delay(uint32_t cycles){
//	//TODO
//	/*
//	for(int32_t i = (cycles + 7) >> 3;i >= 0;i--){
//		asm("nop;nop;nop;nop;nop;nop;nop;nop;");
//	}*/
//}

#if defined(SYSTEM_MACHINE_TIMER_APB)
static void io_udelay(uint32_t usec){
	uint32_t mTimePerUsec = SYSTEM_MACHINE_TIMER_HZ/1000000;
	uint32_t limit = read_u32(SYSTEM_MACHINE_TIMER_APB) + usec*mTimePerUsec;
	while((int32_t)(limit-(read_u32(SYSTEM_MACHINE_TIMER_APB))) >= 0);
}
#endif


#endif


