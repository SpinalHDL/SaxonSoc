## Boot sequence

The boot sequence is done in 4 steps :

* bootloader : In the OnChipRam initialized by the FPGA bitstream
  * Initialise the DDR
  * Copy the machineModeSbi and the u-boot binary from the FPGA SPI flash to the DDR
  * Jump to the machineModeSbi binary in machine mode

* machineModeSbi : In the DDR
  * Initialise the serial port used as shell
  * Initialise the machine mode CSR to support futher supervisor SBI call and to emulate some missing CSR
  * Jump to the u-boot binary in supervisor mode

* u-boot : In the DDR
  * Wait two seconds for user inputs
  * Read the linux uImage and dtb from the sdcard first partition
  * Boot linux

* Linux : in the DDR
  * Kernel boot
  * Run Buildroot from the sdcard second partition

## Binary locations

OnChipRam:
- 0x20000000 : bootloader (2 KB)

DDR:
- 0x80000000 : Linux kernel
- 0x81FF0000 : machineModeSbi, 64 KB reserved-memory (Linux can't use that memory space)
- 0x81F00000 : u-boot

FPGA SPI flash:
- 0x000000   : FPGA bitstream
- 0x400000   : machineModeSbi
- 0x410000   : u-boot

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

## Building everything

First, good luck and have fun <3

```
# Getting this repository
git clone https://github.com/SpinalHDL/SaxonSoc.git -b dev --recursive SaxonSoc

# Bootloader
cd SaxonSoc/software/standalone/bootloader
RISCV_BIN=/opt/riscv/bin/riscv64-unknown-elf- make clean all BSP=NexysA7Linux
RISCV_BIN=/opt/riscv/bin/riscv64-unknown-elf- make BSP=NexysA7Linux SPINAL_SIM=yes
cd -

# MachineModeSbi
cd SaxonSoc/software/standalone/machineModeSbi
RISCV_BIN=/opt/riscv/bin/riscv64-unknown-elf- make clean all BSP=NexysA7Linux
cd -

# Netlist
cd SaxonSoc
sbt "runMain saxon.board.digilent.NexysA7Linux"
cd -

# U-Boot
git clone https://github.com/SpinalHDL/u-boot.git -b saxon u-boot
cd u-boot
CROSS_COMPILE=/opt/riscv/bin/riscv64-unknown-elf- make saxon_arty7_defconfig
CROSS_COMPILE=/opt/riscv/bin/riscv64-unknown-elf- make -j$(nproc)
cd ..

# Buildroot
git clone https://github.com/SpinalHDL/buildroot.git -b saxon buildroot
git clone https://github.com/SpinalHDL/linux.git -b vexriscv --depth 100 linux
cd buildroot
make spinal_saxon_arty7_defconfig
make linux-rebuild all -j$(nproc)
output/host/bin/riscv32-linux-objcopy  -O binary output/images/vmlinux output/images/Image
dtc -O dtb -o output/images/dtb board/spinal/saxon_arty7/spinal_saxon_arty7.dts
output/host/bin/mkimage -A riscv -O linux -T kernel -C none -a 0x80000000 -e 0x80000000 -n Linux -d output/images/Image output/images/uImage
cd ..
```

## Sdcard

The sdcard need two ext2 partitions, one for u-boot, one for linux

```
(
echo d
echo
echo d
echo
echo n
echo p
echo 1
echo
echo +100M
echo n
echo p
echo 2
echo
echo +500M
echo w
) | sudo fdisk /dev/mmcblk0

sudo mkfs.ext2 -q /dev/mmcblk0p1
sudo mkfs.ext2 -q /dev/mmcblk0p2
```

Then to copy the files


```
mkdir -p sdcard
sudo mount /dev/mmcblk0p1 sdcard
sudo cp buildroot/output/images/uImage sdcard/uImage
sudo cp buildroot/output/images/dtb sdcard/dtb
sudo umount sdcard

sudo mount /dev/mmcblk0p2 sdcard
sudo tar xf buildroot/output/images/rootfs.tar -C sdcard
sudo umount sdcard
```


## FPGA flashing

```
write_cfgmem  -format mcs -size 16 -interface SPIx4 -loadbit {up 0x00000000 "impl_1/Nexys7ALinux.bit" } -loaddata {up 0x00400000 "software/standalone/machineModeSbi/build/machineModeSbi.bin" up 0x00410000 "../u-boot/u-boot.bin" } -force -file "prog.mcs"
```

## Connecting to the USB uart

```
screen /dev/ttyUSB1 115200
or
picocom -b 115200 /dev/ttyUSB1 --imap lfcrlf
```

## Simulation

WIP

```
cd SaxonSoc/software/standalone/bootloader
make clean all BSP=NexysA7Linux SPINAL_SIM=yes
cd ../../../..
```

