#!/bin/sh

SAXON_BUILDROOT_OUT_OF_TREE_PATH="${SAXON_BUILDROOT_OUT_OF_TREE_PATH:-$SAXON_ROOT/buildroot-spinal-saxon}"
SAXON_BUILDROOT_BUILD_PATH="${SAXON_BUILDROOT_BUILD_PATH:-$SAXON_ROOT/buildroot-build}"
SAXON_BUILDROOT_PATH="${SAXON_BUILDROOT_PATH:-$SAXON_ROOT/buildroot}"
SAXON_BUILDROOT_IMAGE_PATH="${SAXON_BUILDROOT_IMAGE_PATH:-$SAXON_BUILDROOT_BUILD_PATH/images}"


saxon_buildroot_unset(){
  unset SAXON_BUILDROOT_OUT_OF_TREE_PATH
  unset SAXON_BUILDROOT_BUILD_PATH
  unset SAXON_BUILDROOT_PATH
  unset SAXON_BUILDROOT_IMAGE_PATH
}

saxon_buildroot(){
  saxon_buildroot_clean
  saxon_buildroot_setup
  saxon_buildroot_compile
}

saxon_buildroot_dts(){
  cd $SAXON_BUILDROOT_BUILD_PATH
  make linux-rebuild
}

saxon_buildroot_clean(){
  cd $SAXON_BUILDROOT_BUILD_PATH
  make clean
}

saxon_buildroot_setup(){
  mkdir -p $SAXON_BUILDROOT_BUILD_PATH
  cd $SAXON_BUILDROOT_BUILD_PATH
  make O=$PWD  BR2_EXTERNAL=$SAXON_BUILDROOT_OUT_OF_TREE_PATH  -C $SAXON_BUILDROOT_PATH $SAXON_BUILDROOT_DEFCONFIG
}

saxon_buildroot_compile(){
  cd $SAXON_BUILDROOT_BUILD_PATH
  make linux-rebuild all -j$(nproc)
}

saxon_buildroot_config(){
  cd $SAXON_BUILDROOT_BUILD_PATH
  make $SAXON_BUILDROOT_DEFCONFIG xconfig; make savedefconfig update-defconfig
}

saxon_buildroot_linux_config(){
  cd $SAXON_BUILDROOT_BUILD_PATH
  make $SAXON_BUILDROOT_DEFCONFIG linux-xconfig linux-update-defconfig
}