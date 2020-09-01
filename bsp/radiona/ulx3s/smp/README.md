


```sh
export SDCARD=???
export SDCARD_P1=???
export SDCARD_p2=???

(
echo o
echo n
echo p
echo 1
echo
echo +100M
echo y
echo n
echo p
echo 2
echo
echo +200M
echo y
echo p
echo t
echo 1
echo b
echo w
) | fdisk $SDCARD


sudo mkfs.vfat $SDCARD_P1
sudo mke2fs $SDCARD_P2

cd $SAXON_ROOT
mkdir -p sdcard
sudo mount $SDCARD_P1 sdcard
sudo cp buildroot/output/images/dtb sdcard/dtb
sudo cp buildroot/output/images/rootfs.cpio.uboot sdcard/rootfs.cpio.uboot
sudo cp buildroot/output/images/uImage sdcard/uImage
sudo umount sdcard
rm -r sdcard

cd $SAXON_ROOT
mkdir -p sdcard
sudo mount $SDCARD_P2 sdcard
sudo tar xf buildroot/output/images/rootfs.tar -C sdcard
sudo umount sdcard
rm -r sdcard
```
