#include <stdint.h>

#include "saxon.h"
#include "machineTimer.h"
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
    #define TIMER_TICK_DELAY (MACHINE_TIMER_HZ/200) //Faster timer tick in simulation to avoid having to wait too long
#else
    #define TIMER_TICK_DELAY (MACHINE_TIMER_HZ)
#endif

void main() {
    init();

    uart_writeStr(UART_A, "Hello world\n");
    while(1); //Idle
}


void init(){
    //configure PLIC
    plic_set_threshold(PLIC, PLIC_CPU_0, 0); //cpu 0 accept all interrupts with priority above 0

    //enable GPIO_A pin 0 rising edge interrupt
    plic_set_enable(PLIC, PLIC_CPU_0, PLIC_GPIO_A_0, 1);
    plic_set_priority(PLIC, PLIC_GPIO_A_0, 1);
    GPIO_A->INTERRUPT_RISE_ENABLE = 1; //Enable pin 1 rising edge interrupts

    //configure timer
    initTimer();

    //enable interrupts
    csr_write(mtvec, trap_entry); //Set the machine trap vector (../common/trap.S)
    csr_set(mie, MIE_MTIE | MIE_MEIE); //Enable machine timer and external interrupts
    csr_write(mstatus, MSTATUS_MPP | MSTATUS_MIE);
}

uint64_t timerCmp; //Store the next interrupt time

void initTimer(){
    timerCmp = machineTimer_getTime(MACHINE_TIMER);
    scheduleTimer();
}

//Make the timer tick in 1 second. (if SPINAL_SIM=yes, then much faster for simulations reasons)
void scheduleTimer(){
    timerCmp += TIMER_TICK_DELAY;
    machineTimer_setCmp(MACHINE_TIMER, timerCmp);
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

    uart_writeStr(UART_A, "MACHINE_TIMER ");
    uart_write(UART_A, '0' + counter);
    uart_write(UART_A, '\n');
    if(++counter == 10) counter = 0;
}

void externalInterrupt(){
    uint32_t claim;
    //While there is pending interrupts
    while(claim = plic_claim(PLIC, PLIC_CPU_0)){
        switch(claim){
        case PLIC_GPIO_A_0: uart_writeStr(UART_A, "PLIC_GPIO_A_0\n"); break;
        default: crash(); break;
        }
        plic_release(PLIC, PLIC_CPU_0, claim); //unmask the claimed interrupt
    }
}

//Used on unexpected trap/interrupt codes
void crash(){
    uart_writeStr(UART_A, "\n*** CRASH ***\n");
    while(1);
}

