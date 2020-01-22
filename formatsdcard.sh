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

(
echo d
echo
echo d
echo
echo n
echo p
echo 1
echo
echo +100M
echo n
echo p
echo 2
echo
echo +500M
echo w
) | sudo fdisk /dev/${SDCARD}

sudo mkfs.ext2 -q /dev/${SDCARD}1
sudo mkfs.ext2 -q /dev/${SDCARD}2
