#!/bin/sh

SAXON_SOURCED_SH=$(realpath ${BASH_SOURCE})
SAXON_ROOT=$(dirname $SAXON_SOURCED_SH)/"../../../.."


saxon_source(){
  cd $SAXON_ROOT
  source $SAXON_SOURCED_SH
}

saxon_clone() {
  cd $SAXON_ROOT
  git clone https://github.com/SpinalHDL/u-boot.git --branch smp 
  git clone https://github.com/SpinalHDL/buildroot.git --branch spinal 
  git clone https://github.com/SpinalHDL/linux.git --branch vexriscv 
  git clone https://github.com/SpinalHDL/opensbi.git --branch spinal 
  git clone https://github.com/SpinalHDL/openocd_riscv.git
}

saxon_openocd(){
  cd $SAXON_ROOT/openocd_riscv
  ./bootstrap
  ./configure --enable-ftdi --enable-dummy
  make -j$(nproc)
}


saxon_bootloader(){
  cd $SAXON_ROOT/SaxonSoc/software/standalone/bootloader
  make clean all BSP=digilent/ArtyA7SmpLinux
}


saxon_sdramInit(){
  cd $SAXON_ROOT/SaxonSoc/software/standalone/sdramInit
  make clean all BSP=digilent/ArtyA7SmpLinux
}


saxon_rtl(){
  cd $SAXON_ROOT/SaxonSoc
  sbt "runMain saxon.board.digilent.ArtyA7SmpLinux"
}


saxon_bitstream(){
  cd $SAXON_ROOT/SaxonSoc/hardware/synthesis/digilent/ArtyA7SmpLinux
  make all
}

saxon_fpga_load(){
  cd $SAXON_ROOT/SaxonSoc
  $SAXON_ROOT/openocd_riscv/src/openocd -s $SAXON_ROOT/openocd_riscv/tcl -s bsp/digilent/ArtyA7SmpLinux/openocd -c 'set CPU0_YAML cpu0.yaml' -f usb_connect.cfg -f fpga_load.cfg
}

saxon_buildroot_load(){
  cd $SAXON_ROOT/SaxonSoc
  $SAXON_ROOT/openocd_riscv/src/openocd -s $SAXON_ROOT/openocd_riscv/tcl -s bsp/digilent/ArtyA7SmpLinux/openocd -c 'set CPU0_YAML cpu0.yaml' -f usb_connect.cfg -f soc_init.cfg -f linux_boot.cfg -c 'exit'
  echo ""
  echo "!!! u-boot will by default try to boot on the SDCARD !!!"
  echo "In order to boot from the jtag, you will have to stop the boot sequance and enter :"
  echo "bootm 0x80000000 0x80FFFFC0 0x80FF0000"
}

saxon_buildroot_clean(){
  cd $SAXON_ROOT/buildroot
  make clean 
}

saxon_buildroot_setup(){
  cd $SAXON_ROOT/buildroot
  make spinal_saxon_arty_a7_smp_defconfig
}  

saxon_buildroot_compile(){
  cd $SAXON_ROOT/buildroot
  make linux-rebuild all -j$(nproc)
  sleep 2
  riscv64-unknown-elf-objcopy  -O binary output/images/vmlinux output/images/Image
  riscv64-unknown-elf-objdump  -S -d output/images/vmlinux > output/images/linux.asm
  output/host/bin/mkimage -A riscv -O linux -T kernel -C none -a 0x80000000 -e 0x80000000 -n Linux -d output/images/Image output/images/uImage
  saxon_buildroot_dts
}  

saxon_buildroot(){
  saxon_buildroot_clean
  saxon_buildroot_setup
  saxon_buildroot_compile
}

saxon_buildroot_dts(){
  cd $SAXON_ROOT/buildroot
  dtc -O dtb -o output/images/dtb board/spinal/saxon_arty_a7_smp/dts
}  

saxon_opensbi(){
  cd $SAXON_ROOT/opensbi
  export CROSS_COMPILE=/opt/riscv/bin/riscv64-unknown-elf-
  export PLATFORM_RISCV_XLEN=32
  make PLATFORM=spinal/saxon/digilent/artyA7Smp clean 
  make PLATFORM=spinal/saxon/digilent/artyA7Smp -j$(nproc) SAXON_PATH=../SaxonSoc BSP=digilent/ArtyA7SmpLinux
  riscv64-unknown-elf-objdump  -S -d /media/data/open/opensbi/build/platform/spinal/vexriscv/sim/smp/firmware/fw_jump.elf > fw_jump.asm
}

saxon_uboot_clean(){
  cd $SAXON_ROOT/u-boot
  CROSS_COMPILE=/opt/riscv_uboot/bin/riscv64-unknown-elf- make clean
}

saxon_uboot_compile(){
  cd $SAXON_ROOT/u-boot
  CROSS_COMPILE=/opt/riscv_xpacks/bin/riscv-none-embed- make saxon_arty_a7_smp_defconfig
  CROSS_COMPILE=/opt/riscv_xpacks/bin/riscv-none-embed- make -j$(nproc)
  rm -p u-boot.asm
  riscv64-unknown-elf-objdump  -S -d u-boot >  u-boot.asm
}

saxon_uboot(){
  saxon_uboot_clean
  saxon_uboot_compile
}

saxon_serial(){
  picocom -b 115200 /dev/ttyUSB1 --imap lfcrlf
}

saxon_ssh(){
  ssh root@192.168.0.157
}

saxon_ftp_init(){
  cd $SAXON_ROOT
  cp SaxonSoc/hardware/synthesis/digilent/ArtyA7SmpLinux/build/vivado_project/fpga.runs/impl_1/ArtyA7SmpLinux.bit /var/ftp/pub/saxon/digilent/artyA7SmpLinux
  cp buildroot/output/images/dtb /var/ftp/pub/saxon/digilent/artyA7SmpLinux
  cp buildroot/output/images/uImage /var/ftp/pub/saxon/digilent/artyA7SmpLinux
  cp buildroot/output/images/rootfs.tar /var/ftp/pub/saxon/digilent/artyA7SmpLinux
  cp buildroot/output/images/rootfs.cpio.uboot /var/ftp/pub/saxon/digilent/artyA7SmpLinux
  cp opensbi/build/platform/spinal/saxon/digilent/artyA7Smp/firmware/fw_jump.bin /var/ftp/pub/saxon/digilent/artyA7SmpLinux
  cp u-boot/u-boot.bin /var/ftp/pub/saxon/digilent/artyA7SmpLinux
  cp scripts/flash_soft.sh /var/ftp/pub/saxon/digilent/artyA7SmpLinux
  cp scripts/flash_hard.sh /var/ftp/pub/saxon/digilent/artyA7SmpLinux
  cp scripts/sdcard_format.sh /var/ftp/pub/saxon/digilent/artyA7SmpLinux
  cp scripts/sdcard_soft.sh /var/ftp/pub/saxon/digilent/artyA7SmpLinux
}

