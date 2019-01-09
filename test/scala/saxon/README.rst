
Memo
######


sudo src/openocd -f tcl/interface/ftdi/ft2232h_breakout.cfg -c 'set SAXON_CPU0_YAML ../SaxonSoc/cpu0.yaml' -f tcl/target/saxon_xip.cfg

rm -rf * ; cmake -DBOARD=vexriscv_saxon_up5k_evn ..; make -j${nproc}

/opt/zephyr-sdk/sysroots/x86_64-pokysdk-linux/usr/bin/riscv32-zephyr-elf/riscv32-zephyr-elf-gdb

-DCONFIG_DEBUG=y

sudo add-apt-repository ppa:beineri/opt-qt-5.11.1-xenial
sudo apt update
sudo apt install qt511-meta-full