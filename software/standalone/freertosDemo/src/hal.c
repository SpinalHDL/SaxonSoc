#include "FreeRTOS.h"
#include "task.h"
#include "portmacro.h"
#include "bsp.h"
#include "plic.h"

#define uxTimerIncrementsForOneTick (( size_t ) ( configCPU_CLOCK_HZ / configTICK_RATE_HZ )) /* Assumes increment won't go over 32-bits. */

extern uint64_t ullNextTime;
extern volatile uint64_t * pullMachineTimerCompareRegister;
void vPortSetupTimerInterrupt( void ){
    uint32_t ulCurrentTimeHigh, ulCurrentTimeLow;
    volatile uint32_t * const pulTimeHigh = ( volatile uint32_t * const ) ( BSP_MACHINE_TIMER + 0x4 );
    volatile uint32_t * const pulTimeLow = ( volatile uint32_t * const ) ( BSP_MACHINE_TIMER + 0x0 );

    pullMachineTimerCompareRegister  = (volatile uint64_t *) (BSP_MACHINE_TIMER + 0x8);

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
    plic_set_threshold(BSP_PLIC, BSP_PLIC_CPU_0, 0); //cpu 0 accept all interrupts with priority above 0
    plic_set_enable(BSP_PLIC, BSP_PLIC_CPU_0, BSP_MACHINE_TIMER_PLIC_ID, 1);
    plic_set_priority(BSP_PLIC, BSP_MACHINE_TIMER_PLIC_ID, 1);
}

void BSP_MACHINE_TIMER_interrupt(){
    pullMachineTimerCompareRegister  = (volatile uint64_t *) (BSP_MACHINE_TIMER + 0x8);
    *pullMachineTimerCompareRegister = ullNextTime;
    ullNextTime += ( uint64_t ) uxTimerIncrementsForOneTick;

    BaseType_t needContextSwitch = xTaskIncrementTick();
    if(needContextSwitch) {
        vTaskSwitchContext();
    }
}

void external_interrupt_handler(void){
    uint32_t claim;
    while(claim = plic_claim(BSP_PLIC, BSP_PLIC_CPU_0)){
        switch(claim){
        case BSP_MACHINE_TIMER_PLIC_ID:
            BSP_MACHINE_TIMER_interrupt();
            break;
        }
        plic_release(BSP_PLIC, BSP_PLIC_CPU_0, claim); //unmask the claimed interrupt
    }
}
