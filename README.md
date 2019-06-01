## Description

This repo experiment multiple things at once :
- The BMB (Banana Memory Bus) which can cover both cached and cacheless SoC without compromises
- A hardware description paradigm made of generators and depedancies which should be able to solve SoC toplevel hell
- Linux on VexRiscv

## Various commands

openocd =>
https://github.com/SpinalHDL/openocd_riscv.git

openocd for simulation
src/openocd -f tcl/interface/jtag_tcp.cfg -c 'set SAXON_CPU0_YAML ../SaxonSoc/cpu0.yaml' -f tcl/target/saxon_xip.cfg


Zephyr build =>
git clone https://github.com/SpinalHDL/zephyr.git -b vexriscv

cd zephyr
unset ZEPHYR_GCC_VARIANT
unset ZEPHYR_SDK_INSTALL_DIR
export CROSS_COMPILE="/opt/riscv/bin/riscv64-unknown-elf-"
export ZEPHYR_TOOLCHAIN_VARIANT="cross-compile"
export ZEPHYR_GCC_VARIANT="cross-compile"
source zephyr-env.sh

cd samples/philosophers
mkdir build
cd build

cmake -DBOARD=vexriscv_saxon_up5k_evn ..
make -j${nproc}


```
git clone https://github.com/SpinalHDL/buildroot.git buildroot
git clone https://github.com/SpinalHDL/linux.git -b vexriscv --depth 100 linux
cd buildroot
make spinal_saxon_default_defconfig
make -j$(nproc); output/host/bin/riscv32-linux-objcopy  -O binary output/images/vmlinux output/images/Image
dtc -O dtb -o output/images/dtb board/spinal/saxon_default/spinal_saxon_default_de1_soc.dts
```

riscv64-unknown-elf-objdump -S -d output/images/vmlinux > output/images/vmlinux.asm
make linux-rebuild all -j$(nproc)

src/openocd -f tcl/interface/ftdi/ft2232h_breakout.cfg -c 'set BRIEY_CPU0_YAML ../SaxonSoc.git/cpu0.yaml' -f tcl/target/saxon.cfg
cu -l /dev/ttyUSB -s 1000000