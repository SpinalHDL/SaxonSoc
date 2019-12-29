## Description

This repo experiment multiple things at once :
- The BMB (Banana Memory Bus) which can cover both cached and cacheless SoC without compromises
- A hardware description paradigm made of generators and depedancies which should be able to solve SoC toplevel hell
- Linux and U-Boot on VexRiscv

A few kits are supported :
- ulx3s (ECP5)
- Arty-A7 (Artix 7), documented in bsp/Arty7Linux/README.md


## BMB spec (WIP)

### Why BMB

The needs I had :

- A memory bus which could be used from for cacheless + low latency to cachefull SoC design without overhead
- Interconnect/Adapters which fit well in FPGA (without asyncronus ram reads)

Why not adopting a existing memory bus :

- AXI4 and Tilelink memory ordering has overhead for cacheless CPU designs
- AXI4 do not fit cacheless design as the AW W channels split add overhead to the interconnect
- TileLink isn't FPGA friendly, as its rely on tracking each transaction (unique source identifier)
- Nor AXI4, Tilelink, Wishbone, Avalon provide the features required for state-less adapters
- With the SaxonSoc out of order elaboration, there was a quite some room for experimentation and automation

### Key features

Feature which target the interconnect and adapters :

- Context signals which allow a master to retrieve information from the bus responses, and consequently allow state-less adapters
- State-less adapters allow unlimited number of pending transactions and avoid the usage of RAM/FIFO in adapters
- Address and write data are part of the same link, which allow to have low latency interconnect (in comparison to AXI)
- Allow out of oder completion via the 'source' signals

Feature to make slave implementation easier :

- Address alignment parameter (BYTE, WORD, POW2) to allow simple slave implementations
- Length width parameter, which combined with the alignement parameter, allow a slave to not support bursts (the interconnect will add the required adapters)

Other features :

- WriteOnly, readOnly support

### Parameters and signal

BMB can has the following parameters :

| Name         | Type     | Description                                                 |
| ------------ | -------- | ------------                                                |
| addressWidth | Bitcount | Addresses are always in byte                                |
| dataWidth    | Bitcount | Should be multiple of 8                                     |
| lengthWidth  | Bitcount | Number of byte of a burst = length                          |
| sourceWidth  | Bitcount | Used for out of order completion                            |
| contextWidth | Bitcount | Used by masters/adapters to link informations to bursts     |
| alignment    | Enum     | Smallest alignement used by the master (BYTE, WORD, POW2)   |
| canRead      | Boolean  | Allow reads                                                 |
| canWrite     | Boolean  | Allow writes                                                |

BMB is composed of streams to carry transaction between a source and a sink. A stream is composed of :

| Name    | Direction      | Description                                                                  |
| ------- | -------------- | ---------------------------------------------------------------------------- |
| valid   | Source => Sink | transaction present on the interface                                         |
| payload | Source => Sink | transaction content                                                          |
| ready   | Source <= Sink | consume the transaction on the bus, don't care if there is no transaction    |

More details on https://spinalhdl.github.io/SpinalDoc-RTD/SpinalHDL/Libraries/stream.html

BMB is composed of two streams :
- cmd : to carry requests, (read, write + data)
- rsp : to carry responses (read + data, write)

The cmd stream is consquantly composed of the following signals

| Name    | Bitcount     | Description                                                                                         |
| ------- | ------------ | ------------                                                                                        |
| valid   | 1            | Stream valid                                                                                        |
| ready   | 1            | Stream ready                                                                                        |
| source  | sourceWidth  | Transaction source ID, allow out of order completion between different sources, similar to AXI ID   |
| opcode  | 1            | 0 => READ, 1 => WRITE                                                                               |
| address | addressWidth | Address of the first byte of the transaction, stay the same during a burst                          |
| length  | lengthWidth  | Burst bytes count                                                                                   |
| data    | dataWidth    | Data used for writes                                                                                |
| mask    | dataWidth/8  | Data mask used for writes                                                                           |
| context | contextWidth | Can be used by a master/adapter to link some informations to a burst (returned on rsp transactions) |

During a write burst the source, opcode, address, length and context signal should remain stable.

And the rsp stream is :

| Name    | Bitcount     | Description                                |
| ------- | ------------ | ------------                               |
| valid   | 1            | Stream valid                               |
| ready   | 1            | Stream ready                               |
| source  | sourceWidth  | Identical to the corresponding cmd source  |
| opcode  | 1            | 0 => SUCCESS, 1 => ERROR                   |
| data    | dataWidth    | Data used for reads                        |
| context | contextWidth | Identical to the corresponding cmd context |

