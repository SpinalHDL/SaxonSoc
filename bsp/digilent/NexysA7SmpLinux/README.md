# Please read ArtyA7SmpLinux README.md

## Hardware

- Arty A7 100T
- USB micro cable
- Optional Ethernet cable
- Optional SDCARD

## Implemented peripherals

* Ethernet RMII with linux driver
* SPI, which provide
  * FPGA SPI flash access in Linux
  * SDCARD in linux
  * User usage SPI
* VGA in linux
* Audio (mono) in Linux
* GPIO access in linux

## Sourcing the build script
```sh
source SaxonSoc/bsp/digilent/NexysA7SmpLinux/source.sh
```
