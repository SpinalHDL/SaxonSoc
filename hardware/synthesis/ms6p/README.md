# saxon on miniSpartan6+

## build saxon rtl

```sh
cd SaxonSoc/
cd ext/SpinalHDL
sbt clean
cd -
sbt "runMain saxon.board.scarab.MS6PLinux"
```

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

## build and program hw

```sh
cd hardware/synthesis/ms6p/
source /opt/Xilinx/ISE/14.7/ISE_DS/settings64.sh
xtclsh build.tcl rebuild_project
xc3sprog -c ftdi work/top.bit
```

## debug

```sh
#terminal 1
openocd/src/openocd -f interface/ftdi/ft2232h_breakout.cfg -c "set CPU0_YAML $PWD/SaxonSoc_dev/cpu0.yaml" -f target/saxon.cfg -s openocd/tcl

#terminal 2
#/opt/riscv/bin/riscv64-unknown-elf-gdb SaxonSoc_dev/software/standalone/bootloader/build/bootloader.elf --eval-command "target remote :3333"
#set $pc=0x20000000
/opt/riscv/bin/riscv64-unknown-elf-gdb SaxonSoc_dev/software/standalone/machineModeSbi/build/machineModeSbi.elf --eval-command "target remote :3333"
load
cont
```
