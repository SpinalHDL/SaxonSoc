# saxon on miniSpartan6+

## build software

```sh
cd software/standalone/bootloader/
RISCV_BIN=/opt/riscv/bin/riscv64-unknown-elf- make clean all BSP=MS6PLinux
cd -

cd software/standalone/machineModeSbi/
RISCV_BIN=/opt/riscv/bin/riscv64-unknown-elf- make clean all BSP=MS6PLinux
cd -

cd ../u-boot/
CROSS_COMPILE=/opt/riscv/bin/riscv64-unknown-elf- make saxon_defconfig
CROSS_COMPILE=/opt/riscv/bin/riscv64-unknown-elf- make
cd -
```

## storage

SPI flash
 - 0x00000000: fpga 0x00060000 (96K)
 - 0x00060000: sbi 0x00010000 (16K)
 - 0x00070000: u-boot-spl 0x00030000 (48K)

SD card
 - FAT partition mmc 0:1, which contains uImage and dtb binaries
 - EXT4 partition mmc 0:2, which contains rootfs

## memory

ROM
 - 0x20000000: bootloader 0x00000800 (2K)

SDRAM
 - 0x80000000: sbi 0x00010000 (16K)
 - 0x80010000: u-boot-spl 0x00030000 (48K)
 - 0x8007f000: dtb 0x00001000 (4K)
 - 0x80080000: kernel 0x00400000 (4M)

## build saxon rtl

```sh
cd SaxonSoc/
cd ext/SpinalHDL
sbt clean
cd -
sbt "runMain saxon.board.scarab.MS6PLinux"
```

## build and program hw

```sh
#make bitstream
cd hardware/synthesis/ms6p/
source /opt/Xilinx/ISE/14.7/ISE_DS/settings64.sh
xtclsh build.tcl rebuild_project

#write images to spi flash and power cycle
xc3sprog -c ftdi bscan_spi_s6lx9_ftg256.bit
xc3sprog -c ftdi -I work/top.bit:W:0x00000000:BIT ../../../software/standalone/machineModeSbi/build/machineModeSbi.bin:W:0x00060000:BIN ../../../../u-boot/spl/u-boot-spl.bin:W:0x00070000:BIN
```

## run

On POR the bootloader in ROM is executed. The bootloader initialkize SDRAM and loads sbi and u-boot-spl binaries to SDRAM. Then it jumps to sbi. The sbi initialize CSR and jumps to u-boot-spl. Finally u-boot-spl loads Linux kernel and device tree from SD to SDRAM and boots Linux.

```sh
minicom -D /dev/ttyUSB1
```

## debug (optional)

```sh
#terminal 1
openocd/src/openocd -f interface/ftdi/ft2232h_breakout.cfg -c "set CPU0_YAML $PWD/SaxonSoc/cpu0.yaml" -f target/saxon.cfg -s openocd/tcl

#terminal 2
/opt/riscv/bin/riscv64-unknown-elf-gdb SaxonSoc/software/standalone/machineModeSbi/build/machineModeSbi.elf --eval-command "target remote :3333"
load
restore u-boot/spl/u-boot-spl.bin binary 0x80010000
cont
```
