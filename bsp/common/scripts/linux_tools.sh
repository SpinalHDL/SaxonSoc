#!/bin/sh

saxon_fpga_flash(){
  wget $FTP_PATH/bitstream -O bitstream
  flash_erase /dev/mtd0 0 0
  cat bitstream > /dev/mtd0
  rm bitstream
}

saxon_opensbi_flash(){
  wget $FTP_PATH/fw_jump.bin -O fw_jump.bin
  flash_erase /dev/mtd1 0 0
  cat fw_jump.bin > /dev/mtd1
  rm fw_jump.bin
}

saxon_uboot_flash(){
  wget $FTP_PATH/u-boot.bin -O u-boot.bin
  flash_erase /dev/mtd2 0 0
  cat u-boot.bin > /dev/mtd2
  rm u-boot.bin
}

saxon_sdcard_format(){
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
  ) | fdisk /dev/mmcblk0
}

saxon_sdcard_p1(){
  mkdosfs /dev/mmcblk0p1
  mkdir -p sdcard
  mount /dev/mmcblk0p1 sdcard
  wget $FTP_PATH/dtb -O sdcard/dtb
  wget $FTP_PATH/rootfs.cpio.uboot -O sdcard/rootfs.cpio.uboot
  wget $FTP_PATH/uImage -O sdcard/uImage
  umount sdcard
  rm -r sdcard
}

saxon_sdcard_p2(){
  mke2fs /dev/mmcblk0p2
  mkdir -p sdcard
  wget $FTP_PATH/rootfs.tar -O rootfs.tar
  mount /dev/mmcblk0p2 sdcard
  tar xf rootfs.tar -C sdcard
  umount sdcard
  rm rootfs.tar
  rm -r sdcard
}

saxon_flash_all(){
  saxon_fpga_flash
  saxon_opensbi_flash
  saxon_uboot_flash
  saxon_sdcard_format
  saxon_sdcard_p1
  saxon_sdcard_p2
}
