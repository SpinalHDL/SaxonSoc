VERILOG = ../../netlist/BlackiceMxSocMinimal.v

generate :
	(cd ../../..; sbt "runMain saxon.board.blackicemx.BlackiceMxSocMinimal")

../../netlist/BlackiceMxSocMinimal.v :
	(cd ../../..; sbt "runMain saxon.board.blackicemx.BlackiceMxSocMinimal")

../../netlist/BlackiceMxSocMinimal.v*.bin:

bin/toplevel.json : ${VERILOG} ../../netlist/BlackiceMxSocMinimal.v*.bin
	mkdir -p bin
	rm -f BlackiceMxSocMinimal.v*.bin
	cp ../../netlist/BlackiceMxSocMinimal.v*.bin . | true
	yosys -v3 -p "synth_ice40 -top BlackiceMxSocMinimal -json bin/toplevel.json" ${VERILOG}

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

clean :
	rm -rf bin
	rm -f BlackiceMxSocMinimal.v*.bin