During a read burst the source and context signal should remain stable.

## Various commands

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


```
git clone https://github.com/SpinalHDL/buildroot.git -b saxon buildroot
git clone https://github.com/SpinalHDL/linux.git -b vexriscv --depth 100 linux
cd buildroot
make spinal_saxon_default_defconfig
make linux-rebuild all -j$(nproc)
output/host/bin/riscv32-linux-objcopy  -O binary output/images/vmlinux output/images/Image
dtc -O dtb -o output/images/dtb board/spinal/saxon_default/spinal_saxon_default_de1_soc.dts
```

//clean all target files
rm -rf output/target
find output/ -name ".stamp_target_installed" |xargs rm -rf


riscv64-unknown-elf-objdump -S -d output/images/vmlinux > output/images/vmlinux.asm
make linux-rebuild all -j$(nproc)

printf "\x0f\x01" > /dev/spidev0.0
echo 3 > /proc/sys/kernel/printk
dd if=/dev/zero of=speed bs=1M count=1 conv=fsync

src/openocd -f tcl/interface/ftdi/ft2232h_breakout.cfg -c 'set BRIEY_CPU0_YAML ../SaxonSoc.git/cpu0.yaml' -f tcl/target/saxon.cfg
cu -l /dev/ttyUSB -s 115200
picocom -b 115200 /dev/ttyUSB --imap lfcrlf

GPIO => https://www.emcraft.com/stm32f429discovery/controlling-gpio-from-linux-user-space

Warning, if you want to run eclipse against a simulated target, you would need to add some delay after the reset

monitor reset halt
monitor sleep 1000



## GPIO
export PIN=511
echo $PIN > /sys/class/gpio/export
echo in > /sys/class/gpio/gpio$PIN/direction
echo both > /sys/class/gpio/gpio${PIN}/edge
hello $PIN

## Ethernet
ifconfig eth0 up
ifconfig eth0 0.0.0.0 0.0.0.0 && dhclient
ifconfig eth0:0 192.168.1.6 up
ifconfig eth0 192.168.1.1

/sbin/udhcpc -i eth0

nano /etc/network/interfaces
auto eth0
iface eth0 inet dhcp
ifdown eth0; ifup eth0


auto eth1
iface eth1 inet dhcp
ifdown eth1; ifup eth1

ftp://speedtest.tele2.net/
rr
wget --output-document=/dev/null ftp://speedtest.tele2.net/5MB.zip


dmesg | grep eth
ifconfig -a


make hello-rebuild


you can run 'scripts/get_maintainer.pl <your-patch.diff>', or 'scripts/get_maintainer.pl -f drivers/gpio/gpio-sifive.c' to find out
<jn__> the most important mailing lists in the case should be linux-gpio@vger.kernel.org and linux-riscv@lists.infradead.org



nano /etc/wilc_wpa_supplicant

ctrl_interface=/var/run/wpa_supplicant
update_config=1
country=US

network={
    ssid="Rawrrr"
    key_mgmt=NONE
}

wpa_supplicant -i wlan0 -D n180211 -c /etc/wilc_wpa_supplicant



**** connect to
nano  /etc/network/interfaces

auto wlan0
iface wlan0 inet dhcp

ifconfig wlan0 up

iwlist wlan0 scan

iwconfig wlan0 essid miaou-P65-P67SE key s:prout

### Connect to a open wifi
echo 4 > /proc/sys/kernel/printk
nano /etc/wpa_supplicant.conf
network={
    ssid="miaou-P65-P67SE"
    key_mgmt=NONE
    priority=100
}

wpa_supplicant -B -i wlan0 -c /etc/wpa_supplicant.conf -D nl80211
udhcpc -i wlan0

#### connect to close wifi
https://www.linuxbabe.com/command-line/ubuntu-server-16-04-wifi-wpa-supplicant

echo 4 > /proc/sys/kernel/printk
ifconfig wlan0 up
iwlist wlan0 scan | grep ESSID
wpa_passphrase yourESSID password | tee /etc/wpa_supplicant.conf
wpa_supplicant -c /etc/wpa_supplicant.conf -i wlan0 &
udhcpc -i wlan0




**** AP mode
nano /etc/wilc_hostapd_open.conf

interface=wlan0
driver=nl80211
ctrl_interface=/var/run/hostapd
ssid=wilc1000_SoftAP
dtim_period=2
beacon_int=100
channel=7
hw_mode=g
max_num_sta=8
ap_max_inactivity=300

hostapd /etc/wilc_hostapd_open.conf -B &

