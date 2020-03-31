#ROOT shall point to location of build.sbt
ROOT=../../..
SWBASE=$(ROOT)/software/standalone
SOCSW=bootloader
SOCMEMSRC=$(SWBASE)/$(SOCSW)/build/$(SOCSW).elf
SOCMEM=build/soc.mem

TOP=ArtyA7Linux
TOPV=../../netlist/$(TOP).v

RISCV_BIN?=/opt/riscv/bin/riscv64-unknown-elf-

all : build/latest.bit

repo-setup:
	git submodule update --init --recursive
	(cd $(ROOT)/..;git clone https://github.com/SpinalHDL/u-boot.git -b saxon u-boot)

spi-flash-sw:
	RISCV_BIN=$(RISCV_BIN) make -C $(SWBASE)/machineModeSbi clean all BSP=digilent/ArtyA7Linux
	(cd $(ROOT)/../u-boot;CROSS_COMPILE=$(RISCV_BIN) make saxon_arty7_defconfig)
	CROSS_COMPILE=$(RISCV_BIN) make -C $(ROOT)/../u-boot -j$(nproc)

sdcard:
	make -C $(ROOT) formatsdcard
	make -C $(ROOT) linux2sdcard

generate:
	(cd ../../..; sbt "runMain saxon.board.digilent.ArtyA7Linux")

$(TOPV) : $(SOCMEM) generate


.PHONY: $(SOCMEMSRC)
$(SOCMEMSRC):
	mkdir -p build
	RISCV_BIN=$(RISCV_BIN) make -C $(SWBASE)/$(SOCSW) BSP=digilent/ArtyA7Linux

$(SOCMEM) : $(SOCMEMSRC)
	cp -u $(SOCMEMSRC) $(SOCMEM)

build/latest.bit : arty_a7.xdc $(TOPV)
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
	make -C $(SWBASE)/$(SOCSW) clean BSP=digilent/ArtyA7Linux

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
