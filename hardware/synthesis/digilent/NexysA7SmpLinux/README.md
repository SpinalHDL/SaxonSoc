# Saxon on Nexys A7-100

## Repo setup

```sh
cd hardware/synthesis/digilent/NexysA7SmpLinux/
make repo-setup
```

## Build

### SPI flash software

```sh
cd hardware/synthesis/digilent/NexysA7SmpLinux/
make spi-flash-sw
```

### Hardware (and bootloader)

```sh
cd hardware/synthesis/digilent/NexysA7SmpLinux/
make clean-sw
make
```

## Program

### QSPI flash programming (load hardware + u-boot)

```sh
cd hardware/synthesis/digilent/NexysA7SmpLinux/
make flash
```

### Direct FPGA RAM programming (update only hardware for quick test)

```sh
cd hardware/synthesis/digilent/NexysA7SmpLinux/
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

### Load Linux with PXE boot

```sh
setenv pxefile_addr_r 81000000
setenv kernel_addr_r 80400000
setenv ramdisk_addr_r 80ffffc0
setenv fdt_addr_r 80ff0000
saveenv

dhcp
pxe get
pxe boot
```

#### Configs


```sh
$ sudo apt install dnsmasq
#disable DNS, enable DHCP and TFTP
$ cat /etc/dnsmasq.conf 
port=0
#my interface connected to board is enxd8eb97b31ea5
interface=enxd8eb97b31ea5
no-hosts
dhcp-range=10.0.0.50,10.0.0.150
enable-tftp
tftp-root=/local/tftpboot

#tftp server files (0A0000=10.0.0)
$ tree /local/tftpboot/
/local/tftpboot/
├── dtb
├── pxelinux.cfg
│   └── 0A0000
├── rootfs.cpio.uboot
└── uImage

1 directory, 4 files

$ cat /local/tftpboot/pxelinux.cfg/0A0000
label sdcard
kernel uImage
fdt dtb
#initrd rootfs.cpio.uboot
#uncomment line above to use ramfs from RAM instead of rootfs from SD
```

#### Bootlog

```sh
=> print                                                                        
arch=riscv                                                                      
baudrate=115200                                                                 
board=saxon                                                                     
board_name=saxon                                                                
bootdelay=-1                                                                    
cpu=generic                                                                     
ethact=mac@10040000                                                             
fdt_addr_r=80ff0000                                                             
fdt_high=0xffffffff                                                             
fdtcontroladdr=87fec270                                                         
fileaddr=81000000                                                               
initrd_high=0xffffffff                                                          
kernel_addr_r=80400000                                                          
pxefile_addr_r=81000000                                                         
ramdisk_addr_r=80ffffc0                                                         
stderr=serial@10010000                                                          
stdin=serial@10010000                                                           
stdout=serial@10010000                                                          
vendor=vexriscv                                                                 
                                                                                
Environment size: 362/126972 bytes                                              
=> dhcp                                                                         
BOOTP broadcast 1                                                               
BOOTP broadcast 2                                                               
BOOTP broadcast 3                                                               
BOOTP broadcast 4                                                               
DHCP client bound to address 10.0.0.60 (3021 ms)                                
*** Warning: no boot file name; using '0A00003C.img'                            
Using mac@10040000 device                                                       
TFTP from server 10.0.0.1; our IP address is 10.0.0.60                          
Filename '0A00003C.img'.                                                        
Load address: 0x80f00000                                                        
Loading: *                                                                      
TFTP error: 'file /local/tftpboot/0A00003C.img not found' (1)                   
Not retrying...                                                                 
=> pxe get                                                                      
missing environment variable: pxeuuid                                           
missing environment variable: bootfile                                          
Retrieving file: pxelinux.cfg/0A00003C                                          
Using mac@10040000 device                                                       
TFTP from server 10.0.0.1; our IP address is 10.0.0.60                          
Filename 'pxelinux.cfg/0A00003C'.                                               
Load address: 0x81000000                                                        
Loading: *                                                                      
TFTP error: 'file /local/tftpboot/pxelinux.cfg/0A00003C not found' (1)          
Not retrying...                                                                 
missing environment variable: bootfile                                          
Retrieving file: pxelinux.cfg/0A00003                                           
Using mac@10040000 device                                                       
TFTP from server 10.0.0.1; our IP address is 10.0.0.60                          
Filename 'pxelinux.cfg/0A00003'.                                                
Load address: 0x81000000                                                        
Loading: *                                                                      
TFTP error: 'file /local/tftpboot/pxelinux.cfg/0A00003 not found' (1)           
Not retrying...                                                                 
missing environment variable: bootfile                                          
Retrieving file: pxelinux.cfg/0A0000                                            
Using mac@10040000 device                                                       
TFTP from server 10.0.0.1; our IP address is 10.0.0.60                          
Filename 'pxelinux.cfg/0A0000'.                                                 
Load address: 0x81000000                                                        
Loading: #                                                                      
         33.2 KiB/s                                                             
