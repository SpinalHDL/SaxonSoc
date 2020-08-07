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
u32 buffer[2][BUFFER_SIZE];

u32 dmasg_completion;

void init(){
    //configure PLIC
    plic_set_threshold(BSP_PLIC, BSP_PLIC_CPU_0, 0); //cpu 0 accept all interrupts with priority above 0

    //enable PLIC DMASG channel 0 interrupt listening (But for the demo, we enable the DMASG internal interrupts later)
    plic_set_enable(BSP_PLIC, BSP_PLIC_CPU_0, PLIC_DMASG_CHANNEL, 1);
    plic_set_priority(BSP_PLIC, PLIC_DMASG_CHANNEL, 1);

    //enable interrupts
    csr_write(mtvec, trap_entry); //Set the machine trap vector (../common/trap.S)
    csr_set(mie, MIE_MTIE | MIE_MEIE); //Enable machine timer and external interrupts
    csr_write(mstatus, MSTATUS_MPP | MSTATUS_MIE);
}


void main() {
    bsp_putString("DMA demo");

    // M -> M transfer using pulling to wait completion
    dmasg_push_memory(DMASG_BASE, DMASG_CHANNEL,  (u32)buffer[0], 16);
    dmasg_pop_memory (DMASG_BASE, DMASG_CHANNEL,  (u32)buffer[1], 16);
    dmasg_start(DMASG_BASE, DMASG_CHANNEL, BUFFER_SIZE*4, 0);
    while(dmasg_busy(DMASG_BASE, DMASG_CHANNEL));
    bsp_putString("first transfer done");

    dmasg_push_memory(DMASG_BASE, DMASG_CHANNEL,  (u32)buffer[0], 16);
    dmasg_pop_memory (DMASG_BASE, DMASG_CHANNEL,  (u32)buffer[1], 16);
    dmasg_interrupt_config(DMASG_BASE, DMASG_CHANNEL, DMASG_CHANNEL_INTERRUPT_COMPLETION_MASK); //Enable interrupt when the DMA finish its transfer
    dmasg_completion = 0; //Used for the demo purpose, allow to wait the interrupt by pulling this variable.
    dmasg_start(DMASG_BASE, DMASG_CHANNEL, BUFFER_SIZE*4, 0);
    while(!dmasg_completion);
    bsp_putString("seconde transfer done");

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
    while(claim = plic_claim(BSP_PLIC, PLIC_DMASG_CHANNEL)){
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
