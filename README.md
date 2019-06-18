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
git clone https://github.com/SpinalHDL/buildroot.git -b saxon buildroot
git clone https://github.com/SpinalHDL/linux.git -b vexriscv --depth 100 linux
cd buildroot
make spinal_saxon_default_defconfig
make linux-rebuild all -j$(nproc);
output/host/bin/riscv32-linux-objcopy  -O binary output/images/vmlinux output/images/Image;
dtc -O dtb -o output/images/dtb board/spinal/saxon_default/spinal_saxon_default_de1_soc.dts;
```

riscv64-unknown-elf-objdump -S -d output/images/vmlinux > output/images/vmlinux.asm
make linux-rebuild all -j$(nproc)

printf "\x0f\x01" > /dev/spidev0.0
echo 3 > /proc/sys/kernel/printk
dd if=/dev/zero of=speed bs=1M count=1 conv=fsync

src/openocd -f tcl/interface/ftdi/ft2232h_breakout.cfg -c 'set BRIEY_CPU0_YAML ../SaxonSoc.git/cpu0.yaml' -f tcl/target/saxon.cfg
cu -l /dev/ttyUSB -s 115200
picocom -b 115200 /dev/ttyUSB --imap lfcrlf

GPIO => https://www.emcraft.com/stm32f429discovery/controlling-gpio-from-linux-user-space

Warning, if you want to run eclipse against a simulated target, you would need to add some delay after the reset

monitor reset halt
monitor sleep 1000



## GPIO
export PIN=511
echo $PIN > /sys/class/gpio/export
echo in > /sys/class/gpio/gpio$PIN/direction
echo both > /sys/class/gpio/gpio${PIN}/edge
hello $PIN



make hello-rebuild