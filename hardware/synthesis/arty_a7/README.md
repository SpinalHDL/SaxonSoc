# Saxon on Arty A7-35

This guide shows the step to program the FPGA bit file and the SPI flash.
With that you just get u-boot running. To boot linux you need further steps, see
https://github.com/SpinalHDL/SaxonSoc/tree/dev/bsp/Arty7Linux

NOTE: you will need a sdcard Pmod module to boot linux https://store.digilentinc.com/pmod-microsd-microsd-card-slot/

## Before Starting

You should make sure you have the following tools installed:
 * vivado 2018.1 or later
 * riscv toolchain (riscv64-unknown-elf)
 * sbt

## Board setup
Make sure you have a rev E board. If you have a later version check that the
flash part is S25FL128SAGMF100.

Jumper settings for board rev E:
 * Disconnect anything from the connectors (Pmod, Arduino)
 * Jumpers: JP1 and JP2 on, others off.

## Repo setup
You need to do this once to get dependencies.

WARNING: this create u-boot directory aside of the SaxonSoc directory

```sh
cd hardware/synthesis/arty_a7
make repo-setup
```

## Building

### SPI flash software
```sh
cd hardware/synthesis/arty_a7
make spi-flash-sw
```

### Hardware (and bootloader)
```sh
cd hardware/synthesis/arty_a7
make
```

You should get output similar to this
```
mkdir -p build
RISCV_BIN=/opt/riscv/bin/riscv64-unknown-elf- make -C ../../../software/standalone/bootloader BSP=Arty7Linux
...
Memory region         Used Size  Region Size  %age Used
             ram:        1840 B         8 KB     22.46%
...
(cd ../../..; sbt "runMain saxon.board.digilent.Arty7Linux")
...
[info] [Progress] at 3.413 : Generate Verilog
[info] [Warning] memReadAsync can only be write first into Verilog
[info] [Warning] memReadAsync can only be write first into Verilog
[info] [Warning] memReadAsync can only be write first into Verilog
[info] [Warning] 936 signals were pruned. You can call printPruned on the backend report to get more informations.
[info] [Done] at 4.004
[success] Total time: 96 s, completed Jan 18, 2020 12:55:30 PM
mkdir -p build
./make_vivado_project
...
Creating bitstream...
Writing bitstream ./Arty7Linux.bit...
INFO: [Vivado 12-1842] Bitgen Completed Successfully.
...
Implementation done!
...
```

The process should take around 8 minutes on a reasonably fast computer.

## Programming

### QSPI flash programming (load hardware + u-boot)

Run `make flash` to program the bit file to the QSPI flash.

You should get output like the following;
```
...
****** Xilinx hw_server v2018.1
  **** Build date : Apr  4 2018-18:56:09
    ** Copyright 1986-2018 Xilinx, Inc. All Rights Reserved.


INFO: [Labtoolstcl 44-466] Opening hw_target localhost:3121/xilinx_tcf/Digilent/210319AB569AA
INFO: [Labtools 27-1434] Device xc7a35t (JTAG device index = 0) is programmed with a design that has no supported debug core(s) in it.
...
INFO: [Labtools 27-3164] End of startup status: HIGH
Mfg ID : 1   Memory Type : 20   Memory Capacity : 18   Device ID 1 : 0   Device ID 2 : 0
Performing Erase Operation...
Erase Operation successful.
Performing Program and Verify Operations...
Program/Verify Operation successful.
INFO: [Labtoolstcl 44-377] Flash programming completed successfully
program_hw_cfgmem: Time (s): cpu = 00:00:00.11 ; elapsed = 00:00:52 . Memory (MB): peak = 1792.711 ; gain = 8.000 ; free physical = 17712 ; free virtual = 56943
INFO: [Labtoolstcl 44-464] Closing hw_target localhost:3121/xilinx_tcf/Digilent/210319AB569AA
...
INFO: [Common 17-206] Exiting Vivado at Thu Nov 28 04:06:28 2019...
```

After programming the software starts and send some output on the uart.
NOTE: the RESET button is not connected to the design. To reboot, use the
 "PROG" button on the board. Then after a second or so the "DONE" LED shall be
 ON and something shall be sent on the uart.


### Direct FPGA RAM programming (update only hardware for quick test)

Run `make prog` to program the bit file directly to FPGA RAM.
Next time you power up the board the FPGA RAM will be reloaded with content from
QSPI flash so remember that anything loaded using this method is only temporary.

You should get output like the following;
```
...
****** Xilinx hw_server v2018.1
  **** Build date : Apr  4 2018-18:56:09
    ** Copyright 1986-2018 Xilinx, Inc. All Rights Reserved.

INFO: [Labtoolstcl 44-466] Opening hw_target localhost:3121/xilinx_tcf/Digilent/210319AB569AA
INFO: [Labtools 27-1434] Device xc7a35t (JTAG device index = 0) is programmed with a design that has no supported debug core(s) in it.
WARNING: [Labtools 27-3361] The debug hub core was not detected.
Resolution:
1. Make sure the clock connected to the debug hub (dbg_hub) core is a free running clock and is active.
2. Make sure the BSCAN_SWITCH_USER_MASK device property in Vivado Hardware Manager reflects the user scan chain setting in the design and refresh the device.  To determine the user scan chain setting in the design, open the implemented design and use 'get_property C_USER_SCAN_CHAIN [get_debug_cores dbg_hub]'.
For more details on setting the scan chain property, consult the Vivado Debug and Programming User Guide (UG908).
INFO: [Labtools 27-3164] End of startup status: HIGH
INFO: [Common 17-206] Exiting Vivado at Thu Nov 28 04:01:36 2019...
```

