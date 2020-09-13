#pragma once

#include "bsp.h"
#include "dmasg.h"
#include "vga.h"

#define DMASG_BASE SYSTEM_DMA_CTRL

#define VGA_BASE SYSTEM_VGA_CTRL
#define VGA_CHANNEL     0
#define VGA_PORT     0
#define VGA_SINK 0

#ifdef SPINAL_SIM
    #define VGA_TIMINGS vga_simRes_h160_v120
    #define VGA_WIDTH 160
    #define VGA_HEIGHT 120
#else
    #define VGA_TIMINGS vga_h640_v480_r60
    #define VGA_WIDTH 640
    #define VGA_HEIGHT 480
#endif

#define VGA_BYTE_PER_PIXEL 2
#define VGA_BUFFER     0x80E00000
#define VGA_BUFFER_SIZE (VGA_WIDTH * VGA_HEIGHT * VGA_BYTE_PER_PIXEL)

void vgaInit(){
    bsp_putString("VGA init\n");

    vga_set_timing(VGA_BASE, VGA_TIMINGS);
    vga_start(VGA_BASE);

    u32 i = 0;
    for(u32 y = 0;y < VGA_HEIGHT;y++){
        for(u32 x = 0;x < VGA_WIDTH;x++){
            ((u16*)VGA_BUFFER)[y*VGA_WIDTH + x] = y == 0 || x == 0 || x == VGA_WIDTH-1 || y == VGA_HEIGHT-1 ? 0xFFFF : (((x & 0x1F) << 11) | ((((x+y) >> 2) & 0x3F) << 5) | ((y & 0x1F) << 0));
        }
    }
    dmasg_input_memory(DMASG_BASE, VGA_CHANNEL,  (u32)VGA_BUFFER, 0);
    dmasg_output_stream (DMASG_BASE, VGA_CHANNEL, VGA_PORT, 0, VGA_SINK, 1);
    dmasg_direct_start(DMASG_BASE, VGA_CHANNEL, VGA_BUFFER_SIZE, 1);
}


