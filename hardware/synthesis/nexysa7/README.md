# saxon on nexysa7

## build saxon rtl

```sh
cd SaxonSoc/
cd ext/SpinalHDL
sbt clean
cd -
sbt "runMain saxon.board.digilent.NexysA7Linux"
```

## build software

```sh
cd software/standalone/bootloader/
RISCV_BIN=/opt/riscv/bin/riscv64-unknown-elf- make clean all BSP=NexysA7Linux
cd -

cd software/standalone/machineModeSbi/
RISCV_BIN=/opt/riscv/bin/riscv64-unknown-elf- make clean all BSP=NexysA7Linux
cd -

cd ../u-boot/
CROSS_COMPILE=/opt/riscv/bin/riscv64-unknown-elf- make saxon_defconfig
CROSS_COMPILE=/opt/riscv/bin/riscv64-unknown-elf- make
cd -
```

## build and program hw

```sh
cd hardware/synthesis/nexysa7/
source /opt/Xilinx/Vivado/2019.2/settings64.sh
vivado -mode batch -source build.tcl -tclargs "--rebuild"
vivado -mode batch -source build.tcl -tclargs "--flash"
```
