## Description

This repo experiment multiple things at once :
- The BMB (Banana Memory Bus) which can cover both cached and cacheless SoC without compromises
- A hardware description paradigm made of generators and depedancies which should be able to solve SoC toplevel hell
- Linux and U-Boot on VexRiscv

A few kits are supported :
- ulx3s (ECP5) , documented in bsp/radiona/ulx3s/smp/README.md
- Arty-A7 (Artix 7), documented in bsp/digilent/ArtyA7SmpLinux/README.md
- [Efinix Xyloni](./hardware/scala/saxon/board/efinix/xyloni/readme.md)   ![xyloni_1](https://user-images.githubusercontent.com/26599790/172013963-737f0f52-707d-4ab9-9b78-31ec360a097c.jpg)

- ...


## Repository structure

```
- hardware
  - scala      : SpinalHDL hardware description
  - netlist    : Folder used by SpinalHDL to generate the netlist
  - synthesis  : Contains synthesis scripts for various boards
- bsp          : Contains multiple Board Support Package used to build the software
- software
  - standalone : Contains multiple demo software to run in the CPU
- ext
  - SpinalHDL  : Hardware description language compiler
  - VexRiscv   : CPU hardware description
```

## Dependencies

On Ubuntu 14 :

```sh
# JAVA JDK >= 8
sudo add-apt-repository -y ppa:openjdk-r/ppa
sudo apt-get update
sudo apt-get install openjdk-8-jdk -y
sudo update-alternatives --config java
sudo update-alternatives --config javac

# Install SBT - https://www.scala-sbt.org/
echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list
echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | sudo tee /etc/apt/sources.list.d/sbt_old.list
curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo apt-key add
sudo apt-get update
sudo apt-get install sbt

# Verilator (for simulation only, v3.9+, in general apt-get will give you 3.8)
sudo apt-get install git make autoconf g++ flex bison -y  # First time prerequisites
git clone http://git.veripool.org/git/verilator   # Only first time
unsetenv VERILATOR_ROOT  # For csh; ignore error if on bash
unset VERILATOR_ROOT  # For bash
cd verilator
git pull        # Make sure we're up-to-date
git checkout v4.040
autoconf        # Create ./configure script
./configure
make -j$(nproc)
sudo make install
echo "DONE"

# A fiew dependencies ?
sudo apt install pkg-config shtool libtool cpio bc unzip rsync mercurial
sudo apt install libusb-1.0-0-dev libyaml-dev
```

## Jar package

To package the project into a dependence free jar : 

```
sbt clean assembly
```

The produced jar will be in `target/scala-2.11/SaxonSoc-assembly-1.0.0.jar`

To run that jar : 

```
java -cp target/scala-2.11/SaxonSoc-assembly-1.0.0.jar YOUR_SCALA_PACKAGE.YOUR_MAIN
```

## BMB spec (WIP)

### Why BMB

The needs I had :

- A memory bus which could be used from for cacheless + low latency to cachefull SoC design without overhead
- Interconnect/Adapters which fit well in FPGA (without asyncronus ram reads)

Why not adopting a existing memory bus :

- AXI4 and Tilelink memory ordering has overhead for cacheless CPU designs
- AXI4 do not fit cacheless design as the AW W channels split add overhead to the interconnect
- TileLink isn't FPGA friendly, as its rely on tracking each transaction (unique source identifier)
- Nor AXI4, Tilelink, Wishbone, Avalon provide the features required for state-less adapters
- With the SaxonSoc out of order elaboration, there was a quite some room for experimentation and automation

### Key features

Feature which target the interconnect and adapters :

- Context signals which allow a master to retrieve information from the bus responses, and consequently allow state-less adapters
- State-less adapters allow unlimited number of pending transactions and avoid the usage of RAM/FIFO in adapters
- Address and write data are part of the same link, which allow to have low latency interconnect (in comparison to AXI)
- Allow out of oder completion via the 'source' signals

Feature to make slave implementation easier :

- Address alignment parameter (BYTE, WORD, POW2) to allow simple slave implementations
- Length width parameter, which combined with the alignement parameter, allow a slave to not support bursts (the interconnect will add the required adapters)

Other features :

- WriteOnly, readOnly support

### Signal

BMB is composed of streams to carry transaction between a source and a sink. A stream is composed of :

| Name    | Direction      | Description                                                                  |
| ------- | -------------- | ---------------------------------------------------------------------------- |
| valid   | Source => Sink | transaction present on the interface                                         |
| payload | Source => Sink | transaction content                                                          |
| ready   | Source <= Sink | consume the transaction on the bus, don't care if there is no transaction    |

More details on https://spinalhdl.github.io/SpinalDoc-RTD/SpinalHDL/Libraries/stream.html

BMB is composed of two mandatory streams :
- cmd : M->S, to carry requests, (read, write + data)
- rsp : M<-S, to carry responses (read + data, write)

and three optional streams to handle memory coherency : 
- inv  : M<-S, for the interconnect to ask a master to invalidate a portion of memory
- ack  : M->S, for the master to notify the interconnect that an invalidation is now effective
- sync : M<-S, for the interconnect to notify a master which issued a write that the given write is now observable by all other masters

The cmd stream is consquantly composed of the following signals

| Name    | Bitcount     | Description                                                                                         |
| ------- | ------------ | ------------                                                                                        |
| valid   | 1            | Stream valid                                                                                        |
| ready   | 1            | Stream ready                                                                                        |
| source  | sourceWidth  | Transaction source ID, allow out of order completion between different sources, similar to AXI ID   |
| opcode  | 1            | 0 => READ, 1 => WRITE                                                                               |
| address | addressWidth | Address of the first byte of the transaction, stay the same during a burst                          |
| length  | lengthWidth  | Burst bytes count - 1                                                                               |
| data    | dataWidth    | Data used for writes                                                                                |
| mask    | dataWidth/8  | Data mask used for writes                                                                           |
| context | contextWidth | Can be used by a master/adapter to link some informations to a burst (returned on rsp transactions) |

During a write burst the source, opcode, address, length and context signal should remain stable.

The rsp stream is :

| Name    | Bitcount     | Description                                |
| ------- | ------------ | ------------                               |
| valid   | 1            | Stream valid                               |
| ready   | 1            | Stream ready                               |
| source  | sourceWidth  | Identical to the corresponding cmd source  |
| opcode  | 1            | 0 => SUCCESS, 1 => ERROR                   |
| data    | dataWidth    | Data used for reads                        |
| context | contextWidth | Identical to the corresponding cmd context |

During a read burst the source and context signal should remain stable.

The inv stream is : 

| Name    | Bitcount     | Description                                |
| ------- | ------------ | ------------                               |
| valid   | 1            | Stream valid                               |
| ready   | 1            | Stream ready                               |
| all     | 1            | 0 => all masters, 1 => all masters but the source one should be invalidated. |
| address | addressWidth | Address of the first byte to invalidate    |
| length  | lengthWidth  | How many bytes should be invalidated - 1   |
| source  | sourceWidth  | See the all signal                         |

The ack stream has no payload attached :

| Name    | Bitcount     | Description                                |
| ------- | ------------ | ------------                               |
| valid   | 1            | Stream valid                               |
| ready   | 1            | Stream ready                               |

The sync stream is : 

| Name    | Bitcount     | Description                                |
| ------- | ------------ | ------------                               |
| valid   | 1            | Stream valid                               |
| ready   | 1            | Stream ready                               |
| source  | sourceWidth  | Identify which master should be notified   |


