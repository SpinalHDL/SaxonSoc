#!/bin/sh

SIZELIMIT=$1
SDCARD=$2

SIZE=`sudo blockdev --getsize64 /dev/${SDCARD}`

if [ ${SIZE} -gt ${SIZELIMIT} ]
then
    echo Target device larger than the limit, are you sure it is not your hard disk ?!
    echo Device ${SDCARD} size:
    echo ${SIZE} bytes
    echo Limit:
    echo ${SIZELIMIT} bytes
    echo Get over this check by defining SIZELIMIT
    exit 1
fi

BUILDROOT_PATH=../buildroot

mkdir -p sdcard
sudo mount /dev/${SDCARD}1 sdcard
sudo cp -f ${BUILDROOT_PATH}/output/images/uImage sdcard/uImage
sudo cp -f ${BUILDROOT_PATH}/output/images/dtb sdcard/dtb
sudo umount sdcard

sudo mount /dev/${SDCARD}2 sdcard
sudo tar xf ${BUILDROOT_PATH}/output/images/rootfs.tar -C sdcard
sudo umount sdcard
