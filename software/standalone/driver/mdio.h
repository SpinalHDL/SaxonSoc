#pragma once

#include "type.h"

u16 mdio_read(u32 spi, u32 cs, u32 phy, u32 reg){
    u16 data;
    spi_select(spi, cs);
    spi_write(spi, 0xFF);
    spi_write(spi, 0xFF);
    spi_write(spi, 0xFF);
    spi_write(spi, 0xFF);
    spi_write(spi, 0x60 | (phy >> 1) & 0x0F);
    spi_write(spi, (phy << 7) & 0x80 | (reg << 2) & 0x7C);
    data  = ((u16) spi_read(spi)) << 8;
    data |= ((u16) spi_read(spi)) << 0;
    spi_diselect(spi, cs);
    return data;
}

void mdio_write(u32 spi, u32 cs, u32 phy, u32 reg, u16 data){
    spi_select(spi, cs);
    spi_write(spi, 0xFF);
    spi_write(spi, 0xFF);
    spi_write(spi, 0xFF);
    spi_write(spi, 0xFF);
    spi_write(spi, 0x50 | (phy >> 1) & 0x0F);
    spi_write(spi, (phy << 7) & 0x80 | (reg << 2) & 0x7C | 0x02);
    spi_write(spi, data >> 8);
    spi_write(spi, data >> 0);
    spi_diselect(spi, cs);
}

