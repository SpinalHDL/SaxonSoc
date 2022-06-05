PROJ_NAME=xyloni

STANDALONE = ../../standalone
BSP_PATH = ./bsp

SRCS = 	$(wildcard src/*.c) \
		$(wildcard src/*.cpp) \
		$(wildcard src/*.S) \
		${STANDALONE}/common/start.S



CFLAGS += -I${BSP_PATH}/include
CFLAGS += -I${BSP_PATH}/app

include ${BSP_PATH}/include/soc.mk

LDSCRIPT ?= ${BSP_PATH}/linker/default.ld


include ${STANDALONE}/common/riscv64-unknown-elf.mk
include ${STANDALONE}/common/standalone.mk
