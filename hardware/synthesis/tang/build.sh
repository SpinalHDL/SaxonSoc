#!/bin/bash
TD_HOME=/opt/TD
set -ex
cp ../../netlist/TangLinux* .
#uncomment next line when building with yosys
#yosys build.ys
$TD_HOME/bin/td build.tcl
