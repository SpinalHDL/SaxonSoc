#include "FreeRTOS.h"
#include "task.h"
#include "portmacro.h"
#include "bsp.h"
#include "plic.h"


void hal_setup(){
    //configure PLIC
    plic_set_threshold(BSP_PLIC, BSP_PLIC_CPU_0, 0); //cpu 0 accept all interrupts with priority above 0

    //enable PLIC gatways as you wish here
    //plic_set_priority(..)
    //plic_set_enable(..)
}

void external_interrupt_handler(void){
    uint32_t claim;
    while(claim = plic_claim(BSP_PLIC, BSP_PLIC_CPU_0)){
        switch(claim){
//        case YOUR_INTERRUPT_ID:
//            //...
//            break;
        }
        plic_release(BSP_PLIC, BSP_PLIC_CPU_0, claim); //unmask the claimed interrupt
    }
}
