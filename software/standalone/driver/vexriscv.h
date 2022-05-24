#pragma once

#include "riscv.h"

#define data_cache_invalidate_all() asm(".word(0x500F)");
#define data_cache_invalidate_address(rs1)     \
({                                             \
    asm volatile(                              \
     ".word ((0x500F) | (regnum_%0 << 15));"   \
     :                                         \
     : "r" (rs1)                               \
    );                                         \
})
#define instruction_cache_invalidate() asm("fence.i");