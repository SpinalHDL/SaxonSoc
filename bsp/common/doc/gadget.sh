#!/bin/sh


export CONFIGFS_HOME="/root/usb"
export GADGET_BASE_DIR="${CONFIGFS_HOME}/usb_gadget/g1"
export DEV_ETH_ADDR="aa:bb:cc:dd:ee:f1"
export HOST_ETH_ADDR="aa:bb:cc:dd:ee:f2"
export USBDISK="/dev/sda1"

# Create directory structure
mkdir -p "${CONFIGFS_HOME}/usb_gadget"
mount none $CONFIGFS_HOME -t configfs
mkdir -p "${GADGET_BASE_DIR}"
cd "${GADGET_BASE_DIR}"
mkdir -p configs/c.1/strings/0x409
mkdir -p strings/0x409

# Serial device
###
mkdir functions/acm.usb0
ln -s functions/acm.usb0 configs/c.1/
###

# Ethernet device
###
mkdir functions/ecm.usb0
echo "${DEV_ETH_ADDR}" > functions/ecm.usb0/dev_addr
echo "${HOST_ETH_ADDR}" > functions/ecm.usb0/host_addr
ln -s functions/ecm.usb0 configs/c.1/
###

# Mass Storage device
###
#mkdir functions/mass_storage.usb0
#echo 1 > functions/mass_storage.usb0/stall
#echo 0 > functions/mass_storage.usb0/lun.0/cdrom
#echo 0 > functions/mass_storage.usb0/lun.0/ro
#echo 0 > functions/mass_storage.usb0/lun.0/nofua
#echo "${USBDISK}" > functions/mass_storage.usb0/lun.0/file
#ln -s functions/mass_storage.usb0 configs/c.1/
###

# Activate gadgets
echo 100b0000.udc > UDC
