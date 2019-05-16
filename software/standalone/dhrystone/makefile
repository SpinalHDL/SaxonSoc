PROJ_NAME=dhrystone
DEBUG=no
MULDIV=no
COMPRESSED=no
STANDALONE = ..

CFLAGS += -fno-inline -fno-common
CFLAGS += -DTIME -DCORE_HZ=12000000ll

SRCS = 	$(wildcard src/*.c) \
		$(wildcard src/*.cpp)  \
		$(wildcard src/*.S)


LDSCRIPT = ${STANDALONE}/common/linker_ram.ld

include ${STANDALONE}/common/riscv64-unknown-elf.mk
include ${STANDALONE}/common/standalone.mk

