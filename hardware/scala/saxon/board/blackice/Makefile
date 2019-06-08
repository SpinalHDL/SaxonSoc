VERILOG = ../../../../netlist/BlackiceSocArduino.v blackice_pll.v

generate :
	(cd ../../../../..; sbt "runMain saxon.board.blackice.BlackiceSocArduino")
	cp ../../../../../soc.h ../../../../../software/bsp/BlackiceArduino/include

../../../../netlist/BlackiceSocArduino.v :
	(cd ../../../../..; sbt "runMain saxon.board.blackice.BlackiceSocArduino")
	cp ../../../../../soc.h ../../../../../software/bsp/BlackiceArduino/include

../../../../netlist/BlackiceSocArduino.v*.bin:

bin/toplevel.json : ${VERILOG} ../../../../netlist/BlackiceSocArduino.v*.bin
	mkdir -p bin
	rm -f BlackiceSocArduino.v*.bin
	cp ../../../../netlist/BlackiceSocArduino.v*.bin . | true
	yosys -v3 -p "synth_ice40 -top BlackiceSocArduino -json bin/toplevel.json" ${VERILOG}

bin/toplevel.asc : blackice.pcf bin/toplevel.json
	nextpnr-ice40 --freq 25 --hx8k --package tq144:4k --json bin/toplevel.json --pcf blackice.pcf --asc bin/toplevel.asc --opt-timing --placer heap

bin/toplevel.bin : bin/toplevel.asc
	icepack bin/toplevel.asc bin/toplevel.bin

compile : bin/toplevel.bin

time: bin/toplevel.bin
	icetime -tmd hx8k bin/toplevel.asc

prog : bin/toplevel.bin
	stty -F /dev/ttyACM0 raw
	cat bin/toplevel.bin >/dev/ttyACM0

blackice_pll.v   :
	icepll -i 100 -o 25 -f blackice_pll.v -m -n blackice_pll

clean :
	rm -rf bin
	rm -f BlackiceSocArduino.v*.bin
