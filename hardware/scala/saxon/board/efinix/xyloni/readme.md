
## [Xyloni](https://github.com/Efinix-Inc/xyloni) ##

![xyloni](https://user-images.githubusercontent.com/26599790/172009205-4fb53bf2-fd0e-4e8a-a237-dc30ee2922d7.jpg)

### A Saxon SoC ver 0.3 implementation for Xyloni

### List of some killer features
- Dual Port RAM
- Fifo
- 16 bit External Pipeline Bus
- SPI bus writing to DAC from on chip RAM

### Other Takeaways
- SoC and user logic are segregated to generate two verilog output files. This eases developement effort
- Charles' code using the full power of Scala is formidable to a hardware engineer
- This implementation uses a flat hierarchy and hence hardware engineer friendly
- Creating an SoC will now be more easy and powerful
- The lack of a _Platform Designer_ like tool is no more to worry. __SaxonSoC__ is here to your rescue
- As you migrate to Efinix, make a quantum jump to __SpinalHdl__
- I have created a medical grade device using SpinalHdl. So what are you waiting for?

### How to start
- You should already be running Xyloni. Refer this [video turtorial](https://youtu.be/dIEofQYBnDA) and also the wonderful resources at Efinix on [Xyloni](https://github.com/Efinix-Inc/xyloni)
- [Install and run SpinalHdl](https://spinalhdl.github.io/SpinalDoc-RTD/master/SpinalHDL/Getting%20Started/getting_started.html)
- Build and generate the verilog hdl files
- Build the VexRiscV application as you did before for Opal SoC.
- Use the generated verilog and on chip RAM init files to get the bitstrean with Efinity and download using JTAG as you did for the Opal SoC
- 
_Here is a video of the walk through_


Ravi Ganesh





