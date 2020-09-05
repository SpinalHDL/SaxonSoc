#include "type.h"

#include "bsp.h"
#include "dmasg.h"
#include "plic.h"
#include "riscv.h"
#include "dmasgConfig.h"


void trap_entry();
void trap();
void externalInterrupt();
void crash();

#define BUFFER_SIZE 64
u8 source_buffer[BUFFER_SIZE];
u8 destination_buffer[BUFFER_SIZE];

volatile u32 dmasg_completion; //Need to be volatile as we do active pulling on it for the demo.

void init(){
    //configure PLIC
    plic_set_threshold(BSP_PLIC, BSP_PLIC_CPU_0, 0); //cpu 0 accept all interrupts with priority above 0

    //enable PLIC DMASG channel 0 interrupt listening (But for the demo, we enable the DMASG internal interrupts later)
    plic_set_enable(BSP_PLIC, BSP_PLIC_CPU_0, PLIC_DMASG_CHANNEL, 1);
    plic_set_priority(BSP_PLIC, PLIC_DMASG_CHANNEL, 1);

    //enable interrupts
    csr_write(mtvec, trap_entry); //Set the machine trap vector (../common/trap.S)
    csr_set(mie, MIE_MEIE); //Enable external interrupts
    csr_write(mstatus, MSTATUS_MPP | MSTATUS_MIE);
}

void flush_data_cache(){
    asm(".word(0x500F)");
}

struct dmasg_descriptor descriptors[3];

void main() {
    bsp_putString("\nDMA demo\n");
    init();

    for(u32 i = 0; i < BUFFER_SIZE; i++){
        source_buffer[i] = i;
        destination_buffer[i] = 0xFF;
    }

    // direct M -> M transfer using pulling to wait completion
    dmasg_input_memory(DMASG_BASE, DMASG_CHANNEL,  (u32)source_buffer, 16);
    dmasg_output_memory (DMASG_BASE, DMASG_CHANNEL,  (u32)destination_buffer, 16);
    dmasg_direct_start(DMASG_BASE, DMASG_CHANNEL, BUFFER_SIZE, 0);
    while(dmasg_busy(DMASG_BASE, DMASG_CHANNEL));
    flush_data_cache();
    bsp_putString("first transfer done\n");


    // direct M -> M transfer using interrupt to wait completion
    dmasg_input_memory(DMASG_BASE, DMASG_CHANNEL,  (u32)source_buffer, 16);
    dmasg_output_memory (DMASG_BASE, DMASG_CHANNEL,  (u32)destination_buffer, 16);
    dmasg_interrupt_config(DMASG_BASE, DMASG_CHANNEL, DMASG_CHANNEL_INTERRUPT_CHANNEL_COMPLETION_MASK); //Enable interrupt when the DMA finish its transfer
    dmasg_completion = 0; //Used for the demo purpose, allow to wait the interrupt by pulling this variable.
    dmasg_direct_start(DMASG_BASE, DMASG_CHANNEL, BUFFER_SIZE, 0);
    while(!dmasg_completion);
    flush_data_cache();
    bsp_putString("seconde transfer done\n");


    // linked list M -> M transfer using pulling to wait completion
    descriptors[0].control = BUFFER_SIZE/2-1; //Transfer the half of the buffer
    descriptors[0].from    = (u32) source_buffer;
    descriptors[0].to      = (u32) destination_buffer;
    descriptors[0].next    = (u32) (descriptors + 1);
    descriptors[0].status  = 0; //Clear the completion flag

    descriptors[1].control = BUFFER_SIZE/2-1; //Transfer the (second) half of the buffer
    descriptors[1].from    = (u32) source_buffer + BUFFER_SIZE/2;
    descriptors[1].to      = (u32) destination_buffer + BUFFER_SIZE/2;
    descriptors[1].next    = (u32) (descriptors + 2);
    descriptors[1].status  = 0; //Clear the completion flag

    descriptors[2].status  = DMASG_DESCRIPTOR_STATUS_COMPLETED; //Set the completion flag to stop the DMA

    dmasg_input_memory(DMASG_BASE, DMASG_CHANNEL, 0, 16); // (the address do not care as it will be loaded by the linked list
    dmasg_output_memory (DMASG_BASE, DMASG_CHANNEL, 0, 16);
    dmasg_linked_list_start(DMASG_BASE, DMASG_CHANNEL, (u32) descriptors);
    while(dmasg_busy(DMASG_BASE, DMASG_CHANNEL));
    flush_data_cache();
    bsp_putString("third transfer done\n");
}

//Called by trap_entry on both exceptions and interrupts events
void trap(){
    int32_t mcause = csr_read(mcause);
    int32_t interrupt = mcause < 0;    //Interrupt if true, exception if false
    int32_t cause     = mcause & 0xF;
    if(interrupt){
        switch(cause){
        case CAUSE_MACHINE_EXTERNAL: externalInterrupt(); break;
        default: crash(); break;
        }
    } else {
        crash();
    }
}


void externalInterrupt(){
    uint32_t claim;
    //While there is pending interrupts
    while(claim = plic_claim(BSP_PLIC, SYSTEM_PLIC_SYSTEM_CORES_CPU_0_EXTERNAL_INTERRUPT)){
        switch(claim){
        case PLIC_DMASG_CHANNEL:
            bsp_putString("DMASG interrupt\n");
            dmasg_interrupt_config(DMASG_BASE, DMASG_CHANNEL, 0); //Disable dmasg channel interrupt
            dmasg_completion = 1;
            break;
        default: crash(); break;
        }
        plic_release(BSP_PLIC, BSP_PLIC_CPU_0, claim); //unmask the claimed interrupt
    }
}

//Used on unexpected trap/interrupt codes
void crash(){
    bsp_putString("\n*** CRASH ***\n");
    while(1);
}
