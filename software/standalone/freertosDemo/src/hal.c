#include "FreeRTOS.h"
#include "task.h"
#include "portmacro.h"
#include "saxon.h"
#include "freertosHalConfig.h"
#include "plic.h"

#define uxTimerIncrementsForOneTick (( size_t ) ( configCPU_CLOCK_HZ / configTICK_RATE_HZ )) /* Assumes increment won't go over 32-bits. */

extern uint64_t ullNextTime;
extern volatile uint64_t * pullMachineTimerCompareRegister;
void vPortSetupTimerInterrupt( void ){
    uint32_t ulCurrentTimeHigh, ulCurrentTimeLow;
    volatile uint32_t * const pulTimeHigh = ( volatile uint32_t * const ) ( MACHINE_TIMER + 0x4 );
    volatile uint32_t * const pulTimeLow = ( volatile uint32_t * const ) ( MACHINE_TIMER + 0x0 );

	pullMachineTimerCompareRegister  = (volatile uint64_t *) (MACHINE_TIMER + 0x8);

	do
	{
		ulCurrentTimeHigh = *pulTimeHigh;
		ulCurrentTimeLow = *pulTimeLow;
	} while( ulCurrentTimeHigh != *pulTimeHigh );

	ullNextTime = ( uint64_t ) ulCurrentTimeHigh;
	ullNextTime <<= 32ULL;
	ullNextTime |= ( uint64_t ) ulCurrentTimeLow;
	ullNextTime += ( uint64_t ) uxTimerIncrementsForOneTick;
	*pullMachineTimerCompareRegister = ullNextTime;

	/* Prepare the time to use after the next tick interrupt. */
	ullNextTime += ( uint64_t ) uxTimerIncrementsForOneTick;
    
    for(int i = 0;i < 4;i++) asm("nop"); //write propagation wait

	//configure PLIC
	plic_set_threshold(PLIC, PLIC_CPU_0, 0); //cpu 0 accept all interrupts with priority above 0
	plic_set_enable(PLIC, PLIC_CPU_0, PLIC_MACHINE_TIMER_ID, 1);
	plic_set_priority(PLIC, PLIC_MACHINE_TIMER_ID, 1);
}

void machine_timer_interrupt(){
	pullMachineTimerCompareRegister  = (volatile uint64_t *) (MACHINE_TIMER + 0x8);
	*pullMachineTimerCompareRegister = ullNextTime;
	ullNextTime += ( uint64_t ) uxTimerIncrementsForOneTick;

	BaseType_t needContextSwitch = xTaskIncrementTick();
	if(needContextSwitch) {
		vTaskSwitchContext();
	}
}

void external_interrupt_handler(void){
	uint32_t claim;
	while(claim = plic_claim(PLIC, PLIC_CPU_0)){
		switch(claim){
		case PLIC_MACHINE_TIMER_ID:
			machine_timer_interrupt();
			break;
		}
		plic_release(PLIC, PLIC_CPU_0, claim); //unmask the claimed interrupt
	}
}
