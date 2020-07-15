variable script_file
set script_file "vivado_script"
variable project_base
set project_base [file join vivado_project]
set project_name fpga
variable project_file
set project_file [file join ${project_base} ${project_name}.xpr]
variable program_file
set program_file [file join ${project_base} ${project_name}.runs impl_1 ArtyA7Linux.bit]
variable flash_file
set flash_file latest.mcs
variable sbi_file
set sbi_file "../../../../software/standalone/machineModeSbi/build/machineModeSbi.bin"
variable boot_file
set boot_file "../../../../../u-boot/u-boot.bin"
variable fpga_part xc7a35t

proc help {} {
	variable script_file
	puts "Syntax:"
	puts "$script_file -tclargs \"--synthesize\""
	puts "$script_file -tclargs \"--implement\""
	puts "$script_file -tclargs \"--bitstream\""
	puts "$script_file -tclargs \"--rebuild\""
	puts "$script_file -tclargs \"--program\""
	puts "$script_file -tclargs \"--cfgmem\""
	puts "$script_file -tclargs \"--flash\""
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
    variable fpga_part
    puts [pwd]
	open_hw
	connect_hw_server
	open_hw_target
	current_hw_device [get_hw_devices ${fpga_part}_0]
	set_property PROGRAM.FILE $program_file [get_hw_devices ${fpga_part}_0]
	program_hw_devices [get_hw_devices ${fpga_part}_0]
	close_hw_target
	disconnect_hw_server
	close_hw
}

proc cfgmem {} {
	variable program_file
	variable flash_file
	variable sbi_file
	variable boot_file
	write_cfgmem -format mcs -size 16 -interface SPIx4 -loadbit "up 0x00000000 $program_file" -loaddata "up 0x00400000 $sbi_file up 0x00410000 $boot_file" -force -file $flash_file
}

proc flash {} {
	variable flash_file
    variable fpga_part
	open_hw
	connect_hw_server
	open_hw_target
	current_hw_device [get_hw_devices ${fpga_part}_0]
	create_hw_cfgmem -hw_device [lindex [get_hw_devices ${fpga_part}_0] 0] [lindex [get_cfgmem_parts {s25fl128sxxxxxx0-spi-x1_x2_x4}] 0]
	set_property PROGRAM.ADDRESS_RANGE  {use_file} [ get_property PROGRAM.HW_CFGMEM [lindex [get_hw_devices ${fpga_part}_0] 0]]
	set_property PROGRAM.FILES [list $flash_file] [ get_property PROGRAM.HW_CFGMEM [lindex [get_hw_devices ${fpga_part}_0] 0]]
	set_property PROGRAM.PRM_FILE {} [ get_property PROGRAM.HW_CFGMEM [lindex [get_hw_devices ${fpga_part}_0] 0]]
	set_property PROGRAM.UNUSED_PIN_TERMINATION {pull-none} [ get_property PROGRAM.HW_CFGMEM [lindex [get_hw_devices ${fpga_part}_0] 0]]
	set_property PROGRAM.BLANK_CHECK  0 [ get_property PROGRAM.HW_CFGMEM [lindex [get_hw_devices ${fpga_part}_0] 0]]
	set_property PROGRAM.ERASE  1 [ get_property PROGRAM.HW_CFGMEM [lindex [get_hw_devices ${fpga_part}_0] 0]]
	set_property PROGRAM.CFG_PROGRAM  1 [ get_property PROGRAM.HW_CFGMEM [lindex [get_hw_devices ${fpga_part}_0] 0]]
	set_property PROGRAM.VERIFY  1 [ get_property PROGRAM.HW_CFGMEM [lindex [get_hw_devices ${fpga_part}_0] 0]]
	set_property PROGRAM.CHECKSUM  0 [ get_property PROGRAM.HW_CFGMEM [lindex [get_hw_devices ${fpga_part}_0] 0]]
	create_hw_bitstream -hw_device [lindex [get_hw_devices ${fpga_part}_0] 0] [get_property PROGRAM.HW_CFGMEM_BITFILE [ lindex [get_hw_devices ${fpga_part}_0] 0]]
	program_hw_devices [lindex [get_hw_devices ${fpga_part}_0] 0]
	refresh_hw_device [lindex [get_hw_devices ${fpga_part}_0] 0]
	program_hw_cfgmem -hw_cfgmem [ get_property PROGRAM.HW_CFGMEM [lindex [get_hw_devices ${fpga_part}_0] 0]]
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
			"--rebuild"    {synthesize; implement; bitstream; return 0}
			"--program"    {program; return 0}
			"--cfgmem"     {cfgmem; return 0}
			"--flash"      {cfgmem; flash; program; return 0}
			default        {help}
		}
	}
} else {
	help
}
