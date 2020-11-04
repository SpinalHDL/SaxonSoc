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
  saxon_clone_single "u-boot" "https://github.com/SpinalHDL/u-boot.git --branch smp"
  saxon_clone_single "buildroot" "https://github.com/SpinalHDL/buildroot.git --branch spinal"
  saxon_clone_single "linux" "https://github.com/SpinalHDL/linux.git --branch vexriscv"
  saxon_clone_single "opensbi" "https://github.com/SpinalHDL/opensbi.git --branch spinal"
  saxon_clone_single "openocd_riscv" "https://github.com/SpinalHDL/openocd_riscv.git"
  saxon_clone_single "SaxonSoc" "https://github.com/SpinalHDL/SaxonSoc.git"
  saxon_fix
}
saxon_update() {
    saxon_clone
}

saxon_standalone_compile(){
  cd $SAXON_SOC/software/standalone/$1
  make clean all BSP_PATH=$SAXON_BSP_PATH "${@:2}"
}

saxon_fix(){
  saxon_fix_from $SAXON_BSP_PATH
  for fix in $SAXON_FIXES; do
    saxon_fix_from $fix
  done
}

saxon_fix_from(){
  echo "***"
  echo "*** Apply patches from $1"
  echo "***"
  cd $SAXON_ROOT
  if [ -d "$1/rsync" ]; then
    rsync -v -r -a $1/rsync/* $SAXON_ROOT
  fi

  if [ -d "$1/patch" ]; then
    for patch in $1/patch/*; do
      if ! patch -R -p0 -s -f --dry-run < $patch >/dev/null; then
        patch -p0 < $patch
      fi
    done
  fi
}

