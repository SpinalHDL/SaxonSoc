#ifndef SDRAM_H_
#define SDRAM_H_

#include <stdint.h>
#include <io.h>

#define SDRAM_CONFIG 0x000
#define SDRAM_CTRL_FLAGS 0x004

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
	uint32_t REF;

	uint32_t RAS;
	uint32_t RP;
	uint32_t RFC;
	uint32_t RRD;

	uint32_t RCD;

	uint32_t RTW;
	uint32_t RTP;
	uint32_t WTR;
	uint32_t WTP;


	uint32_t FAW;
	uint32_t ODT;
} SdramTiming;

inline static SdramTiming sdram_timingsFrom(SdramTiming ps, SdramTiming cycle, uint32_t corePeriodPs, uint32_t phyClkRatio){
	SdramTiming config;
	for(uint32_t idx = 0;idx < sizeof(SdramTiming)/sizeof(uint32_t);idx++){
		uint32_t t1 = (((uint32_t*)&ps)[idx] + corePeriodPs-1)/corePeriodPs;
		uint32_t t2 = (((uint32_t*)&cycle)[idx] + phyClkRatio -1) / phyClkRatio;
		((uint32_t*)&config)[idx] = (t1 > t2 ? t1 : t2)-1;
	}
	return config;
}


const SdramTiming MT41K128M16JT_125_ps = {
		.REF = 7800000,
		.RAS =  35000,
		.RP  =  13750,
		.RFC = 160000,
		.RRD =   7500,
		.RCD =  13750,
		.RTW =      0,
		.RTP =   7500,
		.WTR =   7500,
		.WTP =  15000,
		.FAW =  40000,
		.ODT =      0,
};


const SdramTiming MT41K128M16JT_125_cycle = {
		.REF = 0,
		.RAS = 0,
		.RP  = 0,
		.RFC = 0,
		.RRD = 0,
		.RCD = 4,
		.RTW = 6,
		.RTP = 4,
		.WTR = 8,
		.WTP = 0,
		.FAW = 0,
		.ODT = 6,
};


const uint8_t wrToMr[] = {1,2,3,4,-1,5,-1,6,-1,7,-1,0};



static void sdram_command(uint32_t core, uint32_t cmd, uint32_t bank, uint32_t address){
	write_u32_ad(core + SDRAM_SOFT_BA, bank);
	write_u32_ad(core + SDRAM_SOFT_ADDR, address);
	write_u32_ad(core + SDRAM_SOFT_CMD, cmd);
	write_u32_ad(core + SDRAM_SOFT_PUSH, 0);
}

void i2x(uint32_t value, char* str){
	for(uint32_t i = 0;i < 8;i++){
		uint32_t e = (value >> (i*4)) & 0xF;
		str[i] = e < 10 ? e + '0' : e-10+'A';
	}
}

static inline void write_u32_ad_log(uint32_t address, uint32_t data){
	char buffer[] = "0xAAAAAAAA 0xDDDDDDDD\n";
	i2x(address, buffer+2); i2x(data, buffer+2+8+1+2);
	uart_writeStr(UART_A, buffer);
	write_u32_ad(address, data);
}

