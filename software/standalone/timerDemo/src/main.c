#include <stdint.h>

#include <saxon.h>
#include "machineTimer.h"


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
	csr_write(mstatus, MSTATUS_MPP | MSTATUS_MIE); //Enable interrupts

	//configure timer
	scheduleTimer();
	csr_write(mie, MIE_MTIE); //Enable machine external interrupts
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
		default: crash(); break;
		}
	} else {
		crash();
	}
}

uint32_t counter = 0;
void timerInterrupt(){
	scheduleTimer();

	uart_write(UART_A, '0' + counter);
	uart_write(UART_A, '\n');
	if(++counter == 10) counter = 0;
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

