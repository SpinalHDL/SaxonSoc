#ifndef SDRAM_H_
#define SDRAM_H_

#include <stdint.h>
#include <io.h>

#ifndef MAX
#define MAX(a,b)   (((a) > (b)) ? (a) : (b))
#endif
#ifndef MIN
#define MIN(a,b)   (((a) < (b)) ? (a) : (b))
#endif

#define SDRAM_CONFIG 0x000
#define SDRAM_PHASE 0x004
#define SDRAM_WRITE_LATENCY 0x008
#define SDRAM_READ_LATENCY 0x00C


#define SDRAM_SOFT_PUSH 0x100
#define SDRAM_SOFT_CMD 0x104
#define SDRAM_SOFT_ADDR 0x108
#define SDRAM_SOFT_BA 0x10C
#define SDRAM_SOFT_CLOCKING 0x110

#define SDRAM_S7_IDELAY_VALUE 0x000
#define SDRAM_S7_IDELAY_LOAD_DQS 0x010
#define SDRAM_S7_IDELAY_LOAD_DQ 0x020
#define SDRAM_S7_BITSLEEP 0x040


#define SDRAM_AUTO_REFRESH 1
#define SDRAM_NO_ACTIVE 2


#define SDRAM_CSN (1 << 1)
#define SDRAM_RASN  (1 << 2)
#define SDRAM_CASN  (1 << 3)
#define SDRAM_WEN  (1 << 4)

#define SDRAM_RESETN 1
#define SDRAM_CKE 2

#define SDRAM_PRE  (SDRAM_CASN)
#define SDRAM_REF  (SDRAM_WEN)
#define SDRAM_MOD  (0)
#define SDRAM_ZQCL  (SDRAM_RASN | SDRAM_CASN)

#define SDRAM_MPR 4

#define SDRAM_TIMING_REF 0x010
#define SDRAM_TIMING_0 0x020
#define SDRAM_TIMING_1 0x024
#define SDRAM_TIMING_2 0x028

#define SDRAM_FAW 0x030
#define SDRAM_ODT 0x034

typedef struct
{
	uint32_t RFC;
	uint32_t RAS;
	uint32_t RP ;
	uint32_t RCD;
	uint32_t WTR;
	uint32_t WTP;
	uint32_t RTP;
	uint32_t RRD;
	uint32_t REF;
	uint32_t FAW;

} SdramTiming;

//inline static SdramTiming sdram_timingsFrom(SdramTiming ps, SdramTiming cycle, uint32_t corePeriodPs, uint32_t phyClkRatio){
//	SdramTiming config;
//	for(uint32_t idx = 0;idx < sizeof(SdramTiming)/sizeof(uint32_t);idx++){
//		uint32_t t1 = (((uint32_t*)&ps)[idx] + corePeriodPs-1)/corePeriodPs;
//		uint32_t t2 = (((uint32_t*)&cycle)[idx] + phyClkRatio -1) / phyClkRatio;
//		((uint32_t*)&config)[idx] = (t1 > t2 ? t1 : t2)-2;
//	}
//	return config;
//}


static int32_t t2c(int32_t startPhase, int32_t nextPhase, int32_t duration, int32_t sdramPeriod, int32_t phyClkRatio) {
	return (startPhase + (duration + sdramPeriod - 1)/sdramPeriod - nextPhase + phyClkRatio - 1) / phyClkRatio;
}

static uint32_t sat(int32_t v) {
	return MAX(v,0);
}

const SdramTiming MT41K128M16JT_125_ps = {
	.REF =  7800000,
	.RAS =    35000,
	.RP  =    13750,
	.RFC =   160000,
	.RRD =     7500,
	.RCD =    13750,
	.RTP =     7500,
	.WTR =     7500,
	.WTP =    15000,
	.FAW =    40000
};

const SdramTiming MT47H64M16HR_25_ps = {
	.REF =  7800000,
	.RAS =    40000,
	.RP  =    15000,
	.RFC =   127500,
	.RRD =    10000,
	.RCD =    15000,
	.RTP =     7500,
	.WTR =     7500,
	.WTP =    15000,
	.FAW =    45000
};