static void sdram_init(uint32_t core, uint32_t phy, uint32_t cl, uint32_t wr, SdramTiming t, uint32_t phyClkRatio){
	uint32_t commandToDataCycle = (cl+phyClkRatio-1)/phyClkRatio;
	uint32_t commandPhase = phyClkRatio - (commandToDataCycle*phyClkRatio-cl);
	uint32_t sdramConfig = commandPhase | (commandToDataCycle-1 << 16);

	write_u32_ad(core + SDRAM_CONFIG, sdramConfig);
	write_u32_ad(core + SDRAM_CTRL_FLAGS, SDRAM_NO_ACTIVE);

	write_u32_ad(core + SDRAM_TIMING_REF, t.REF);
	write_u32_ad(core + SDRAM_TIMING_0, (t.RAS << 0) | (t.RP << 8) | (t.RFC << 16) | (t.RRD << 24));
	write_u32_ad(core + SDRAM_TIMING_1, (t.RCD << 0));
	write_u32_ad(core + SDRAM_TIMING_2, (t.RTW << 0) | (t.RTP << 8) | (t.WTR << 16) | (t.WTP << 24));
	write_u32_ad(core + SDRAM_FAW, t.FAW);
	write_u32_ad(core + SDRAM_ODT, (3 << 0) | (1 << 8));

	write_u32_ad(core + SDRAM_SOFT_CLOCKING, 0);
	io_udelay(200);
	write_u32_ad(core + SDRAM_SOFT_CLOCKING, SDRAM_RESETN);
	io_udelay(500);
	write_u32_ad(core + SDRAM_SOFT_CLOCKING, SDRAM_RESETN | SDRAM_CKE);

	uint32_t clConfig = (cl-3) & 0xF;
	uint32_t wlConfig = (cl-5);
	sdram_command(core, SDRAM_MOD, 2, 0x200 | (wlConfig << 3));
    sdram_command(core, SDRAM_MOD, 3, 0);
	sdram_command(core, SDRAM_MOD, 1, 0x44);
	sdram_command(core, SDRAM_MOD, 0, (wrToMr[wr-5] << 9) | 0x100 | ((clConfig & 1) << 2) | ((clConfig & 0xE) << 3)); //DDL reset
	io_udelay(100);
	sdram_command(core, SDRAM_ZQCL, 0, 0x400);
	io_udelay(100);

//	write_u32_ad(phy + SDRAM_S7_BITSLEEP, 0x00);
//	write_u32_ad(phy + SDRAM_S7_IDELAY_VALUE, 0x10);
//	write_u32_ad(phy + SDRAM_S7_IDELAY_LOAD_DQS, 0xFFFFFFFF);
//	write_u32_ad(phy + SDRAM_S7_IDELAY_LOAD_DQS, 0x00000000);
	write_u32_ad(phy + SDRAM_S7_IDELAY_VALUE, 0x10);
	write_u32_ad(phy + SDRAM_S7_IDELAY_LOAD_DQ, 0xFFFFFFFF);
	write_u32_ad(phy + SDRAM_S7_IDELAY_LOAD_DQ, 0x00000000);
//
//
	write_u32_ad(core + SDRAM_CTRL_FLAGS, 0);
	asm(".word(0x500F)");
	write_u32_ad(0x80000000, 0xFFFF0000);
	write_u32_ad(0x80000004, 0x5555AAAA);
	asm(".word(0x500F)");
//	read_u32(0x80000000);
//	read_u32(0x80000004);


	for(int32_t bitsleep = 0; bitsleep < 8;bitsleep++){
		write_u32_ad(phy + SDRAM_S7_BITSLEEP, 0xFFFFFFFF);
		asm(".word(0x500F)"); //Flush data cache
		read_u32(0x80000000);
	}

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
//    for(uint32_t idx = 6;idx < 32-6;idx++){
//    	write_u32_ad(phy + SDRAM_S7_IDELAY_VALUE, idx);
//    	write_u32_ad(phy + SDRAM_S7_IDELAY_LOAD_DQS, 0xFFFFFFFF);
//    	write_u32_ad(phy + SDRAM_S7_IDELAY_LOAD_DQS, 0x00000000);
//    	write_u32_ad(phy + SDRAM_S7_IDELAY_VALUE, idx-6);
//    	write_u32_ad(phy + SDRAM_S7_IDELAY_LOAD_DQ, 0xFFFFFFFF);
//    	write_u32_ad(phy + SDRAM_S7_IDELAY_LOAD_DQ, 0x00000000);
//
//    	asm(".word(0x500F)"); //Flush data cache
//    	read_u32(0x80000000);
//    }
}

#endif /* SDRAM_H_ */


