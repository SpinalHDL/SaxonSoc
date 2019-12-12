BUILDROOT=../buildroot
mkdir sd
sudo mount /dev/mmcblk0p1 sd
sudo cp $BUILDROOT/output/images/uImage sd/uImage
sudo cp $BUILDROOT/output/images/dtb sd/dtb
sudo umount sd

sudo mount /dev/mmcblk0p2 sd
sudo tar xf $BUILDROOT/output/images/rootfs.tar -C sd
sudo umount sd

write_cfgmem  -format mcs -size 16 -interface SPIx4 -loadbit {up 0x00000000 "/home/miaou/pro/riscv/arty7_linux/arty7_linux.runs/impl_3/Arty7Linux.bit" } -loaddata {up 0x00300000 "/home/miaou/pro/riscv/SaxonSoc.git/software/standalone/machineModeSbi/build/machineModeSbi.bin" up 0x00310000 "/home/miaou/pro/riscv/u-boot/u-boot.bin" } -force -file "/home/miaou/pro/riscv/arty7_linux/prog.mcs"