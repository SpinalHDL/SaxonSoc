ZEPHYR=../zephyr/zephyrSpinalHdl
SHELL=/bin/bash
NETLIST_DEPENDENCIES=$(shell find hardware/scala -type f)
.ONESHELL:
ROOT=$(shell pwd)
SIZELIMIT?=15931539456

saxonUp5kEvn_prog_icecube2:
	iceprog -o 0x00000 hardware/synthesis/icecube2/icecube2_Implmnt/sbt/outputs/bitmap/SaxonUp5kEvn_bitmap.bin

saxonUp5kEvn_prog_icecube2_soft:
	iceprog -S -o 0x00000 hardware/synthesis/icecube2/icecube2_Implmnt/sbt/outputs/bitmap/SaxonUp5kEvn_bitmap.bin


saxonUp5kEvn_prog_demo: software/bootloader
	iceprog -o 0x100000 software/bootloader/up5kEvnDemo.bin

formatsdcard:
	./formatsdcard.sh $(SIZELIMIT) $(SDCARD)

linux2sdcard:
	./linux2sdcard.sh $(SIZELIMIT) $(SDCARD)

.PHONY: software/bootloader
software/bootloader:
	source ${ZEPHYR}/zephyr-env.sh
	make -C software/bootloader all
