VERILOG = ../../../../netlist/Ulx3sSmp.v pll_linux.v

FPGA_SIZE ?= 12

SDRAM_SIZE ?= 32

FPGA_KS ?= $(FPGA_SIZE)k

ifeq ($(FPGA_SIZE), 12)
	CHIP_ID=0x21111043
	FPGA_KS = 25k
	NEXTPNR_FLAGS += "--timing-allow-fail"
endif
ifeq ($(FPGA_SIZE), 25)
	CHIP_ID=0x41111043
	NEXTPNR_FLAGS += "--timing-allow-fail"
endif
ifeq ($(FPGA_SIZE), 45)
	CHIP_ID=0x41112043
	NEXTPNR_FLAGS += "--timing-allow-fail"
endif
ifeq ($(FPGA_SIZE), 85)
	CHIP_ID=0x41113043
	NEXTPNR_FLAGS += "--timing-allow-fail"
endif

IDCODE ?= $(CHIP_ID)

compile : bin/toplevel.bit

prog: bin/toplevel.bit
	ujprog $<

bin/toplevel.json: ${VERILOG}
	mkdir -p bin
	rm -f Ulx3sSmp.v*.bin
	cp ../../../../netlist/Ulx3sSmp.v*.bin . | true
	yosys \
		-p "synth_ecp5 -json $@" \
		${VERILOG}

#  -noflatten -abc9

bin/toplevel.config: bin/toplevel.json
	nextpnr-ecp5 \
		--json $< \
		--textcfg $@ \
		--lpf ulx3s_v20_linux_uboot.lpf \
		--$(FPGA_KS) \
		--freq 50 \
		--seed 123 \
		--package CABGA381 ${NEXTPNR_FLAGS}

bin/toplevel.bit: bin/toplevel.config
	ecppack --compress --idcode $(IDCODE) $< $@

pll:
	ecppll \
                -i 25 \
                --clkout0 125 \
		--clkout1 100 \
		--clkout2 50 \
		--clkout3 25 \
		--phase1 270 \
                -n  pll_linux\
                -f pll_linux.v
clean:
	$(RM) -rf bin *.bin
