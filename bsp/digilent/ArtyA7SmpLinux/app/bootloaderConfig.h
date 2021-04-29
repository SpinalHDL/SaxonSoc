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

#define GPIO SYSTEM_GPIO_A_CTRL

#define OPENSBI_MEMORY 0x80F80000
#define OPENSBI_FLASH  0x00400000
#define OPENSBI_SIZE      0x40000

#define UBOOT_MEMORY     0x80E00000
#define UBOOT_SBI_FLASH  0x00480000
#define UBOOT_SIZE          0x80000

#define RL 5
#define WL 5
#define CTRL_BURST_LENGHT 2
#define PHY_CLK_RATIO 2

void bspMain() {
    bsp_putString("\n");
    bsp_putString("SDRAM init\n");
    sdram_init(
        SDRAM_CTRL,
        RL,
        WL,
        MT41K128M16JT_125_ps,
        CTRL_BURST_LENGHT,
        PHY_CLK_RATIO,
        3300
    );

    sdram_ddr3_init(
        SDRAM_CTRL,
        RL,
        WL,
        CTRL_BURST_LENGHT,
        PHY_CLK_RATIO
    );

#ifndef SPINAL_SIM
    sdram_phy_s7(
        SDRAM_CTRL,
        SDRAM_PHY,
        SDRAM_BASE
    );

    spiFlash_init(SPI, SPI_CS);
    spiFlash_wake(SPI, SPI_CS);
    bsp_putString("OpenSBI copy\n");
    spiFlash_f2m(SPI, SPI_CS, OPENSBI_FLASH, OPENSBI_MEMORY, OPENSBI_SIZE);
    bsp_putString("U-Boot copy\n");
    spiFlash_f2m(SPI, SPI_CS, UBOOT_SBI_FLASH, UBOOT_MEMORY, UBOOT_SIZE);

    vgaInit();
#endif

    bsp_putString("Payload boot\n");
    void (*userMain)(u32, u32, u32) = (void (*)(u32, u32, u32))OPENSBI_MEMORY;
    smp_unlock(userMain);
    userMain(0,0,0);
}


