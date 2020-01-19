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
# If you have the 32 MB SDRAM :
make clean all BSP=Ulx3sLinuxUboot CFLAGS_ARGS="-DSDRAM_TIMING=MT48LC16M16A2_6A_ps"
# If you have the 64 MB SDRAM :
make clean all BSP=Ulx3sLinuxUboot CFLAGS_ARGS="-DSDRAM_TIMING=AS4C32M16SB_7TCN_ps"
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
mkdir -p sdcard
# Change sdcard device name to one used on your system
# unmount it, if automatically mounted
sudo mount /dev/mmcblk0p1 sdcard 
sudo mkdir -p sdcard/boot
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
put SaxonSoc/software/standalone/machineModeSbi/build/machineModeSbi.bin flash@0x300000
put u-boot/u-boot.bin flash@0x310000
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

## Ethernet 

You can use an [ENC28J60](https://www.ebay.co.uk/itm/1-x-ENC28J60-LAN-Ethernet-Network-Board-Module-For-Arduino-SPI-Interface/262699636321) module for connection to the internet va Ethernet.

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

## GPIO

You can access GPIO pins via /sys/class/gpio

There are 24 pins that are mapped on to GPIO numbers 488 - 511.
The first 8 map to the leds, although led 1 is not connected as gpio[1] is currently used for the interrupt on the ENC28J60 device.

`echo number > export` makes the pin available to gpio
`echo number > unexport` makes it unavailable.
`value` is used to read or write the pin.
`direction` can be used to set the pin to `in` or `out`.

For example, to blink led 0:

```
#!/bin/sh
cd /sys/class/gpio
echo 488 > export
echo out > gpio488/direction
for i in 1 0 1 0 1 0
do
  sleep  0.1
  echo   $i > gpio488/value
done
echo 488 > unexport
```

## Different Ulx3s boards

The build instructions above are for a Ulx3s 12F board, but a variety of Ulx3s boards are available.

Some of the 85F boards, particularly the ones with a blue PCB, have a 64MB SDRAM chip.

To build for other boards you can use the FPGA_SIZE and SDRAM_SIZE parameters to the make file. SDRAM_SIZE can be 32 or 64,
and FPGA_SIZE can be 12, 25, 45 or 85.

Also, if you have a board, such as a blue 85F, with a 64MB SDRAM chip, you need to build the bootloader for that SDRAM chip - see the Bootloader section of *Building everything* above. For such boards, you also need to tell Linux that 64MB of memory is available. This can be done by using a dtb that specifies 64MB of memory, but a better solution is to use the standard dtb that specifies 32MB of memory, but then use u-boot comnmands to override it. You only have to execute these u-boot commands once, as the u-boot environment is saved on the SD card, The u-boot commands are:

```
fdt add 0x81EF0000
fdt memory 0x80000000 0x04000000
saveenv
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