const SdramTiming MT48LC16M16A2_6A_ps = {
	.REF =  7812500,
	.RAS =    42000,
	.RP  =    18000,
	.RFC =    60000,
	.RRD =    12000,
	.RCD =    18000,
	.RTP =        0,
	.WTR =        0,
	.WTP =     6000,
	.FAW =        0
};


const uint8_t wrToMr[] = {1,2,3,4,-1,5,-1,6,-1,7,-1,0};
const uint8_t rlToMr[] = {2,4,6,8,10,12,14,1,3,5};

static void sdram_command(uint32_t core, uint32_t cmd, uint32_t bank, uint32_t address){
	write_u32_ad(core + SDRAM_SOFT_BA, bank);
	write_u32_ad(core + SDRAM_SOFT_ADDR, address);
	write_u32_ad(core + SDRAM_SOFT_CMD, cmd);
	write_u32_ad(core + SDRAM_SOFT_PUSH, 0);
}

//void i2x(uint32_t value, char* str){
//	for(uint32_t i = 0;i < 8;i++){
//		uint32_t e = (value >> (i*4)) & 0xF;
//		str[i] = e < 10 ? e + '0' : e-10+'A';
//	}
//}
//
//static inline void write_u32_ad_log(uint32_t address, uint32_t data){
//	char buffer[] = "0xAAAAAAAA 0xDDDDDDDD\n";
//	i2x(address, buffer+2); i2x(data, buffer+2+8+1+2);
//	uart_writeStr(UART_A, buffer);
//	write_u32_ad(address, data);
//}

static void sdram_udelay(uint32_t us){
    #ifndef SPINAL_SIM
    io_udelay(us);
    #endif
}

static void sdram_init(uint32_t core, uint32_t rl, uint32_t wl, uint32_t bl, SdramTiming timing, uint32_t phyClkRatio, uint32_t sdramPeriod){
    uint32_t readToDataCycle = (rl+phyClkRatio-1)/phyClkRatio;
    uint32_t readPhase = readToDataCycle*phyClkRatio-rl;
    uint32_t writeToDataCycle = (wl+phyClkRatio-1)/phyClkRatio;
    uint32_t writePhase = writeToDataCycle*phyClkRatio-wl;
    uint32_t activePhase = 0;
	uint32_t prechargePhase = 0;
	//TODO missued 4
    int32_t ctrlPeriod = sdramPeriod*phyClkRatio;
    int32_t cREF = t2c(0, 0, timing.REF, sdramPeriod, phyClkRatio);
    int32_t cRAS = t2c(activePhase     , prechargePhase                 , timing.RAS, sdramPeriod, phyClkRatio);
    int32_t cRP  = t2c(prechargePhase  , activePhase                    , timing.RP, sdramPeriod, phyClkRatio);
    int32_t cRFC = t2c(activePhase     , activePhase                    , timing.RFC, sdramPeriod, phyClkRatio);
    int32_t cRRD = t2c(activePhase     , activePhase                    , MAX(timing.RRD, bl*sdramPeriod), sdramPeriod, phyClkRatio);
    int32_t cRCD = t2c(activePhase     , MIN(writePhase, readPhase)     , timing.RCD, sdramPeriod, phyClkRatio);
    int32_t cRTW = t2c(readPhase       , writePhase                     , (rl+bl+2-wl)*sdramPeriod, sdramPeriod, phyClkRatio);
    int32_t cRTP = t2c(readPhase       , prechargePhase                 , MAX(timing.RTP, bl*sdramPeriod), sdramPeriod, phyClkRatio);
    int32_t cWTR = t2c(writePhase      , readPhase                      , MAX(timing.WTR, bl*sdramPeriod) + (wl+bl)*sdramPeriod, sdramPeriod, phyClkRatio);
    int32_t cWTP = t2c(writePhase      , prechargePhase                 , timing.WTP + (wl+bl)*sdramPeriod, sdramPeriod, phyClkRatio);
    int32_t cFAW = t2c(activePhase     , activePhase                    , timing.FAW, sdramPeriod, phyClkRatio);

    write_u32( (prechargePhase << 24) | (activePhase << 16) | (readPhase << 8)   | (writePhase << 0), core + SDRAM_PHASE);
    write_u32( 0, core + SDRAM_WRITE_LATENCY);
    write_u32( SDRAM_NO_ACTIVE, core + SDRAM_CONFIG);

    write_u32(cREF-1, core + SDRAM_TIMING_REF);
    write_u32((sat(cRRD-2) << 24) | (sat(cRFC-2) << 16) | (sat(cRP-2) << 8)   | (sat(cRAS-2) << 0), core + SDRAM_TIMING_0);
    write_u32(                                                                   (sat(cRCD-2) << 0), core + SDRAM_TIMING_1);
    write_u32((sat(cWTP-2) << 24)  | (sat(cWTR-2) << 16) | (sat(cRTP-2) << 8)   | (sat(cRTW-2) << 0), core + SDRAM_TIMING_2);
    write_u32(sat(cFAW-1), core + SDRAM_FAW);

    int32_t ODTend = (1 << (writePhase + 6)%phyClkRatio)-1;
    if(ODTend == 0) ODTend = (1 << phyClkRatio)-1;
	int32_t ODT = (writePhase+6+phyClkRatio-1)/phyClkRatio-1;
	write_u32((ODT << 0) | (ODTend << 8), core + SDRAM_ODT);
}

