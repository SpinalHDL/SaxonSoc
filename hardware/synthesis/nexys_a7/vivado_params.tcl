set base ..
set flash_files [list latest.mcs]
set flash_file latest.mcs
set flash_part s25fl128sxxxxxx0-spi-x1_x2_x4
set fpga_part xc7a100t
set boot_file ../../../../../u-boot/u-boot.bin
set outputdir ./vivado_project
set part xc7a100ticsg324-1L
set project_base [file join vivado_project]
set project_name fpga
set top NexysA7SmpLinux
set project_file [file join ${project_base} ${project_name}.xpr]
set program_file [file join ${project_base} ${project_name}.runs impl_1 ${top}.bit]
set sbi_file ../../../../../opensbi/build/platform/spinal/saxon/digilent/artyA7Smp/firmware/fw_jump.bin
set script_file vivado_script
set topv ${base}/../../netlist/${top}.v
