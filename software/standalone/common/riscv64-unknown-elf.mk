ifneq (, $(shell which riscv64-unknown-elf-gcc))
RISCV_BIN ?= riscv64-unknown-elf-
else
ifneq (, $(shell which riscv-none-embed-gcc))
RISCV_BIN ?= riscv-none-embed-
else
$(error No RISC-V toolchain detected, please install riscv-none-embed- from xpack)
endif
endif

RISCV_CC=${RISCV_BIN}gcc
RISCV_OBJCOPY=${RISCV_BIN}objcopy
RISCV_OBJDUMP=${RISCV_BIN}objdump

MARCH := rv32i
BENCH ?= no
DEBUG?=no
DEBUG_Og?=no

ifeq ($(RV_M),yes)
	MARCH := $(MARCH)m
endif

#ifeq ($(RV_A),yes)
#	MARCH := $(MARCH)a
#else ifeq ($(RV_C),yes)
#	MARCH := $(MARCH)ac
#endif

ifeq ($(RV_C),yes)
	MARCH := $(MARCH)ac
endif


ifeq ($(DEBUG),yes)
ifneq ($(DEBUG_Og),yes)
	CFLAGS += -g3 -O0
else
	CFLAGS += -g3 -Og
endif
endif

ifneq ($(DEBUG),yes)
ifneq ($(BENCH),yes)
	CFLAGS += -Os
else
	CFLAGS += -O3
endif
endif

CFLAGS += -march=$(MARCH) -mabi=ilp32 -DUSE_GP
LDFLAGS += -march=$(MARCH) -mabi=ilp32

