## Boot sequence

The boot sequence is done in 4 steps :

* bootloader : In the OnChipRam initialized by the FPGA bitstream
  * Copy the machineModeSbi and the u-boot binary from the FPGA SPI flash to the SDRAM
  * Jump to the machineModeSbi binary in machine mode

* machineModeSbi : In the SDRAM
  * Initialise the serial port used as shell
  * Initialise the machine mode CSR to support futher supervisor SBI call and to emulate some missing CSR
  * Jump to the u-boot binary in supervisor mode

* u-boot : In the SDRAM
  * Wait two seconds for user inputs
  * Read the linux kernel uImage and dtb from the sdcard /boot directory
  * Boot linux

* Linux : in the SDRAM
  * Kernel boot
  * Run Buildroot from the sdcard first partition

## Binary locations

OnChipRam:
- 0x20000000 : bootloader (2 KB)

SDRAM:
- 0x80000000 : Linux kernel
- 0x80800000 : machineModeSbi, 64 KB reserved-memory (Linux can't use that memory space)
- 0x81F00000 : u-boot

FPGA SPI flash:
- 0x300000   : machineModeSbi
- 0x310000   : u-boot

Sdcard :
- p1:        : Buildroot rootfs and uImage and dtb in /boot directory

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

## Building everything

First, good luck and have fun <3

```
# Getting this repository
git clone https://github.com/SpinalHDL/SaxonSoc.git -b dev --recursive SaxonSoc

# Bootloader
cd SaxonSoc/software/standalone/bootloader
make clean all BSP=Ulx3sLinuxUboot
cd ../../../..

# MachineModeSbi
cd SaxonSoc/software/standalone/machineModeSbi
make clean all BSP=Ulx3sLinuxUboot
cd ../../../..

# Netlist
cd SaxonSoc/hardware/synthesis/ulx3s
cp makefile.uboot makefile
make generate

# Bitstream
# Still in SaxonSoc/hardware/synthesis/ulx3s directory
make
cd ../../../..

# U-Boot
git clone https://github.com/SpinalHDL/u-boot.git -b saxon u-boot
cd u-boot
CROSS_COMPILE=/opt/riscv/bin/riscv64-unknown-elf- make saxon_ulx3s_defconfig
CROSS_COMPILE=/opt/riscv/bin/riscv64-unknown-elf- make -j$(nproc)
cd ..

# Buildroot
git clone https://github.com/SpinalHDL/buildroot.git -b saxon buildroot
git clone https://github.com/SpinalHDL/linux.git -b vexriscv --depth 100 linux
cd buildroot
make spinal_saxon_ulx3s_defconfig
make menuconfig # Choose extra options you require
make busybox-menuconfig # Chhose extra options you require 
make linux-rebuild all -j$(nproc)
output/host/bin/riscv32-linux-objcopy  -O binary output/images/vmlinux output/images/Image
dtc -O dtb -o output/images/dtb board/spinal/saxon_ulx3s/spinal_saxon_ulx3s.dts
output/host/bin/mkimage -A riscv -O linux -T kernel -C none -a 0x80000000 -e 0x80000000 -n Linux -d output/images/Image output/images/uImage
cd ..
```

## Sdcard

The sdcard need an ext2 partition, for u-boot and linux

```
# Assumes you have an sdcard with an empty ext2 filesystem parition created
mkdir sdcard
# Change sdcard device name to one used on your system
# unmount it, if automatically mounted
sudo mount /dev/mmcblk0p1 sdcard 
sudo cp buildroot/output/images/uImage sdcard/boot/uImage
sudo cp buildroot/output/images/dtb sdcard/boot/dtb
# Copy anything else you require such as network configuration files

sudo tar xf buildroot/output/images/rootfs.tar -C sdcard
sudo umount sdcard
```


## FPGA flashing

If you have micropython on your Ulx3s ESP32, you can do:

```
ftp <ESP32 IP address>
put ../software/standalone/machineModeSbi/build/machineModeSbi.bin flash@0x300000
put ../u-boot/u-boot.bin flash@0x310000
```

## Upload bitstream
```
cd  SaxonSoc/hardware/synthesis/ulx3s
make prog
cd ../../../..
```

or

```
ftp <ESP32 IP address>
put SaxonSoc/hardware/synthesis/ulx3s/bin/toplevel.bit fpga
```

## Connecting to the USB uart

```
screen /dev/ttyUSB1 115200
```

## Simulation

You need a recent version of Verilator to run the simulation:

```
sudo apt-get install git make autoconf g++ flex bison -y  # First time prerequisites
git clone http://git.veripool.org/git/verilator   # Only first time
unsetenv VERILATOR_ROOT  # For csh; ignore error if on bash
unset VERILATOR_ROOT  # For bash
cd verilator
git pull        # Make sure we're up-to-date
git checkout verilator_3_916
autoconf        # Create ./configure script
./configure
make -j$(nproc)
sudo make install
cd ..
```

You will also need an sdcard image, which you can get from this repository:

```
git clone https://github.com/lawrie/saxonsoc-ulx3s-bin
```

An then to run the simulation:

```
cd SaxonSoc/hardware/synthesis/ulx3s
make sim
cd ../../../..
```

Note that the sd card simulation does not work correctly, so the simulation will currently only get to u-boot, and will not boot Linux. Also, be careful that the wave file does not get too big. Writing to the wave file is currently started when you press a key to send input to u-boot.
