#ROOT shall point to location of build.sbt
ROOT=../../..
SWBASE=$(ROOT)/software/standalone
SOCSW=bootloader
SOCMEMSRC=$(SWBASE)/$(SOCSW)/build/$(SOCSW).elf
SOCMEM=build/soc.mem

TOP=Arty7Linux
TOPV=../../netlist/$(TOP).v

RISCV_BIN?=/opt/riscv/bin/riscv64-unknown-elf-

all : build/latest.bit

repo-setup:
	cd $(ROOT)
	git submodule update --init --recursive
	cd ..
	git clone https://github.com/SpinalHDL/u-boot.git -b saxon u-boot
	cd -

spi-flash-sw:
	cd $(ROOT)

	cd software/standalone/machineModeSbi/
	RISCV_BIN=/opt/riscv/bin/riscv64-unknown-elf- make clean all BSP=Arty7Linux
	cd -

	cd ../u-boot/
	CROSS_COMPILE=/opt/riscv/bin/riscv64-unknown-elf- make saxon_arty7_defconfig
	CROSS_COMPILE=/opt/riscv/bin/riscv64-unknown-elf- make -j$(nproc)
	cd -

git clone https://github.com/SpinalHDL/u-boot.git -b saxon u-boot

$(TOPV) : $(SOCMEM)
	(cd ../../..; sbt "runMain saxon.board.digilent.Arty7Linux")

.PHONY: $(SOCMEMSRC)
$(SOCMEMSRC):
	mkdir -p build
	RISCV_BIN=$(RISCV_BIN) make -C $(SWBASE)/$(SOCSW) BSP=Arty7Linux

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
	make -C $(SWBASE)/$(SOCSW) clean BSP=Arty7Linux

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
