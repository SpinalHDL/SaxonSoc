# Saxon on Nexys A7-100

## Repo setup

```sh
cd hardware/synthesis/nexys_a7
make repo-setup
```

## Build

### SPI flash software

```sh
cd hardware/synthesis/nexys_a7
make spi-flash-sw
```

### Hardware (and bootloader)

```sh
cd hardware/synthesis/nexys_a7
make clean-sw
make
```

## Program

### QSPI flash programming (load hardware + u-boot)

```sh
cd hardware/synthesis/nexys_a7
make flash
```

### Direct FPGA RAM programming (update only hardware for quick test)

```sh
cd hardware/synthesis/nexys_a7
make prog
```

### SD card Programming

Identify the sdcard in your system, usually something like `sdb` or `mmcblk0`.
One way is to insert it and type `dmesg|tail`:

```sh
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

```sh
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

```sh
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

## Debug

### Build openocd

```sh
git clone git@github.com:SpinalHDL/openocd_riscv.git -b riscv_spinal
cd openocd
./bootstrap
./configure --enable-legacy-ft2232_libftdi --enable-ftdi --enable-dummy --disable-werror
make
cd -
```

### Connect openocd

```sh
# write image to qspi flash
openocd/src/openocd -c "set CPU0_YAML $PWD/SaxonSoc/cpu0.yaml" -s openocd/tcl -s SaxonSoc/bsp/digilent/NexysA7SmpLinux/openocd -f usb_connect.cfg -f fpga_flash.cfg
# initialize ddr
openocd/src/openocd -c "set CPU0_YAML $PWD/SaxonSoc/cpu0.yaml" -s openocd/tcl -s SaxonSoc/bsp/digilent/NexysA7SmpLinux/openocd -f usb_connect.cfg -f soc_init.cfg
# program fpga
openocd/src/openocd -c "set CPU0_YAML $PWD/SaxonSoc/cpu0.yaml" -s openocd/tcl -s SaxonSoc/bsp/digilent/NexysA7SmpLinux/openocd -f usb_connect.cfg -f fpga_load.cfg
# boot to linux
openocd/src/openocd -c "set CPU0_YAML $PWD/SaxonSoc/cpu0.yaml" -s openocd/tcl -s SaxonSoc/bsp/digilent/NexysA7SmpLinux/openocd -f usb_connect.cfg -f soc_init.cfg -f linux_boot.cfg
```

### Connect gdb

```sh
/opt/riscv_xpacks/bin/riscv-none-embed-gdb SaxonSoc/software/standalone/bootloader/build/bootloader.elf --eval-command "target remote :3333"

/opt/riscv_xpacks/bin/riscv-none-embed-gdb opensbi/build/platform/spinal/saxon/digilent/artyA7Smp/firmware/fw_jump.elf --eval-command "target remote :3333"
```

## Misc

### Load Linux from SD
 Use ramfs in RAM
```sh
load mmc 0 80000000 uImage
load mmc 0 80ff0000 dtb
load mmc 0 80ffffc0 rootfs.cpio.uboot
bootm 80000000 80ffffc0 80ff0000
```
 Use rootfs from SD
```sh
load mmc 0 80000000 uImage
load mmc 0 80ff0000 dtb
bootm 80000000 - 80ff0000
```

### Load Linux from TFTP server

```sh
setenv serverip 10.0.0.1
setenv ipaddr 10.0.0.2
saveenv

tftp 80400000 uImage
tftp 80ff0000 dtb
tftp 80ffffc0 rootfs.cpio.uboot
bootm 80400000 80ffffc0 80ff0000
```

```sh
setenv serverip 10.0.0.1
setenv ipaddr 10.0.0.2
setenv pxefile_addr_r 81000000
setenv kernel_addr_r 80400000
setenv ramdisk_addr_r 80ffffc0
setenv fdt_addr_r 80ff0000
saveenv

pxe get
pxe boot
```

```sh
#tftp server (0A000002=10.0.0.2)
$ tree /local/tftpboot/
/local/tftpboot/
├── dtb
├── pxelinux.cfg
│   └── 0A000002
├── rootfs.cpio.uboot
└── uImage

1 directory, 4 files

$ cat /local/tftpboot/pxelinux.cfg/0A000002
label sdcard
kernel uImage
fdt dtb
#initrd rootfs.cpio.uboot
```
 Uncomment the last line above to use ramfs in RAM instead of rootfs from SD

### Linux

```sh
ifconfig eth0 10.0.0.2 up
tcpdump -i any &
ping 10.0.0.1
```

### DTS

```dts
/dts-v1/;

