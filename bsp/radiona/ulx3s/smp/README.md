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
* Uart to host and to esp32 co-processor
* GPIO access in Linux
* Ethernet MII with Linux driver
* Bit-banged I2C with Linux driver for RTC
* Optional PPPD networking via the esp32 co-processor

## Boot sequence

The boot sequence is done in 4 steps :

* bootloader : In the OnChipRam initialized by the FPGA bitstream
  * Copy the openSbi and the u-boot binary from the FPGA SPI flash to the SDRAM
  * Jump to the openSbi binary in machine mode

* openSbi : In the SDRAM
  * Initialise the machine mode CSR to support futher supervisor SBI call and to emulate some missing CSR
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
echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
sudo apt-get update
sudo apt-get install sbt

# RISC-V toolchain
wget https://static.dev.sifive.com/dev-tools/riscv64-unknown-elf-gcc-20171231-x86_64-linux-centos6.tar.gz
tar -xzvf riscv64-unknown-elf-gcc-20171231-x86_64-linux-centos6.tar.gz
sudo mv riscv64-unknown-elf-gcc-20171231-x86_64-linux-centos6 /opt/riscv64-unknown-elf-gcc-20171231-x86_64-linux-centos6
sudo mv /opt/riscv64-unknown-elf-gcc-20171231-x86_64-linux-centos6 /opt/riscv
echo 'export PATH=/opt/riscv/bin:$PATH' >> ~/.bashrc
export PATH=/opt/riscv/bin:$PATH
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
fujprog -j FLASH -f 0x380000 buildroot-build/images/u-boot.bin
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
sudo cp buildroot/output/images/dtb sdcard/dtb
sudo cp buildroot/output/images/rootfs.cpio.uboot sdcard/rootfs.cpio.uboot
sudo cp buildroot/output/images/uImage sdcard/uImage
sudo umount sdcard
rm -r sdcard

mkdir -p sdcard
sudo mount $SDCARD_P2 sdcard
sudo tar xf buildroot/output/images/rootfs.tar -C sdcard
sudo umount sdcard
rm -r sdcard
```
