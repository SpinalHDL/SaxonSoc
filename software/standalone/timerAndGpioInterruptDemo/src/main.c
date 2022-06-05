#include <stdint.h>

#include "bsp.h"
#include "clint.h"
#include "clint.h"
#include "riscv.h"
#include "gpio.h"
#include "plic.h"
#include "timerAndGpioInterruptDemo.h"

void init();
void main();
void trap();
void crash();
void trap_entry();
void timerInterrupt();
void externalInterrupt();
void initTimer();
void scheduleTimer();

#ifdef SPINAL_SIM
    #define TIMER_TICK_DELAY (BSP_CLINT_HZ/200) //Faster timer tick in simulation to avoid having to wait too long
#else
    #define TIMER_TICK_DELAY (BSP_CLINT_HZ)
#endif

void main() {
    init();

    bsp_putString("Hello world\n");
    while(1); //Idle
}


void init(){
    //configure PLIC
    plic_set_threshold(BSP_PLIC, BSP_PLIC_CPU_0, 0); //cpu 0 accept all interrupts with priority above 0

    //enable GPIO_A pin 0 rising edge interrupt
    plic_set_enable(BSP_PLIC, BSP_PLIC_CPU_0, PLIC_GPIO_A_0, 1);
    plic_set_priority(BSP_PLIC, PLIC_GPIO_A_0, 1);
    gpio_setInterruptRiseEnable(GPIO_A, 1); //Enable pin 1 rising edge interrupts

    //configure timer
    initTimer();

    //enable interrupts
    csr_write(mtvec, trap_entry); //Set the machine trap vector (../common/trap.S)
    csr_set(mie, MIE_MTIE | MIE_MEIE); //Enable machine timer and external interrupts
    csr_write(mstatus, MSTATUS_MPP | MSTATUS_MIE);
}

uint64_t timerCmp; //Store the next interrupt time

void initTimer(){
    timerCmp = clint_getTime(BSP_CLINT);
    scheduleTimer();
}

//Make the timer tick in 1 second. (if SPINAL_SIM=yes, then much faster for simulations reasons)
void scheduleTimer(){
    timerCmp += TIMER_TICK_DELAY;
    clint_setCmp(BSP_CLINT, timerCmp, 0);
}

//Called by trap_entry on both exceptions and interrupts events
void trap(){
    int32_t mcause = csr_read(mcause);
    int32_t interrupt = mcause < 0;    //Interrupt if true, exception if false
    int32_t cause     = mcause & 0xF;
    if(interrupt){
        switch(cause){
        case CAUSE_MACHINE_TIMER: timerInterrupt(); break;
        case CAUSE_MACHINE_EXTERNAL: externalInterrupt(); break;
        default: crash(); break;
        }
    } else {
        crash();
    }
}


void timerInterrupt(){
    static uint32_t counter = 0;

    scheduleTimer();

    bsp_putString("BSP_CLINT ");
    bsp_putChar('0' + counter);
    bsp_putChar('\n');
    if(++counter == 10) counter = 0;
}

void externalInterrupt(){
    uint32_t claim;
    //While there is pending interrupts
    while(claim = plic_claim(BSP_PLIC, BSP_PLIC_CPU_0)){
        switch(claim){
        case PLIC_GPIO_A_0: bsp_putString("PLIC_GPIO_A_0\n"); break;
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