static void sdram_sdr_init(uint32_t core,  uint32_t rl, uint32_t bl){
	uint32_t blMod;
	switch(bl){
	case 1: blMod = 0; break;
	case 2: blMod = 1; break;
	case 4: blMod = 2; break;
	case 8: blMod = 3; break;
	}

    write_u32(rl-1, SYSTEM_SDRAM_A_APB + SDRAM_READ_LATENCY);

    write_u32(0, core + SDRAM_SOFT_CLOCKING); sdram_udelay(100);
    write_u32(SDRAM_CKE, core + SDRAM_SOFT_CLOCKING); sdram_udelay(100);
    sdram_command(core, SDRAM_PRE,0,0x400);
    sdram_command(core, SDRAM_REF,0,0x000);
    sdram_command(core, SDRAM_REF,0,0x000);
    sdram_command(core, SDRAM_MOD,0,(rl << 4) | blMod);
    write_u32(SDRAM_AUTO_REFRESH, core + SDRAM_CONFIG);
}

static void sdram_ddr2_init(uint32_t core,  uint32_t rl, uint32_t wl, uint32_t bl, uint32_t al){
	write_u32(0, core + SDRAM_SOFT_CLOCKING);
	sdram_udelay(200);
	write_u32(SDRAM_RESETN, core + SDRAM_SOFT_CLOCKING);
	sdram_udelay(500);
	write_u32(SDRAM_RESETN | SDRAM_CKE, core + SDRAM_SOFT_CLOCKING);

	sdram_command(core, SDRAM_MOD, 2, 0);
	sdram_command(core, SDRAM_MOD, 3, 0);
	sdram_command(core, SDRAM_MOD, 1, (1 << 11) | ((al & 7) << 3) | 0x44);
	sdram_command(core, SDRAM_MOD, 0, (1 << 12) | (((wl - 1) & 7) << 9) | ((rl & 7) << 4) | (((bl & 15) >> 3) | 2));
	sdram_udelay(200);
	write_u32(SDRAM_AUTO_REFRESH, core + SDRAM_CONFIG);
}

static void sdram_ddr3_init(uint32_t core,  uint32_t rl, uint32_t wl, uint32_t bl){
	write_u32(0, core + SDRAM_SOFT_CLOCKING);
	sdram_udelay(200);
    write_u32(SDRAM_RESETN, core + SDRAM_SOFT_CLOCKING);
    sdram_udelay(500);
    write_u32(SDRAM_RESETN | SDRAM_CKE, core + SDRAM_SOFT_CLOCKING);

	sdram_command(core, SDRAM_MOD, 2, 0x000 | ((wl - 5) << 3));
	sdram_command(core, SDRAM_MOD, 3, 0);
	sdram_command(core, SDRAM_MOD, 1, 0x44);
	sdram_command(core, SDRAM_MOD, 0, (wrToMr[wl - 5] << 9) | 0x100 | ((rlToMr[rl-5] & 1) << 2) | ((rlToMr[rl-5] & 0xE) << 3)); //DDL reset
	sdram_udelay(100);
	sdram_command(core, SDRAM_ZQCL, 0, 0x400);
	sdram_udelay(100);
    write_u32(SDRAM_AUTO_REFRESH, core + SDRAM_CONFIG);
}

