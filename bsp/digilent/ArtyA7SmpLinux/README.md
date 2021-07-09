## Hardware

- Arty A7 35T
- USB micro cable
- Optional Ethernet cable
- Optional SDCARD in port JD via https://reference.digilentinc.com/reference/pmod/pmodmicrosd/start

## Implemented peripherals

* Ethernet MII with linux driver
* SPI, which provide
  * FPGA SPI flash access in Linux
  * SDCARD in linux
  * User usage SPI
* VGA, which can be used with DirectFB or X11 in linux
* Audio out (stereo) via sigma delta pin, Alsa driver provided
* GPIO access in linux

![SoC](assets/diagram.png?raw=true "")

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
- 0x400000   : openSBI
- 0x480000   : u-boot

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
mkdir ArtyA7SmpLinux 
cd ArtyA7SmpLinux
git clone https://github.com/SpinalHDL/SaxonSoc.git -b dev-0.3 --recursive SaxonSoc

# Sourcing the build script
source SaxonSoc/bsp/digilent/ArtyA7SmpLinux/source.sh
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
source SaxonSoc/bsp/digilent/ArtyA7SmpLinux/source.sh

# Boot linux using a ram file system (no sdcard), look at the saxon_buildroot_load end message
saxon_fpga_load
saxon_buildroot_load

# Connecting the USB serial port (assuming you don't have nother ttyUSB pluged)
saxon_serial
```

## Flashing everything from the onboard linux and FTP

Setup a FTP server accordingly to :
https://www.digitalocean.com/community/tutorials/how-to-set-up-vsftpd-for-anonymous-downloads-on-ubuntu-16-04

It is expected that the folder /var/ftp/pub/ would be accessible via ftp://localhost/pub/

Then on your PC :

```
source SaxonSoc/bsp/digilent/ArtyA7SmpLinux/source.sh
saxon_ftp_load
```

Then on your VexRiscv linux :

```
# !!! Be sure your linux booted on a ramfs and that the SDCARD isn't mounted !!!

# Get the flashing script
export FTP_IP=YOUR_PC_IP
export FTP_PATH=ftp://$FTP_IP/pub/saxon/digilent/ArtyA7SmpLinux
wget $FTP_PATH/linux_tools.sh
source ./linux_tools.sh

# FPGA SPI flash
saxon_fpga_flash
saxon_opensbi_flash
saxon_uboot_flash

# SDCARD flash
saxon_sdcard_format
saxon_sdcard_p1
saxon_sdcard_p2
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
source SaxonSoc/bsp/digilent/ArtyA7SmpLinux/source.sh

saxon_standalone_compile freertosDemo SPINAL_SIM=yes
saxon_standalone_compile bootloader SPINAL_SIM=yes

saxon_sim --bin=$SAXON_SOC/software/standalone/freertosDemo/build/freertosDemo_spinal_sim.bin
```

## tftp +nfs boot from uboot

On host to setup the tftp: 

```sh
cd $SAXON_ROOT/buildroot-build/images
py3tftp -p 69
```

On host to setup the nfs files (note the NFS server setup should be already in place and have /srv/saxon-soc/nfs_root in read/write mode) : 

```sh
sudo rm -rf /srv/saxon-soc/nfs_root
sudo cp -rf $SAXON_ROOT/buildroot-build/images/nfs_root /srv/saxon-soc/nfs_root
```

Same than above, but preserve /root :

```sh
sudo cp -rf /srv/saxon-soc/nfs_root/root /srv/saxon-soc/root_backup
sudo rm -rf /srv/saxon-soc/nfs_root
sudo cp -rf $SAXON_ROOT/buildroot-build/images/nfs_root /srv/saxon-soc/nfs_root
sudo rm -rf /srv/saxon-soc/nfs_root/root
sudo cp -rf /srv/saxon-soc/root_backup/* /srv/saxon-soc/nfs_root/root
```

If you want to update an already existing /srv/saxon-soc/nfs_root : 

```sh
sudo rsync -r -l $SAXON_ROOT/buildroot-build/images/nfs_root/ /srv/saxon-soc/nfs_root
```

uboot config :

```
env set boot_net "dhcp 0x80000000 192.168.0.24:uImage; tftp 0x80FF0000 192.168.0.24:linux.dtb; bootm 0x80000000 - 0x80FF0000"
env set bootargs "rootwait console=hvc0 earlycon=sbi root=/dev/nfs nfsroot=192.168.0.24:/srv/saxon-soc/nfs_root ip=dhcp init=/sbin/init mmc_core.use_spi_crc=0"
env set bootcmd "run boot_net"
boot
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




## Junk


find images/nfs_root/share/man/man1 -type f -exec sed -i  's|/media/data/open/SaxonSoc/artyA7SmpUsb/buildroot-build/per-package/wmaker/target||g' {} ";"
find images/nfs_root/man/man1  -type f -exec sed -i  's|/media/data/open/SaxonSoc/artyA7SmpUsb/buildroot-build/per-package/wmaker/target||g' {} ";"
find images/nfs_root/WindowMaker  -type f -exec sed -i  's|/media/data/open/SaxonSoc/artyA7SmpUsb/buildroot-build/per-package/wmaker/target||g' {} ";"
find images/nfs_root/etc/WindowMaker  -type f -exec sed -i  's|/media/data/open/SaxonSoc/artyA7SmpUsb/buildroot-build/per-package/wmaker/target||g' {} ";"
find images/nfs_root/bin/wmaker.inst  -type f -exec sed -i  's|/media/data/open/SaxonSoc/artyA7SmpUsb/buildroot-build/per-package/wmaker/target||g' {} ";"

xwd -root -out screenshot.xwd