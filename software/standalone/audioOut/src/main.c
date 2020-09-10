#include "type.h"

#include "bsp.h"
#include "audioOutConfig.h"
#include "dmasg.h"

#define AUDIO_OUT_STATUS_RUN BIT_0
#define AUDIO_OUT_STATUS 0x10
#define AUDIO_OUT_RATE 0x14

void main() {
    bsp_putString("Audio out demo\n");

    u16 *samples = (u16*) AUDIO_OUT_BUFFER_BASE;
    for(u32 sampleId = 0; sampleId < AUDIO_OUT_BUFFER_SAMPLES; sampleId++){
        samples[sampleId] = sampleId << 8;
    }

    dmasg_input_memory(DMASG_BASE, AUDIO_OUT_CHANNEL,  (u32)AUDIO_OUT_BUFFER_BASE, 0);
    dmasg_output_stream (DMASG_BASE, AUDIO_OUT_CHANNEL, AUDIO_OUT_PORT, 0, AUDIO_OUT_SINK, 1);
    dmasg_direct_start(DMASG_BASE, AUDIO_OUT_CHANNEL, AUDIO_OUT_BUFFER_SIZE, 1);

    write_u32(BSP_CLINT_HZ/48000, AUDIO_OUT_BASE + AUDIO_OUT_RATE);
    write_u32(AUDIO_OUT_STATUS_RUN, AUDIO_OUT_BASE + AUDIO_OUT_STATUS);

    bsp_putString("done\n");
}


