BSP_PATH ?= ${STANDALONE}/../../bsp/${BSP}
CFLAGS += -I${BSP_PATH}/include
CFLAGS += -I${BSP_PATH}/app

include ${BSP_PATH}/include/soc.mk

LDSCRIPT ?= ${BSP_PATH}/linker/default.ld