VERILOG = ../../netlist/Ulx3sLinuxXip85.v pll_linux.v

IDCODE ?= 0x41113043 # 12f

compile : bin/toplevel.bit

prog: bin/toplevel.bit
	ujprog $<

generate:
	(cd ../../..; sbt "runMain saxon.board.ulx3s.Ulx3sLinuxXip85")

sim:
	(cd ../../..; sbt "runMain saxon.board.ulx3s.Ulx3sLinuxXip85SystemSim")

bin/toplevel.json: ${VERILOG}
	mkdir -p bin
	rm -f Ulx3sLinuxXip85.v*.bin
	cp ../../netlist/Ulx3sLinuxXip85.v*.bin . | true
	yosys \
		-p "synth_ecp5 -json $@" \
		${VERILOG}

bin/toplevel.config: bin/toplevel.json
	nextpnr-ecp5 \
		--json $< \
		--textcfg $@ \
		--lpf ulx3s_v20_linux_xip.lpf \
		--85k \
		--freq 50 \
		--package CABGA381 \
		--lpf-allow-unconstrained

bin/toplevel.bit: bin/toplevel.config
	ecppack --idcode $(IDCODE) $< $@

pll:
	ecppll \
                -i 25 \
                -o 50 \
		--clkout1 50 \
		--phase1 90 \
                -n  pll_linux\
                -f pll_linux.v
clean:
	$(RM) -rf bin *.bin
