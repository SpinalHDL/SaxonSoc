create_clock -period 40.000000 -waveform { 0.000000 20.000000 } -name ICE_CLK [ get_ports { ICE_CLK } ]
 create_clock -period 1000.000 -name SaxonUp5kEvn|IOB_18A [ get_nets IOB_18A_c ]
create_clock -period 1000.000000 -name clk501 [get_nets IOB_18A_c]
