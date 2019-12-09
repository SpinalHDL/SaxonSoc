RISCV_BIN ?= riscv64-unknown-elf-
RISCV_CC=${RISCV_BIN}gcc
RISCV_OBJCOPY=${RISCV_BIN}objcopy
RISCV_OBJDUMP=${RISCV_BIN}objdump

MARCH := rv32i
BENCH ?= no
DEBUG?=no
DEBUG_Og?=yes

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
ifeq ($(DEBUG_Og),no)
	CFLAGS += -g3 -O0
else
	CFLAGS += -g3 -Og
endif
endif

ifeq ($(DEBUG),no)
ifeq ($(BENCH),no)
	CFLAGS += -Os
else
	CFLAGS += -O3
endif
endif

CFLAGS += -march=$(MARCH) -mabi=ilp32 -DUSE_GP
LDFLAGS += -march=$(MARCH) -mabi=ilp32

