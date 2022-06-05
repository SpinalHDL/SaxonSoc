#include "type.h"

#include "bsp.h"
#include "ethernetConfig.h"
#include "spi.h"
#include "gpio.h"
#include "mac.h"

u16 mdio_read(u32 spi, u32 cs, u32 phy, u32 reg){
    u16 data;
    spi_select(spi, cs);
    spi_write(spi, 0x60 | (phy >> 1) & 0x0F);
    spi_write(spi, (phy << 7) & 0x80 | (reg << 2) & 0x7C);
    data  = ((u16) spi_read(spi)) << 8;
    data |= ((u16) spi_read(spi)) << 0;
    spi_diselect(spi, cs);
    return data;
}

void mdio_write(u32 spi, u32 cs, u32 phy, u32 reg, u16 data){
    spi_select(spi, cs);
    spi_write(spi, 0x50 | (phy >> 1) & 0x0F);
    spi_write(spi, (phy << 7) & 0x80 | (reg << 2) & 0x7C | 0x02);
    spi_write(spi, data >> 8);
    spi_write(spi, data >> 0);
    spi_diselect(spi, cs);
}

void mdio_init(u32 spi, u32 cs){
    spi_diselect(spi, cs);
    gpio_setOutput(RESETN_GPIO, 0);
    bsp_uDelay(100);
    gpio_setOutput(RESETN_GPIO, RESETN_PIN);
    bsp_uDelay(200000);

    spi_select(spi, cs);
    spi_write(spi, 0xFF);
    spi_write(spi, 0xFF);
    spi_write(spi, 0xFF);
    spi_write(spi, 0xFF);
    spi_diselect(spi, cs);

    bsp_uDelay(100);
}

void putU8Hex(u8 value){
    for(s32 i = 1; i >=  0;i--){
        u32 hex = (value >> i*4) & 0xF;
        bsp_putChar(hex > 9 ? 'A' + hex - 10 : '0' + hex);
    }
}

//#pragma GCC optimize ("O3")
void main() {
    bsp_putString("Ethernet demo");

    mac_setCtrl(MAC, MAC_CTRL_TX_RESET | MAC_CTRL_RX_RESET);
    bsp_uDelay(10);
    mac_setCtrl(MAC, 0);


    // TX
    while(1){
//        while(!mac_txReady(MAC));
//        static u8 frame[] = {0x33,0x33,0x00,0x00,0x00,0x02,0x00,0x0A,0xCD,0x2C,0x15,0x94,0x86,0xDD,0x60,0x0B,0xDD,0x41,0x00,0x08,0x3A,0xFF,0xFE,0x80,0x00,0x00,0x00,0x00,0x00,0x00,0xFC,0x3B,0x9A,0x3C,0xE0,0xE2,0x39,0x55,0xFF,0x02,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x02,0x85,0x00,0xCC,0x86,0x00,0x00,0x00,0x00,0x59,0x01,0xA3,0x28};
//        u32 bits = sizeof(frame)*8;
//        u32 words = (bits+31)/32;
//        u32 *ptr = (u32*) frame;
//        while(mac_getTxAvailability(MAC) == 0);
//        mac_pushTx(MAC, bits);
//        while(words != 0){
//            u32 tockens = mac_getTxAvailability(MAC);
//            if(tockens > words) tockens = words;
//            words -= tockens;
//            while(tockens != 0){
//                mac_pushTx(MAC, *ptr++);
//                tockens--;
//            }
//        }

        static u32 counter = 0;
        while(!mac_txReady(MAC));
        u32 bits = 32;
        u32 words = (bits+31)/32;
        while(mac_getTxAvailability(MAC) < 2);
        mac_pushTx(MAC, bits);
        mac_pushTx(MAC, counter++);


    /*    bsp_putChar('*');
        bsp_uDelay(1000*500);*/
    }


//    RX
//    while(1){
//        while(!mac_rxPending(MAC));
//
//        u32 bits = mac_getRx(MAC);
//        u32 words = (bits+31)/32;
//        u32 byteRemaining = (bits+7)/8;
//        for(u32 wordId = 0;wordId < words;wordId++){
//            u32 word = mac_getRx(MAC);
//            u32 byteCount = byteRemaining > 4 ? 4 : byteRemaining;
//            for(u32 byteId = 0;byteId < byteCount; byteId++){
//                putU8Hex(word >> byteId*8);
//            }
//            bsp_putChar(' ');
//            byteRemaining -= byteCount;
//        }
//        bsp_putChar('\n');
//        bsp_putChar('\n');
//        bsp_putChar('\n');
//    }



//    MDIO
//    u32 clkDivider = BSP_CLINT_HZ/(1000000*2)-1;
//
//    Spi_Config spiCfg;
//    spiCfg.cpol = 0;
//    spiCfg.cpha = 0;
//    spiCfg.mode = 1;
//    spiCfg.clkDivider = clkDivider;
//    spiCfg.ssSetup = clkDivider;
//    spiCfg.ssHold = clkDivider;
//    spiCfg.ssDisable = clkDivider;
//    spi_applyConfig(SPI, &spiCfg);
//
//    gpio_setOutputEnable(RESETN_GPIO, RESETN_PIN);
//
//    u32 address = 0;
//    while(1){
//        mdio_init(SPI,SPI_CS);
//
//        u16 control = mdio_read(SPI, SPI_CS, 1, 0);
//        u16 status = mdio_read(SPI, SPI_CS, 1, 1);
//        u16 id1 = mdio_read(SPI, SPI_CS, 1, 2);
//        u16 id2 = mdio_read(SPI, SPI_CS, 1, 3);
//        mdio_write(SPI, SPI_CS, 1, 0, control | 0x0200);
//        while(1){
//            control = mdio_read(SPI, SPI_CS, 1, 1);
//            bsp_uDelay(50000);
//        }
//
//        asm("nop");
//    }
    bsp_putString("done");
}

