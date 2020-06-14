#pragma once

#include "type.h"
#include "io.h"

#define MAC_CTRL 0x00
#define MAC_RX   0x10
#define MAC_TX   0x20
#define MAC_TX_AVAILABILITY   0x24

#define MAC_CTRL_RX_PENDING 0x00000010
#define MAC_CTRL_TX_READY 0x00000020
#define MAC_CTRL_CLEAR 0x00000001

readReg_u32 (mac_getCtrl , MAC_CTRL)
writeReg_u32 (mac_setCtrl , MAC_CTRL)
readReg_u32 (mac_getRx , MAC_RX)
writeReg_u32 (mac_pushTx , MAC_TX)
readReg_u32 (mac_getTxAvailability , MAC_TX_AVAILABILITY)


static u32 mac_rxPending(u32 p){
    return mac_getCtrl(p) & MAC_CTRL_RX_PENDING;
}

static u32 mac_txReady(u32 p){
    return mac_getCtrl(p) & MAC_CTRL_TX_READY;
}





