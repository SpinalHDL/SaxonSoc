#include "type.h"

#include "bsp.h"
#include "stdio.h"
#include "gpio.h"
#include "math.h"


void _sbrk(){ bsp_putString("_sbrk\n"); }
void _exit(int a){ bsp_putString("_exit\n"); while(1); }
void _kill(){ bsp_putString("_kill\n"); }
void _isatty(){ bsp_putString("_isatty\n"); }
void _getpid(){ bsp_putString("_getpid\n"); }
void _write(){ bsp_putString("_write\n"); }
void _close(){ bsp_putString("_close\n"); }
void _fstat(){ bsp_putString("_fstat\n"); }
void _lseek(){ bsp_putString("_lseek\n"); }
void _read(){ bsp_putString("_read\n"); }

void main() {
    bsp_putString("Fpu demo\n");

    volatile int a = 42;
    volatile float b = a;
    volatile double d;
    volatile int c = b;
    char str[100];

    gpio_setOutput(SYSTEM_GPIO_A_CTRL, 0);


    b = M_PI/7 + M_PI*14;
    asm("nop");
    a = *((u32*) &b);
    asm("nop");
    gpio_setOutput(SYSTEM_GPIO_A_CTRL, a);
    asm("nop");
    gpio_setOutput(SYSTEM_GPIO_A_CTRL, 1);
    b = sinf(b);
    gpio_setOutput(SYSTEM_GPIO_A_CTRL, 2);
    a = *((u32*) &b);
    gpio_setOutput(SYSTEM_GPIO_A_CTRL, a);
    gpio_setOutput(SYSTEM_GPIO_A_CTRL, 3);
    d = M_PI/7 + M_PI*14;
    d = sin(d);

//    sprintf(str, "miaou\n"); bsp_putString(str);
//
//    gpio_setOutput(SYSTEM_GPIO_A_CTRL, 1);
//    sprintf(str, "%f\n",b); bsp_putString(str);
//    gpio_setOutput(SYSTEM_GPIO_A_CTRL, 2);
//    b += 3.11f;
//    gpio_setOutput(SYSTEM_GPIO_A_CTRL, 3);
//    sprintf(str, "%f\n",b); bsp_putString(str);
//    gpio_setOutput(SYSTEM_GPIO_A_CTRL, 4);

    bsp_putString("done\n");
    gcvt(b, 15,str);  bsp_putString(str); bsp_putChar('\n');
    gcvt(d, 15,str);  bsp_putString(str); bsp_putChar('\n');
//    gcvt(b, 6,str);  bsp_putString(str);
//    gcvt(b, 6,str);  bsp_putString(str);
//    gcvt(b, 6,str);  bsp_putString(str);
//    sprintf(str, "%f\n",d); bsp_putString(str);
//    sprintf(str, "%f\n",b); bsp_putString(str);
}

