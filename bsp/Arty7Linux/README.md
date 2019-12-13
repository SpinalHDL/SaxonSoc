## Boot sequance

The boot sequance is done in 4 steps :

1) bootloader : In the OnChipRam initialized by the FPGA bitstream
  a) Initialise the DDR3
  b) Copy the machineModeSbi and the u-boot binary from the FPGA SPI flash to the DDR3
  c) Jump to the machineModeSbi binary in machine mode

2) machineModeSbi : In the DDR3
  a) Initialise the serial port used as shell
  b) Initialise the machine mode CSR to support futher supervisor SBI call and to emulate some missing CSR
  c) Jump to the u-boot binary in supervisor mode

3) u-boot : In the DDR3
  a) Wait two seconds for user inputs
  b) Read the linux uImage and dtb from the sdcard first partition
  c) Boot linux

4) Linux : in the DDR3
  a) Kernel boot
  b) Run Buildroot from the sdcard second partition

## Binary locations

OnChipRam:
- 0x20000000 : bootloader (2 KB)

DDR3:
- 0x80000000 : Linux kernel
- 0x81FF0000 : machineModeSbi, 64 KB reserved-memory (Linux can't use that memory space)
- 0x81F00000 : u-boot

FPGA SPI flash:
- 0x000000   : FPGA bitstream
- 0x300000   : machineModeSbi
- 0x310000   : u-boot

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
make clean all BSP=Arty7Linux
cd ../../../..

# MachineModeSbi
cd SaxonSoc/software/standalone/machineModeSbi
make clean all BSP=Arty7Linux
cd ../../../..

# Netlist
cd SaxonSoc
sbt "runMain saxon.board.digilent.Arty7Linux"
cd ..

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
mkdir sdcard
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
write_cfgmem  -format mcs -size 16 -interface SPIx4 -loadbit {up 0x00000000 "/home/miaou/pro/riscv/arty7_linux/arty7_linux.runs/impl_3/Arty7Linux.bit" } -loaddata {up 0x00300000 "/home/miaou/pro/riscv/SaxonSoc.git/software/standalone/machineModeSbi/build/machineModeSbi.bin" up 0x00310000 "/home/miaou/pro/riscv/u-boot/u-boot.bin" } -force -file "/home/miaou/pro/riscv/arty7_linux/prog.mcs"
```

