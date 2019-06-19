#ifndef IO_H
#define IO_H

#include <stdint.h>

inline uint32_t read_u32(uint32_t address){
	return *((uint32_t*) address);
}

inline void write_u32(uint32_t data, uint32_t address){
	*((uint32_t*) address) = data;
}

#endif


