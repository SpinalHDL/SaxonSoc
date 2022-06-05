#include "hal.h"
#include "machineModeSbiConfig.h"
#include "bsp.h"
#include "soc.h"
#include "io.h"
#include "machineTimer.h"

void stopSim(){
    bsp_putString("\nmachineModeSbi exception\n");
    while(1);
}

void putC(char c){
    bsp_putChar(c);
}

s32 getC(){
    if(uart_readOccupancy(BSP_UART_TERMINAL) == 0) return -1;
    return uart_read(BSP_UART_TERMINAL);
}

u32 rdtime(){
    return machineTimer_getTimeLow(MACHINE_TIMER);
}

u32 rdtimeh(){
    return machineTimer_getTimeHigh(MACHINE_TIMER);
}

void setMachineTimerCmp(u64 cmp){
    machineTimer_setCmp(MACHINE_TIMER, cmp);
}

void halInit(){

}
