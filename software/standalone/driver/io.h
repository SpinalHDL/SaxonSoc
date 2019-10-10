#ifndef IO_H
#define IO_H

#include <stdint.h>
#include "soc.h"

inline uint32_t read_u32(uint32_t address){
	return *((volatile uint32_t*) address);
}

inline void write_u32(uint32_t data, uint32_t address){
	*((volatile uint32_t*) address) = data;
}

inline void write_u32_ad(uint32_t address, uint32_t data){
	*((volatile uint32_t*) address) = data;
}

void io_delay(uint32_t cycles){
	//TODO
	/*
	for(int32_t i = (cycles + 7) >> 3;i >= 0;i--){
		asm("nop;nop;nop;nop;nop;nop;nop;nop;");
	}*/
}

void io_udelay(uint32_t usec){
	io_delay(usec*200); //TODO
}

#endif


