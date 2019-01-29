CFLAGS= -march=rv32i -mabi=ilp32 -MD
LFLAGS= -nostdlib  -mcmodel=medany -nostartfiles -ffreestanding -fPIC -fPIE

RISCV_BIN ?= riscv64-unknown-elf-
RISCV_GCC=${RISCV_BIN}gcc
RISCV_OBJCOPY=${RISCV_BIN}objcopy
RISCV_OBJDUMP=${RISCV_BIN}objdump

%.o: %.S
	$(RISCV_GCC) -c $(CFLAGS) -o $@ $^

%.elf: %.S
	$(RISCV_GCC)  $(CFLAGS) -o $@ $^ $(LFLAGS) -Wl,-Bstatic,-T,mapping.ld,-Map,$@.map,--print-memory-usage
	$(RISCV_OBJCOPY) --remove-section=.crap $@
	$(RISCV_OBJDUMP) -S -d $@ > $@.asm

%.bin: %.elf
	$(RISCV_OBJCOPY) -O binary $^ $@

all: up5kEvn.bin up5kEvnDemo.bin

clean:
	rm -rf *.d
	rm -rf *.bin
	rm -rf *.o
	rm -rf *.elf.*