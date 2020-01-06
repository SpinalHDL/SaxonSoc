#pragma once

#include "type.h"
#include "soc.h"

static inline u32 read_u32(u32 address){
	return *((volatile u32*) address);
}

static inline void write_u32(u32 data, u32 address){
	*((volatile u32*) address) = data;
}

static inline u16 read_u16(u32 address){
	return *((volatile u16*) address);
}

static inline void write_u16(u16 data, u32 address){
	*((volatile u16*) address) = data;
}

static inline u8 read_u8(u32 address){
	return *((volatile u8*) address);
}

static inline void write_u8(u8 data, u32 address){
	*((volatile u8*) address) = data;
}

static inline void write_u32_ad(u32 address, u32 data){
	*((volatile u32*) address) = data;
}

//static void io_delay(u32 cycles){
//	//TODO
//	/*
//	for(int32_t i = (cycles + 7) >> 3;i >= 0;i--){
//		asm("nop;nop;nop;nop;nop;nop;nop;nop;");
//	}*/
//}

#if defined(SYSTEM_MACHINE_TIMER_APB)
static void io_udelay(u32 usec){
	u32 mTimePerUsec = SYSTEM_MACHINE_TIMER_HZ/1000000;
	u32 limit = read_u32(SYSTEM_MACHINE_TIMER_APB) + usec*mTimePerUsec;
	while((int32_t)(limit-(read_u32(SYSTEM_MACHINE_TIMER_APB))) >= 0);
}
#endif




