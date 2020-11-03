#!/bin/sh


saxon_opensbi(){
  saxon_patch
  cd $SAXON_ROOT/opensbi
  export CROSS_COMPILE=riscv-none-embed-
  export PLATFORM_RISCV_XLEN=32
  make PLATFORM=$SAXON_OPENSBI_PLATEFORM clean
  make PLATFORM=$SAXON_OPENSBI_PLATEFORM -j$(nproc) SAXON_PATH=$SAXON_SOC SAXON_BSP_PATH=$SAXON_BSP_PATH
  riscv-none-embed-objdump  -S -d build/platform/$SAXON_OPENSBI_PLATEFORM/firmware/fw_jump.elf > fw_jump.asm
}

