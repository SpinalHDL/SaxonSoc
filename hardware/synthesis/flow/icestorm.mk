TOPLEVEL ?=
VERILOGS ?=
BINARY ?=
PCF ?=
DEVICE ?=
PACKAGE ?=
CONSTRAINT ?=

ifneq ($(PCF),)
	PCF_CMD ?= --pcf ${PCF}
endif


bin/toplevel.json : ${VERILOG} ${BINARY}
	mkdir -p bin
	rm -f *.bin
	cp ${BINARY} . | true
	yosys -v3 -p "synth_ice40 -top ${TOPLEVEL} -json bin/toplevel.json" ${VERILOG}

bin/toplevel.asc : ${PCF} bin/toplevel.json ${CONSTRAINT}
	nextpnr-ice40 --${DEVICE} --package ${PACKAGE} --json bin/toplevel.json ${PCF_CMD} --asc bin/toplevel.asc --pre-pack ${CONSTRAINT} --opt-timing --placer heap

bin/toplevel.bin : bin/toplevel.asc
	icepack -s bin/toplevel.asc bin/toplevel.bin

compile : bin/toplevel.bin

time: bin/toplevel.bin
	icetime -tmd ${DEVICE} bin/toplevel.asc

prog : bin/toplevel.bin
	iceprog bin/toplevel.bin

clean :
	rm -rf bin
	rm -f *.bin

.SECONDARY:
.PHONY: all prog clean
