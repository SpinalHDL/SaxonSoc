# SaxonSoc on Lichee-Tang

## get Sipeed IDE

 - Download IDE TD1909_linux.rar from:
http://dl.sipeed.com/TANG/Premier/IDE
 - Unpack it to `~/` folder
 - Create symlink to `/opt/TD`

```sh
sudo ln -s ~/TD_RELEASE_September2019_r4.6.2 /opt/TD
```

## get Yosys (optional)

 - Install yosys following:
https://tang.sipeed.com/en/dev-guide/using-yosys/
 - edit `build.sh` and `build.tcl` to uncomment lines there when building with yosys

## build software

```sh
cd software/standalone/machineModeSbi/
RISCV_BIN=/opt/riscv/bin/riscv64-unknown-elf- make clean all BSP=TangLinux
cd -

cd ../u-boot/
CROSS_COMPILE=/opt/riscv/bin/riscv64-unknown-elf- make saxon_tang_defconfig
CROSS_COMPILE=/opt/riscv/bin/riscv64-unknown-elf- make
cd -
```

## build saxon rtl

```sh
cd SaxonSoc/
cd ext/SpinalHDL
sbt clean
cd -
sbt "runMain saxon.board.sipeed.TangLinux"
```

## build and program hw

```sh
cd hardware/synthesis/tang/
./build.sh
```

## debug

```sh
#terminal 1
openocd/src/openocd -f interface/ftdi/ft2232h_breakout.cfg -c "set CPU0_YAML $PWD/SaxonSoc/cpu0.yaml" -f target/saxon.cfg -s openocd/tcl

#terminal 2
/opt/riscv/bin/riscv64-unknown-elf-gdb SaxonSoc/software/standalone/machineModeSbi/build/machineModeSbi.elf --eval-command "target remote :3333"
monitor reset halt
load
restore u-boot/u-boot.bin binary 0x80200000
add-symbol-file u-boot/u-boot 0x80200000
b cpu_init_f
c
print/x ((gd_t *)$x3)->relocaddr
#add-symbol-file u-boot/u-boot 0xrelocaddr

#restore u-boot/spl/u-boot-spl.bin binary 0x80200000
#add-symbol-file u-boot/spl/u-boot-spl 0x80200000


#terminal 3
minicom -D /dev/ttyUSB1
mmc info
load mmc 0:1 80007fc0 uImage
load mmc 0:1 80007000 dtb
#bootm 80007fc0 - 80007000
```
