PROJ_NAME=dhrystone

DEBUG=no
BENCH=yes
STANDALONE = ..

CFLAGS += -fno-inline -fno-common
CFLAGS += -DTIME -DCORE_HZ=12000000ll

SRCS = 	${shell find src -type f -name \*.c} \
		${shell find src -type f -name \*.cpp}  \
		${shell find src -type f -name \*.S} \
        ${STANDALONE}/common/start.S

include ${STANDALONE}/common/bsp.mk
include ${STANDALONE}/common/riscv64-unknown-elf.mk
include ${STANDALONE}/common/standalone.mk

