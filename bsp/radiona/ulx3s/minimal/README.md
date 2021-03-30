
### Clone and build

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

