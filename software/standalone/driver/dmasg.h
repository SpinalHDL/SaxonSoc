#pragma once

#include "type.h"
#include "io.h"


#define dmasg_ca(base, channel) (base + channel*0x80)
#define DMASG_CHANNEL_INPUT_ADDRESS 0x00
#define DMASG_CHANNEL_INPUT_STREAM 0x08
#define DMASG_CHANNEL_INPUT_CONFIG 0x0C
#define DMASG_CHANNEL_INPUT_CONFIG_MEMORY BIT_12
#define DMASG_CHANNEL_INPUT_CONFIG_STREAM 0
#define DMASG_CHANNEL_INPUT_CONFIG_COMPLETION_ON_PACKET BIT_13
#define DMASG_CHANNEL_INPUT_CONFIG_WAIT_ON_PACKET BIT_14

#define DMASG_CHANNEL_OUTPUT_ADDRESS 0x10
#define DMASG_CHANNEL_OUTPUT_STREAM 0x18
#define DMASG_CHANNEL_OUTPUT_CONFIG  0x1C
#define DMASG_CHANNEL_OUTPUT_CONFIG_MEMORY BIT_12
#define DMASG_CHANNEL_OUTPUT_CONFIG_STREAM 0
#define DMASG_CHANNEL_OUTPUT_CONFIG_LAST BIT_13

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
// Interrupt at the end of each descriptor
#define DMASG_CHANNEL_INTERRUPT_DESCRIPTOR_COMPLETION_MASK BIT_0
// Interrupt at the middle of each descriptor, require the half_completion_interrupt option to be enabled for the channel
#define DMASG_CHANNEL_INTERRUPT_DESCRIPTOR_COMPLETION_HALF_MASK BIT_1
// Interrupt when the channel is going off (not busy anymore)
#define DMASG_CHANNEL_INTERRUPT_CHANNEL_COMPLETION_MASK BIT_2
// Interrupt each time that a linked list's descriptor status field is updated
#define DMASG_CHANNEL_INTERRUPT_LINKED_LIST_UPDATE_MASK BIT_3
// Interrupt each time a S -> M  channel has done transferring a packet into the memory
#define DMASG_CHANNEL_INTERRUPT_INPUT_PACKET_MASK BIT_4

#define DMASG_CHANNEL_PROGRESS_BYTES 0x60
#define DMASG_CHANNEL_LINKED_LIST_HEAD 0x70

// Number of bytes (minus one) reserved at the descriptor FROM/TO addresses.
// If you want to transfer 10 bytes, this field should take the value 9
#define DMASG_DESCRIPTOR_CONTROL_BYTES 0x7FFFFFF
//Only for M -> S transfers, specify if a end of packet should be send at the end of the transfer
#define DMASG_DESCRIPTOR_CONTROL_END_OF_PACKET BIT_30

// Number of bytes transferred by the DMA for this descriptor.
#define DMASG_DESCRIPTOR_STATUS_BYTES 0x7FFFFFF
// Only for S -> M transfers, specify if the descriptor mark the end of a received packet
// Can be used when the dmasg_input_stream function is called with completion_on_packet set.
#define DMASG_DESCRIPTOR_STATUS_END_OF_PACKET BIT_30
// Specify if the descriptor was executed by the DMA.
// If the DMA read a completed descriptor, the channel is stopped and will produce a CHANNEL_COMPLETION interrupt.
#define DMASG_DESCRIPTOR_STATUS_COMPLETED BIT_31

// Should be aligned to 64 bytes !
struct dmasg_descriptor {
   // See all DMASG_DESCRIPTOR_STATUS_* defines
   // Updated by the DMA at the end of each descriptor and when a S -> M packet is completely transferred into memory
   u32 status;
   // See all DMASG_DESCRIPTOR_CONTROL_* defines
   u32 control;
   // For M -> ? transfers, memory address of the input data
   u64 from;
   // For ? -> M transfers, memory address of the output data
   u64 to;
   // Memory address of the next descriptor
   u64 next;
};

// byte_per_burst need to be a power of two, can be set to zero if the channel has hardcoded burst length
static void dmasg_input_memory(u32 base, u32 channel, u32 address, u32 byte_per_burst){
    u32 ca = dmasg_ca(base, channel);
    write_u32(address, ca + DMASG_CHANNEL_INPUT_ADDRESS);
    write_u32(DMASG_CHANNEL_INPUT_CONFIG_MEMORY | (byte_per_burst-1 & 0xFFF), ca + DMASG_CHANNEL_INPUT_CONFIG);
}

// byte_per_burst need to be a power of two, can be set to zero if the channel has hardcoded burst length
static void dmasg_output_memory(u32 base, u32 channel, u32 address, u32 byte_per_burst){
    u32 ca = dmasg_ca(base, channel);
    write_u32(address, ca + DMASG_CHANNEL_OUTPUT_ADDRESS);
    write_u32(DMASG_CHANNEL_OUTPUT_CONFIG_MEMORY | (byte_per_burst-1 & 0xFFF), ca + DMASG_CHANNEL_OUTPUT_CONFIG);
}

