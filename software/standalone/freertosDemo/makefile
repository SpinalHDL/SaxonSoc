PROJ_NAME=freertosDemo

STANDALONE = ..
FREERTOS=../../../ext/FreeRTOS-mirror/FreeRTOS/Source
FREERTOS_RISCV=${FREERTOS}/portable/GCC/RISC-V

SRCS = 	${shell find src -type f -name \*.c} \
		${shell find src -type f -name \*.cpp}  \
		${shell find src -type f -name \*.S} \
    	${shell find ${FREERTOS} -maxdepth 1 -type f -name \*.c} \
        ${FREERTOS}/portable/MemMang/heap_4.c \
        ${FREERTOS_RISCV}/port.c \
        ${FREERTOS_RISCV}/portASM.S \

CFLAGS += -Isrc
CFLAGS += -I${FREERTOS}/include
CFLAGS += -I${FREERTOS_RISCV}
CFLAGS += -DportasmHANDLE_INTERRUPT=external_interrupt_handler

include ${STANDALONE}/common/bsp.mk
include ${STANDALONE}/common/riscv64-unknown-elf.mk
include ${STANDALONE}/common/standalone.mk

