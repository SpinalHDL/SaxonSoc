#include "type.h"

#include "bsp.h"
#include "ethernetConfig.h"
#include "spi.h"
#include "gpio.h"
#include "mac.h"


#define read(reg) read_u32(0x10091000+reg*4)
#define write(data, reg) write_u32(data, 0x10091000+reg*4)


void main() {
    bsp_putString("demo");
    read(0);
    read(4);
    write(0x11223344,0);
    write(0x12345678,4);
    bsp_putString("done");
}

