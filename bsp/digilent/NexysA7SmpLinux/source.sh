#!/bin/sh

# Project
SAXON_BSP_NAME=$(basename $(dirname $BASH_SOURCE))
export SAXON_CPU_COUNT=2

# Locations
SAXON_SOURCED_SH=$(realpath $BASH_SOURCE)
SAXON_BSP_PATH=$(dirname $SAXON_SOURCED_SH)
SAXON_ROOT=$SAXON_BSP_PATH/"../../../.."
SAXON_BSP_COMMON_SCRIPTS=$SAXON_ROOT/SaxonSoc/bsp/common/scripts

# Configurations
SAXON_BUILDROOT_DEFCONFIG=saxon_nexys_a7_100_defconfig

# Functionalities
source $SAXON_BSP_COMMON_SCRIPTS/base.sh
source $SAXON_BSP_COMMON_SCRIPTS/openocd.sh
source $SAXON_BSP_COMMON_SCRIPTS/buildroot_full.sh

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

saxon_ftp_load(){
  cd $SAXON_ROOT
  mkdir -p /var/ftp/pub/saxon/digilent/$SAXON_BSP_NAME
  cp $SAXON_SOC/bsp/common/scripts/linux_tools.sh /var/ftp/pub/saxon/digilent/$SAXON_BSP_NAME
  cp $SAXON_SOC/hardware/synthesis/digilent/$SAXON_BSP_NAME/build/vivado_project/fpga.runs/impl_1/$SAXON_BSP_NAME.bit /var/ftp/pub/saxon/digilent/$SAXON_BSP_NAME/bitstream
  cp $SAXON_BUILDROOT_IMAGE_PATH/linux.dtb /var/ftp/pub/saxon/digilent/$SAXON_BSP_NAME
  cp $SAXON_BUILDROOT_IMAGE_PATH/uImage /var/ftp/pub/saxon/digilent/$SAXON_BSP_NAME
  cp $SAXON_BUILDROOT_IMAGE_PATH/rootfs.tar /var/ftp/pub/saxon/digilent/$SAXON_BSP_NAME
  cp $SAXON_BUILDROOT_IMAGE_PATH/rootfs.cpio.uboot /var/ftp/pub/saxon/digilent/$SAXON_BSP_NAME
  cp $SAXON_BUILDROOT_IMAGE_PATH/fw_jump.bin /var/ftp/pub/saxon/digilent/$SAXON_BSP_NAME
  cp $SAXON_BUILDROOT_IMAGE_PATH/u-boot.bin /var/ftp/pub/saxon/digilent/$SAXON_BSP_NAME
}


saxon_sim(){
  echo "runMain saxon.board.digilent.${SAXON_BSP_NAME}SystemSim $@"
  (cd $SAXON_SOC && sbt "runMain saxon.board.digilent.${SAXON_BSP_NAME}SystemSim $@")
}
