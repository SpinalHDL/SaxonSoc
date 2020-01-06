PROJ_NAME=timerAndGpioInterruptDemo

STANDALONE = ..


SRCS = 	${shell find src -type f -name \*.c} \
		${shell find src -type f -name \*.cpp}  \
		${shell find src -type f -name \*.S} \
        ${STANDALONE}/common/start.S \
        ${STANDALONE}/common/trap.S


include ${STANDALONE}/common/bsp.mk
include ${STANDALONE}/common/riscv64-unknown-elf.mk
include ${STANDALONE}/common/standalone.mk

