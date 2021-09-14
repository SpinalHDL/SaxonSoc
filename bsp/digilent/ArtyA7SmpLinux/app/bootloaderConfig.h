#pragma once

#include "bsp.h"
#include "start.h"
#include "sdram.h"
#include "spiFlash.h"
#include "vgaInit.h"
#include "mdio.h"

#define SDRAM_CTRL SYSTEM_SDRAM_A_CTRL
#define SDRAM_PHY  SDRAM_DOMAIN_PHY_A_CTRL
#define SDRAM_BASE SYSTEM_SDRAM_A0_BMB

#define SPI SYSTEM_SPI_A_CTRL
#define SPI_CS 0
#define MD_CS 2

#define GPIO SYSTEM_GPIO_A_CTRL

#define OPENSBI_MEMORY   0x80F80000
#define OPENSBI_FLASH    0x00F00000
#define OPENSBI_SIZE        0x40000

#define UBOOT_MEMORY     0x80E00000
#define UBOOT_SBI_FLASH  0x00F40000
#define UBOOT_SIZE          0xC0000

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

    // MDIO
    bsp_putString("Configuring MDIO\n");

    u32 clkDivider = BSP_CLINT_HZ/(1000000*2)-1;

    Spi_Config spiCfg;
    spiCfg.cpol = 0;
    spiCfg.cpha = 0;
    spiCfg.mode = 1;
    spiCfg.clkDivider = clkDivider;
    spiCfg.ssSetup = clkDivider;
    spiCfg.ssHold = clkDivider;
    spiCfg.ssDisable = clkDivider;
    spi_applyConfig(SPI, &spiCfg);

    bsp_putString("  control (old): ");
    u16 control = mdio_read(SPI, MD_CS, 1, 0);
    bsp_putU32(control);
    bsp_putString("\n");

    // Set 100Mbps and auto-negotiate
    mdio_write(SPI, MD_CS, 1, 4, 0x0181);
    mdio_write(SPI, MD_CS, 1, 0, 0x1000);
    #ifndef SPINAL_SIM
    bsp_uDelay(10000);
    #endif

    bsp_putString("  control (new): ");
    control = mdio_read(SPI, MD_CS, 1, 0);
    bsp_putU32(control);
    bsp_putString("\n");

    bsp_putString("  status: ");
    control = mdio_read(SPI, MD_CS, 1, 1);
    bsp_putU32(control);
    bsp_putString("\n");

    bsp_putString("  status 4: ");
    control = mdio_read(SPI, MD_CS, 1, 4);
    bsp_putU32(control);
    bsp_putString("\n");

    bsp_putString("Payload boot\n");
    void (*userMain)(u32, u32, u32) = (void (*)(u32, u32, u32))OPENSBI_MEMORY;
    smp_unlock(userMain);
    userMain(0,0,0);
}


