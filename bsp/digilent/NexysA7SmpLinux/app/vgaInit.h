#pragma once

#include "bsp.h"
#include "dmasg.h"
#include "vga.h"

#define DMASG_BASE           SYSTEM_DMA_CTRL

#define VGA_BASE             SYSTEM_VGA_CTRL
#define VGA_CHANNEL          0
#define VGA_PORT             0
#define VGA_SINK             0

#ifdef SPINAL_SIM
    #define VGA_TIMINGS      vga_simRes_h160_v120
    #define VGA_WIDTH        160
    #define VGA_HEIGHT       120
#else
    #define VGA_TIMINGS      vga_h640_v480_r60
    #define VGA_WIDTH        640
    #define VGA_HEIGHT       480
#endif

#define VGA_BYTE_PER_PIXEL   2
#define VGA_BUFFER           0x87000000
#define VGA_BUFFER_SIZE     (VGA_WIDTH * VGA_HEIGHT * VGA_BYTE_PER_PIXEL)

/* Dreaw R,G,B,W colors in the four quadrants using RGB565 */
void fillFrameBuffer(){
    for(u32 y = 0;y < VGA_HEIGHT;y++){
        for(u32 x = 0;x < VGA_WIDTH;x++){
            ((u16*)VGA_BUFFER)[y*VGA_WIDTH + x] = (y < VGA_HEIGHT/2 ?
                (x < VGA_WIDTH/2 ? 0xf800 : 0x07e0) :
                (x < VGA_WIDTH/2 ? 0x001f : 0xffff));
        }
    }
}

void vgaInit(){
    bsp_putString("VGA init\n");
    fillFrameBuffer();

    vga_set_timing(VGA_BASE, VGA_TIMINGS);
    vga_start(VGA_BASE);

    dmasg_input_memory(DMASG_BASE, VGA_CHANNEL,  (u32)VGA_BUFFER, 0);
    dmasg_output_stream(DMASG_BASE, VGA_CHANNEL, VGA_PORT, 0, VGA_SINK, 1);
    dmasg_direct_start(DMASG_BASE, VGA_CHANNEL, VGA_BUFFER_SIZE, 1);
}
