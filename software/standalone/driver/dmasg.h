#pragma once

#include "type.h"
#include "io.h"


#define dmasg_ca(base, channel) (base + channel*0x80)
#define DMASG_CHANNEL_PUSH_ADDRESS 0x00
#define DMASG_CHANNEL_PUSH_STREAM 0x08
#define DMASG_CHANNEL_PUSH_CONFIG 0x0C
#define DMASG_CHANNEL_PUSH_CONFIG_MEMORY BIT_12
#define DMASG_CHANNEL_PUSH_CONFIG_STREAM 0
#define DMASG_CHANNEL_PUSH_CONFIG_COMPLETION_ON_PACKET BIT_13
#define DMASG_CHANNEL_PUSH_CONFIG_WAIT_ON_PACKET BIT_14

#define DMASG_CHANNEL_POP_ADDRESS 0x10
#define DMASG_CHANNEL_POP_STREAM 0x18
#define DMASG_CHANNEL_POP_CONFIG  0x1C
#define DMASG_CHANNEL_POP_CONFIG_MEMORY BIT_12
#define DMASG_CHANNEL_POP_CONFIG_STREAM 0
#define DMASG_CHANNEL_POP_CONFIG_LAST BIT_13

#define DMASG_CHANNEL_DIRECT_BYTES 0x20
#define DMASG_CHANNEL_STATUS 0x2C
#define DMASG_CHANNEL_STATUS_DIRECT_START BIT_0
#define DMASG_CHANNEL_STATUS_BUSY BIT_0
#define DMASG_CHANNEL_STATUS_SELF_RESTART BIT_1
#define DMASG_CHANNEL_STATUS_STOP BIT_2
#define DMASG_CHANNEL_STATUS_LINKED_LIST_START BIT_4

#define DMASG_CHANNEL_FIFO 0x40
#define DMASG_CHANNEL_PRIORITY 0x44

#define DMASG_CHANNEL_INTERRUPT_ENABLE 0x50
#define DMASG_CHANNEL_INTERRUPT_PENDING 0x54
#define DMASG_CHANNEL_INTERRUPT_DESCRIPTOR_COMPLETION_MASK BIT_0
#define DMASG_CHANNEL_INTERRUPT_DESCRIPTOR_COMPLETION_HALF_MASK BIT_1
#define DMASG_CHANNEL_INTERRUPT_CHANNEL_COMPLETION_MASK BIT_2
#define DMASG_CHANNEL_INTERRUPT_LINKED_LIST_UPDATE_MASK BIT_3
#define DMASG_CHANNEL_INTERRUPT_INPUT_PACKET_MASK BIT_4

#define DMASG_CHANNEL_PROGRESS_BYTES 0x60

#define DMASG_CHANNEL_LINKED_LIST_HEAD 0x70


#define DMASG_DESCRIPTOR_CONTROL_BYTES 0x7FFFFFF  // !! Minus one !!
#define DMASG_DESCRIPTOR_CONTROL_END_OF_PACKET BIT_30

#define DMASG_DESCRIPTOR_STATUS_BYTES 0x7FFFFFF
#define DMASG_DESCRIPTOR_STATUS_END_OF_PACKET BIT_30
#define DMASG_DESCRIPTOR_STATUS_COMPLETED BIT_31

struct dmasg_descriptor {
   u64 from;
   u64 to;
   u64 next;
   u32 control;
   u32 status;
};

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

static void dmasg_push_stream(u32 base, u32 channel, u32 port, u32 wait_on_packet, u32 completion_on_packet){
    u32 ca = dmasg_ca(base, channel);
    write_u32(port << 0, ca + DMASG_CHANNEL_PUSH_STREAM);
    write_u32(DMASG_CHANNEL_PUSH_CONFIG_STREAM | (completion_on_packet ? DMASG_CHANNEL_PUSH_CONFIG_COMPLETION_ON_PACKET : 0) | (wait_on_packet ? DMASG_CHANNEL_PUSH_CONFIG_WAIT_ON_PACKET : 0), ca + DMASG_CHANNEL_PUSH_CONFIG);
}

static void dmasg_pop_stream(u32 base, u32 channel, u32 port, u32 source, u32 sink, u32 last){
    u32 ca = dmasg_ca(base, channel);
    write_u32(port << 0 | source << 8 | sink << 16, ca + DMASG_CHANNEL_POP_STREAM);
    write_u32(DMASG_CHANNEL_POP_CONFIG_STREAM | (last ? DMASG_CHANNEL_POP_CONFIG_LAST : 0), ca + DMASG_CHANNEL_POP_CONFIG);
}

static void dmasg_direct_start(u32 base, u32 channel, u32 bytes, u32 self_restart){
    u32 ca = dmasg_ca(base, channel);
    write_u32(bytes-1, ca + DMASG_CHANNEL_DIRECT_BYTES);
    write_u32(DMASG_CHANNEL_STATUS_DIRECT_START | (self_restart ? DMASG_CHANNEL_STATUS_SELF_RESTART : 0), ca + DMASG_CHANNEL_STATUS);
}

static void dmasg_linked_list_start(u32 base, u32 channel, u32 head){
    u32 ca = dmasg_ca(base, channel);
    write_u32((u32) head, ca + DMASG_CHANNEL_LINKED_LIST_HEAD);
    write_u32(DMASG_CHANNEL_STATUS_LINKED_LIST_START, ca + DMASG_CHANNEL_STATUS);
}

static void dmasg_interrupt_config(u32 base, u32 channel, u32 mask){
    u32 ca = dmasg_ca(base, channel);
    write_u32(0xFFFFFFFF, ca+DMASG_CHANNEL_INTERRUPT_PENDING);
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

static u32 dmasg_progress_bytes(u32 base, u32 channel){
    u32 ca = dmasg_ca(base, channel);
    return read_u32(ca + DMASG_CHANNEL_PROGRESS_BYTES);
}