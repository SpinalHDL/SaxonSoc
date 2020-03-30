#ifndef I2C_H_
#define I2C_H_

#include "type.h"
#include "io.h"


#define I2C_TX_DATA                 0x00
#define I2C_TX_ACK                  0x04
#define I2C_RX_DATA                 0x08
#define I2C_RX_ACK                  0x0C
#define I2C_INTERRUPT_ENABLE        0x20
#define I2C_INTERRUPT_FLAG          0x24
#define I2C_SAMPLING_CLOCK_DIVIDER  0x28
#define I2C_TIMEOUT                 0x2C
#define I2C_TSUDAT                  0x30
#define I2C_MASTER_STATUS           0x40
#define I2C_TLOW                    0x50
#define I2C_THIGH                   0x54
#define I2C_TBUF                    0x58
#define I2C_FILTERING_HIT           0x80
#define I2C_FILTERING_STATUS        0x84
#define I2C_FILTERING_CONFIG        0x88

typedef struct {
    //Master/Slave mode
    u32 samplingClockDivider; //Number of cycle - 1 between each SDA/SCL sample
    u32 timeout;              //Number of cycle - 1 after which an inactive frame is considered dropped.
    u32 tsuDat;               //Number of cycle - 1 SCL should be keept low (clock stretching) after having feed the data to
                                   //  the SDA to ensure a correct propagation to other devices
    //Master mode
    u32 tLow;  //SCL low (cycle count -1)
    u32 tHigh; //SCL high (cycle count -1)
    u32 tBuf;  //Minimum time between the Stop/Drop -> Start transition
} I2c_Config;

#define I2C_MODE_CPOL (1 << 0)
#define I2C_MODE_CPHA (1 << 1)

#define I2C_TX_VALUE (0xFF)
#define I2C_TX_VALID (1 << 8)
#define I2C_TX_ENABLE (1 << 9)
#define I2C_TX_REPEAT (1 << 10)
#define I2C_TX_DISABLE_ON_DATA_CONFLICT (1 << 11)

#define I2C_RX_VALUE (0xFF)
#define I2C_RX_VALID (1 << 8)
#define I2C_RX_LISTEN (1 << 9)

#define I2C_MASTER_BUSY (1 << 0)
#define I2C_MASTER_START (1 << 4)
#define I2C_MASTER_STOP (1 << 5)
#define I2C_MASTER_DROP (1 << 6)

#define I2C_FILTER_7_BITS (0)
#define I2C_FILTER_10_BITS (1 << 14)
#define I2C_FILTER_ENABLE (1 << 15)

#define I2C_INTERRUPT_TX_DATA (1 << 2)
#define I2C_INTERRUPT_TX_ACK (1 << 3)
#define I2C_INTERRUPT_DROP (1 << 7)

#define I2C_INTERRUPT_CLOCK_GEN_BUSY (1 << 16)
#define I2C_INTERRUPT_FILTER (1 << 17)

static void i2c_applyConfig(u32 reg, I2c_Config *config){
    write_u32(config->samplingClockDivider, reg + I2C_SAMPLING_CLOCK_DIVIDER);
    write_u32(config->timeout, reg + I2C_TIMEOUT);
    write_u32(config->tsuDat, reg + I2C_TSUDAT);

    write_u32(config->tLow, reg + I2C_TLOW);
    write_u32(config->tHigh, reg + I2C_THIGH);
    write_u32(config->tBuf, reg + I2C_TBUF);
}

static inline void i2c_filterEnable(u32 reg, u32 filterId, u32 config){
    write_u32(config, reg + I2C_FILTERING_CONFIG + 4*filterId);
}



static inline void i2c_masterStart(u32 reg){
    write_u32(I2C_MASTER_START, reg + I2C_MASTER_STATUS);
}


static int i2c_masterBusy(u32 reg){
    return (read_u32(reg + I2C_MASTER_STATUS) & I2C_MASTER_BUSY) != 0;
}

