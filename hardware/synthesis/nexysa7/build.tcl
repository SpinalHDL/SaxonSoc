variable script_file
set script_file "build.tcl"
variable project_file
set project_file "NexysA7Linux.xpr"
variable program_file
set program_file "NexysA7Linux.runs/impl_1/NexysA7Linux.bit"

proc help {} {
	variable script_file
	puts "Syntax:"
	puts "$script_file -tclargs \"--synthesize\""
	puts "$script_file -tclargs \"--implement\""
	puts "$script_file -tclargs \"--bitstream\""
	puts "$script_file -tclargs \"--rebuild\""
	puts "$script_file -tclargs \"--program\""
	exit 0
}

proc synthesize {} {
	variable project_file
	open_project $project_file
	reset_run synth_1
	launch_runs synth_1 -force -jobs 8
	wait_on_run synth_1
	close_project
}

proc implement {} {
	variable project_file
	open_project $project_file
	reset_run impl_1
	launch_runs impl_1 -jobs 8
	wait_on_run impl_1
	close_project
}

proc bitstream {} {
	variable project_file
	open_project $project_file
	launch_runs impl_1 -to_step write_bitstream -jobs 8
	wait_on_run impl_1
	close_project
}

proc program {} {
	variable program_file
	open_hw
	connect_hw_server
	open_hw_target [lindex [get_hw_targets] 0]
	set_property PROGRAM.FILE $program_file [current_hw_device]
	program_hw_device [current_hw_device]
	close_hw_target
	disconnect_hw_server
	close_hw
}

if {$::argc > 0} {
	for {set i 0} {$i < [llength $::argc]} {incr i} {
		set option [string trim [lindex $::argv $i]]
		switch -regexp -- $option {
			"--synthesize" {synthesize; return 0}
			"--implement"  {implement; return 0}
			"--bitstream"  {bitstream; return 0}
			"--program"    {program; return 0}
			"--rebuild"    {synthesize; implement; bitstream; return 0}
			default        {help}
		}
	}
} else {
	help
}
