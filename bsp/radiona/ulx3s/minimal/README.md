## Dependencies

```sh
# Java JDK 8 (higher is ok)
sudo add-apt-repository -y ppa:openjdk-r/ppa
sudo apt-get update
sudo apt-get install openjdk-8-jdk -y
sudo update-alternatives --config java
sudo update-alternatives --config javac

# SBT (Scala build tool)
echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
sudo apt-get update
sudo apt-get install sbt

# RISC-V toolchain
VERSION=8.3.0-1.2
mkdir -p ~/opt
cd ~/opt
wget https://github.com/xpack-dev-tools/riscv-none-embed-gcc-xpack/releases/download/v$VERSION/xpack-riscv-none-embed-gcc-$VERSION-linux-x64.tar.gz
tar -xvf xpack-riscv-none-embed-gcc-$VERSION-linux-x64.tar.gz
rm xpack-riscv-none-embed-gcc-$VERSION-linux-x64.tar.gz
mv xpack-riscv-none-embed-gcc-$VERSION xpack-riscv-none-embed-gcc
echo 'export PATH=~/opt/xpack-riscv-none-embed-gcc/bin:$PATH' >> ~/.bashrc
export PATH=~/opt/xpack-riscv-none-embed-gcc/bin:$PATH
```

## Clone and build

```sh
# Getting this repository into a directory $SAXON_ROOT
cd $SAXON_ROOT
git clone https://github.com/SpinalHDL/SaxonSoc.git -b dev-0.3 --recursive SaxonSoc

# Compile the software
cd $SAXON_ROOT/SaxonSoc/software/standalone/blinkAndEcho
make BSP_PATH=../../../bsp/radiona/ulx3s/minimal

# Generate the netlist
cd $SAXON_ROOT/SaxonSoc
sbt "runMain saxon.board.radiona.ulx3s.Ulx3sMinimal"

# Build and upload the bitstream
cd $SAXON_ROOT/SaxonSoc/hardware/synthesis/radiona/ulx3s/minimal
make prog
```

You should then see the led blinking.

To echo characters you can run a serial terminal emulator such as `screen /dev/ttyUSB0 115200`.
