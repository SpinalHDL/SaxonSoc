TOPLEVEL = SaxonUp5kEvn
ROOT = ../../..
PIN_DEF = ../mapping.pcf
VERILOGS = $(ROOT)/hardware/netlist/${TOPLEVEL}.v
DEVICE = up5k

ifeq (, $(shell which nextpnr-ice40))
NEXTPNR ?= no
else
NEXTPNR ?= yes
endif



all: $(TOPLEVEL).rpt $(TOPLEVEL)*.bin

ifeq ($(NEXTPNR),yes)
%.json: ${VERILOGS}
	rm -f ${TOPLEVEL}.v*.bin
	cp -f  ${ROOT}/hardware/netlist/${TOPLEVEL}.v*.bin . | true
	yosys -p 'synth_ice40 -top $(TOPLEVEL) -json $@' $<  

%.asc: $(PIN_DEF) %.json constraint.py
	nextpnr-ice40 --$(DEVICE) --json $(TOPLEVEL).json --pcf $(PIN_DEF) --asc $(TOPLEVEL).asc --pre-pack constraint.py $(NEXTPNR_ARG) 
else
%.blif: ${VERILOGS}
	rm -f ${TOPLEVEL}.v*.bin
	cp -f  ${ROOT}/hardware/netlist/${TOPLEVEL}.v*.bin . | true
	yosys -p 'synth_ice40 -top ${TOPLEVEL} -blif $@' $<

%.asc: $(PIN_DEF) %.blif
	arachne-pnr -d $(subst up,,$(subst hx,,$(subst lp,,$(DEVICE)))) -o $@ -p $^
endif

%.bin: %.asc
	icepack $< $@

%.rpt: %.asc
	icetime -d $(DEVICE) -mtr $@ $<

%_tb: %_tb.v %.v
	iverilog -o $@ $^

%_tb.vcd: %_tb
	vvp -N $< +vcd=$@

%_syn.v: %.blif
	yosys -p 'read_blif -wideports $^; write_verilog $@'

%_syntb: %_tb.v %_syn.v
	iverilog -o $@ $^ `yosys-config --datdir/ice40/cells_sim.v`

%_syntb.vcd: %_syntb
	vvp -N $< +vcd=$@

time: ${TOPLEVEL}.rpt
	cat ${TOPLEVEL}.rpt


prog: $(TOPLEVEL).bin
	iceprog $<

sudo-prog: $(TOPLEVEL).bin
	@echo 'Executing prog as root!!!'
	sudo iceprog $<

clean:
	rm -f $(TOPLEVEL).blif $(TOPLEVEL).asc $(TOPLEVEL).rpt $(TOPLEVEL).bin $(TOPLEVEL).json

.SECONDARY:
.PHONY: all prog clean
