#ROOT shall point to location of build.sbt
ROOT=../../../..
SWBASE=$(ROOT)/software/standalone
SOCSW=bootloader
SOCMEMSRC=$(SWBASE)/$(SOCSW)/build/$(SOCSW).elf
SOCMEM=build/soc.mem

TOP=QmtechK7SmpLinux
TOPV=../../../netlist/$(TOP).v ../../xilinx/common/RamXilinx.v

RISCV_BIN?=/opt/riscv/bin/riscv64-unknown-elf-

all : build/latest.bit

$(TOPV) : $(SOCMEM)


.PHONY: $(SOCMEMSRC)
$(SOCMEMSRC):
	mkdir -p build
	RISCV_BIN=$(RISCV_BIN) make -C $(SWBASE)/$(SOCSW) BSP=qmtech/QmtechK7SmpLinux

$(SOCMEM) : $(SOCMEMSRC)
	cp -u $(SOCMEMSRC) $(SOCMEM)

build/latest.bit : qmtech_k7.xdc $(TOPV)
	mkdir -p build
	./make_vivado_project
	cp build/vivado_project/fpga.runs/impl_1/$(TOP).bit build/latest.bit

build/latest.mcs : build/latest.bit
	./make_mcs_file

prog : build/latest.bit
	./write_fpga

flash : build/latest.mcs
	./write_flash

clean-soc-sw:
	make -C $(SWBASE)/$(SOCSW) clean BSP=qmtech/QmtechK7SmpLinux

soc-sw: clean-soc-sw $(SOCMEM)

.PHONY: clean
clean :
	rm -rf build
	mkdir build
	rm -f updatemem.jou
	rm -f updatemem.log

clean-sw: clean-soc-sw

clean-all : clean clean-sw
	rm -f $(TOPV)
	rm -f $(TOPV)_*
