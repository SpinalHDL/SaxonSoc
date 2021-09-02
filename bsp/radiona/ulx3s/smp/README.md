## Hardware

- Ulx3s 12f, 45f or 85f ECP5 board with 32Mb or 64Mb of SDRAM
- USB micro cable
- Micro SD card
- Optional LAN8720 Ethernet board and cable
- Optional HDMI monitor and cable

## Implemented peripherals

* SPI, which provides
  * FPGA SPI flash access in Linux
  * SDCARD in Linux
  * User usage SPI
* HDMI output, which can be used with DirectFB or X11 in Linux
* Audio out (stereo) via sigma delta, Alsa driver provided
* USB host and USB device
* Uart to host and to esp32 co-processor
* GPIO access in Linux
* Ethernet MII with Linux driver
* Bit-banged I2C with Linux driver for RTC
* Optional PPPD networking via the esp32 co-processor

![SoC](assets/diagram.png?raw=true "")

## Boot sequence

The boot sequence is done in 4 steps :

* bootloader : In the OnChipRam initialized by the FPGA bitstream
  * Copy the openSbi and the u-boot binary from the FPGA SPI flash to the SDRAM
  * Jump to the openSbi binary in machine mode

* openSbi : In the SDRAM
  * Initialise the machine mode CSR to support futher supervisor SBI call
  * Jump to the u-boot binary in supervisor mode

* u-boot : In the SDRAM
  * Wait two seconds for user inputs
  * Read the linux kernel uImage and dtb from the sdcard first partition
  * Boot linux

* Linux : in the SDRAM
  * Kernel boot
  * Run Buildroot from the sdcard second partition

## Binary locations

OnChipRam:
- 0x20000000 : bootloader (~2 KB)

