#include <stdint.h>

#include "bsp.h"
#include "gpio.h"
#include "uart.h"

#define MEMORY 0x80000000

void main() {
    bsp_putString("h\n");
    write_u32(0x12345678, SYSTEM_LAB_CTRL);
    for(u32 i = 0;i < 100;i++){
        write_u32(i, SYSTEM_LAB_CTRL);
    }
    write_u32(read_u32(SYSTEM_LAB_CTRL)+1000, SYSTEM_LAB_CTRL);

    write_u32(0x11223344, MEMORY + 0x10000+8);
    write_u32(0x55667788, MEMORY + 0x10000);
    write_u32(0xAABBCCDD, MEMORY + 0x10000-8);

    asm("fence w, r");

    bsp_putString("\n");
    bsp_putU32(read_u32(MEMORY + 0x10000+8)); //112233BB

    bsp_putString("\n");
    bsp_putU32(read_u32(MEMORY + 0x10000));   //55667777

    bsp_putString("\n");
    bsp_putU32(read_u32(MEMORY + 0x10000-8)); //AABBCCDD
    bsp_putString("\ndone\n");
}

