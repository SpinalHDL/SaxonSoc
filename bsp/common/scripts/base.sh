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
  saxon_clone_single "buildroot-spinal-saxon" "https://github.com/SpinalHDL/buildroot-spinal-saxon.git --branch main"
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

# Requires py3tftp server installed (install via pip3)
# requires sudo to operate on port 69

# create file /etc/exports.d/saxon-soc.exports and add line
# /srv/saxon-soc 192.168.1.0/24(rw,nohide,insecure,no_subtree_check,async,no_root_squash)
saxon_serve(){
  sudo mkdir -p /srv/saxon-soc
  sudo mount --bind $SAXON_ROOT/buildroot-build/images /srv/saxon-soc
  cd /srv/saxon-soc
  sudo py3tftp -p 69 
}
