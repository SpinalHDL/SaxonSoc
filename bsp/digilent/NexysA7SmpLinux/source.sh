#!/bin/sh

# Project
SAXON_BSP_NAME=$(basename $(dirname $BASH_SOURCE))

# Locations
SAXON_SOURCED_SH=$(realpath $BASH_SOURCE)
SAXON_BSP_PATH=$(dirname $SAXON_SOURCED_SH)
SAXON_ROOT=$SAXON_BSP_PATH/"../../../.."
SAXON_BSP_COMMON_SCRIPTS=$SAXON_ROOT/SaxonSoc/bsp/common/scripts

# Configurations
SAXON_OPENSBI_PLATEFORM=spinal/saxon/bsp
SAXON_UBOOT_DEFCONFIG=saxon_bsp_defconfig
SAXON_BUILDROOT_DEFCONFIG=spinal_saxon_bsp_defconfig
SAXON_BUILDROOT_DTS=board/spinal/saxon_bsp/dts

# Fixes
SAXON_FIXES=$SAXON_ROOT/SaxonSoc/bsp/common/fixes/buildroot/dropbear/vexriscv_aes
SAXON_FIXES=$SAXON_ROOT/SaxonSoc/bsp/common/fixes/buildroot/dropbear/no_swap

# Functionalities
source $SAXON_BSP_COMMON_SCRIPTS/base.sh
source $SAXON_BSP_COMMON_SCRIPTS/openocd.sh
source $SAXON_BSP_COMMON_SCRIPTS/opensbi.sh
source $SAXON_BSP_COMMON_SCRIPTS/uboot.sh
source $SAXON_BSP_COMMON_SCRIPTS/buildroot.sh

saxon_netlist(){
  cd $SAXON_SOC
  sbt "runMain saxon.board.digilent.$SAXON_BSP_NAME"
}

saxon_bitstream(){
  cd $SAXON_SOC/hardware/synthesis/digilent/$SAXON_BSP_NAME
  make all
}

saxon_serial(){
  picocom -b 115200 /dev/ttyUSB1 --imap lfcrlf
}

saxon_ssh(){
  ssh root@192.168.0.157
}

saxon_ftp_load(){
  cd $SAXON_ROOT
  mkdir -p /var/ftp/pub/saxon/digilent/$SAXON_BSP_NAME
  cp SaxonSoc/hardware/synthesis/digilent/$SAXON_BSP_NAME/build/vivado_project/fpga.runs/impl_1/$SAXON_BSP_NAME.bit /var/ftp/pub/saxon/digilent/$SAXON_BSP_NAME/bitstream
  cp buildroot/output/images/dtb /var/ftp/pub/saxon/digilent/$SAXON_BSP_NAME
  cp buildroot/output/images/uImage /var/ftp/pub/saxon/digilent/$SAXON_BSP_NAME
  cp buildroot/output/images/rootfs.tar /var/ftp/pub/saxon/digilent/$SAXON_BSP_NAME
  cp buildroot/output/images/rootfs.cpio.uboot /var/ftp/pub/saxon/digilent/$SAXON_BSP_NAME
  cp opensbi/build/platform/spinal/saxon/digilent/artyA7Smp/firmware/fw_jump.bin /var/ftp/pub/saxon/digilent/$SAXON_BSP_NAME
  cp u-boot/u-boot.bin /var/ftp/pub/saxon/digilent/$SAXON_BSP_NAME
  cp SaxonSoc/bsp/common/scripts/linux_tools.sh /var/ftp/pub/saxon/digilent/$SAXON_BSP_NAME
}


saxon_sim(){
  echo "runMain saxon.board.digilent.${SAXON_BSP_NAME}SystemSim $@"
  (cd $SAXON_SOC && sbt "runMain saxon.board.digilent.${SAXON_BSP_NAME}SystemSim $@")
}
