PROJ_NAME=freertosDemo

STANDALONE = ..
FREERTOS=../../../ext/FreeRTOS-mirror/FreeRTOS/Source
FREERTOS_RISCV=${FREERTOS}/portable/GCC/RISC-V

SRCS = 	$(wildcard src/*.c) \
		$(wildcard src/*.cpp) \
		$(wildcard src/*.S) \
		$(wildcard ${FREERTOS}/*.c) \
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

