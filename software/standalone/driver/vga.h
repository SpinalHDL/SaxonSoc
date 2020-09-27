#pragma once

#include "type.h"
#include "io.h"

typedef struct {
	u32 hSyncStart ,hSyncEnd;
	u32 hColorStart,hColorEnd;
	u32 hPolarity;

	u32 vSyncStart ,vSyncEnd;
	u32 vColorStart,vColorEnd;
	u32 vPolarity;
}Vga_Timing;

static const Vga_Timing vga_h640_v480_r60 = {
    .hSyncStart  = 96,
    .hSyncEnd    = 800,
    .hColorStart = 96 + 16,
    .hColorEnd   = 800 - 48,
    .hPolarity   = 0,
    .vSyncStart  = 2,
    .vSyncEnd 	 = 525,
    .vColorStart = 2 + 10,
    .vColorEnd 	 = 525 - 33,
    .vPolarity   = 0,
};

static const Vga_Timing vga_h800_v600_r72 = {
    .hSyncStart  = 120,
    .hSyncEnd    = 1040,
    .hColorStart = 120 + 56,
    .hColorEnd   = 1040 - 64,
    .hPolarity   = 1,
    .vSyncStart  = 6,
    .vSyncEnd 	 = 666,
    .vColorStart = 6 + 37,
    .vColorEnd 	 = 666 - 23,
    .vPolarity   = 1,
};

static const Vga_Timing vga_h800_v600_r60 = {
    .hSyncStart  = 128,
    .hSyncEnd    = 1056,
    .hColorStart = 128 + 40,
    .hColorEnd   = 1056 - 88,
    .hPolarity   = 1,
    .vSyncStart  = 4,
    .vSyncEnd 	 = 628,
    .vColorStart = 4 + 1,
    .vColorEnd 	 = 628 - 23,
    .vPolarity   = 1,
};

static const Vga_Timing vga_simRes = {
    .hSyncStart  = 8,
    .hSyncEnd    = 70,
    .hColorStart = 16,
    .hColorEnd   = 64,
    .hPolarity   = 0,
    .vSyncStart  = 2,
    .vSyncEnd 	 = 48,
    .vColorStart = 8,
    .vColorEnd 	 = 40,
    .vPolarity   = 0,
};

static const Vga_Timing vga_simRes_h160_v120 = {
	.hSyncStart  = 8,
	.hSyncEnd    = 24+160,
	.hColorStart = 16,
	.hColorEnd   = 16+160,
    .hPolarity   = 0,
	.vSyncStart  = 2,
	.vSyncEnd 	 = 10+120,
	.vColorStart = 6,
	.vColorEnd 	 = 6+120,
    .vPolarity   = 0,
};

static void vga_start(u32 base){
    write_u32(1 , base);
}
static void vga_stop(u32 base){
    write_u32(0 , base);
}

static void vga_set_timing(u32 base, Vga_Timing t){
    write_u32(t.hSyncStart  , base + 0x40);
    write_u32(t.hSyncEnd    , base + 0x44);
    write_u32(t.hColorStart , base + 0x48);
    write_u32(t.hColorEnd   , base + 0x4C);
    write_u32(t.hPolarity   , base + 0x50);
    write_u32(t.vSyncStart  , base + 0x54);
    write_u32(t.vSyncEnd    , base + 0x58);
    write_u32(t.vColorStart , base + 0x5C);
    write_u32(t.vColorEnd   , base + 0x60);
    write_u32(t.vPolarity   , base + 0x64);
}