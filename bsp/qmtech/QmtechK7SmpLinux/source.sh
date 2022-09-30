#!/bin/sh

# Locations
SAXON_SOURCED_SH=$(realpath ${BASH_SOURCE})
SAXON_BSP_PATH=$(dirname $SAXON_SOURCED_SH)
SAXON_ROOT=$SAXON_BSP_PATH/"../../../.."
SAXON_BSP_COMMON_SCRIPTS=$SAXON_ROOT/SaxonSoc/bsp/common/scripts

# Configurations
SAXON_BUILDROOT_DEFCONFIG=saxon_arty_a7_35_defconfig
SAXON_BUILDROOT_FULL_OOT_GIT="https://github.com/SpinalHDL/buildroot-spinal-saxon.git --branch main"

# Functionalities
source $SAXON_BSP_COMMON_SCRIPTS/base.sh
source $SAXON_BSP_COMMON_SCRIPTS/openocd.sh
source $SAXON_BSP_COMMON_SCRIPTS/buildroot_full.sh

saxon_netlist(){
  cd $SAXON_SOC
  sbt "runMain saxon.board.qmtech.QmtechK7SmpLinux"
}

saxon_bitstream(){
  cd $SAXON_SOC/hardware/synthesis/qmtech/QmtechK7SmpLinux
  make all
}

saxon_serial(){
  picocom -b 115200 /dev/ttyUSB1 --imap lfcrlf
}

saxon_ftp_load(){
  cd $SAXON_ROOT
  mkdir -p /var/ftp/pub/saxon/qmtech/QmtechK7SmpLinux
  cp $SAXON_SOC/bsp/common/scripts/linux_tools.sh /var/ftp/pub/saxon/qmtech/QmtechK7SmpLinux
  cp $SAXON_SOC/hardware/synthesis/qmtech/QmtechK7SmpLinux/build/vivado_project/fpga.runs/impl_1/QmtechK7SmpLinux.bit /var/ftp/pub/saxon/qmtech/QmtechK7SmpLinux/bitstream
  cp $SAXON_BUILDROOT_IMAGE_PATH/linux.dtb /var/ftp/pub/saxon/qmtech/QmtechK7SmpLinux
  cp $SAXON_BUILDROOT_IMAGE_PATH/uImage /var/ftp/pub/saxon/qmtech/QmtechK7SmpLinux
  cp $SAXON_BUILDROOT_IMAGE_PATH/rootfs.tar /var/ftp/pub/saxon/qmtech/QmtechK7SmpLinux
  cp $SAXON_BUILDROOT_IMAGE_PATH/rootfs.cpio.uboot /var/ftp/pub/saxon/qmtech/QmtechK7SmpLinux
  cp $SAXON_BUILDROOT_IMAGE_PATH/fw_jump.bin /var/ftp/pub/saxon/qmtech/QmtechK7SmpLinux
  cp $SAXON_BUILDROOT_IMAGE_PATH/u-boot.bin /var/ftp/pub/saxon/qmtech/QmtechK7SmpLinux
}


saxon_sim(){
  echo "runMain saxon.board.qmtech.QmtechK7SmpLinuxSystemSim $@"
  (cd $SAXON_SOC && sbt "runMain saxon.board.qmtech.QmtechK7SmpLinuxSystemSim $@")
}


