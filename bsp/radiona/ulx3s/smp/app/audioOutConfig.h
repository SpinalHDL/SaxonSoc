#pragma once

#include "bsp.h"

#define DMASG_BASE SYSTEM_DMA_CTRL
#define AUDIO_OUT_BASE SYSTEM_AUDIO_OUT_CTRL
#define AUDIO_OUT_CHANNEL 1
#define AUDIO_OUT_SINK 0
#define AUDIO_OUT_PORT 0


#define AUDIO_OUT_BUFFER_BASE 0x81000000
#define AUDIO_OUT_BUFFER_SAMPLES 10000
#define AUDIO_OUT_BUFFER_SIZE AUDIO_OUT_BUFFER_SAMPLES*2
