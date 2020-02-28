#ifndef I2C_H_
#define I2C_H_

#include "type.h"

typedef struct
{
	volatile u32 TX_DATA;
	volatile u32 TX_ACK;
	volatile u32 RX_DATA;
	volatile u32 RX_ACK;
	volatile u32 _a[4];
	volatile u32 INTERRUPT_ENABLE;
	volatile u32 INTERRUPT_FLAG;
	volatile u32 SAMPLING_CLOCK_DIVIDER;
	volatile u32 TIMEOUT;
	volatile u32 TSUDAT;
	volatile u32 _c[3];
	volatile u32 MASTER_STATUS;
	volatile u32 _d[3];
	volatile u32 TLOW;
	volatile u32 THIGH;
	volatile u32 TBUF;
	volatile u32 _e[9];
	volatile u32 FILTERING_HIT;
	volatile u32 FILTERING_STATUS;
	volatile u32 FILTERING_CONFIG[];
} I2c_Reg;




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

static void i2c_applyConfig(I2c_Reg *reg, I2c_Config *config){
    reg->SAMPLING_CLOCK_DIVIDER = config->samplingClockDivider;
    reg->TIMEOUT = config->timeout;
    reg->TSUDAT = config->tsuDat;

    reg->TLOW = config->tLow;
    reg->THIGH = config->tHigh;
    reg->TBUF = config->tBuf;
}

static void i2c_filterEnable(I2c_Reg *reg, u32 filterId, u32 config){
	reg->FILTERING_CONFIG[filterId] = config;
}



static void i2c_masterStart(I2c_Reg *reg){
	reg->MASTER_STATUS = I2C_MASTER_START;
}


static int i2c_masterBusy(I2c_Reg *reg){
	return (reg->MASTER_STATUS & I2C_MASTER_BUSY) != 0;
}

static void i2c_masterStartBlocking(I2c_Reg *reg){
	i2c_masterStart(reg);
	while(!i2c_masterBusy(reg));
}




static void i2c_masterStop(I2c_Reg *reg){
	reg->MASTER_STATUS = I2C_MASTER_STOP;
}


static void i2c_masterStopWait(I2c_Reg *reg){
	while(i2c_masterBusy(reg));
}

static void i2c_masterDrop(I2c_Reg *reg){
	reg->MASTER_STATUS = I2C_MASTER_DROP;
}


static void i2c_masterStopBlocking(I2c_Reg *reg){
	i2c_masterStop(reg);
	i2c_masterStopWait(reg);
}


static void i2c_listenAck(I2c_Reg *reg){
	reg->RX_ACK = I2C_RX_LISTEN;
}

static void i2c_txByte(I2c_Reg *reg,u8 byte){
	reg->TX_DATA = byte | I2C_TX_VALID | I2C_TX_ENABLE | I2C_TX_DISABLE_ON_DATA_CONFLICT;
}


static void i2c_txAck(I2c_Reg *reg){
	reg->TX_ACK = I2C_TX_VALID | I2C_TX_ENABLE;
}
static void i2c_txNack(I2c_Reg *reg){
	reg->TX_ACK = 1 | I2C_TX_VALID | I2C_TX_ENABLE;
}
static void i2c_txAckWait(I2c_Reg *reg){
	while(reg->TX_ACK & I2C_TX_VALID);
}


static void i2c_txAckBlocking(I2c_Reg *reg){
	i2c_txAck(reg);
	i2c_txAckWait(reg);
}
static void i2c_txNackBlocking(I2c_Reg *reg){
	i2c_txNack(reg);
	i2c_txAckWait(reg);
}

static u32 i2c_rxData(I2c_Reg *reg){
	return reg->RX_DATA & I2C_RX_VALUE;
}

static int i2c_rxNack(I2c_Reg *reg){
	return (reg->RX_ACK & I2C_RX_VALUE) != 0;
}

static int i2c_rxAck(I2c_Reg *reg){
	return (reg->RX_ACK & I2C_RX_VALUE) == 0;
}

static void i2c_txByteRepeat(I2c_Reg *reg,u8 byte){
    reg->TX_DATA = byte | I2C_TX_VALID | I2C_TX_ENABLE | I2C_TX_DISABLE_ON_DATA_CONFLICT | I2C_TX_REPEAT;
}

static void i2c_txNackRepeat(I2c_Reg *reg){
    reg->TX_ACK = 1 | I2C_TX_VALID | I2C_TX_ENABLE | I2C_TX_REPEAT;
}

#endif /* I2C_H_ */


