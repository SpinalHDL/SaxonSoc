#!/bin/sh


saxon_openocd(){
  cd $SAXON_ROOT/openocd_riscv
  ./bootstrap
  ./configure --enable-ftdi --enable-dummy
  make -j$(nproc)
}

saxon_fpga_load(){
  cd $SAXON_SOC
  $SAXON_ROOT/openocd_riscv/src/openocd -s $SAXON_ROOT/openocd_riscv/tcl -s $SAXON_BSP_PATH/openocd -c 'set CPU0_YAML cpu0.yaml' -f usb_connect.cfg -f fpga_load.cfg
}

saxon_openocd_connect(){
  cd $SAXON_SOC
  $SAXON_ROOT/openocd_riscv/src/openocd -s $SAXON_ROOT/openocd_riscv/tcl -s $SAXON_BSP_PATH/openocd -c 'set CPU0_YAML cpu0.yaml' -f usb_connect.cfg -f soc_init.cfg
}

saxon_buildroot_load(){
  cd $SAXON_SOC
  $SAXON_ROOT/openocd_riscv/src/openocd -s $SAXON_ROOT/openocd_riscv/tcl -s $SAXON_BSP_PATH/openocd -c 'set CPU0_YAML cpu0.yaml'  -c "set SAXON_BUILDROOT_IMAGE_PATH $SAXON_BUILDROOT_IMAGE_PATH" -f usb_connect.cfg -f soc_init.cfg -f linux_boot.cfg -c 'exit'
}

saxon_baremetal_load(){
  cd $SAXON_SOC
  $SAXON_ROOT/openocd_riscv/src/openocd -s $SAXON_ROOT/openocd_riscv/tcl -s $SAXON_BSP_PATH/openocd -c 'set CPU0_YAML cpu0.yaml' -c "set APP_BIN $1" -f usb_connect.cfg -f soc_init.cfg -f baremetal.cfg
}

saxon_standalone_load(){
  cd $SAXON_SOC
  $SAXON_ROOT/openocd_riscv/src/openocd -s $SAXON_ROOT/openocd_riscv/tcl -s $SAXON_BSP_PATH/openocd -c 'set CPU0_YAML cpu0.yaml' -c "set APP_BIN software/standalone/$1/build/$1.bin" -f usb_connect.cfg -f soc_init.cfg -f baremetal.cfg
}