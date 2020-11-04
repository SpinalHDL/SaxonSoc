
## How to create a buildroot patch 

diff -ruN dropbear-2019.78_ori dropbear-2019.78 > 0000-vexriscv-aes.patch
mkdir -p SaxonSoc/bsp/common/fixes/buildroot/dropbear/vexriscv_aes/rsync/buildroot/package/dropbear
cp 0000-vexriscv-aes.patch SaxonSoc/bsp/common/fixes/buildroot/dropbear/vexriscv_aes/rsync/buildroot/package/dropbear