done                                                                            
Bytes transferred = 139 (8b hex)                                                
Config file found                                                               
=> pxe boot                                                                     
1:      sdcard                                                                  
missing environment variable: bootfile                                          
Retrieving file: rootfs.cpio.uboot                                              
Using mac@10040000 device                                                       
TFTP from server 10.0.0.1; our IP address is 10.0.0.60                          
Filename 'rootfs.cpio.uboot'.                                                   
Load address: 0x80ffffc0                                                        
Loading: #################################################################      
         #################################################################      
         #################################################################      
         #################################################################      
         #################################################################      
         #################################################################      
         #################################################################      
         #################################################################      
         #################################################################      
         #################################################################      
         #################################################################      
         #################################################################      
         ###################################                                    
         174.8 KiB/s                                                            
done                                                                            
Bytes transferred = 11952192 (b66040 hex)                                       
missing environment variable: bootfile                                          
Retrieving file: uImage                                                         
Using mac@10040000 device                                                       
TFTP from server 10.0.0.1; our IP address is 10.0.0.60                          
Filename 'uImage'.                                                              
Load address: 0x80400000                                                        
Loading: #################################################################      
         #################################################################      
         #################################################################      
         #################################################################      
         #################################################################      
         #######################                                                
         174.8 KiB/s                                                            
done                                                                            
Bytes transferred = 5104524 (4de38c hex)                                        
missing environment variable: bootfile                                          
Retrieving file: dtb                                                            
Using mac@10040000 device                                                       
TFTP from server 10.0.0.1; our IP address is 10.0.0.60                          
Filename 'dtb'.                                                                 
Load address: 0x80ff0000                                                        
Loading: #                                                                      
         149.4 KiB/s                                                            
done                                                                            
Bytes transferred = 2758 (ac6 hex)                                              
## Booting kernel from Legacy Image at 80400000 ...                             
   Image Name:   Linux                                                          
   Image Type:   RISC-V Linux Kernel Image (uncompressed)                       
   Data Size:    5104460 Bytes = 4.9 MiB                                        
   Load Address: 80400000                                                       
   Entry Point:  80400000                                                       
   Verifying Checksum ... OK                                                    
## Loading init Ramdisk from Legacy Image at 80ffffc0 ...                       
   Image Name:                                                                  
   Image Type:   RISC-V Linux RAMDisk Image (uncompressed)                      
   Data Size:    11952128 Bytes = 11.4 MiB                                      
   Load Address: 00000000                                                       
   Entry Point:  00000000                                                       
   Verifying Checksum ... OK                                                    
## Flattened Device Tree blob at 80ff0000                                       
   Booting using the fdt blob at 0x80ff0000                                     
   Loading Kernel Image                                                         
   Using Device Tree in place at 80ff0000, end 80ff3ac5                         
                                                                                
Starting kernel ...                                                             
                                                                                