/ {
  #address-cells = <1>;
  #size-cells = <1>;
  compatible = "spinal,vexriscv";
  model = "spinal,vexriscv";

  chosen {
    bootargs = "rootwait console=hvc0  earlycon=sbi root=/dev/mmcblk0p2 init=/sbin/init";
  };

  cpus {
    #address-cells = <1>;
    #size-cells = <0>;
    timebase-frequency = <100000000>;

    cpu@0 {
      device_type = "cpu";
      compatible = "riscv";
      riscv,isa = "rv32ima";
      mmu-type = "riscv,sv32";
      reg = <0>;
      status = "okay";

      L0: interrupt-controller {
        #interrupt-cells = <1>;
        interrupt-controller;
        compatible = "riscv,cpu-intc";
      };
    };

    cpu@1 {
      device_type = "cpu";
      compatible = "riscv";
      riscv,isa = "rv32ima";
      mmu-type = "riscv,sv32";
      reg = <1>;
      status = "okay";

      L1: interrupt-controller {
        #interrupt-cells = <1>;
        interrupt-controller;
        compatible = "riscv,cpu-intc";
      };
    };
  };

  memory@80000000 {
    device_type = "memory";
    reg = <0x80000000 0x08000000>;
  };

  reserved-memory {
    #address-cells = <1>;
    #size-cells = <1>;
    ranges;

    opensbi: sbi@80f80000 {
      reg = <0x80f80000 0x80000>;
    };
  };

  clocks {
    compatible = "simple-bus";
    #address-cells = <1>;
    #size-cells = <0>;

    apbA_clock: clock@1 {
      compatible = "fixed-clock";
      reg = <1 0>;
      #clock-cells = <0>;
      clock-frequency = <100000000>;
    };
  };

  apbA@10000000 {
    compatible = "simple-bus";
    #address-cells = <1>;
    #size-cells = <1>;
    ranges = <0x0 0x10000000 0x01000000>;

    plic: interrupt-controller@c00000 {
      compatible = "sifive,plic-1.0.0";
      #interrupt-cells = <1>;
      interrupt-controller;
      interrupts-extended = <
        &L0 11 &L0 9
        &L1 11 &L1 9>;
      reg = <0x00C00000 0x400000>;
      riscv,ndev = <32>;
    };

    gpioA: gpio@0 {
      compatible = "spinal-lib,gpio-1.0";
      interrupt-parent = <&plic>;
      ngpio = <16>;
      interrupts = <4 5 6 7>;
      reg = <0x000000 0x1000>;
      gpio-controller;
      #gpio-cells = <2>;
      interrupt-controller;
      #interrupt-cells = <2>;
    };

    mac0: mac@40000 {
      compatible = "spinal,lib_mac";
      reg = <0x40000 0x1000>;
      interrupt-parent = <&plic>;
      interrupts = <3>;
    };

    spiA: spi@20000 {
      compatible = "spinal-lib,spi-1.0";
      #address-cells = <1>;
      #size-cells = <0>;
      reg = <0x020000 0x1000>;
      cmd_fifo_depth = <256>;
      rsp_fifo_depth = <256>;
      clocks = <&apbA_clock 0>;
      cs-gpios = <0>, <0>;

      flash: flash@0 {
        #address-cells = <1>;
        #size-cells = <1>;
        compatible = "spi-nor";
        reg = <0>;
        spi-max-frequency = <25000000>;

        partition@0 {
          label = "fpga_bitstream";
          reg = <0x000000 0x400000>;
        };

        parition@1 {
          label = "opensbi";
          reg = <0x400000 0x080000>;
        };

        parition@2 {
          label = "uboot";
          reg = <0x480000 0x080000>;
        };

        parition@3 {
          label = "user";
          reg = <0x500000 0xB00000>;
        };
      };

      mmc-slot@1 {
        compatible = "mmc-spi-slot";
        reg = <1>;
        voltage-ranges = <3300 3300>;
        spi-max-frequency = <25000000>;
      };
    };
  };
};
```

## Simulation

### Ethernet

```sh
make RISCV_BIN=/opt/riscv_xpacks/bin/riscv-none-embed- -C software/standalone/bootloader BSP=digilent/NexysA7SmpLinux SPINAL_SIM=yes clean all
make RISCV_BIN=/opt/riscv_xpacks/bin/riscv-none-embed- -C software/standalone/ethernet BSP=digilent/NexysA7SmpLinux SPINAL_SIM=yes clean all
sbt "runMain saxon.board.digilent.NexysA7SmpLinuxSystemSim"
```
