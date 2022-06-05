#include "soc.h"
#include "type.h"
#include "riscv.h"
#include "start.h"
#include "config.h"
#include "bsp.h"

//Example of expected STDOUT :
//
//smpDemo
//0312
//synced
//acdb


extern void smpInit();
void mainSmp();

//Stack space used by smpInit.S to provide stack to secondary harts
u8 hartStack[STACK_PER_HART*HART_COUNT] __attribute__((aligned(16)));


void main() {
    bsp_putString("smpDemo\n");
    smp_unlock(smpInit); //Will bring other harts to life. They will spawn smpInit (smpInit.S), which will redirect them to mainSmp (main.cpp)
    mainSmp();
}

volatile u32 hartCounter = 0; //Used as a syncronization barrier between all threads
volatile u32 ready = 0; //Flag used by hart 0 to notify the other harts that the "value" variable is loaded
char value = '?';

__inline__ s32 atomicAdd(s32 *a, u32 increment) {
    s32 old;
    __asm__ volatile(
          "amoadd.w %[old], %[increment], (%[atomic])"
        : [old] "=r"(old)
        : [increment] "r"(increment), [atomic] "r"(a)
        : "memory"
    );
    return old;
}

void mainSmp(){
    u32 hartId = csr_read(mhartid);
    bsp_putChar('0' + hartId);

    atomicAdd((s32*)&hartCounter, 1);
    while(hartCounter != HART_COUNT);

    //Hart 0 will provide a value to the other harts, other harts wait on it by pulling the "ready" variable
    if(hartId == 0){
        bsp_putString("\nsynced\n");
        value = 'a';
        asm("fence w,w");
        ready = 1;
        bsp_putChar(value + hartId);
    } else {
        int dummy = (volatile char)value; //Load "value" in the cache (for the example, to prove SMP)
        while(!ready);
        asm("fence r,r"); //Because of SMP we don't need to flush the cache manualy (flush value), but we need to enforce memory ordering via a fence
        bsp_putChar(value + hartId);
    }
}

