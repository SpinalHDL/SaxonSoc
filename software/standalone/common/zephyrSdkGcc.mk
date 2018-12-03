RISCV_BIN ?= ${ZEPHYR_SDK_INSTALL_DIR}/sysroots/x86_64-pokysdk-linux/usr/bin/riscv32-zephyr-elf/riscv32-zephyr-elf-
RISCV_CC=${RISCV_BIN}gcc
RISCV_OBJCOPY=${RISCV_BIN}objcopy
RISCV_OBJDUMP=${RISCV_BIN}objdump

MARCH := RV32I
ifeq ($(MULDIV),yes)
	MARCH := $(MARCH)M
endif
ifeq ($(COMPRESSED),yes)
	MARCH := $(MARCH)AC
endif

CFLAGS += -march=$(MARCH)
LDFLAGS += -march=$(MARCH)

