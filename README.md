
openocd =>
https://github.com/SpinalHDL/openocd_riscv.git

openocd for simulation
src/openocd -f tcl/interface/jtag_tcp.cfg -c 'set SAXON_CPU0_YAML ../SaxonSoc/cpu0.yaml' -f tcl/target/saxon_xip.cfg


Zephyr build =>
git clone https://github.com/SpinalHDL/zephyr.git -b vexriscv

cd zephyr
unset ZEPHYR_GCC_VARIANT
unset ZEPHYR_SDK_INSTALL_DIR
export CROSS_COMPILE="/opt/riscv/bin/riscv64-unknown-elf-"
export ZEPHYR_TOOLCHAIN_VARIANT="cross-compile"
export ZEPHYR_GCC_VARIANT="cross-compile"
source zephyr-env.sh

cd samples/philosophers
mkdir build
cd build

cmake -DBOARD=vexriscv_saxon_up5k_evn ..
make -j${nproc}



git clone http://github.com/buildroot/buildroot
cd buildroot
cp -r ../SaxonSoc.git/software/buildroot/* ./
make saxon_default_defconfig
make -j$(nproc)
output/host/bin/riscv32-linux-objcopy  -O binary output/images/vmlinux output/images/Image