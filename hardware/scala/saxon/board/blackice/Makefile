VERILOG = ../../../../netlist/BlackiceSocMinimal.v blackice_pll.v

generate :
	(cd ../../../../..; sbt "runMain saxon.board.blackice.BlackiceSocMinimal")

../../../../netlist/BlackiceSocMinimal.v :
	(cd ../../../../..; sbt "runMain saxon.board.blackice.BlackiceSocMinimal")

../../../../netlist/BlackiceSocMinimal.v*.bin:

bin/toplevel.json : ${VERILOG} ../../../../netlist/BlackiceSocMinimal.v*.bin
	mkdir -p bin
	rm -f BlackiceSocMinimal.v*.bin
	cp ../../../../netlist/BlackiceSocMinimal.v*.bin . | true
	yosys -v3 -p "synth_ice40 -top BlackiceSocMinimal -json bin/toplevel.json" ${VERILOG}

bin/toplevel.asc : blackice.pcf bin/toplevel.json
	nextpnr-ice40 --freq 25 --hx8k --package tq144:4k --json bin/toplevel.json --pcf blackice.pcf --asc bin/toplevel.asc --opt-timing --placer heap

bin/toplevel.bin : bin/toplevel.asc
	icepack bin/toplevel.asc bin/toplevel.bin

compile : bin/toplevel.bin

time: bin/toplevel.bin
	icetime -tmd hx8k bin/toplevel.asc

prog : bin/toplevel.bin
	cat bin/toplevel.bin >/dev/ttyACM0

clean :
	rm -rf bin
	rm -f BlackiceSocMinimal.v*.bin