static void i2c_masterStartBlocking(u32 reg){
    i2c_masterStart(reg);
    while(!i2c_masterBusy(reg));
}
static inline void i2c_masterStop(u32 reg){
    write_u32(I2C_MASTER_STOP, reg + I2C_MASTER_STATUS);
}

static void i2c_masterStopWait(u32 reg){
    while(i2c_masterBusy(reg));
}

static inline void i2c_masterDrop(u32 reg){
    write_u32(I2C_MASTER_DROP, reg + I2C_MASTER_STATUS);
}


static void i2c_masterStopBlocking(u32 reg){
    i2c_masterStop(reg);
    i2c_masterStopWait(reg);
}


static inline void i2c_listenAck(u32 reg){
    write_u32(I2C_RX_LISTEN ,reg + I2C_RX_ACK);
}

static inline void i2c_txByte(u32 reg,u8 byte){
    write_u32(byte | I2C_TX_VALID | I2C_TX_ENABLE | I2C_TX_DISABLE_ON_DATA_CONFLICT, reg + I2C_TX_DATA);
}


static inline void i2c_txAck(u32 reg){
    write_u32(I2C_TX_VALID | I2C_TX_ENABLE, reg + I2C_TX_ACK);
}
static inline void i2c_txNack(u32 reg){
    write_u32(1 | I2C_TX_VALID | I2C_TX_ENABLE, reg + I2C_TX_ACK);
}
static void i2c_txAckWait(u32 reg){
    while(read_u32(reg + I2C_TX_ACK) & I2C_TX_VALID);
}


static void i2c_txAckBlocking(u32 reg){
    i2c_txAck(reg);
    i2c_txAckWait(reg);
}
static void i2c_txNackBlocking(u32 reg){
    i2c_txNack(reg);
    i2c_txAckWait(reg);
}

static u32 i2c_rxData(u32 reg){
    return read_u32(reg + I2C_RX_DATA) & I2C_RX_VALUE;
}

static int i2c_rxNack(u32 reg){
    return (read_u32(reg + I2C_RX_ACK) & I2C_RX_VALUE) != 0;
}

static int i2c_rxAck(u32 reg){
    return (read_u32(reg + I2C_RX_ACK) & I2C_RX_VALUE) == 0;
}

static void i2c_txByteRepeat(u32 reg,u8 byte){
    write_u32(byte | I2C_TX_VALID | I2C_TX_ENABLE | I2C_TX_DISABLE_ON_DATA_CONFLICT | I2C_TX_REPEAT, reg + I2C_TX_DATA);
}

static void i2c_txNackRepeat(u32 reg){
    write_u32(1 | I2C_TX_VALID | I2C_TX_ENABLE | I2C_TX_REPEAT, reg + I2C_TX_ACK);
}

static inline void i2c_setFilterConfig(u32 reg, u32 filterId, u32 value){
    write_u32(value, reg + I2C_FILTERING_CONFIG + 4*filterId);
}

static void i2c_enableInterrupt(u32 reg, u32 value){
    write_u32(value | read_u32(reg + I2C_INTERRUPT_ENABLE), reg + I2C_INTERRUPT_ENABLE);
}

static void i2c_disableInterrupt(u32 reg, u32 value){
    write_u32(~value & read_u32(reg + I2C_INTERRUPT_ENABLE), reg + I2C_INTERRUPT_ENABLE);
}


static inline void i2c_clearInterruptFlag(u32 reg, u32 value){
    write_u32(value, reg + I2C_INTERRUPT_FLAG);
}

readReg_u32 (gpio_getInterruptFlag   , I2C_INTERRUPT_FLAG)
readReg_u32 (gpio_getMasterStatus    , I2C_MASTER_STATUS)
readReg_u32 (gpio_getFilteringHit    , I2C_FILTERING_HIT)
readReg_u32 (gpio_getFilteringStatus , I2C_FILTERING_STATUS)




#endif /* I2C_H_ */


