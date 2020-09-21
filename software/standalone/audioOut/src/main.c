#include "type.h"

#include "bsp.h"
#include "audioOutConfig.h"
#include "dmasg.h"
#include "math.h"

#define AUDIO_OUT_STATUS_RUN BIT_0
#define AUDIO_OUT_STATUS 0x10
#define AUDIO_OUT_RATE 0x14

volatile struct dmasg_descriptor descriptors[2]  __attribute__ ((aligned (64)));

//(0 to 127).map(i => Math.sin(i*Math.PI*2/128)*0x3FFF toShort).mkString(",")
s16 lut[] = {0,803,1605,2403,3196,3980,4755,5519,6269,7004,7722,8422,9101,9759,10393,11002,11584,12139,12664,13158,13621,14052,14448,14810,15135,15425,15677,15892,16068,16205,16304,16363,16383,16363,16304,16205,16068,15892,15677,15425,15135,14810,14448,14052,13621,13158,12664,12139,11584,11002,10393,9759,9101,8422,7722,7004,6269,5519,4755,3980,3196,2403,1605,803,0,-803,-1605,-2403,-3196,-3980,-4755,-5519,-6269,-7004,-7722,-8422,-9101,-9759,-10393,-11002,-11584,-12139,-12664,-13158,-13621,-14052,-14448,-14810,-15135,-15425,-15677,-15892,-16068,-16205,-16304,-16363,-16383,-16363,-16304,-16205,-16068,-15892,-15677,-15425,-15135,-14810,-14448,-14052,-13621,-13158,-12664,-12139,-11584,-11002,-10393,-9759,-9101,-8422,-7722,-7004,-6269,-5519,-4755,-3980,-3196,-2403,-1605,-803};


void main() {
    bsp_putString("Audio out demo\n");

    // Init the sound buffer with a ramp
    s16 (*samples)[2] = (s16 (*)[2])AUDIO_OUT_BUFFER_BASE;
    for(u32 sampleId = 0; sampleId < AUDIO_OUT_BUFFER_SAMPLES; sampleId++){
            samples[sampleId][0] = lut[sampleId]; //sinf(sampleId/16.0f)*0x7FFF;
            samples[sampleId][1] = sampleId << 9;
    }

    // Init the sigma delta DAC
    write_u32(BSP_CLINT_HZ/48000, AUDIO_OUT_BASE + AUDIO_OUT_RATE);
    write_u32(2, AUDIO_OUT_BASE + AUDIO_OUT_STATUS);

    // If the were used in direct control / self reload mode :
//    dmasg_input_memory(DMASG_BASE, AUDIO_OUT_CHANNEL,  (u32)AUDIO_OUT_BUFFER_BASE, 0);
//    dmasg_output_stream (DMASG_BASE, AUDIO_OUT_CHANNEL, AUDIO_OUT_PORT, 0, AUDIO_OUT_SINK, 1);
//    dmasg_direct_start(DMASG_BASE, AUDIO_OUT_CHANNEL, AUDIO_OUT_BUFFER_SIZE, 1);
//    while(1);

    // Configure the DMA in linked list mode
    descriptors[0].control = AUDIO_OUT_BUFFER_SIZE/2-1; //Transfer the half of the buffer
    descriptors[0].from    = (u32) AUDIO_OUT_BUFFER_BASE;
    descriptors[0].to      = 0; // can be anything
    descriptors[0].next    = (u32) (descriptors + 1);
    descriptors[0].status  = 0; //Clear the completion flag

    descriptors[1].control = AUDIO_OUT_BUFFER_SIZE/2-1; //Transfer the (second) half of the buffer
    descriptors[1].from    = (u32) AUDIO_OUT_BUFFER_BASE + AUDIO_OUT_BUFFER_SIZE/2;
    descriptors[1].to      = 0; // can be anything
    descriptors[1].next    = (u32) (descriptors + 0);
    descriptors[1].status  = 0; //Clear the completion flag

    dmasg_input_memory(DMASG_BASE, AUDIO_OUT_CHANNEL, 0, 16); // (the address do not care as it will be loaded by the linked list
    dmasg_output_stream (DMASG_BASE, AUDIO_OUT_CHANNEL, AUDIO_OUT_PORT, 0, AUDIO_OUT_SINK, 1);
    dmasg_linked_list_start(DMASG_BASE, AUDIO_OUT_CHANNEL, (u32) descriptors);
    bsp_putString("running\n");

    //actively wait for descriptor completion to re-enable them ASAP
    while(1){
        while(!(descriptors[0].status & DMASG_DESCRIPTOR_STATUS_COMPLETED));
        bsp_putString("0");
        descriptors[0].status  = 0;
        while(!(descriptors[1].status & DMASG_DESCRIPTOR_STATUS_COMPLETED));
        bsp_putString("1");
        descriptors[1].status  = 0;
    }
}


