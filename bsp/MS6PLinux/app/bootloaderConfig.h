#pragma once

#include "saxon.h"
#include "spiFlash.h"

#define SPI SYSTEM_SPI_B_APB

#define MACHINE_MODE_SBI_MEMORY 0x80000000
#define MACHINE_MODE_SBI_FLASH  0x00060000
#define MACHINE_MODE_SBI_SIZE   0x00020000

#define UBOOT_MEMORY    0x80200000
#define UBOOT_SBI_FLASH 0x00080000
#define UBOOT_SIZE      0x00080000

#if 1
void putString(char* s){
  while(*s){
    UART_A->DATA = *s;
    s++;
  }
}
void putHex(int value){
  for(int i = 7; i >=      0;i--){
    int hex = (value >> i*4) & 0xF;
    UART_A->DATA = (hex > 9 ? 'A' + hex - 10 : '0' + hex);
  }
}
#else
#define putString(x)
#define putHex(x)
#endif

void bspMain() {
  putString("Starting bootloader\n");
#ifndef SPINAL_SIM
  putHex(*(int *)MACHINE_MODE_SBI_MEMORY);putString("\n");
  spiFlash_init((void *)SPI, 0);
  spiFlash_wake((void *)SPI, 0);
  spiFlash_f2m((void *)SPI, 0, MACHINE_MODE_SBI_FLASH, MACHINE_MODE_SBI_MEMORY, MACHINE_MODE_SBI_SIZE);
  spiFlash_f2m((void *)SPI, 0, UBOOT_SBI_FLASH, UBOOT_MEMORY, UBOOT_SIZE);
  putHex(*(int *)MACHINE_MODE_SBI_MEMORY);putString("\n");
#endif
  putHex(MACHINE_MODE_SBI_MEMORY);putString("\n");
  putString("Calling userMain\n");
  void (*userMain)() = (void (*)())MACHINE_MODE_SBI_MEMORY;
  userMain();
}
