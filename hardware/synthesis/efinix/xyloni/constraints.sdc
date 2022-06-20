

set_time_format -unit ns -decimal_places 3


create_clock -name {Clk33MHz}   -period 30.003 -waveform { 0.000 15.002 } [get_ports {Clk33MHz}]
create_clock -name {IfClk} 	-period 20.833 -waveform { 0.000 10.416 } [get_ports {IfClk}]


create_clock -name {ClkCore}     -period 50.1   -waveform { 0.000 25.05 }   [get_ports {ClkCore}]
create_clock -name {Clk80MHz}      -period 12.5   -waveform { 0.000 6.25 }   [get_ports {Clk80MHz}]



set_clock_groups -exclusive -group {IfClk} -group {ClkCore Clk80MHz }


set_false_path -from Clk80MHz -to [get_ports Dac|Spi_sftReg*]