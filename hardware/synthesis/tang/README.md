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
add-symbol-file u-boot/spl/u-boot-spl 0x20004000
c

#terminal 3
minicom -D /dev/ttyUSB0
```
