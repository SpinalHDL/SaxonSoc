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
VERSION=riscv64-unknown-elf-gcc-8.3.0-2019.08.0-x86_64-linux-ubuntu14
wget https://static.dev.sifive.com/dev-tools/$VERSION.tar.gz
tar -xzvf $VERSION.tar.gz
sudo mv $VERSION /opt/riscv
echo 'export PATH=/opt/riscv/bin:$PATH' >> ~/.bashrc
export PATH=/opt/riscv/bin:$PATH

# Vivado in the path for synthesis
```

## Building everything

It will take quite a while to build, good luck and have fun <3

```
# Getting this repository
mkdir ArtyA7SmpLinux 
cd ArtyA7SmpLinux
git clone https://github.com/SpinalHDL/SaxonSoc.git -b dev_software --recursive SaxonSoc

# Sourcing the build script
source SaxonSoc/bsp/digilent/ArtyA7SmpLinux/source.sh

# Clone opensbi, u-boot, linux, buildroot, openocd
saxon_clone

# Build the FPGA bitstream
saxon_bootloader
saxon_rtl
saxon_bitstream

# Build the firmware
saxon_opensbi
saxon_uboot
saxon_buildroot

# Build the programming tools
saxon_sdramInit
saxon_openocd
```

## Loading the FPGA and booting linux with ramfs using openocd

```
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
export FTP_PATH=ftp://YOUR_PC_IP/pub/saxon/digilent/ArtyA7SmpLinux
wget $FTP_PATH/linux_tools.sh
source ./linux_tools.sh

# FPGA SPI flash
tool_fpga_flash
tool_opensbi_flash
tool_uboot_flash

# SDCARD flash
tool_sdcard_format
tool_sdcard_p1
tool_sdcard_p2
```