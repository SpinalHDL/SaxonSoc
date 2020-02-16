#!/bin/bash
TD_HOME=/opt/TD
set -ex

cp ../../netlist/TangLinux* .
cp ../../../software/standalone/machineModeSbi/build/machineModeSbi.bin .
cp ../../../../u-boot/spl/u-boot-spl.bin .

dd if=/dev/zero of=bram16x32k.bin bs=1K count=64
dd if=machineModeSbi.bin of=bram16x32k.bin bs=1K conv=notrunc
dd if=u-boot-spl.bin of=bram16x32k.bin bs=1K seek=16 conv=notrunc

python split.py bram16x32k.bin

#uncomment next line when building with yosys
#yosys build.ys
$TD_HOME/bin/td build.tcl
