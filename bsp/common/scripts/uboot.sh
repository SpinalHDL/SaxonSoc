#!/bin/sh


saxon_uboot_clean(){
  cd $SAXON_ROOT/u-boot
  CROSS_COMPILE=riscv-none-embed- make clean
}

saxon_uboot_compile(){
  cd $SAXON_ROOT/u-boot
  CROSS_COMPILE=riscv-none-embed- make $SAXON_UBOOT_DEFCONFIG
  CROSS_COMPILE=riscv-none-embed- make -j$(nproc)
  rm -p u-boot.asm
  riscv-none-embed-objdump  -S -d u-boot >  u-boot.asm
}

saxon_uboot(){
  saxon_uboot_clean
  saxon_uboot_compile
}
