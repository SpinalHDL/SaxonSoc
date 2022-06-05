VERILOG = ../../../../netlist/Ulx3sMinimal.v

compile : bin/toplevel.bit

prog: bin/toplevel.bit
	fujprog $<

bin/toplevel.json: ${VERILOG}
	mkdir -p bin
	rm -f Ulx3sMinimal.v*.bin
	cp ../../../../netlist/Ulx3sMinimal.v*.bin . | true
	yosys \
		-p "synth_ecp5 -json $@" \
		$<

bin/toplevel.config: bin/toplevel.json
	nextpnr-ecp5 \
		--json $< \
		--textcfg $@ \
		--lpf ulx3s_v20_led.lpf \
		--12k \
		--package CABGA381

bin/toplevel.bit: bin/toplevel.config
	ecppack $< $@

clean:
	$(RM) -rf bin *.bin
