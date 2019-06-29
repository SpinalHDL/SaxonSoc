BSP_PATH ?= ${STANDALONE}/../../bsp/${BSP}
CFLAGS += -I${BSP_PATH}/include

include ${BSP_PATH}/include/soc.mk

LDSCRIPT ?= ${BSP_PATH}/linker/default.ld