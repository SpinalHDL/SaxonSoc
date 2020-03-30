#include "type.h"

#include "bsp.h"
#include "bootloaderConfig.h"

void main() {
    bsp_init();
    bspMain();
}

