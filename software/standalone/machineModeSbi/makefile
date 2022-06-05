PROJ_NAME=machineModeSbi
STANDALONE = ..


SRCS = 	$(wildcard src/*.c) \
		$(wildcard src/*.cpp) \
		$(wildcard src/*.S)


include ${STANDALONE}/common/bsp.mk
SRCS += $(BSP_PATH)/app/machineModeSbiHal.c
CFLAGS += -Isrc

include ${STANDALONE}/common/riscv64-unknown-elf.mk
include ${STANDALONE}/common/standalone.mk

