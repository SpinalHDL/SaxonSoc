## Hardware

- Qmtech Kintex-7 Starter Kit
- VGA 16-bit (R5G6B5)
- Audio Out Jack
- HDMI via ADV7513 IC
- Optional SDCARD

## Implemented peripherals

* SPI, which provide
  * FPGA SPI flash access 
  * SDCARD
  * User usage SPI
* VGA, which can be used with DirectFB or X11
* Audio out (stereo) via sigma delta pin, Alsa driver provided
* UART, User SPI, and GPIO available on JP3 connector
* GPIO access

All those peripherals come with their Linux drivers.

## Serial Port
For communicating with the UART in the SoC, the JP3 connector on the daughterboard has pins for TX(15), RX(16) and GND(1) which
can be connected to a USB-UART adapter.

## GPIO Pins
There are 3 GPIO pins on the JP3 connector that can be used for input, output aswell as interrupt sources.
They may also be used as chip-selects for the User SPI. The pins and their GPIO index are as follows:

* Pin xx (GPIO pin index 18)
* Pin xx (GPIO pin index 19)
* Pin xx (GPIO pin index 20)

## Boot sequence

The boot sequence is done in 4 steps :

* bootloader : In the OnChipRam initialized by the FPGA bitstream
  * Initialise the DDR3
  * Copy the openSBI and the u-boot binary from the FPGA SPI flash to the DDR3
  * Jump to the openSBI binary in machine mode

* openSBI : In the DDR3
  * Initialise the machine mode CSR to support further supervisor SBI call and to emulate some missing CSR
  * Jump to the u-boot binary in supervisor mode

* u-boot : In the DDR3
  * Wait two seconds for user inputs
  * Read the linux uImage and dtb from the sdcard first partition
  * Boot linux

* Linux : in the DDR3
  * Kernel boot
  * Run Buildroot from the sdcard second partition

## Binary locations

OnChipRam:
- 0x20000000 : bootloader (~2 KB)

DDR3:
- 0x80000000 : Linux kernel
- 0x80F80000 : openSBI, 512 KB of reserved-memory (Linux can't use that memory space)
- 0x80F00000 : u-boot

FPGA SPI flash:
- 0x000000   : FPGA bitstream
- 0xD00000   : openSBI
- 0xD40000   : u-boot

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

# Vivado in the path for synthesis
```

## Building everything

It will take quite a while to build, good luck and have fun <3

```
# Getting this repository
mkdir QmtechK7SmpLinux 
cd QmtechK7SmpLinux
git clone https://github.com/SpinalHDL/SaxonSoc.git -b dev-0.3 --recursive SaxonSoc

# Sourcing the build script
source SaxonSoc/bsp/qmtech/QmtechK7SmpLinux/source.sh
export SAXON_CPU_COUNT=2

# Clone opensbi, u-boot, linux, buildroot, openocd
saxon_clone

# Build the FPGA bitstream
saxon_standalone_compile bootloader
saxon_netlist
saxon_bitstream

# Build the firmware
saxon_buildroot

# Build the programming tools
saxon_standalone_compile sdramInit
saxon_openocd
```

## Loading the FPGA and booting linux with ramfs using openocd

```
source SaxonSoc/bsp/qmtech/QmtechK7SmpLinux/source.sh

# Boot linux using a ram file system (no sdcard), look at the saxon_buildroot_load end message
saxon_fpga_load
saxon_buildroot_load
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

## Booting from the sdcard

```
#Format the sdcard with the buildroot image, don't forget to set the SDCARD variable to point to the /dev/xxx
sudo dd if=$SAXON_ROOT/buildroot-build/images/sdcard.img of=$SDCARD bs=4M conv=sync status=progress
```

```
#In U-BOOT
load mmc 0:1 0x80000000 uImage;load mmc 0:1 0x80FF0000 dtb; bootm 0x80000000 - 0x80FF0000 
```

## Booting with a ramfs with a preloaded sdcard in uboot

```
load mmc 0:1 0x80000000 uImage;load mmc 0:1 0x80FF0000 linux.dtb; load mmc 0:1 0x80FFFFC0 rootfs.cpio.uboot;bootm 0x80000000 0x80FFFFC0 0x80FF0000
```

## Running a baremetal simulation

```bash
source SaxonSoc/bsp/qmtech/QmtechK7SmpLinux/source.sh

saxon_standalone_compile freertosDemo SPINAL_SIM=yes
saxon_standalone_compile bootloader SPINAL_SIM=yes

saxon_sim --bin=$SAXON_SOC/software/standalone/freertosDemo/build/freertosDemo_spinal_sim.bin
```

## X11 packages

```
BR2_PACKAGE_SDL2_X11=y
BR2_PACKAGE_XORG7=y
BR2_PACKAGE_XSERVER_XORG_SERVER=y
BR2_PACKAGE_XAPP_SETXKBMAP=y
BR2_PACKAGE_XAPP_TWM=y
BR2_PACKAGE_XAPP_XCALC=y
BR2_PACKAGE_XAPP_XCLOCK=y
BR2_PACKAGE_XAPP_XDPYINFO=y
BR2_PACKAGE_XAPP_XEYES=y
BR2_PACKAGE_XAPP_XINIT=y
BR2_PACKAGE_XAPP_XINPUT=y
BR2_PACKAGE_XAPP_XMODMAP=y
BR2_PACKAGE_XAPP_XREFRESH=y
BR2_PACKAGE_XAPP_XWININFO=y
BR2_PACKAGE_XDRIVER_XF86_INPUT_KEYBOARD=y
BR2_PACKAGE_XDRIVER_XF86_INPUT_MOUSE=y
BR2_PACKAGE_XDRIVER_XF86_VIDEO_FBDEV=y
BR2_PACKAGE_XDOTOOL=y
BR2_PACKAGE_XTERM=y
```

## Change keyboard layout :

On your pc to identify the layout : 
```
setxkbmap -print | grep xkb_symbols
# it give for me : 
# xkb_symbols   { include "pc+ch(fr)+inet(evdev)+terminate(ctrl_alt_bksp)"	};
```

On the SoC :

``` 
setxkbmap -symbols "pc+ch(fr)+inet(evdev)+terminate(ctrl_alt_bksp)"
```


## Change background

```sh
feh --bg-fill background.jpg
```
