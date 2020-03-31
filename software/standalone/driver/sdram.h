#ifndef SDRAM_H_
#define SDRAM_H_

#include "type.h"
#include "io.h"
#include "bsp.h"

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

#define SDRAM_TIMING_SDR 0
#define SDRAM_TIMING_DDR1 1
#define SDRAM_TIMING_DDR2 2
#define SDRAM_TIMING_DDR3 3

typedef struct
{
    u32 generation;
    u32 RFC;
    u32 RAS;
    u32 RP ;
    u32 RCD;
    u32 WTR;
    u32 WTP;
    u32 RTP;
    u32 RRD;
    u32 REF;
    u32 FAW;

} SdramTiming;


static s32 t2c(s32 startPhase, s32 nextPhase, s32 duration, s32 sdramPeriod, s32 phyClkRatio) {
    return (startPhase + (duration + sdramPeriod - 1)/sdramPeriod - nextPhase + phyClkRatio - 1) / phyClkRatio;
}

static u32 sat(s32 v) {
    return MAX(v,0);
}

static const SdramTiming MT41K128M16JT_125_ps = {
    .generation = SDRAM_TIMING_DDR3,
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

static const SdramTiming MT47H64M16HR_25_ps = {
    .generation = SDRAM_TIMING_DDR2,
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

static const SdramTiming MT48LC16M16A2_6A_ps = {
    .generation = SDRAM_TIMING_SDR,
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

static const SdramTiming AS4C32M16SB_7TCN_ps = {
    .generation = SDRAM_TIMING_SDR,
    .REF =  7800000,
    .RAS =    42000,
    .RP  =    21000,
    .RFC =    63000,
    .RRD =    14000,
    .RCD =    21000,
    .RTP =        0,
    .WTR =        0,
    .WTP =    14000-7000,
    .FAW =        0
};

static void sdram_udelay(u32 us){
    #ifndef SPINAL_SIM
    bsp_uDelay(us);
    #endif
}

static void sdram_command(u32 core, u32 cmd, u32 bank, u32 address){
    write_u32_ad(core + SDRAM_SOFT_BA, bank);
    write_u32_ad(core + SDRAM_SOFT_ADDR, address);
    write_u32_ad(core + SDRAM_SOFT_CMD, cmd);
    write_u32_ad(core + SDRAM_SOFT_PUSH, 0);
    sdram_udelay(1);
}


static void sdram_init(u32 core, u32 rl, u32 wl, SdramTiming timing, u32 ctrlBurstLength, u32 phyClkRatio, u32 sdramPeriod){
    u32 readToDataCycle = (rl+phyClkRatio-1)/phyClkRatio;
    u32 readPhase = readToDataCycle*phyClkRatio-rl;
    u32 writeToDataCycle = (wl+phyClkRatio-1)/phyClkRatio;
    u32 writePhase = writeToDataCycle*phyClkRatio-wl;
    u32 activePhase = 0;
    u32 prechargePhase = 0;
    u32 bl = ctrlBurstLength*phyClkRatio;



    u32 cRRD_MIN = 0;
    u32 cRTW_IDLE = 0;
    u32 cWTP_ADD = 1;
    switch(timing.generation) {
      case SDRAM_TIMING_SDR:
        cRTW_IDLE = 1;
        break;
      case SDRAM_TIMING_DDR1:
      case SDRAM_TIMING_DDR2:
        cRTW_IDLE = 2; // Could be 1
        break;
      case SDRAM_TIMING_DDR3:
        cRRD_MIN = 4;
        cRTW_IDLE = 2;
        break;
    };
    
    s32 ctrlPeriod = sdramPeriod*phyClkRatio;
    s32 cREF = t2c(0, 0, timing.REF, sdramPeriod, phyClkRatio);
    s32 cRAS = t2c(activePhase     , prechargePhase                 , timing.RAS, sdramPeriod, phyClkRatio);
    s32 cRP  = t2c(prechargePhase  , activePhase                    , timing.RP, sdramPeriod, phyClkRatio);
    s32 cRFC = t2c(activePhase     , activePhase                    , timing.RFC, sdramPeriod, phyClkRatio);
    s32 cRRD = t2c(activePhase     , activePhase                    , MAX(timing.RRD, cRRD_MIN*sdramPeriod), sdramPeriod, phyClkRatio);
    s32 cRCD = t2c(activePhase     , MIN(writePhase, readPhase), timing.RCD, sdramPeriod, phyClkRatio);
    s32 cRTW = t2c(readPhase       , writePhase                     , (rl+bl+cRTW_IDLE-wl)*sdramPeriod, sdramPeriod, phyClkRatio);
    s32 cRTP = t2c(readPhase       , prechargePhase                 , MAX(timing.RTP, bl*sdramPeriod), sdramPeriod, phyClkRatio);
    s32 cWTR = t2c(writePhase      , readPhase                      , MAX(timing.WTR, bl*sdramPeriod) + (wl+bl)*sdramPeriod, sdramPeriod, phyClkRatio);
    s32 cWTP = t2c(writePhase      , prechargePhase                 , timing.WTP + (wl+bl+cWTP_ADD-1)*sdramPeriod, sdramPeriod, phyClkRatio);
    s32 cFAW = t2c(activePhase     , activePhase                    , timing.FAW, sdramPeriod, phyClkRatio);

    write_u32( (prechargePhase << 24) | (activePhase << 16) | (readPhase << 8)   | (writePhase << 0), core + SDRAM_PHASE);
    write_u32( 0, core + SDRAM_WRITE_LATENCY);
    write_u32( SDRAM_NO_ACTIVE, core + SDRAM_CONFIG);

    write_u32(cREF-1, core + SDRAM_TIMING_REF);
    write_u32((sat(cRRD-2) << 24) | (sat(cRFC-2) << 16) | (sat(cRP-2) << 8)   | (sat(cRAS-2) << 0), core + SDRAM_TIMING_0);
    write_u32(                                                                   (sat(cRCD-2) << 0), core + SDRAM_TIMING_1);
    write_u32((sat(cWTP-2) << 24)  | (sat(cWTR-2) << 16) | (sat(cRTP-2) << 8)   | (sat(cRTW-2) << 0), core + SDRAM_TIMING_2);
    write_u32(sat(cFAW-1), core + SDRAM_FAW);

    s32 ODTend = (1 << (writePhase + 6)%phyClkRatio)-1;
    if(ODTend == 0) ODTend = (1 << phyClkRatio)-1;
    s32 ODT = (writePhase+6+phyClkRatio-1)/phyClkRatio-1;
    write_u32((ODT << 0) | (ODTend << 8), core + SDRAM_ODT);
}

static void sdram_sdr_init(u32 core,  u32 rl, u32 ctrlBurstLength, u32 phyClkRatio){
    u32 blMod;
    u32 bl = ctrlBurstLength*phyClkRatio;
    switch(bl){
    case 1: blMod = 0; break;
    case 2: blMod = 1; break;
    case 4: blMod = 2; break;
    case 8: blMod = 3; break;
    }

    write_u32(rl-1, core + SDRAM_READ_LATENCY);

    write_u32(0, core + SDRAM_SOFT_CLOCKING); sdram_udelay(100);
    write_u32(SDRAM_CKE, core + SDRAM_SOFT_CLOCKING); sdram_udelay(100);
    sdram_command(core, SDRAM_PRE,0,0x400);
    sdram_command(core, SDRAM_REF,0,0x000);
    sdram_command(core, SDRAM_REF,0,0x000);
    sdram_command(core, SDRAM_MOD,0,(rl << 4) | blMod);
    write_u32(SDRAM_AUTO_REFRESH, core + SDRAM_CONFIG);
}

static void sdram_ddr2_init(u32 core, u32 rl, SdramTiming timing, u32 ctrlBurstLength, u32 phyClkRatio, u32 sdramPeriod){
    u32 al = 0;
    u32 bl = ctrlBurstLength*phyClkRatio;

    write_u32(0, core + SDRAM_SOFT_CLOCKING);
    sdram_udelay(200);
    write_u32(SDRAM_CKE, core + SDRAM_SOFT_CLOCKING);
    sdram_udelay(10);

    u32 emr1 = ((al & 7) << 3) | 0x44;
    u32 wr = (timing.WTP+sdramPeriod-1)/sdramPeriod;
    sdram_command(core, SDRAM_PRE, 0, 0x400);
    sdram_command(core, SDRAM_MOD, 2, 0);
    sdram_command(core, SDRAM_MOD, 3, 0);
    sdram_command(core, SDRAM_MOD, 1, emr1);
    sdram_command(core, SDRAM_MOD, 0, 0x100); sdram_udelay(20);
    sdram_command(core, SDRAM_PRE, 0, 0x400);
    sdram_command(core, SDRAM_REF, 0, 0x000);
    sdram_command(core, SDRAM_REF, 0, 0x000);
    sdram_command(core, SDRAM_MOD, 0, (((wr - 1) & 7) << 9) | ((rl & 7) << 4) | ((bl & 15) >> 3) | 2); sdram_udelay(20);
    sdram_command(core, SDRAM_MOD, 1, emr1 | 0x380);
    sdram_command(core, SDRAM_MOD, 1, emr1);
    sdram_udelay(10);

    write_u32(SDRAM_AUTO_REFRESH, core + SDRAM_CONFIG);
}

static void sdram_ddr3_init(u32 core,  u32 rl, u32 wl, u32 ctrlBurstLength, u32 phyClkRatio){
    static const uint8_t wrToMr[] = {1,2,3,4,-1,5,-1,6,-1,7,-1,0};
    static const uint8_t rlToMr[] = {2,4,6,8,10,12,14,1,3,5};

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

static u32 sdram_mem_test(u32 address, u32 range){
    write_u32(0xFFFF0000, address + 0);
    write_u32(0x5555AAAA, address + 4);
    write_u32(0x11112222, address + 8);
    write_u32(0x33334444, address + 12);

    asm(".word(0x500F)"); //Flush data cache
    u32 d0 = read_u32(0x80000000);
    u32 d1 = read_u32(0x80000004);
    u32 d2 = read_u32(0x80000008);
    u32 d3 = read_u32(0x8000000C);

    return d0 != 0xFFFF0000 || d1 != 0x5555AAAA || d2 != 0x11112222 || d3 != 0x33334444;
}

static void sdram_phy_s7(u32 core, u32 phy){
    bsp_putString("\nS7 phy calibration\n");
    while(1){
        bsp_putString("  DQ eye : ");

        u32 eye_start      = 0;
        u32 eye_best_start = 0;
        u32 eye_best_last  = 0;
        for(u32 dq_delay = 0; dq_delay < 64; dq_delay++){
            write_u32(dq_delay, phy + SDRAM_S7_IDELAY_VALUE);
            write_u32(0xFFFFFFFF, phy + SDRAM_S7_IDELAY_LOAD_DQ);
            write_u32(0x00000000, phy + SDRAM_S7_IDELAY_LOAD_DQ);
            u32 mem_test_success = 0;
            for(s32 readLatency = 0;readLatency < 4;readLatency++){
                write_u32(readLatency, core + SDRAM_READ_LATENCY);

                for(s32 bitsleep = 0; bitsleep < 8;bitsleep++){
                    write_u32(0xFFFFFFFF, phy + SDRAM_S7_BITSLEEP);
                    mem_test_success |= sdram_mem_test(0x80000000, 16) == 0;
                }
            }
            if(mem_test_success){
                bsp_putChar('X');
                if(dq_delay - eye_start > eye_best_last - eye_best_start){
                    eye_best_start = eye_start;
                    eye_best_last = dq_delay;
                }
            }else{
                bsp_putChar('.');
                eye_start = dq_delay + 1;
            }
        }

        bsp_putChar('\n');

        u32 idelay = (eye_best_start + eye_best_last) >> 1;
        for(u32 i = 0;i < 11 + idelay;i++) bsp_putChar(' ');
        bsp_putString("^\n");

        write_u32(idelay, phy + SDRAM_S7_IDELAY_VALUE);
        write_u32(0xFFFFFFFF, phy + SDRAM_S7_IDELAY_LOAD_DQ);
        write_u32(0x00000000, phy + SDRAM_S7_IDELAY_LOAD_DQ);
        for(s32 readLatency = 0;readLatency < 4;readLatency++){
            write_u32(readLatency, core + SDRAM_READ_LATENCY);

            for(s32 bitsleep = 0; bitsleep < 8;bitsleep++){
                write_u32(0xFFFFFFFF, phy + SDRAM_S7_BITSLEEP);
                u32 memTest = sdram_mem_test(0x80000000, 16);
                if(!memTest) {
                    bsp_putString("  OK\n");
                    return;
                }
            }
        }
    }
}

#endif /* SDRAM_H_ */



