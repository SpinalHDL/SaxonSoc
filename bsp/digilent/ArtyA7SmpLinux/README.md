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
echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
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
git clone https://github.com/SpinalHDL/SaxonSoc.git -b dev-0.1 --recursive SaxonSoc

# Sourcing the build script
source SaxonSoc/bsp/digilent/ArtyA7SmpLinux/source.sh

# Clone opensbi, u-boot, linux, buildroot, openocd
saxon_clone

# Build the FPGA bitstream
saxon_standalone_compile bootloader
saxon_netlist
saxon_bitstream

# Build the firmware
saxon_opensbi
saxon_uboot
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

## Booting with a ramfs with a preloaded sdcard in uboot

```
load mmc 0:1 0x80000000 uImage;load mmc 0:1 0x80FF0000 dtb; load mmc 0:1 0x80FFFFC0 rootfs.cpio.uboot;bootm 0x80000000 0x80FFFFC0 0x80FF0000
```

## Running a baremetal simulation

```bash
source SaxonSoc/bsp/digilent/ArtyA7SmpLinux/source.sh

saxon_standalone_compile freertosDemo SPINAL_SIM=yes
saxon_standalone_compile bootloader SPINAL_SIM=yes

saxon_sim --bin=$SAXON_ROOT/SaxonSoc/software/standalone/freertosDemo/build/freertosDemo_spinal_sim.bin
```
