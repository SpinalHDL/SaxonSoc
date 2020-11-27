#!/bin/sh

# Locations
SAXON_SOURCED_SH=$(realpath ${BASH_SOURCE})
SAXON_BSP_PATH=$(dirname $SAXON_SOURCED_SH)
SAXON_ROOT=$SAXON_BSP_PATH/"../../../.."
SAXON_BSP_COMMON_SCRIPTS=$SAXON_ROOT/SaxonSoc/bsp/common/scripts

# Configurations
SAXON_BUILDROOT_DEFCONFIG=saxon_arty_a7_35_defconfig

# Functionalities
source $SAXON_BSP_COMMON_SCRIPTS/base.sh
source $SAXON_BSP_COMMON_SCRIPTS/openocd.sh
source $SAXON_BSP_COMMON_SCRIPTS/buildroot_full.sh

saxon_netlist(){
  cd $SAXON_SOC
  sbt "runMain saxon.board.digilent.ArtyA7SmpLinux"
}

saxon_bitstream(){
  cd $SAXON_SOC/hardware/synthesis/digilent/ArtyA7SmpLinux
  make all
}

saxon_serial(){
  picocom -b 115200 /dev/ttyUSB1 --imap lfcrlf
}

saxon_ftp_load(){
  cd $SAXON_ROOT
  mkdir -p /var/ftp/pub/saxon/digilent/ArtyA7SmpLinux
  cp $SAXON_SOC/bsp/common/scripts/linux_tools.sh /var/ftp/pub/saxon/digilent/ArtyA7SmpLinux
  cp $SAXON_SOC/hardware/synthesis/digilent/ArtyA7SmpLinux/build/vivado_project/fpga.runs/impl_1/ArtyA7SmpLinux.bit /var/ftp/pub/saxon/digilent/ArtyA7SmpLinux/bitstream
  cp $SAXON_BUILDROOT_IMAGE_PATH/linux.dtb /var/ftp/pub/saxon/digilent/ArtyA7SmpLinux
  cp $SAXON_BUILDROOT_IMAGE_PATH/uImage /var/ftp/pub/saxon/digilent/ArtyA7SmpLinux
  cp $SAXON_BUILDROOT_IMAGE_PATH/rootfs.tar /var/ftp/pub/saxon/digilent/ArtyA7SmpLinux
  cp $SAXON_BUILDROOT_IMAGE_PATH/rootfs.cpio.uboot /var/ftp/pub/saxon/digilent/ArtyA7SmpLinux
  cp $SAXON_BUILDROOT_IMAGE_PATH/fw_jump.bin /var/ftp/pub/saxon/digilent/ArtyA7SmpLinux
  cp $SAXON_BUILDROOT_IMAGE_PATH/u-boot.bin /var/ftp/pub/saxon/digilent/ArtyA7SmpLinux
}


saxon_sim(){
  echo "runMain saxon.board.digilent.ArtyA7SmpLinuxSystemSim $@"
  (cd $SAXON_SOC && sbt "runMain saxon.board.digilent.ArtyA7SmpLinuxSystemSim $@")
}
