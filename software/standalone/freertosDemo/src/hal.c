#include "FreeRTOS.h"
#include "task.h"
#include "portmacro.h"
#include "bsp.h"
#include "plic.h"

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
