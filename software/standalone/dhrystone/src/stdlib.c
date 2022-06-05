// This is free and unencumbered software released into the public domain.
//
// Anyone is free to copy, modify, publish, use, compile, sell, or
// distribute this software, either in source code form or as a compiled
// binary, for any purpose, commercial or non-commercial, and by any
// means.

#include <stdarg.h>
#include <stdint.h>

void setStats(int enable)
{

}

#include "bsp.h"

#ifdef BSP_MACHINE_TIMER
long time(){
  return machineTimer_getTime(SYSTEM_MACHINE_TIMER_APB);
}
#else
#ifdef BSP_CLINT
long time(){
    return clint_getTime(BSP_CLINT);
}
#else
#include "dhrystoneHal.h"
#endif
#endif

static void printf_c(int c)
{
    putchar(c);
}

static void printf_s(char *p)
{
    while (*p)
        putchar(*(p++));
}

static void printf_d(int val)
{
    char buffer[32];
    char *p = buffer;
    if (val < 0) {
        printf_c('-');
        val = -val;
    }
    while (val || p == buffer) {
        *(p++) = '0' + val % 10;
        val = val / 10;
    }
    while (p != buffer)
        printf_c(*(--p));
}

int printf(const char *format, ...)
{
    int i;
    va_list ap;

    va_start(ap, format);

    for (i = 0; format[i]; i++)
        if (format[i] == '%') {
            while (format[++i]) {
                if (format[i] == 'c') {
                    printf_c(va_arg(ap,int));
                    break;
                }
                if (format[i] == 's') {
                    printf_s(va_arg(ap,char*));
                    break;
                }
                if (format[i] == 'd') {
                    printf_d(va_arg(ap,int));
                    break;
                }
            }
        } else
            printf_c(format[i]);

    va_end(ap);
}


int puts(char *s){
  while (*s) {
    putchar(*s);
    s++;
  }
  putchar('\n');
  return 0;
}

int putchar(int c){
    bsp_putChar(c);
    return c;
}


//See https://github.com/zephyrproject-rtos/meta-zephyr-sdk/issues/110
//It does not interfere with the benchmark code.
unsigned long long __divdi3 (unsigned long long numerator,unsigned  long long divisor)
{
    unsigned long long result = 0;
    unsigned long long count = 0;
    unsigned long long remainder = numerator;

    while((divisor & 0x8000000000000000ll) == 0) {
        divisor = divisor << 1;
        count++;
    }
    while(remainder != 0) {
        if(remainder >= divisor) {
            remainder = remainder - divisor;
            result = result | (1 << count);
        }
        if(count == 0) {
            break;
        }
        divisor = divisor >> 1;
        count--;
    }
    return result;
}
