PROJ_NAME=bootloader
STANDALONE = ..


SRCS = 	$(wildcard src/*.c) \
		$(wildcard src/*.cpp) \
		$(wildcard src/*.S) \
        ${STANDALONE}/common/start.S

LDSCRIPT ?= ${BSP_PATH}/linker/bootloader.ld
CFLAGS += -fno-builtin -fPIC -DNO_LIBC_INIT_ARRAY
LDFLAGS += -fno-builtin -fPIC

include ${STANDALONE}/common/bsp.mk
include ${STANDALONE}/common/riscv64-unknown-elf.mk
include ${STANDALONE}/common/standalone.mk