[    0.000000] OF: fdt: Ignoring memory range 0x80000000 - 0x80400000           
[    0.000000] No DTB passed to the kernel                                      
[    0.000000] Linux version 5.0.9 (roman@XPS-15-9530) (gcc version 8.4.0 (Buil0
[    0.000000] earlycon: sbi0 at I/O port 0x0 (options '')                      
[    0.000000] printk: bootconsole [sbi0] enabled                               
[    0.000000] Initial ramdisk at: 0x(ptrval) (11952128 bytes)                  
[    0.000000] Zone ranges:                                                     
[    0.000000]   Normal   [mem 0x0000000080400000-0x0000000087ffffff]           
[    0.000000] Movable zone start for each node                                 
[    0.000000] Early memory node ranges                                         
[    0.000000]   node   0: [mem 0x0000000080400000-0x0000000087ffffff]          
[    0.000000] Initmem setup node 0 [mem 0x0000000080400000-0x0000000087ffffff] 
[    0.000000] elf_hwcap is 0x1101                                              
[    0.000000] percpu: Embedded 10 pages/cpu @(ptrval) s18316 r0 d22644 u40960  
[    0.000000] Built 1 zonelists, mobility grouping on.  Total pages: 31496     
[    0.000000] Kernel command line: rootwait console=hvc0  earlycon=sbi root=/dt
[    0.000000] Dentry cache hash table entries: 16384 (order: 4, 65536 bytes)   
[    0.000000] Inode-cache hash table entries: 8192 (order: 3, 32768 bytes)     
[    0.000000] Sorting __ex_table...                                            
[    0.000000] Memory: 108624K/126976K available (3890K kernel code, 146K rwdat)
[    0.000000] SLUB: HWalign=64, Order=0-3, MinObjects=0, CPUs=2, Nodes=1       
[    0.000000] rcu: Hierarchical RCU implementation.                            
[    0.000000] rcu:     RCU restricting CPUs from NR_CPUS=8 to nr_cpu_ids=2.    
[    0.000000] rcu: RCU calculated value of scheduler-enlistment delay is 25 ji.
[    0.000000] rcu: Adjusting geometry for rcu_fanout_leaf=16, nr_cpu_ids=2     
[    0.000000] NR_IRQS: 0, nr_irqs: 0, preallocated irqs: 0                     
[    0.000000] plic: mapped 32 interrupts to 4 (out of 4) handlers.             
[    0.000000] clocksource: riscv_clocksource: mask: 0xffffffffffffffff max_cycs
[    0.000091] sched_clock: 64 bits at 100MHz, resolution 10ns, wraps every 439s
[    0.009203] Console: colour dummy device 80x25                               
[    0.012734] printk: console [hvc0] enabled                                   
[    0.012734] printk: console [hvc0] enabled                                   
[    0.020997] printk: bootconsole [sbi0] disabled                              
[    0.020997] printk: bootconsole [sbi0] disabled                              
[    0.030308] Calibrating delay loop (skipped), value calculated using timer f)
[    0.040639] pid_max: default: 32768 minimum: 301                             
[    0.047078] Mount-cache hash table entries: 1024 (order: 0, 4096 bytes)      
[    0.052037] Mountpoint-cache hash table entries: 1024 (order: 0, 4096 bytes) 
[    0.072835] rcu: Hierarchical SRCU implementation.                           
[    0.079974] smp: Bringing up secondary CPUs ...                              
[    0.087538] smp: Brought up 1 node, 2 CPUs                                   
[    0.093849] devtmpfs: initialized                                            
[    0.112225] random: get_random_bytes called from setup_net+0x4c/0x198 with c0
[    0.116264] clocksource: jiffies: mask: 0xffffffff max_cycles: 0xffffffff, ms
[    0.119320] futex hash table entries: 512 (order: 3, 32768 bytes)            
[    0.128766] NET: Registered protocol family 16                               
[    0.212586] clocksource: Switched to clocksource riscv_clocksource           
[    0.323528] NET: Registered protocol family 2                                
[    0.332932] tcp_listen_portaddr_hash hash table entries: 512 (order: 0, 6144)
[    0.335014] TCP established hash table entries: 1024 (order: 0, 4096 bytes)  
[    0.337216] TCP bind hash table entries: 1024 (order: 1, 8192 bytes)         
[    0.343520] TCP: Hash tables configured (established 1024 bind 1024)         
[    0.350402] UDP hash table entries: 256 (order: 1, 8192 bytes)               
[    0.355792] UDP-Lite hash table entries: 256 (order: 1, 8192 bytes)          
[    0.366074] Unpacking initramfs...                                           
[    1.250048] workingset: timestamp_bits=30 max_order=15 bucket_order=0        
[    1.415704] Block layer SCSI generic (bsg) driver version 0.4 loaded (major )
[    1.417827] io scheduler mq-deadline registered                              
[    1.418983] io scheduler kyber registered                                    
[    1.426163] spinal_lib_gpio 10000000.gpio: Spinal lib GPIO chip registered 1s
[    2.143218] m25p80 spi0.0: s25fl128s (16384 Kbytes)                          
[    2.144927] 4 fixed-partitions partitions found on MTD device spi0.0         
[    2.146480] Creating 4 MTD partitions on "spi0.0":                           
[    2.148712] 0x000000000000-0x000000400000 : "fpga_bitstream"                 
[    2.159025] 0x000000400000-0x000000480000 : "opensbi"                        
[    2.165015] 0x000000480000-0x000000500000 : "uboot"                          
[    2.170773] 0x000000500000-0x000001000000 : "user"                           
[    2.179390] spinal-lib,spi-1.0 10020000.spi: base (ptrval), irq -6           
[    2.183253] libphy: Fixed MDIO Bus: probed                                   
[    2.185363] spinal_lib_mac_probe                                             
[    2.192774] spinal_lib_mac 10040000.mac eth0: irq 5, mapped at a0405000      
[    2.194361] spinal_lib_mac_probe done                                        
[    2.225214] mmc_spi spi0.1: SD/MMC host mmc0, no WP, no poweroff, cd polling 
[    2.248686] NET: Registered protocol family 10                               
[    2.261893] Segment Routing with IPv6                                        
[    2.263601] sit: IPv6, IPv4 and MPLS over IPv4 tunneling driver              
[    2.272676] NET: Registered protocol family 17                               
[    2.285948] Freeing unused kernel memory: 160K                               
[    2.287058] This architecture does not have kernel memory protection.        
[    2.288899] Run /init as init process                                        
[    2.307678] mmc0: host does not support reading read-only switch, assuming we
[    2.310580] mmc0: new SDHC card on SPI                                       
[    2.325056] mmcblk0: mmc0:0000 F0F0F 3.70 GiB                                
[    2.344806]  mmcblk0: p1 p2                                                  
Starting syslogd: OK                                                            
Starting klogd: OK                                                              
Running sysctl: OK                                                              
Saving random seed: [    3.279138] random: dd: uninitialized urandom read (512 )
OK                                                                              
Starting network: OK                                                            
Starting dropbear sshd: [    3.861869] random: dropbear: uninitialized urandom )
OK                                                                              
                                                                                