static void sdram_phy_s7(uint32_t core, uint32_t phy){
	write_u32(10, phy + SDRAM_S7_IDELAY_VALUE);
	write_u32(0xFFFFFFFF, phy + SDRAM_S7_IDELAY_LOAD_DQ);
	write_u32(0x00000000, phy + SDRAM_S7_IDELAY_LOAD_DQ);


	asm(".word(0x500F)");
	write_u32(0xFFFF0000, 0x80000000);
	write_u32(0x5555AAAA, 0x80000004);
	write_u32(0x11112222, 0x80000008);
	write_u32(0x33334444, 0x8000000C);
	asm(".word(0x500F)");


	for(int32_t readLatency = 0;readLatency < 2;readLatency++){
	    write_u32(readLatency, core + SDRAM_READ_LATENCY);
		for(int32_t bitsleep = 0; bitsleep < 4;bitsleep++){
			write_u32(0xFFFFFFFF, phy + SDRAM_S7_BITSLEEP);
			asm(".word(0x500F)"); //Flush data cache
			uint32_t d0 = read_u32(0x80000000);
			uint32_t d1 = read_u32(0x80000004);
			uint32_t d2 = read_u32(0x80000008);
			uint32_t d3 = read_u32(0x8000000C);
			if(d0 == 0xFFFF0000) {
				readLatency = 10;
				break;
			}
			asm("nop");
		}
	}

	//	return;
	//    sdram_command(core, SDRAM_MOD, 3, SDRAM_MPR);

	    //0x1E 0x0A
	////    for(int32_t dqsDelay = 0; dqsDelay < 16;dqsDelay++){
	//		for(int32_t dqDelay = 0; dqDelay < 32;dqDelay++){
	////    	int32_t dqDelay = dqsDelay + 16;
	//    		write_u32_ad(phy + SDRAM_S7_BITSLEEP, 0x01);
	////	    	write_u32_ad(phy + SDRAM_S7_IDELAY_VALUE, dqsDelay);
	////	    	write_u32_ad(phy + SDRAM_S7_IDELAY_LOAD_DQS, 0xFFFFFFFF);
	////	    	write_u32_ad(phy + SDRAM_S7_IDELAY_LOAD_DQS, 0x00000000);
	//	    	write_u32_ad(phy + SDRAM_S7_IDELAY_VALUE, dqDelay);
	//	    	write_u32_ad(phy + SDRAM_S7_IDELAY_LOAD_DQ, 0xFFFFFFFF);
	//	    	write_u32_ad(phy + SDRAM_S7_IDELAY_LOAD_DQ, 0x00000000);
	//			//TODO iserdes flush
	//	    	asm(".word(0x500F)"); //Flush data cache
	//	    	read_u32(0x80000000);
	//		}
	////    }



//	    for(uint32_t idx = 0;idx < 32;idx++){
//	    	write_u32_ad(phy + SDRAM_S7_IDELAY_VALUE, idx);
//	    	write_u32_ad(phy + SDRAM_S7_IDELAY_LOAD_DQ, 0xFFFFFFFF);
//	    	write_u32_ad(phy + SDRAM_S7_IDELAY_LOAD_DQ, 0x00000000);
//
//	    	asm(".word(0x500F)"); //Flush data cache
//			uint32_t d0 = read_u32(0x80000000);
//			uint32_t d1 = read_u32(0x80000004);
//			uint32_t d2 = read_u32(0x80000008);
//			uint32_t d3 = read_u32(0x8000000C);
//			asm("nop");
//	    }
//		asm(".word(0x500F)");
//		write_u32(28-16, phy + SDRAM_S7_IDELAY_VALUE);
//


//
//
//		for(uint32_t idx = 0;idx < 100;idx++){
//			write_u32(idx, idx);
//		}
//
//		asm(".word(0x500F)"); //Flush data cache
//
//		for(uint32_t idx = 0;idx < 100;idx++){
//			if(read_u32(idx) == idx){
//				asm("nop");
//			}
//		}
}

#endif /* SDRAM_H_ */



