#!/bin/sh

saxon_buildroot(){
  saxon_buildroot_clean
  saxon_buildroot_setup
  saxon_buildroot_compile
}

saxon_buildroot_dts(){
  cd $SAXON_ROOT/buildroot
  cpp -nostdinc -I ../linux/include -I arch  -undef -x assembler-with-cpp  $SAXON_BUILDROOT_DTS output/images/dts.preprocessed
  dtc -i ../linux/include  -O dtb -o output/images/dtb output/images/dts.preprocessed
}

saxon_buildroot_clean(){
  cd $SAXON_ROOT/buildroot
  make clean
}

saxon_buildroot_setup(){
  cd $SAXON_ROOT/buildroot
  make $SAXON_BUILDROOT_DEFCONFIG
}

saxon_buildroot_compile(){
  cd $SAXON_ROOT/buildroot
  make linux-rebuild all -j$(nproc)
  sleep 2
  riscv-none-embed-objcopy  -O binary output/images/vmlinux output/images/Image
  riscv-none-embed-objdump  -S -d output/images/vmlinux > output/images/linux.asm
  output/host/bin/mkimage -A riscv -O linux -T kernel -C none -a 0x80000000 -e 0x80000000 -n Linux -d output/images/Image output/images/uImage
  saxon_buildroot_dts
}