Welcome to Buildroot                                                            
buildroot login: root                                                           
            ___                                             ___     ___     ___ 
    o O O  / __|   __ _    __ __    ___    _ _      ___    / __|   / _ \   / __ 
   o       \__ \  / _` |   \ \ /   / _ \  | ' \    |___|   \__ \  | (_) | | (__ 
  TS__[O]  |___/  \__,_|   /_\_\   \___/  |_||_|   _____   |___/   \___/   \___ 
 {======|_|"""""|_|"""""|_|"""""|_|"""""|_|"""""|_|     |_|"""""|_|"""""|_|"""" 
./o--000'"`-0-0-'"`-0-0-'"`-0-0-'"`-0-0-'"`-0-0-'"`-0-0-'"`-0-0-'"`-0-0-'"`-0-0 
                                                                                
login[96]: root login on 'hvc0'                                                 
root@buildroot:~# ifup eth0                                                     
[  220.353241] spinal_lib_mac_open                                              
[  220.354182] spinal_lib_mac_open done                                         
udhcpc: started, v1.31.1                                                        
udhcpc: sending discover                                                        
udhcpc: sending discover                                                        
udhcpc: sending select for 10.0.0.126                                           
udhcpc: lease of 10.0.0.126 obtained, lease time 3600                           
deleting routers                                                                
root@buildroot:~# ifconfig                                                      
eth0      Link encap:Ethernet  HWaddr B2:14:10:DF:DB:B4                         
          inet addr:10.0.0.126  Bcast:10.255.255.255  Mask:255.0.0.0            
          inet6 addr: fe80::b014:10ff:fedf:dbb4/64 Scope:Link                   
          UP BROADCAST RUNNING MULTICAST  MTU:1500  Metric:1                    
          RX packets:17 errors:0 dropped:0 overruns:0 frame:0                   
          TX packets:38 errors:0 dropped:0 overruns:0 carrier:0                 
          collisions:0 txqueuelen:1000                                          
          RX bytes:3098 (3.0 KiB)  TX bytes:3832 (3.7 KiB)                      
          Interrupt:5                                                           
                                                                                
lo        Link encap:Local Loopback                                             
          inet addr:127.0.0.1  Mask:255.0.0.0                                   
          inet6 addr: ::1/128 Scope:Host                                        
          UP LOOPBACK RUNNING  MTU:65536  Metric:1                              
          RX packets:8 errors:0 dropped:0 overruns:0 frame:0                    
          TX packets:8 errors:0 dropped:0 overruns:0 carrier:0                  
          collisions:0 txqueuelen:1000                                          
          RX bytes:512 (512.0 B)  TX bytes:512 (512.0 B)                        
                                                                                
root@buildroot:~# ping 10.0.0.1                                                 
PING 10.0.0.1 (10.0.0.1): 56 data bytes                                         
64 bytes from 10.0.0.1: seq=0 ttl=64 time=1.203 ms                              
64 bytes from 10.0.0.1: seq=1 ttl=64 time=1.067 ms                              
64 bytes from 10.0.0.1: seq=2 ttl=64 time=1.030 ms                              
^C                                                                              
--- 10.0.0.1 ping statistics ---                                                
3 packets transmitted, 3 packets received, 0% packet loss                       
round-trip min/avg/max = 1.030/1.100/1.203 ms                                   
root@buildroot:~# 
```

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

### Audio

```sh
tftp -g -r sample3.mp3 10.0.0.1
mpg123 -m sample3.mp3 
```

### Video

```sh
tftp -g -r sample.mp4 10.0.0.1
ffmpeg -i sample.mp4 -pix_fmt rgb565le -f fbdev /dev/fb0
```

## Simulation

### Ethernet

```sh
make RISCV_BIN=/opt/riscv_xpacks/bin/riscv-none-embed- -C software/standalone/bootloader BSP=digilent/NexysA7SmpLinux SPINAL_SIM=yes clean all
make RISCV_BIN=/opt/riscv_xpacks/bin/riscv-none-embed- -C software/standalone/ethernet BSP=digilent/NexysA7SmpLinux SPINAL_SIM=yes clean all
sbt "runMain saxon.board.digilent.NexysA7SmpLinuxSystemSim"
```
