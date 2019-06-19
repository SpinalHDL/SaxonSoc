#include <stdint.h>

#include <saxon.h>
#include "machineTimer.h"
#include "plic.h"


void init();
void main();
void trap();
void crash();
void trap_entry();
void timerInterrupt();
void scheduleTimer();



void init(){
	//configure RISC-V interrupt CSR
	csr_write(mtvec, trap_entry); //Set the machine trap vector (trap.S)

	//configure timer
	scheduleTimer();
	csr_set(mie, MIE_MTIE); //Enable machine timer interrupts

	//configure PLIC
	plic_set_threshold(PLIC, PLIC_CPU_0, 0); //cpu 0 accept all interrupts with priority above 0
	csr_set(mie, MIE_MEIE); //Enable machine external interrupts

	//enable GPIO_A pin 0 rising edge interrupt
	plic_set_enable(PLIC, PLIC_CPU_0, PLIC_GPIO_A_0, 1);
	plic_set_priority(PLIC, PLIC_GPIO_A_0, 1);
	GPIO_A->INTERRUPT_RISE_ENABLE = 1; //Enable pin 1 rising edge interrupts

	//enable interrupts
	csr_write(mstatus, MSTATUS_MPP | MSTATUS_MIE);
}

void main() {
	init();

	uart_writeStr(UART_A, "Hello world\n");
	while(1); //Idle
}

//Called by trap_entry on both exceptions and interrupts events
void trap(){
	int32_t mcause = csr_read(mcause);
	int32_t interrupt = mcause < 0;    //Interrupt if set, exception if cleared
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

uint32_t counter = 0;
void timerInterrupt(){
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



void scheduleTimer(){
	uint64_t time = machineTimer_getTime(MACHINE_TIMER);
	uint64_t cmp = time + MACHINE_TIMER_HZ;
	machineTimer_setCmp(MACHINE_TIMER, cmp);
}


void crash(){
	uart_writeStr(UART_A, "\n*** CRASH ***\n");
	while(1);
}

