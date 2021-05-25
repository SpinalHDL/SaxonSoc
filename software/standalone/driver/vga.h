#pragma once

#include "type.h"
#include "io.h"

typedef struct {
	u32 hSyncStart ,hSyncEnd;
	u32 hColorStart,hColorEnd;

	u32 vSyncStart ,vSyncEnd;
	u32 vColorStart,vColorEnd;

	u32 polarities;
}Vga_Timing;

static const Vga_Timing vga_h640_v480_r60 = {
    .hSyncStart  = 96,
    .hSyncEnd    = 800,
    .hColorStart = 96 + 16,
    .hColorEnd   = 800 - 48,
    .vSyncStart  = 2,
    .vSyncEnd 	 = 525,
    .vColorStart = 2 + 10,
    .vColorEnd 	 = 525 - 33,
    .polarities  = 0,
};

static const Vga_Timing vga_h800_v600_r72 = {
    .hSyncStart  = 120,
    .hSyncEnd    = 1040,
    .hColorStart = 120 + 56,
    .hColorEnd   = 1040 - 64,
    .vSyncStart  = 6,
    .vSyncEnd 	 = 666,
    .vColorStart = 6 + 37,
    .vColorEnd 	 = 666 - 23,
    .polarities  = 3,
};

static const Vga_Timing vga_h800_v600_r60 = {
    .hSyncStart  = 127,
    .hSyncEnd    = 1055,
    .hColorStart = 215,
    .hColorEnd   = 1015,
    .vSyncStart  = 3,
    .vSyncEnd 	 = 627,
    .vColorStart = 26,
    .vColorEnd 	 = 626,
    .polarities  = 3,
};

static const Vga_Timing vga_h1024_v768_r60 = {
    .hSyncStart  = 135,
    .hSyncEnd    = 1343,
    .hColorStart = 295,
    .hColorEnd   = 1319,
    .vSyncStart  = 5,
    .vSyncEnd 	 = 805,
    .vColorStart = 34,
    .vColorEnd 	 = 802,
    .polarities  = 0
};

static const Vga_Timing vga_h1920_v1080_r60 = {
    .hSyncStart  = 43,
    .hSyncEnd    = 2199,
    .hColorStart = 191,
    .hColorEnd   = 2111,
    .vSyncStart  = 4,
    .vSyncEnd 	 = 1124,
    .vColorStart = 40,
    .vColorEnd 	 = 1120,
    .polarities  = 3,
};


static const Vga_Timing vga_simRes = {
    .hSyncStart  = 8,
    .hSyncEnd    = 70,
    .hColorStart = 16,
    .hColorEnd   = 64,
    .vSyncStart  = 2,
    .vSyncEnd 	 = 48,
    .vColorStart = 8,
    .vColorEnd 	 = 40,
    .polarities  = 0,
};

static const Vga_Timing vga_simRes_h160_v120 = {
	.hSyncStart  = 8,
	.hSyncEnd    = 24+160,
	.hColorStart = 16,
	.hColorEnd   = 16+160,
	.vSyncStart  = 2,
	.vSyncEnd 	 = 10+120,
	.vColorStart = 6,
	.vColorEnd 	 = 6+120,
    .polarities  = 0,
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
    write_u32(t.vSyncStart  , base + 0x50);
    write_u32(t.vSyncEnd    , base + 0x54);
    write_u32(t.vColorStart , base + 0x58);
    write_u32(t.vColorEnd   , base + 0x5C);
    write_u32(t.polarities  , base + 0x60);
}