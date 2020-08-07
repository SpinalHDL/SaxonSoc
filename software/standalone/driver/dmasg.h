#pragma once

#include "type.h"
#include "io.h"


#define dmasg_ca(base, channel) (base + channel*0x80)
#define DMASG_CHANNEL_PUSH_ADDRESS 0x00
#define DMASG_CHANNEL_PUSH_STREAM 0x08
#define DMASG_CHANNEL_PUSH_CONFIG 0x0C
#define DMASG_CHANNEL_PUSH_CONFIG_MEMORY BIT12
#define DMASG_CHANNEL_PUSH_CONFIG_STREAM 0

#define DMASG_CHANNEL_POP_ADDRESS 0x10
#define DMASG_CHANNEL_POP_STREAM 0x18
#define DMASG_CHANNEL_POP_CONFIG  0x1C
#define DMASG_CHANNEL_POP_CONFIG_MEMORY BIT12
#define DMASG_CHANNEL_POP_CONFIG_STREAM 0
#define DMASG_CHANNEL_POP_CONFIG_LAST BIT13

#define DMASG_CHANNEL_BYTES 0x20
#define DMASG_CHANNEL_STATUS 0x2C
#define DMASG_CHANNEL_STATUS_START BIT0
#define DMASG_CHANNEL_STATUS_BUSY BIT0
#define DMASG_CHANNEL_STATUS_SELF_RESTART BIT1

#define DMASG_CHANNEL_FIFO 0x40
#define DMASG_CHANNEL_PRIORITY 0x40

#define DMASG_CHANNEL_INTERRUPT_ENABLE 0x50
#define DMASG_CHANNEL_INTERRUPT_PENDING 0x54
#define DMASG_CHANNEL_INTERRUPT_COMPLETION_MASK = BIT0
#define DMASG_CHANNEL_INTERRUPT_HALF_COMPLETION_MASK = BIT1

static void dmasg_push_memory(u32 base, u32 channel, u32 address, u32 byte_per_burst){
    u32 ca = dmasg_ca(base, channel);
    write_u32(address, ca + DMASG_CHANNEL_PUSH_ADDRESS);
    write_u32(DMASG_CHANNEL_PUSH_CONFIG_MEMORY | (byte_per_burst-1 & 0xFFF), ca + DMASG_CHANNEL_PUSH_CONFIG);
}

static void dmasg_pop_memory(u32 base, u32 channel, u32 address, u32 byte_per_burst){
    u32 ca = dmasg_ca(base, channel);
    write_u32(address, ca + DMASG_CHANNEL_POP_ADDRESS);
    write_u32(DMASG_CHANNEL_POP_CONFIG_MEMORY | (byte_per_burst-1 & 0xFFF), ca + DMASG_CHANNEL_POP_CONFIG);
}

static void dmasg_push_stream(u32 base, u32 channel, u32 port, u32 source, u32 sink){
    u32 ca = dmasg_ca(base, channel);
    write_u32(port << 0 | source << 8 | sink << 16, ca + DMASG_CHANNEL_PUSH_STREAM);
    write_u32(DMASG_CHANNEL_PUSH_CONFIG_STREAM, ca + DMASG_CHANNEL_PUSH_CONFIG);
}

static void dmasg_pop_stream(u32 base, u32 channel, u32 port, u32 source, u32 sink, u32 last){
    u32 ca = dmasg_ca(base, channel);
    write_u32(port << 0 | source << 8 | sink << 16, ca + DMASG_CHANNEL_POP_STREAM);
    write_u32(DMASG_CHANNEL_POP_CONFIG_STREAM | (last ? DMASG_CHANNEL_POP_CONFIG_LAST : 0), ca + DMASG_CHANNEL_POP_CONFIG);
}

static void dmasg_start(u32 base, u32 channel, u32 bytes, u32 self_restart){
    u32 ca = dmasg_ca(base, channel);
    write_u32(bytes-1, ca + DMASG_CHANNEL_BYTES);
    write_u32(DMASG_CHANNEL_STATUS_START | (self_restart ? DMASG_CHANNEL_STATUS_SELF_RESTART : 0), ca + DMASG_CHANNEL_STATUS);
}

static void dmasg_interrupt_config(u32 base, u32 channel, u32 mask){
    u32 ca = dmasg_ca(base, channel);
    write_u32(mask, ca+DMASG_CHANNEL_INTERRUPT_PENDING);
    write_u32(mask, ca+DMASG_CHANNEL_INTERRUPT_ENABLE);
}

static void dmasg_interrupt_pending_clear(u32 base, u32 channel, u32 mask){
    u32 ca = dmasg_ca(base, channel);
    write_u32(mask, ca+DMASG_CHANNEL_INTERRUPT_PENDING);
}


static u32 dmasg_busy(u32 base, u32 channel){
    u32 ca = dmasg_ca(base, channel);
    return read_u32(ca + DMASG_CHANNEL_STATUS) & DMASG_CHANNEL_STATUS_BUSY;
}

static void dmasg_fifo(u32 base, u32 channel, u32 fifo_base, u32 fifo_bytes){
    u32 ca = dmasg_ca(base, channel);
    write_u32(fifo_base << 0 | fifo_bytes-1 << 16,  ca+DMASG_CHANNEL_FIFO);
}

static void dmasg_priority(u32 base, u32 channel, u32 priority){
    u32 ca = dmasg_ca(base, channel);
    write_u32(priority,  ca+DMASG_CHANNEL_PRIORITY);
}
