# Boot Linux from u-boot

## Kernel and Rootfs

 - build buildroot images (see patches section below):
```sh
git clone https://github.com/SpinalHDL/buildroot.git -b saxon buildroot
git clone https://github.com/SpinalHDL/linux.git -b vexriscv --depth 1 linux

cd buildroot
make spinal_saxon_default_defconfig
make linux-rebuild all -j$(nproc)
output/host/bin/riscv32-linux-objcopy -O binary output/images/vmlinux output/images/Image
mkimage -A riscv -O linux -T kernel -a 0x80400000 -e 0x80400000 -n Linux -C none -d output/images/Image output/images/uImage
mkimage -A riscv -T ramdisk -n "ramdisk" -C none -d output/images/rootfs.cpio output/images/rootfs.cpio.uboot
dtc -O dtb -o output/images/dtb board/spinal/saxon_default/spinal_saxon_ulx3s.dts
cd -
```

## U-boot

 - build u-boot image:
```sh
git clone https://github.com/roman3017/u-boot.git -b saxon
cd u-boot/
CROSS_COMPILE=/opt/riscv/bin/riscv64-unknown-elf- make saxon_defconfig
CROSS_COMPILE=/opt/riscv/bin/riscv64-unknown-elf- make
cd -
```

## SBI

 - build SBI image:
```sh
cd SaxonSoc/software/standalone/
RISCV_BIN=/opt/riscv/bin/riscv64-unknown-elf- CFLAGS="-DOS_CALL=0x80200000 -DDTB=0x80FF0000" make BSP=Ulx3sLinux clean all
cd -
```

## Simulation

 - run simulation:
```sh
cd SaxonSoc/ext/SpinalHDL/
sbt clean compile
cd -

cd SaxonSoc/
sbt "runMain saxon.board.ulx3s.Ulx3sUbootSystemSim"
cd -
```

## Tasks

 - [x] booting Linux from SDRAM with u-boot in SDRAM
   - [x] booting u-boot in SDRAM
 - [ ] booting Linux from SDRAM with u-boot-spl in onchip RAM
   - [x] booting Linux from SDRAM with u-boot-spl in SDRAM
 - [ ] booting Linux from SD with u-boot-spl in onchip RAM
   - [ ] booting Linux from SD with u-boot in SDRAM

## Patches

```diff
diff --git a/board/spinal/saxon_default/linux.config b/board/spinal/saxon_default/linux.config
index ae8da96692..45d4c42f61 100644
--- a/board/spinal/saxon_default/linux.config
+++ b/board/spinal/saxon_default/linux.config
@@ -57,35 +57,6 @@ CONFIG_LEDS_TRIGGERS=y
 CONFIG_LEDS_TRIGGER_HEARTBEAT=y
 CONFIG_LEDS_TRIGGER_GPIO=y
 
-# NET
-CONFIG_NET=y
-CONFIG_INET=y
-CONFIG_NETDEVICES=y
-
-CONFIG_PACKET=y
-CONFIG_PACKET_DIAG=y
-CONFIG_NET_PACKET_ENGINE=y
-
-CONFIG_NET_VENDOR_MICROCHIP=y
-CONFIG_ENC28J60=y
-
-CONFIG_NET_VENDOR_WIZNET=y
-CONFIG_WIZNET_BUS_ANY=y
-CONFIG_WIZNET_W5100=y
-CONFIG_WIZNET_W5100_SPI=y
-
-# WIFI
-CONFIG_STAGING=y
-CONFIG_CFG80211=y
-CONFIG_NL80211_TESTMODE=y
-CONFIG_CFG80211_DEVELOPER_WARNINGS=y
-CONFIG_CFG80211_WEXT=y
-CONFIG_MAC80211=y
-##CONFIG_WILC1000=y
-##CONFIG_WILC1000_SPI=y
-CONFIG_WLAN_VENDOR_MCHP=y
-CONFIG_WILC=y
-CONFIG_WILC_SPI=y
 CONFIG_PM=y
 CONFIG_HOSTAP=y
 CONFIG_HOSTAP_FIRMWARE=y
diff --git a/board/spinal/saxon_default/spinal_saxon_ulx3s.dts b/board/spinal/saxon_default/spinal_saxon_ulx3s.dts
new file mode 100644
index 0000000000..ebc917d5a6
--- /dev/null
+++ b/board/spinal/saxon_default/spinal_saxon_ulx3s.dts
@@ -0,0 +1,94 @@
+
+
+/dts-v1/;
+//include/ "dt-bindings/interrupt-controller/irq.h"
+//include/ "dt-bindings/gpio/gpio.h"
+
+
+
+/ {
+    #address-cells = <1>;
+    #size-cells = <1>;
+    compatible = "spinal,vexriscv";
+    model = "spinal,vexriscv_sim";
+
+    chosen {
+        bootargs = "rootwait console=hvc0 root=/dev/ram0 init=/sbin/init swiotlb=32"; // loglevel=7
+        linux,initrd-start = <0x80800000>;
+        linux,initrd-end =   <0x81000000>;
+    };
+
+
+    cpus {
+        #address-cells = <1>;
+        #size-cells = <0>;
+        timebase-frequency = <50000000>;
+            cpu@0 {
+                device_type = "cpu";
+                compatible = "riscv";
+                riscv,isa = "rv32im";
+                mmu-type = "riscv,sv32";
+                reg = <0>;
+                status = "okay";
+                L1: interrupt-controller {
+                    #interrupt-cells = <1>;
+                    interrupt-controller;
+                    compatible = "riscv,cpu-intc";
+                };
+            };
+        };
+
+    memory@80000000 {
+        device_type = "memory";
+        reg = <0x80000000 0x02000000>;
+    };
+
+    apbA@10000000 {
+        compatible = "simple-bus";
+        #address-cells = <1>;
+        #size-cells = <1>;
+        ranges = <0x00000000 0x10000000 0x01000000>;
+
+        plic: interrupt-controller@c00000 {
+            compatible = "sifive,plic-1.0.0", "sifive,fu540-c000-plic";
+            #interrupt-cells = <1>;
+            interrupt-controller;
+            interrupts-extended = <&L1 11 &L1 9>;
+            reg = <0x00c00000 0x00400000>;
+            riscv,ndev = <32>;
+        };
+
+        gpioA: gpio@0 {
+               compatible = "spinal-lib,gpio-1.0";
+               interrupt-parent = <&plic>;
+               ngpio = <24>;
+               interrupts = <4 5 6 7>;
+               reg = <0x00000000 0x00001000>;
+               gpio-controller;
+               #gpio-cells = <2>;
+               interrupt-controller;
+               #interrupt-cells = <2>;
+        };
+
+
+        spiA: spi@20000 {
+            compatible = "spinal-lib,spi-1.0";
+            #address-cells = <1>;
+            #size-cells = <0>;
+            reg = <0x00020000 0x00001000>;
+            cmd_fifo_depth = <256>;
+            rsp_fifo_depth = <256>;
+            cs-gpios = <&gpioA 8 0>;
+            status = "disabled";
+
+            mmc-slot@0 {
+                compatible = "mmc-spi-slot";
+                reg = <0>;
+                voltage-ranges = <3300 3300>;
+                spi-max-frequency = <50000000>;
+            };
+        };
+    };
+};
+
```
