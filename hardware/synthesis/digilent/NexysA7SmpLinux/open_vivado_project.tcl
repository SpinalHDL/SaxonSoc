source [file join [file dirname [file normalize [info script]]] vivado_params.tcl]

open_project -read_only $outputdir/$project_name
start_gui
