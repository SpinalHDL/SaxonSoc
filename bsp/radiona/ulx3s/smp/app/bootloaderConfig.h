#pragma once

#include "bsp.h"
#include "start.h"
#include "sdram.h"
#include "spiFlash.h"
#include "vgaInit.h"

#define SDRAM_CTRL SYSTEM_SDRAM_A_CTRL
#define SDRAM_PHY  SDRAM_DOMAIN_PHY_A_CTRL
#define SDRAM_BASE SYSTEM_SDRAM_A0_BMB

#define SPI SYSTEM_SPI_A_CTRL
#define SPI_CS 0

#define OPENSBI_MEMORY 0x80F80000
#define OPENSBI_FLASH  0x00340000
#define OPENSBI_SIZE      0x40000

#define UBOOT_MEMORY     0x80F00000
#define UBOOT_SBI_FLASH  0x00380000
#define UBOOT_SIZE          0x80000

#define RL 3
#define WL 0
#define CTRL_BURST_LENGHT 1
#define PHY_CLK_RATIO 2

#ifndef SDRAM_TIMING
#error "You need to define the SDRAM_TIMING via the makefile CFLAGS_ARGS"
//Ex : make clean all BSP=Ulx3sLinuxUboot CFLAGS_ARGS="-DSDRAM_TIMING=MT48LC16M16A2_6A_ps"
//Ex : make clean all BSP=Ulx3sLinuxUboot CFLAGS_ARGS="-DSDRAM_TIMING=AS4C32M16SB_7TCN_ps"
#endif

void putHexU32(int value){
    for(int i = 7; i >= 0;i--){
        int hex = (value >> i*4) & 0xF;
        bsp_putChar(hex > 9 ? 'A' + hex - 10 : '0' + hex);
    }
}

void putHexU8(u8 value){
    for(int i = 1; i >= 0;i--){
        int hex = (value >> i*4) & 0xF;
        bsp_putChar(hex > 9 ? 'A' + hex - 10 : '0' + hex);
    }
}

void bspMain() {
    bsp_uDelay(5000);
    bsp_putString("\n");
    bsp_putString("SDRAM init\n");

    sdram_init(
        SDRAM_CTRL,
        RL,
        WL,
        SDRAM_TIMING,
        CTRL_BURST_LENGHT,
        PHY_CLK_RATIO,
        20000
    );

    sdram_sdr_init(
        SDRAM_CTRL,
        RL,
        CTRL_BURST_LENGHT,
        PHY_CLK_RATIO
    );

#ifndef SPINAL_SIM
    while(1){
        bsp_putString("Mem test .. ");
        sdram_mem_init(SDRAM_BASE, 0x100000);
        if(!sdram_mem_test(SDRAM_BASE, 0x100000)) {
            bsp_putString("pass\n");
            break;
        }

        bsp_putString("failure\n");
        bsp_uDelay(1000000);
    }

    spiFlash_init(SPI, SPI_CS);
    spiFlash_wake(SPI, SPI_CS);
    spiFlash_software_reset(SPI, SPI_CS);
    bsp_putString("Flash ID : 0x"); putHexU8(spiFlash_read_id(SPI, SPI_CS)); bsp_putChar('\n');
    
    bsp_putString("OpenSBI copy\n");
    spiFlash_f2m(SPI, SPI_CS, OPENSBI_FLASH, OPENSBI_MEMORY, OPENSBI_SIZE);
    bsp_putString("U-Boot copy\n");
    spiFlash_f2m(SPI, SPI_CS, UBOOT_SBI_FLASH, UBOOT_MEMORY, UBOOT_SIZE);

    bsp_putString("Image check .. ");
    if(((u32*) OPENSBI_MEMORY)[0] != 0x00050433 || ((u32*) OPENSBI_MEMORY)[1] != 0x000584b3) {
        bsp_putString("OpenSBI missmatch\n");
        putHexU32(((u32*) OPENSBI_MEMORY)[0]); bsp_putChar(' '); putHexU32(((u32*) OPENSBI_MEMORY)[1]);
        while(1);
    }
    if(((u32*) UBOOT_MEMORY)[0] != 0x00050213 || ((u32*) UBOOT_MEMORY)[1] != 0x00058493) {
        bsp_putString("U-Boot missmatch\n");
        putHexU32(((u32*) UBOOT_MEMORY)[0]); bsp_putChar(' '); putHexU32(((u32*) UBOOT_MEMORY)[1]);
        while(1);
    }
    bsp_putString("pass\n");

    vgaInit();
#endif

    bsp_putString("OpenSBI boot\n");
    void (*userMain)(u32, u32, u32) = (void (*)(u32, u32, u32))OPENSBI_MEMORY;
    smp_unlock(userMain);
    userMain(0,0,0);
}


