set  _ENDIAN little
set _TAP_TYPE 1234

if { [info exists CPUTAPID] } {
  set _CPUTAPID $CPUTAPID
} else {
  # set useful default
  set _CPUTAPID 0x10001fff
}

adapter_khz 10000
adapter_nsrst_delay 260
ftdi_tdo_sample_edge falling
  jtag_ntrst_delay 250

set _CHIPNAME fpga_spinal
jtag newtap $_CHIPNAME bridge -expected-id $_CPUTAPID -irlen 4 -ircapture 0x1 -irmask 0xF

target create $_CHIPNAME.cpu0 vexriscv -endian $_ENDIAN -chain-position $_CHIPNAME.bridge -coreid 0 -dbgbase 0xF00F0000
vexriscv readWaitCycles 30
vexriscv cpuConfigFile $CPU0_YAML

poll_period 50


init
#echo "Halting processor"
soft_reset_halt
sleep 1000


load_image software/standalone/sdramInit/build/sdramInit.bin 0x20000000
reg pc 0x20000000
resume
sleep 200
halt

set linuxPath ../buildroot/output/images/
load_image software/standalone/machineModeSbi/build/machineModeSbi.bin 0x80800000
load_image ../u-boot/u-boot.bin 0x81F00000
load_image ${linuxPath}dtb 0x81D00000
load_image ${linuxPath}uImage 0x80000000


load_image software/standalone/machineModeSbi/build/machineModeSbi.bin 0x80A00000
load_image ../u-boot/u-boot.bin 0x80B00000


#save mmc 0 0x81D00000 /boot/dtb 2670
#save mmc 0 0x80B00000 /boot/u-boot.new 0x70000
#flash_erase /dev/mtd2 0 0
#cat /boot/u-boot.new > /dev/mtd2

sleep 20
reg pc 0x80800000
sleep 20
resume
#exit
#bootm 0x80000000 - 0x81D00000