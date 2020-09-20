#!/bin/sh


saxon_source(){
  cd $SAXON_ROOT
  source $SAXON_SOURCED_SH
}

saxon_clone_single() {
  cd $SAXON_ROOT
  if cd $1; then git pull  --recurse-submodules; else git clone $2 ; fi
}

saxon_clone() {
  saxon_clone_single "u-boot" "https://github.com/SpinalHDL/u-boot.git --branch smp"
  saxon_clone_single "buildroot" "https://github.com/SpinalHDL/buildroot.git --branch spinal"
  saxon_clone_single "linux" "https://github.com/SpinalHDL/linux.git --branch vexriscv"
  saxon_clone_single "opensbi" "https://github.com/SpinalHDL/opensbi.git --branch spinal"
  saxon_clone_single "openocd_riscv" "https://github.com/SpinalHDL/openocd_riscv.git"
}
saxon_update() {
    saxon_clone
}

saxon_standalone_compile(){
  cd $SAXON_ROOT/SaxonSoc/software/standalone/$1
  make clean all BSP_PATH=$SAXON_BSP_PATH "${@:2}"
}
