## Boot sequence

The boot sequence is done in 4 steps :

* bootloader : In the OnChipRam initialized by the FPGA bitstream
  * Initialise the DDR3
  * Copy the machineModeSbi and the u-boot binary from the FPGA SPI flash to the DDR3
  * Jump to the machineModeSbi binary in machine mode

* machineModeSbi : In the DDR3
  * Initialise the serial port used as shell
  * Initialise the machine mode CSR to support futher supervisor SBI call and to emulate some missing CSR
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
- 0x20000000 : bootloader (2 KB)

DDR3:
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
make clean all BSP=ArtyA7Linux
cd ../../../..

# MachineModeSbi
cd SaxonSoc/software/standalone/machineModeSbi
make clean all BSP=ArtyA7Linux
cd ../../../..

# Netlist
cd SaxonSoc
sbt "runMain saxon.board.digilent.ArtyA7Linux"
cd ..

# U-Boot
git clone https://github.com/SpinalHDL/u-boot.git -b saxon u-boot
cd u-boot
CROSS_COMPILE=/opt/riscv/bin/riscv64-unknown-elf- make saxon_arty_a7_defconfig
CROSS_COMPILE=/opt/riscv/bin/riscv64-unknown-elf- make -j$(nproc)
cd ..

# Buildroot
git clone https://github.com/SpinalHDL/buildroot.git -b saxon buildroot
git clone https://github.com/SpinalHDL/linux.git -b vexriscv --depth 100 linux
cd buildroot
make spinal_saxon_arty_a7_defconfig
make linux-rebuild all -j$(nproc)
output/host/bin/riscv32-linux-objcopy  -O binary output/images/vmlinux output/images/Image
dtc -O dtb -o output/images/dtb board/spinal/saxon_arty_a7/spinal_saxon_arty_a7.dts
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
write_cfgmem  -format mcs -size 16 -interface SPIx4 -loadbit {up 0x00000000 "/home/miaou/pro/riscv/SaxonSocArtyA7/arty_a7_linux/arty_a7_linux.runs/impl_3/ArtyA7Linux.bit" } -loaddata {up 0x00400000 "/home/miaou/pro/riscv/SaxonSocArtyA7/SaxonSoc.git/software/standalone/machineModeSbi/build/machineModeSbi.bin" up 0x00410000 "/home/miaou/pro/riscv/SaxonSocArtyA7/u-boot/u-boot.bin" } -force -file "/home/miaou/pro/riscv/SaxonSocArtyA7/artyA7_linux/prog.mcs"
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
make clean all BSP=ArtyA7Linux SPINAL_SIM=yes
cd ../../../..
```


## Memo WIP

```
# Test SDCARD speed
hdparm -t /dev/mmcblk0

#https://www.emcraft.com/stm32f769i-discovery-board/accessing-spi-devices-in-linux

hexdump -C -n 100 /dev/mtd3
flash_erase /dev/mtd3 0 1
echo "wuff" > /dev/mtd3
hexdump -C -n 100 /dev/mtd3

https://www.techrepublic.com/article/how-to-quickly-setup-an-ftp-server-on-ubuntu-18-04/
sudo apt-get install vsftpd
sudo systemctl start vsftpd
sudo systemctl enable vsftpd
sudo useradd -m ftpuser
sudo mv /etc/vsftpd.conf /etc/vsftpd.conf.orig
sudo nano /etc/vsftpd.conf

listen=NO
listen_ipv6=YES
local_enable=YES
write_enable=YES
local_umask=022
dirmessage_enable=YES
use_localtime=YES
xferlog_enable=YES
connect_from_port_20=YES
chroot_local_user=YES
secure_chroot_dir=/var/run/vsftpd/empty
pam_service_name=vsftpd
rsa_cert_file=/etc/ssl/certs/ssl-cert-snakeoil.pem
rsa_private_key_file=/etc/ssl/private/ssl-cert-snakeoil.key
ssl_enable=NO
pasv_enable=Yes
pasv_min_port=10000
pasv_max_port=10100
allow_writeable_chroot=YES
anonymous_enable=YES
no_anon_password=YES
anon_root=/home/ftpuser

sudo systemctl reload vsftpd
sudo systemctl restart vsftpd


cp  arty7_linux/arty7_linux.runs/impl_3/Arty7Linux.bit /home/ftpuser/arty7
cp  u-boot/u-boot.bin /home/ftpuser/arty7
cp  SaxonSoc.git/software/standalone/machineModeSbi/build/machineModeSbi.bin /home/ftpuser/arty7
cp  buildroot/output/images/uImage /home/ftpuser/arty7
cp  buildroot/output/images/dtb /home/ftpuser/arty7
cp  buildroot/output/images/rootfs.tar /home/ftpuser/arty7

wget ftp://10.42.0.1/arty7/Arty7Linux.bit -O Arty7Linux.bit
flash_erase /dev/mtd0 0 0
cat Arty7Linux.bit > /dev/mtd0

wget ftp://10.42.0.1/arty7/machineModeSbi.bin -O machineModeSbi.bin
flash_erase /dev/mtd1 0 0
cat machineModeSbi.bin > /dev/mtd1

wget ftp://10.42.0.1/arty7/u-boot.bin -O u-boot.bin
flash_erase /dev/mtd2 0 0
cat u-boot.bin > /dev/mtd2

mkdir -p uboot
mount /dev/mmcblk0p1 uboot
wget ftp://10.42.0.1/arty7/uImage -O uboot/uImage
wget ftp://10.42.0.1/arty7/dtb -O uboot/dtb
umount uboot

wget ftp://10.42.0.1/arty7/rootfs.tar -O rootfs.tar
mkdir -p buildroot
mount /dev/mmcblk0p2 buildroot
tar xf rootfs.tar -C buildroot
umount buildroot
         | 4K I$ 4K D$ | 8K I$ 8K D$
---------|-------------|-----------------
SDCARD   | 0.975       | 1.180 MBytes/s
ENC28J60 | 1.200       | 1.450 MBits/s TCP




cd ../riscv_openocd
src/openocd -f tcl/interface/ftdi/ft2232h_breakout.cfg -c 'set CPU0_YAML ../SaxonSoc.git/cpu0.yaml' -f tcl/target/arty7_linux.cfg
load mmc 0 0x80000000 uboot/uImage
load mmc 0 0x80BFFFC0 uboot/rootfs.cpio.uboot
load mmc 0 0x80BF0000 uboot/dtb
bootm 0x80000000 0x80BFFFC0 0x80BF0000

load mmc 0 0x80000000 uImage
load mmc 0 0x81EF0000 dtb
bootm 0x80000000 - 0x81EF0000

```

