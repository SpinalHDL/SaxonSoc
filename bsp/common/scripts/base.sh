#!/bin/sh


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


saxon_baremetal_compile(){
  cd $SAXON_ROOT/SaxonSoc/software/standalone/$1
  make clean all BSP_PATH=$SAXON_BSP_PATH "${@:2}"
}