SDRAM:
- 0x80000000 : Linux kernel
- 0x80F80000 : openSBI, 512 KB of reserved-memory (Linux can't use that memory space)
- 0x80F00000 : u-boot

FPGA SPI flash:
- 0x340000   : openSBI
- 0x380000   : u-boot

Sdcard :
- p1:uImage  : Linux kernel
- p1:dtb     : Linux device tree binary
- p2:*       : Buildroot

## Dependencies

```
# Java JDK 8 (higher is ok)
sudo add-apt-repository -y ppa:openjdk-r/ppa
sudo apt-get update
sudo apt-get install openjdk-8-jdk -y
sudo update-alternatives --config java
sudo update-alternatives --config javac

# SBT (Scala build tool)
echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list
echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | sudo tee /etc/apt/sources.list.d/sbt_old.list
curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo apt-key add
sudo apt-get update
sudo apt-get install sbt

# RISC-V toolchain
VERSION=8.3.0-1.2
mkdir -p ~/opt
cd ~/opt
wget https://github.com/xpack-dev-tools/riscv-none-embed-gcc-xpack/releases/download/v$VERSION/xpack-riscv-none-embed-gcc-$VERSION-linux-x64.tar.gz
tar -xvf xpack-riscv-none-embed-gcc-$VERSION-linux-x64.tar.gz
rm xpack-riscv-none-embed-gcc-$VERSION-linux-x64.tar.gz
mv xpack-riscv-none-embed-gcc-$VERSION xpack-riscv-none-embed-gcc
echo 'export PATH=~/opt/xpack-riscv-none-embed-gcc/bin:$PATH' >> ~/.bashrc
export PATH=~/opt/xpack-riscv-none-embed-gcc/bin:$PATH
```

You will also need the open source fpga tools: yosys, nextpnr-ecp5, ecppack (which you can get from [YosysHQ](https://github.com/YosysHQ/fpga-toolchain) ) and [fujproj](https://github.com/kost/fujprog) on the path. 


## Clone and build

```
# Getting this repository
mkdir Ulx3sSmp 
cd Ulx3sSmp
git clone https://github.com/SpinalHDL/SaxonSoc.git -b dev-0.3 --recursive SaxonSoc

# Sourcing the build script
source SaxonSoc/bsp/radiona/ulx3s/smp/source.sh
export SAXON_CPU_COUNT=1  # SAXON_CPU_COUNT is the number of VexRiscv CPU, used for the hardware and DTS generation 
export SDRAM_SIZE=32      # 32 MB sdram
export FPGA_SIZE=85       # 85 KLUT FPGA
export SAXON_FPU=0        # Without FPU
export SAXON_USB_HOST=1   # With USB host
export SAXON_USB_DEVICE=0 # Without USB device (can't have host and device at the same time, pin conflict)
  
# Clone opensbi, u-boot, linux, buildroot, openocd
saxon_clone

# Build the FPGA bitstream
saxon_standalone_compile bootloader CFLAGS_ARGS="-DSDRAM_TIMING=MT48LC16M16A2_6A_ps"
saxon_netlist
saxon_bitstream

# Build the firmware
saxon_buildroot

# Build the programming tools
saxon_standalone_compile sdramInit CFLAGS_ARGS="-DSDRAM_TIMING=MT48LC16M16A2_6A_ps"
saxon_openocd
```

If you want once to update the repo, you can do a :

```sh
saxon_update
```

Customize SDRAM_SIZE, FPGA_SIZE and SAXON_CPU_COUNT for blue ULX3S board with 85F and 64Mb SDRAM

```sh
saxon_standalone_compile bootloader CFLAGS_ARGS="-DSDRAM_TIMING=AS4C32M16SB_7TCN_ps"
SDRAM_SIZE=64 SAXON_CPU_COUNT=4 saxon_netlist
FPGA_SIZE=85 saxon_bitstream
```

When you change the number of CPU, you need to update the linux DTB. 
It is automatically done via the `SAXON_CPU_COUNT=??? saxon_buildroot` command, but a much faster way is :  

```sh
SAXON_CPU_COUNT=??? saxon_buildroot_dts
```

You can also set the SDRAM size to 64Mb in the DTS, so than Linux uses the full 64Mb of RAM on a Blue 85f by:

```sh
SDRAM_SIZE=64 SAXON_CPU_COUNT=??? saxon_buildroot
```

or 

```sh
SDRAM_SIZE=64 SAXON_CPU_COUNT=??? saxon_buildroot_dts
```

Omitting SDRAM_SIZE is equivalent to `SDRAM_SIZE=32`.

There is now an option to include a Floating Point Unit (FPU) in the hardware. This is done by:

```sh
SDRAM_SIZE=xx SAXON_CPU_COUNT=? SAXON_FPU=1 saxon_netlist
FPGA_SIZE=85 saxon_bitstream
```

Note that the FPU will not fit on a 12f, but should fit on a 45f.

If you include the FPU in the bitstream, you must also include it in the Linux build. This is done by manually editing the following lines in $SAXON_ROOT/buildroot-spinal-saxon/configs/saxon_ulx3s_defconfig:

```
# Include the following two lines if an FPU is included in the bitstream
#BR2_RISCV_ISA_CUSTOM_RVF=y
#BR2_RISCV_ISA_CUSTOM_RVD=y
```

And changing them to:

```
# Include the following two lines if an FPU is included in the bitstream
BR2_RISCV_ISA_CUSTOM_RVF=y
BR2_RISCV_ISA_CUSTOM_RVD=y
```

You should then do:

```sh
SDRAM_SIZE=?? SAXON_CPU_COUNT=? SAXON_FPU=1 saxon_buildroot
```

### Flash SPI 

```sh
source SaxonSoc/bsp/radiona/ulx3s/smp/source.sh
cd $SAXON_ROOT
fujprog -j FLASH SaxonSoc/hardware/synthesis/radiona/ulx3s/smp/bin/toplevel.bit
fujprog -j FLASH -f 0x340000 buildroot-build/images/fw_jump.bin
fujprog -j FLASH -f 0x360000 buildroot-build/images/u-boot.bin
```

### Flash sdcard the short way : 

```
#Format the sdcard with the buildroot image, don't forget to set the SDCARD variable to point to the /dev/xxx
source SaxonSoc/bsp/radiona/ulx3s/smp/source.sh
sudo dd if=$SAXON_ROOT/buildroot-build/images/sdcard.img of=$SDCARD bs=4M conv=sync status=progress
```

### Flash sdcard the long way : 

```sh
export SDCARD=???
export SDCARD_P1=???
export SDCARD_P2=???

source SaxonSoc/bsp/radiona/ulx3s/smp/source.sh
cd $SAXON_ROOT

(
echo o
echo n
echo p
echo 1
echo
echo +100M
echo y
echo n
echo p
echo 2
echo
echo +200M
echo y
echo t
echo 1
echo b
echo p
echo w
) | sudo fdisk $SDCARD

sudo mkfs.vfat $SDCARD_P1
echo Y | sudo  mke2fs $SDCARD_P2

mkdir -p sdcard
sudo mount $SDCARD_P1 sdcard
sudo cp buildroot/output/images/linux.dtb sdcard/linux.dtb
sudo cp buildroot/output/images/uImage sdcard/uImage
sudo umount sdcard
rm -r sdcard

mkdir -p sdcard
sudo mount $SDCARD_P2 sdcard
sudo tar xf buildroot/output/images/rootfs.tar -C sdcard
sudo umount sdcard
rm -r sdcard
```




## Booting from a USB drive

```
#Format the usb drive with the buildroot image, don't forget to set the USB_DRIVE variable to point to the /dev/xxx
sudo dd if=$SAXON_ROOT/buildroot-build/images/sdcard.img of=$USB_DRIVE bs=4M conv=sync status=progress
```

```
#In U-BOOT
env set bootargs "rootwait console=hvc0 earlycon=sbi root=/dev/sda2 init=/sbin/init"
usb start; load usb 0:1 0x80000000 uImage;load usb 0:1 0x80FF0000 linux.dtb; bootm 0x80000000 - 0x80FF0000 
```

## Using peripherals

### Ethernet 

You can use a LAN8720 Microchip RMII board connected directly to the Ulx3s at the corner next to the sd card reader in the position of gp/gn pins 9-13.

Make sure that you do not connecting to the other corner of the Ulx3s board which has the 5v pin, as the board is not 5v-tolerant.

Alternatively, you can use an [ENC28J60](https://www.ebay.co.uk/itm/1-x-ENC28J60-LAN-Ethernet-Network-Board-Module-For-Arduino-SPI-Interface/262699636321) module for connection to the internet va Ethernet.

The pin mapping, which is suitable for connecting via a Pmod, is:

```
GP14 - CS
GN14 - SI   # MOSI
GP15 - INT
GN15 - SO   # MISO
GN17 - SCK
3.3V - RST
3.3V - 3.3
GND  - GND  # Next to 3.3
NC   - 5v
NC   - GND  # Next to 5V
NC   - CLK
NC   - WOL
```

### GPIO

You can access GPIO pins via /sys/class/gpio

There are 28 pins that are mapped on to sys/class/gpio numbers 480 - 507.
The first 8 (GPIO 0 -7)  map to the leds. GPIO 26 and 27 are DC and RES on the Oled header.

`echo number > export` makes the pin available to gpio
`echo number > unexport` makes it unavailable.
`value` is used to read or write the pin.
`direction` can be used to set the pin to `in` or `out`.

For example, to blink led 0:

```
#!/bin/sh
cd /sys/class/gpio
echo 480 > export
echo out > gpio480/direction
for i in 1 0 1 0 1 0
do
  sleep  0.1
  echo   $i > gpio480/value
done
echo 480 > unexport
```

### USB device

You can make the board behave like a serial/ethernet/storage via USB (US2 connector). For this you can set SAXON_USB_DEVICE to 1, regenerate the hardware/DTS and then use the bsp/common/doc/gadget.sh script in linux.

## Available software

### LCC compiler

There is a native [LCC C compiler](https://github.com/lawrie/saxonsoc-ulx3s-bin/blob/master/Smp/images/riscv32_lcc.tar.gz) available.

To install it you should extract riscv32_lcc.tar.gz in the root direction ext2 parition to create a /riscv32_lcc directory, and then add /riscv32_lcc/lcc/bin
and /riscv32_lcc/binutils/bin to your path.

e.g. `export PATH=/riscv32_lcc/lcc/bin:/riscv32_lcc/binutils/bin:$PATH`

You can compiler and build a c program by `lcc hello.c -o hello`.

### mcpclock utility

The i2c RTC clock is supported. If you install a CR1225 battery in the battery holder (flat side away from the PCB), 
the real time will be maintained when the board is not powered on.

An [mcpclock](https://github.com/emard/hwclock4saxonsoc) utility to manage the RTC is available.

You can compile it by `lcc mcpclock.c -o mcpclock`.

You can set the time by, for example:

```
date -s "2020-10-03 11:23"
./mcpclock -w
```