// port identify which physical input port should be used. ex : If a port can be accessed by 4 channels, and you are the second of those channel, then port=1
// wait_on_packet ensure the channel wait the beggining of a packet before capturing the data (avoid desync)
// completion_on_packet will limit the descriptor to only contains one packet and force its completion when it is fully transferred into memory.
static void dmasg_input_stream(u32 base, u32 channel, u32 port, u32 wait_on_packet, u32 completion_on_packet){
    u32 ca = dmasg_ca(base, channel);
    write_u32(port << 0, ca + DMASG_CHANNEL_INPUT_STREAM);
    write_u32(DMASG_CHANNEL_INPUT_CONFIG_STREAM | (completion_on_packet ? DMASG_CHANNEL_INPUT_CONFIG_COMPLETION_ON_PACKET : 0) | (wait_on_packet ? DMASG_CHANNEL_INPUT_CONFIG_WAIT_ON_PACKET : 0), ca + DMASG_CHANNEL_INPUT_CONFIG);
}

// port identify which physical output port should be used. ex : If a port can be accessed by 4 channels, and you are the second of those channel, then port=1
// source is equivalent to the AXI-Stream TID used in the packet
// sink is equivalent to the AXI-Stream TDEST used in the packet
// last (only for direct DMA control, not linked list) specify if a end of packet should be sent at the end of the transfer
static void dmasg_output_stream(u32 base, u32 channel, u32 port, u32 source, u32 sink, u32 last){
    u32 ca = dmasg_ca(base, channel);
    write_u32(port << 0 | source << 8 | sink << 16, ca + DMASG_CHANNEL_OUTPUT_STREAM);
    write_u32(DMASG_CHANNEL_OUTPUT_CONFIG_STREAM | (last ? DMASG_CHANNEL_OUTPUT_CONFIG_LAST : 0), ca + DMASG_CHANNEL_OUTPUT_CONFIG);
}

// Allow to start a channel without using linked list (direct control). Be sure the channel was enabled to support this mode.
// channel is the DMA channel ID to use.
// bytes is the size of the transfer
// self_restart allow the channel to operate into a circular mode. The DESCRIPTOR_COMPLETION_HALF interrupt can be usefull in that mode
static void dmasg_direct_start(u32 base, u32 channel, u32 bytes, u32 self_restart){
    u32 ca = dmasg_ca(base, channel);
    write_u32(bytes-1, ca + DMASG_CHANNEL_DIRECT_BYTES);
    write_u32(DMASG_CHANNEL_STATUS_DIRECT_START | (self_restart ? DMASG_CHANNEL_STATUS_SELF_RESTART : 0), ca + DMASG_CHANNEL_STATUS);
}

// Allow to start a channel using a linked list. Be sure the channel was enabled to support this mode.
// channel is the DMA channel ID to use.
// head specify the address of the linked list's first element. See dmasg_descriptor struct.
static void dmasg_linked_list_start(u32 base, u32 channel, u32 head){
    u32 ca = dmasg_ca(base, channel);
    write_u32((u32) head, ca + DMASG_CHANNEL_LINKED_LIST_HEAD);
    write_u32(DMASG_CHANNEL_STATUS_LINKED_LIST_START, ca + DMASG_CHANNEL_STATUS);
}

// Ask a channel to stop itself. None blocking, so you need to pull on dmasg_busy if you want to wait it to be effective.
// The status progress (bytes transfered) of the interrupted descriptor will be unknown
static void dmasg_stop(u32 base, u32 channel){
    u32 ca = dmasg_ca(base, channel);
    write_u32(DMASG_CHANNEL_STATUS_STOP, ca + DMASG_CHANNEL_STATUS);
}

// See all DMASG_CHANNEL_INTERRUPT_*_MASK defines for possible interrupts
// Multiple interrupts can be used at once
// This function clear all pending interrupts for the given channel before enabling the mask's interrupts
static void dmasg_interrupt_config(u32 base, u32 channel, u32 mask){
    u32 ca = dmasg_ca(base, channel);
    write_u32(0xFFFFFFFF, ca+DMASG_CHANNEL_INTERRUPT_PENDING);
    write_u32(mask, ca+DMASG_CHANNEL_INTERRUPT_ENABLE);
}

// clear the mask's interrupts, you can mask with 0xFFFFFFFF to clear them all
static void dmasg_interrupt_pending_clear(u32 base, u32 channel, u32 mask){
    u32 ca = dmasg_ca(base, channel);
    write_u32(mask, ca+DMASG_CHANNEL_INTERRUPT_PENDING);
}

// Check the status of the specified channel.
static u32 dmasg_busy(u32 base, u32 channel){
    u32 ca = dmasg_ca(base, channel);
    return read_u32(ca + DMASG_CHANNEL_STATUS) & DMASG_CHANNEL_STATUS_BUSY;
}

// Specify the buffer mapping of the given channel
// You don't need to use this function is the buffer address and buffer size are hardcoded in the hardware
static void dmasg_buffer(u32 base, u32 channel, u32 fifo_base, u32 fifo_bytes){
    u32 ca = dmasg_ca(base, channel);
    write_u32(fifo_base << 0 | fifo_bytes-1 << 16,  ca+DMASG_CHANNEL_FIFO);
}

static void dmasg_priority(u32 base, u32 channel, u32 priority){
    u32 ca = dmasg_ca(base, channel);
    write_u32(priority,  ca+DMASG_CHANNEL_PRIORITY);
}

// Snoop how many bytes were transferred for the current descriptor
static u32 dmasg_progress_bytes(u32 base, u32 channel){
    u32 ca = dmasg_ca(base, channel);
    return read_u32(ca + DMASG_CHANNEL_PROGRESS_BYTES);
}