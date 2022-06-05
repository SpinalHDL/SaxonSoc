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
cd hardware/synthesis/ms6p/
source /opt/Xilinx/ISE/14.7/ISE_DS/settings64.sh
xtclsh build.tcl rebuild_project

#flash qspi and power cycle
xc3sprog -c ftdi bscan_spi_s6lx9_ftg256.bit
xc3sprog -c ftdi -I work/top.bit:W:0x00000000:BIT ../../../software/standalone/machineModeSbi/build/machineModeSbi.bin:W:0x00060000:BIN ../../../../u-boot/u-boot.bin:W:0x00080000:BIN
```

## run

```sh
minicom -D /dev/ttyUSB1
load mmc 0:1 803fffc0 uImage
load mmc 0:1 807f0000 dtb
load mmc 0:1 807fffc0 rootfs.cpio.uboot
bootm 803fffc0 807fffc0 807f0000
```

## debug (optional)

```sh
#terminal 1
openocd/src/openocd -f interface/ftdi/ft2232h_breakout.cfg -c "set CPU0_YAML $PWD/SaxonSoc/cpu0.yaml" -f target/saxon.cfg -s openocd/tcl

#terminal 2
/opt/riscv/bin/riscv64-unknown-elf-gdb SaxonSoc/software/standalone/machineModeSbi/build/machineModeSbi.elf --eval-command "target remote :3333"
load
restore u-boot/u-boot.bin binary 0x80200000
cont
```
