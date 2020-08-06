#include "type.h"

#include "bsp.h"
#include "dmasg.h"
#include "dmasgConfig.h"

#define BUFFER_SIZE 64
u32 buffer[2][BUFFER_SIZE];

void main() {
    bsp_putString("DMA demo");

    dmasg_push_memory(DMASG_BASE, DMASG_CHANNEL,  (u32)buffer[0], 16);
    dmasg_pop_memory (DMASG_BASE, DMASG_CHANNEL,  (u32)buffer[1], 16);
    dmasg_start(DMASG_BASE, DMASG_CHANNEL, BUFFER_SIZE*4, 0);
    while(dmasg_busy(DMASG_BASE, DMASG_CHANNEL));

    bsp_putString("done");
}

