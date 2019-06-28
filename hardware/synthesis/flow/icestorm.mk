TOPLEVEL ?=
VERILOGS ?=
BINARY ?=
PCF ?=
DEVICE ?=
PACKAGE ?=

bin/toplevel.json : ${VERILOG} ${BINARY}
	mkdir -p bin
	rm -f ${BINARY}
	cp ${BINARY} . | true
	yosys -v3 -p "synth_ice40 -top ${TOPLEVEL} -json bin/toplevel.json" ${VERILOG}

bin/toplevel.asc : ${PCF} bin/toplevel.json
	nextpnr-ice40 --${DEVICE} --package ${PACKAGE} --json bin/toplevel.json --pcf ${PCF} --asc bin/toplevel.asc --opt-timing --placer heap

bin/toplevel.bin : bin/toplevel.asc
	icepack bin/toplevel.asc bin/toplevel.bin

compile : bin/toplevel.bin

time: bin/toplevel.bin
	icetime -tmd ${DEVICE} bin/toplevel.asc

prog : bin/toplevel.bin
	iceprog bin/toplevel.bin

clean :
	rm -rf bin
	rm -f ${BINARY}

.SECONDARY:
.PHONY: all prog clean