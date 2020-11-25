#!/bin/sh

SAXON_SOC="${SAXON_SOC:-$SAXON_ROOT/SaxonSoc}"

saxon_source(){
  cd $SAXON_ROOT
  source $SAXON_SOURCED_SH
}

saxon_clone_single() {
  cd $SAXON_ROOT
  if cd $1; then git pull  --recurse-submodules; else git clone $2 ; fi
}

saxon_clone() {
  #saxon_clone_single "u-boot" "https://github.com/SpinalHDL/u-boot.git --branch smp"
  saxon_clone_single "buildroot" "https://github.com/buildroot/buildroot.git --branch master"
  saxon_clone_single "buildroot-spinal-saxon" "https://github.com/svancau/buildroot-spinal-saxon.git --branch main"
  #saxon_clone_single "linux" "https://github.com/SpinalHDL/linux.git --branch vexriscv"
  #saxon_clone_single "opensbi" "https://github.com/SpinalHDL/opensbi.git --branch spinal"
  saxon_clone_single "openocd_riscv" "https://github.com/SpinalHDL/openocd_riscv.git"
  saxon_clone_single "SaxonSoc" "https://github.com/SpinalHDL/SaxonSoc.git"
}
saxon_update() {
    saxon_clone
}

saxon_standalone_compile(){
  cd $SAXON_SOC/software/standalone/$1
  make clean all BSP_PATH=$SAXON_BSP_PATH "${@:2}"
}

