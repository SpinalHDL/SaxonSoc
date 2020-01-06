## Description

This repo experiment multiple things at once :
- The BMB (Banana Memory Bus) which can cover both cached and cacheless SoC without compromises
- A hardware description paradigm made of generators and depedancies which should be able to solve SoC toplevel hell
- Linux and U-Boot on VexRiscv

A few kits are supported :
- ulx3s (ECP5) , documented in bsp/Ulx3sLinuxUboot/README.md
- Arty-A7 (Artix 7), documented in bsp/Arty7Linux/README.md
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
echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
sudo apt-get update
sudo apt-get install sbt

# Verilator (for simulation only, v3.9+, in general apt-get will give you 3.8)
sudo apt-get install git make autoconf g++ flex bison
git clone http://git.veripool.org/git/verilator   # Only first time
unsetenv VERILATOR_ROOT  # For csh; ignore error if on bash
unset VERILATOR_ROOT  # For bash
cd verilator
git pull        # Make sure we're up-to-date
git checkout verilator_3_918
autoconf        # Create ./configure script
./configure
make
sudo make install
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

### Parameters and signal

BMB can has the following parameters :

| Name         | Type     | Description                                                 |
| ------------ | -------- | ------------                                                |
| addressWidth | Bitcount | Addresses are always in byte                                |
| dataWidth    | Bitcount | Should be multiple of 8                                     |
| lengthWidth  | Bitcount | Number of byte of a burst = length                          |
| sourceWidth  | Bitcount | Used for out of order completion                            |
| contextWidth | Bitcount | Used by masters/adapters to link informations to bursts     |
| alignment    | Enum     | Smallest alignement used by the master (BYTE, WORD, POW2)   |
| canRead      | Boolean  | Allow reads                                                 |
| canWrite     | Boolean  | Allow writes                                                |

BMB is composed of streams to carry transaction between a source and a sink. A stream is composed of :

| Name    | Direction      | Description                                                                  |
| ------- | -------------- | ---------------------------------------------------------------------------- |
| valid   | Source => Sink | transaction present on the interface                                         |
| payload | Source => Sink | transaction content                                                          |
| ready   | Source <= Sink | consume the transaction on the bus, don't care if there is no transaction    |

More details on https://spinalhdl.github.io/SpinalDoc-RTD/SpinalHDL/Libraries/stream.html

BMB is composed of two streams :
- cmd : to carry requests, (read, write + data)
- rsp : to carry responses (read + data, write)

The cmd stream is consquantly composed of the following signals

| Name    | Bitcount     | Description                                                                                         |
| ------- | ------------ | ------------                                                                                        |
| valid   | 1            | Stream valid                                                                                        |
| ready   | 1            | Stream ready                                                                                        |
| source  | sourceWidth  | Transaction source ID, allow out of order completion between different sources, similar to AXI ID   |
| opcode  | 1            | 0 => READ, 1 => WRITE                                                                               |
| address | addressWidth | Address of the first byte of the transaction, stay the same during a burst                          |
| length  | lengthWidth  | Burst bytes count                                                                                   |
| data    | dataWidth    | Data used for writes                                                                                |
| mask    | dataWidth/8  | Data mask used for writes                                                                           |
| context | contextWidth | Can be used by a master/adapter to link some informations to a burst (returned on rsp transactions) |

During a write burst the source, opcode, address, length and context signal should remain stable.

And the rsp stream is :

| Name    | Bitcount     | Description                                |
| ------- | ------------ | ------------                               |
| valid   | 1            | Stream valid                               |
| ready   | 1            | Stream ready                               |
| source  | sourceWidth  | Identical to the corresponding cmd source  |
| opcode  | 1            | 0 => SUCCESS, 1 => ERROR                   |
| data    | dataWidth    | Data used for reads                        |
| context | contextWidth | Identical to the corresponding cmd context |

During a read burst the source and context signal should remain stable.