After programming the bootloader included in the bit file is started
(no visual feedback of any kind).
It shall launch the software installed in the QSPI flash.

### SD card Programming
Identify the sdcard in your system, usually something like `sdb` or `mmcblk0`.
One way is to insert it and type `dmesg|tail`:
```
user@lafite:~/Downloads/SaxonSoc/hardware/synthesis/arty_a7$ dmesg|tail
[14396.176825] sd 1:0:0:0: [sdb] Mode Sense: 03 00 00 00
[14396.176965] sd 1:0:0:0: [sdb] No Caching mode page found
[14396.176973] sd 1:0:0:0: [sdb] Assuming drive cache: write through
[14396.179585]  sdb: sdb1 sdb2
[14396.181975] sd 1:0:0:0: [sdb] Attached SCSI removable disk
[14396.633059] EXT4-fs (sdb1): mounting ext2 file system using the ext4 subsystem
[14396.641204] EXT4-fs (sdb1): mounted filesystem without journal. Opts: (null)
[14396.650952] EXT4-fs (sdb2): mounting ext2 file system using the ext4 subsystem
[14396.652644] EXT4-fs (sdb2): warning: mounting unchecked fs, running e2fsck is recommended
[14396.656826] EXT4-fs (sdb2): mounted filesystem without journal. Opts: (null)
```
So here the sdcard is `sdb`. Program it as follow:
```
make sdcard SDCARD=sdb
```

## Connect

After programming you should be able to connect to the serial port and have some output.

On Linux you can do this using a command like `screen /dev/ttyUSB1`. Other good alternatives:

* moserial (GUI)
* picocom (can be launched via the file "picocom_arty")

Parameters:
* port is        : /dev/ttyUSB1
* flowcontrol    : none
* baudrate is    : 115200
* parity is      : none
* databits are   : 8
* stopbits are   : 1

Output when no sdcard is present:
```
*** VexRiscv BIOS ***
*** Supervisor ***


U-Boot 2019.10-02536-gefeedc3 (Jan 18 2020 - 14:18:57 +0100)

DRAM:  31.9 MiB
MMC:   spi@10020000:mmc@1: 0
Loading Environment from EXT4... In:    serial@10010000
Out:   serial@10010000
Err:   serial@10010000
Hit any key to stop autoboot:  2  1  0
Wrong Image Format for bootm command
ERROR: can't get kernel image!
=>
```

Output when sdcard is present:
```
*** VexRiscv BIOS ***
*** Supervisor ***


U-Boot 2019.10-02536-gefeedc3 (Jan 18 2020 - 12:51:00 +0100)

DRAM:  31.9 MiB
MMC:   spi@10020000:mmc@1: 0
Loading Environment from EXT4... ** File not found /uboot.env **

** Unable to read "/uboot.env" from mmc0:1 **
In:    serial@10010000
Out:   serial@10010000
Err:   serial@10010000
Hit any key to stop autoboot:  0
```
and after about 6 minutes...
```
4676772 bytes read in 380006 ms (11.7 KiB/s)
2632 bytes read in 341 ms (6.8 KiB/s)
## Booting kernel from Legacy Image at 80000000 ...
   Image Name:   Linux
   Image Type:   RISC-V Linux Kernel Image (uncompressed)
   Data Size:    4676708 Bytes = 4.5 MiB
   Load Address: 80000000
   Entry Point:  80000000
   Verifying Checksum ... OK
## Flattened Device Tree blob at 81e00000
   Booting using the fdt blob at 0x81e00000
   Loading Kernel Image
   Using Device Tree in place at 81e00000, end 81e03a47

Starting kernel ...

[    0.000000] No DTB passed to the kernel
[    0.000000] Linux version 5.0.9 (user@lafite) (gcc version 8.3.0 (Buildroot 2019.05-git-00657-g2b0446e)) #1 Fri Jan 17 22:36:11 CET 2020
...
[    1.439131] This architecture does not have kernel memory protection.
[    1.441785] Run /sbin/init as init process
Starting syslogd: OK
Starting klogd: OK
Initializing random number generator... [    4.265503] random: dd: uninitialized urandom read (512 bytes read)
done.
Starting network: OK

Welcome to Buildroot
buildroot login:
```
At this point you can login as root (no password)
```
buildroot login: root
            ___                                             ___     ___     ___   
    o O O  / __|   __ _    __ __    ___    _ _      ___    / __|   / _ \   / __|  
   o       \__ \  / _` |   \ \ /   / _ \  | ' \    |___|   \__ \  | (_) | | (__   
  TS__[O]  |___/  \__,_|   /_\_\   \___/  |_||_|   _____   |___/   \___/   \___|  
 {======|_|"""""|_|"""""|_|"""""|_|"""""|_|"""""|_|     |_|"""""|_|"""""|_|"""""|
./o--000'"`-0-0-'"`-0-0-'"`-0-0-'"`-0-0-'"`-0-0-'"`-0-0-'"`-0-0-'"`-0-0-'"`-0-0-'

login[71]: root login on 'hvc0'
root@buildroot:~#
```
