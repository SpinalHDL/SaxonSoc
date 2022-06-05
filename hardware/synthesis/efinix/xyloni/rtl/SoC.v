// Generator : SpinalHDL v1.7.1-SNAPSHOT    git head : 59a0a5d6579e085ed208304cb1f4cd4ba71c4ae7
// Component : SoC
// Git hash  : cbacbd095e046d7ecaedb1055d4bbd5b56e398bc

`timescale 1ns/1ps

module SoC (
  input               debugCdCtrl_external_clk,
  input               debugCdCtrl_external_reset,
  input               system_cpu_jtag_tms,
  input               system_cpu_jtag_tdi,
  output              system_cpu_jtag_tdo,
  input               system_cpu_jtag_tck,
  output              system_uartA_uart_txd,
  input               system_uartA_uart_rxd,
   inout     [7:0]    system_gpioA_gpio
);

  wire                system_cpu_logic_cpu_iBus_rsp_payload_error;
  wire       [7:0]    system_cpu_logic_cpu_debug_bus_cmd_payload_address;
  wire                system_cpu_logic_cpu_dBus_rsp_ready;
  wire                system_cpu_logic_cpu_dBus_rsp_error;
  wire       [14:0]   system_ramA_ctrl_arbiter_io_inputs_0_cmd_payload_fragment_address;
  wire       [14:0]   system_ramA_ctrl_arbiter_io_inputs_1_cmd_payload_fragment_address;
  wire                bufferCC_5_io_dataOut;
  wire                bufferCC_6_io_dataOut;
  wire                system_cpu_logic_cpu_iBus_cmd_valid;
  wire       [31:0]   system_cpu_logic_cpu_iBus_cmd_payload_pc;
  wire                system_cpu_logic_cpu_debug_bus_cmd_ready;
  wire       [31:0]   system_cpu_logic_cpu_debug_bus_rsp_data;
  wire                system_cpu_logic_cpu_debug_resetOut;
  wire                system_cpu_logic_cpu_dBus_cmd_valid;
  wire                system_cpu_logic_cpu_dBus_cmd_payload_wr;
  wire       [31:0]   system_cpu_logic_cpu_dBus_cmd_payload_address;
  wire       [31:0]   system_cpu_logic_cpu_dBus_cmd_payload_data;
  wire       [1:0]    system_cpu_logic_cpu_dBus_cmd_payload_size;
  wire                jtagBridge_1_io_jtag_tdo;
  wire                jtagBridge_1_io_remote_cmd_valid;
  wire                jtagBridge_1_io_remote_cmd_payload_last;
  wire       [0:0]    jtagBridge_1_io_remote_cmd_payload_fragment;
  wire                jtagBridge_1_io_remote_rsp_ready;
  wire                systemDebugger_1_io_remote_cmd_ready;
  wire                systemDebugger_1_io_remote_rsp_valid;
  wire                systemDebugger_1_io_remote_rsp_payload_error;
  wire       [31:0]   systemDebugger_1_io_remote_rsp_payload_data;
  wire                systemDebugger_1_io_mem_cmd_valid;
  wire       [31:0]   systemDebugger_1_io_mem_cmd_payload_address;
  wire       [31:0]   systemDebugger_1_io_mem_cmd_payload_data;
  wire                systemDebugger_1_io_mem_cmd_payload_wr;
  wire       [1:0]    systemDebugger_1_io_mem_cmd_payload_size;
  wire                bufferCC_7_io_dataOut;
  wire                system_cpu_iBus_decoder_io_input_cmd_ready;
  wire                system_cpu_iBus_decoder_io_input_rsp_valid;
  wire                system_cpu_iBus_decoder_io_input_rsp_payload_last;
  wire       [0:0]    system_cpu_iBus_decoder_io_input_rsp_payload_fragment_opcode;
  wire       [31:0]   system_cpu_iBus_decoder_io_input_rsp_payload_fragment_data;
  wire                system_cpu_iBus_decoder_io_outputs_0_cmd_valid;
  wire                system_cpu_iBus_decoder_io_outputs_0_cmd_payload_last;
  wire       [0:0]    system_cpu_iBus_decoder_io_outputs_0_cmd_payload_fragment_opcode;
  wire       [31:0]   system_cpu_iBus_decoder_io_outputs_0_cmd_payload_fragment_address;
  wire       [1:0]    system_cpu_iBus_decoder_io_outputs_0_cmd_payload_fragment_length;
  wire                system_cpu_iBus_decoder_io_outputs_0_rsp_ready;
  wire                system_cpu_dBus_decoder_io_input_cmd_ready;
  wire                system_cpu_dBus_decoder_io_input_rsp_valid;
  wire                system_cpu_dBus_decoder_io_input_rsp_payload_last;
  wire       [0:0]    system_cpu_dBus_decoder_io_input_rsp_payload_fragment_opcode;
  wire       [31:0]   system_cpu_dBus_decoder_io_input_rsp_payload_fragment_data;
  wire       [0:0]    system_cpu_dBus_decoder_io_input_rsp_payload_fragment_context;
  wire                system_cpu_dBus_decoder_io_outputs_0_cmd_valid;
  wire                system_cpu_dBus_decoder_io_outputs_0_cmd_payload_last;
  wire       [0:0]    system_cpu_dBus_decoder_io_outputs_0_cmd_payload_fragment_opcode;
  wire       [31:0]   system_cpu_dBus_decoder_io_outputs_0_cmd_payload_fragment_address;
  wire       [1:0]    system_cpu_dBus_decoder_io_outputs_0_cmd_payload_fragment_length;
  wire       [31:0]   system_cpu_dBus_decoder_io_outputs_0_cmd_payload_fragment_data;
  wire       [3:0]    system_cpu_dBus_decoder_io_outputs_0_cmd_payload_fragment_mask;
  wire       [0:0]    system_cpu_dBus_decoder_io_outputs_0_cmd_payload_fragment_context;
  wire                system_cpu_dBus_decoder_io_outputs_0_rsp_ready;
  wire                system_cpu_dBus_decoder_io_outputs_1_cmd_valid;
  wire                system_cpu_dBus_decoder_io_outputs_1_cmd_payload_last;
  wire       [0:0]    system_cpu_dBus_decoder_io_outputs_1_cmd_payload_fragment_opcode;
  wire       [31:0]   system_cpu_dBus_decoder_io_outputs_1_cmd_payload_fragment_address;
  wire       [1:0]    system_cpu_dBus_decoder_io_outputs_1_cmd_payload_fragment_length;
  wire       [31:0]   system_cpu_dBus_decoder_io_outputs_1_cmd_payload_fragment_data;
  wire       [3:0]    system_cpu_dBus_decoder_io_outputs_1_cmd_payload_fragment_mask;
  wire       [0:0]    system_cpu_dBus_decoder_io_outputs_1_cmd_payload_fragment_context;
  wire                system_cpu_dBus_decoder_io_outputs_1_rsp_ready;
  wire                system_ramA_logic_io_bus_cmd_ready;
  wire                system_ramA_logic_io_bus_rsp_valid;
  wire                system_ramA_logic_io_bus_rsp_payload_last;
  wire       [0:0]    system_ramA_logic_io_bus_rsp_payload_fragment_source;
  wire       [0:0]    system_ramA_logic_io_bus_rsp_payload_fragment_opcode;
  wire       [31:0]   system_ramA_logic_io_bus_rsp_payload_fragment_data;
  wire       [0:0]    system_ramA_logic_io_bus_rsp_payload_fragment_context;
  wire                system_ramA_ctrl_arbiter_io_inputs_0_cmd_ready;
  wire                system_ramA_ctrl_arbiter_io_inputs_0_rsp_valid;
  wire                system_ramA_ctrl_arbiter_io_inputs_0_rsp_payload_last;
  wire       [0:0]    system_ramA_ctrl_arbiter_io_inputs_0_rsp_payload_fragment_opcode;
  wire       [31:0]   system_ramA_ctrl_arbiter_io_inputs_0_rsp_payload_fragment_data;
  wire       [0:0]    system_ramA_ctrl_arbiter_io_inputs_0_rsp_payload_fragment_context;
  wire                system_ramA_ctrl_arbiter_io_inputs_1_cmd_ready;
  wire                system_ramA_ctrl_arbiter_io_inputs_1_rsp_valid;
  wire                system_ramA_ctrl_arbiter_io_inputs_1_rsp_payload_last;
  wire       [0:0]    system_ramA_ctrl_arbiter_io_inputs_1_rsp_payload_fragment_opcode;
  wire       [31:0]   system_ramA_ctrl_arbiter_io_inputs_1_rsp_payload_fragment_data;
  wire                system_ramA_ctrl_arbiter_io_output_cmd_valid;
  wire                system_ramA_ctrl_arbiter_io_output_cmd_payload_last;
  wire       [0:0]    system_ramA_ctrl_arbiter_io_output_cmd_payload_fragment_source;
  wire       [0:0]    system_ramA_ctrl_arbiter_io_output_cmd_payload_fragment_opcode;
  wire       [14:0]   system_ramA_ctrl_arbiter_io_output_cmd_payload_fragment_address;
  wire       [1:0]    system_ramA_ctrl_arbiter_io_output_cmd_payload_fragment_length;
  wire       [31:0]   system_ramA_ctrl_arbiter_io_output_cmd_payload_fragment_data;
  wire       [3:0]    system_ramA_ctrl_arbiter_io_output_cmd_payload_fragment_mask;
  wire       [0:0]    system_ramA_ctrl_arbiter_io_output_cmd_payload_fragment_context;
  wire                system_ramA_ctrl_arbiter_io_output_rsp_ready;
  wire                system_periphBridge_bmb_decoder_io_input_cmd_ready;
  wire                system_periphBridge_bmb_decoder_io_input_rsp_valid;
  wire                system_periphBridge_bmb_decoder_io_input_rsp_payload_last;
  wire       [0:0]    system_periphBridge_bmb_decoder_io_input_rsp_payload_fragment_opcode;
  wire       [31:0]   system_periphBridge_bmb_decoder_io_input_rsp_payload_fragment_data;
  wire       [0:0]    system_periphBridge_bmb_decoder_io_input_rsp_payload_fragment_context;
  wire                system_periphBridge_bmb_decoder_io_outputs_0_cmd_valid;
  wire                system_periphBridge_bmb_decoder_io_outputs_0_cmd_payload_last;
  wire       [0:0]    system_periphBridge_bmb_decoder_io_outputs_0_cmd_payload_fragment_opcode;
  wire       [23:0]   system_periphBridge_bmb_decoder_io_outputs_0_cmd_payload_fragment_address;
  wire       [1:0]    system_periphBridge_bmb_decoder_io_outputs_0_cmd_payload_fragment_length;
  wire       [31:0]   system_periphBridge_bmb_decoder_io_outputs_0_cmd_payload_fragment_data;
  wire       [3:0]    system_periphBridge_bmb_decoder_io_outputs_0_cmd_payload_fragment_mask;
  wire       [0:0]    system_periphBridge_bmb_decoder_io_outputs_0_cmd_payload_fragment_context;
  wire                system_periphBridge_bmb_decoder_io_outputs_0_rsp_ready;
  wire                system_periphBridge_bmb_decoder_io_outputs_1_cmd_valid;
  wire                system_periphBridge_bmb_decoder_io_outputs_1_cmd_payload_last;
  wire       [0:0]    system_periphBridge_bmb_decoder_io_outputs_1_cmd_payload_fragment_opcode;
  wire       [23:0]   system_periphBridge_bmb_decoder_io_outputs_1_cmd_payload_fragment_address;
  wire       [1:0]    system_periphBridge_bmb_decoder_io_outputs_1_cmd_payload_fragment_length;
  wire       [31:0]   system_periphBridge_bmb_decoder_io_outputs_1_cmd_payload_fragment_data;
  wire       [3:0]    system_periphBridge_bmb_decoder_io_outputs_1_cmd_payload_fragment_mask;
  wire       [0:0]    system_periphBridge_bmb_decoder_io_outputs_1_cmd_payload_fragment_context;
  wire                system_periphBridge_bmb_decoder_io_outputs_1_rsp_ready;
  wire                system_periphBridge_bmb_decoder_io_outputs_2_cmd_valid;
  wire                system_periphBridge_bmb_decoder_io_outputs_2_cmd_payload_last;
  wire       [0:0]    system_periphBridge_bmb_decoder_io_outputs_2_cmd_payload_fragment_opcode;
  wire       [23:0]   system_periphBridge_bmb_decoder_io_outputs_2_cmd_payload_fragment_address;
  wire       [1:0]    system_periphBridge_bmb_decoder_io_outputs_2_cmd_payload_fragment_length;
  wire       [31:0]   system_periphBridge_bmb_decoder_io_outputs_2_cmd_payload_fragment_data;
  wire       [3:0]    system_periphBridge_bmb_decoder_io_outputs_2_cmd_payload_fragment_mask;
  wire       [0:0]    system_periphBridge_bmb_decoder_io_outputs_2_cmd_payload_fragment_context;
  wire                system_periphBridge_bmb_decoder_io_outputs_2_rsp_ready;
  wire                system_clint_logic_io_bus_cmd_ready;
  wire                system_clint_logic_io_bus_rsp_valid;
  wire                system_clint_logic_io_bus_rsp_payload_last;
  wire       [0:0]    system_clint_logic_io_bus_rsp_payload_fragment_opcode;
  wire       [31:0]   system_clint_logic_io_bus_rsp_payload_fragment_data;
  wire       [0:0]    system_clint_logic_io_bus_rsp_payload_fragment_context;
  wire       [63:0]   system_clint_logic_io_time;
  wire       [7:0]    system_gpioA_logic_io_gpio_write;
  wire       [7:0]    system_gpioA_logic_io_gpio_writeEnable;
  wire                system_gpioA_logic_io_bus_cmd_ready;
  wire                system_gpioA_logic_io_bus_rsp_valid;
  wire                system_gpioA_logic_io_bus_rsp_payload_last;
  wire       [0:0]    system_gpioA_logic_io_bus_rsp_payload_fragment_opcode;
  wire       [31:0]   system_gpioA_logic_io_bus_rsp_payload_fragment_data;
  wire       [0:0]    system_gpioA_logic_io_bus_rsp_payload_fragment_context;
  wire       [7:0]    system_gpioA_logic_io_interrupt;
  wire                system_uartA_logic_io_bus_cmd_ready;
  wire                system_uartA_logic_io_bus_rsp_valid;
  wire                system_uartA_logic_io_bus_rsp_payload_last;
  wire       [0:0]    system_uartA_logic_io_bus_rsp_payload_fragment_opcode;
  wire       [31:0]   system_uartA_logic_io_bus_rsp_payload_fragment_data;
  wire       [0:0]    system_uartA_logic_io_bus_rsp_payload_fragment_context;
  wire                system_uartA_logic_io_uart_txd;
  wire                system_uartA_logic_io_interrupt;
  reg                 _zz_system_gpioA_gpio;
  reg                 _zz_system_gpioA_gpio_1;
  reg                 _zz_system_gpioA_gpio_2;
  reg                 _zz_system_gpioA_gpio_3;
  reg                 _zz_system_gpioA_gpio_4;
  reg                 _zz_system_gpioA_gpio_5;
  reg                 _zz_system_gpioA_gpio_6;
  reg                 _zz_system_gpioA_gpio_7;
  reg                 debugCdCtrl_logic_inputResetTrigger;
  reg                 debugCdCtrl_logic_outputResetUnbuffered;
  reg        [11:0]   debugCdCtrl_logic_holdingLogic_resetCounter;
  wire                when_ClockDomainGenerator_l77;
  reg                 debugCdCtrl_logic_outputReset;
  wire                debugCdCtrl_logic_inputResetAdapter_stuff_syncTrigger;
  reg                 systemCdCtrl_logic_inputResetTrigger;
  reg                 systemCdCtrl_logic_outputResetUnbuffered;
  reg        [5:0]    systemCdCtrl_logic_holdingLogic_resetCounter;
  wire                when_ClockDomainGenerator_l77_1;
  reg                 systemCdCtrl_logic_outputReset;
  wire                system_cpu_iBus_cmd_valid;
  wire                system_cpu_iBus_cmd_ready;
  wire                system_cpu_iBus_cmd_payload_last;
  wire       [0:0]    system_cpu_iBus_cmd_payload_fragment_opcode;
  wire       [31:0]   system_cpu_iBus_cmd_payload_fragment_address;
  wire       [1:0]    system_cpu_iBus_cmd_payload_fragment_length;
  wire                system_cpu_iBus_rsp_valid;
  wire                system_cpu_iBus_rsp_ready;
  wire                system_cpu_iBus_rsp_payload_last;
  wire       [0:0]    system_cpu_iBus_rsp_payload_fragment_opcode;
  wire       [31:0]   system_cpu_iBus_rsp_payload_fragment_data;
  wire                system_cpu_dBus_cmd_valid;
  reg                 system_cpu_dBus_cmd_ready;
  wire                system_cpu_dBus_cmd_payload_last;
  wire       [0:0]    system_cpu_dBus_cmd_payload_fragment_opcode;
  wire       [31:0]   system_cpu_dBus_cmd_payload_fragment_address;
  wire       [1:0]    system_cpu_dBus_cmd_payload_fragment_length;
  wire       [31:0]   system_cpu_dBus_cmd_payload_fragment_data;
  wire       [3:0]    system_cpu_dBus_cmd_payload_fragment_mask;
  wire       [0:0]    system_cpu_dBus_cmd_payload_fragment_context;
  wire                system_cpu_dBus_rsp_valid;
  wire                system_cpu_dBus_rsp_ready;
  wire                system_cpu_dBus_rsp_payload_last;
  wire       [0:0]    system_cpu_dBus_rsp_payload_fragment_opcode;
  wire       [31:0]   system_cpu_dBus_rsp_payload_fragment_data;
  wire       [0:0]    system_cpu_dBus_rsp_payload_fragment_context;
  reg        [1:0]    _zz_system_cpu_dBus_cmd_payload_fragment_length;
  reg        [3:0]    _zz_system_cpu_dBus_cmd_payload_fragment_mask;
  reg                 system_cpu_debugReset;
  wire                system_cpu_logic_cpu_debug_bus_cmd_fire;
  reg                 system_cpu_logic_cpu_debug_bus_cmd_fire_regNext;
  wire                system_cpu_dBus_cmd_m2sPipe_valid;
  wire                system_cpu_dBus_cmd_m2sPipe_ready;
  wire                system_cpu_dBus_cmd_m2sPipe_payload_last;
  wire       [0:0]    system_cpu_dBus_cmd_m2sPipe_payload_fragment_opcode;
  wire       [31:0]   system_cpu_dBus_cmd_m2sPipe_payload_fragment_address;
  wire       [1:0]    system_cpu_dBus_cmd_m2sPipe_payload_fragment_length;
  wire       [31:0]   system_cpu_dBus_cmd_m2sPipe_payload_fragment_data;
  wire       [3:0]    system_cpu_dBus_cmd_m2sPipe_payload_fragment_mask;
  wire       [0:0]    system_cpu_dBus_cmd_m2sPipe_payload_fragment_context;
  reg                 system_cpu_dBus_cmd_rValid;
  reg                 system_cpu_dBus_cmd_rData_last;
  reg        [0:0]    system_cpu_dBus_cmd_rData_fragment_opcode;
  reg        [31:0]   system_cpu_dBus_cmd_rData_fragment_address;
  reg        [1:0]    system_cpu_dBus_cmd_rData_fragment_length;
  reg        [31:0]   system_cpu_dBus_cmd_rData_fragment_data;
  reg        [3:0]    system_cpu_dBus_cmd_rData_fragment_mask;
  reg        [0:0]    system_cpu_dBus_cmd_rData_fragment_context;
  wire                when_Stream_l368;
  wire                system_periphBridge_bmb_cmd_valid;
  wire                system_periphBridge_bmb_cmd_ready;
  wire                system_periphBridge_bmb_cmd_payload_last;
  wire       [0:0]    system_periphBridge_bmb_cmd_payload_fragment_opcode;
  wire       [23:0]   system_periphBridge_bmb_cmd_payload_fragment_address;
  wire       [1:0]    system_periphBridge_bmb_cmd_payload_fragment_length;
  wire       [31:0]   system_periphBridge_bmb_cmd_payload_fragment_data;
  wire       [3:0]    system_periphBridge_bmb_cmd_payload_fragment_mask;
  wire       [0:0]    system_periphBridge_bmb_cmd_payload_fragment_context;
  wire                system_periphBridge_bmb_rsp_valid;
  wire                system_periphBridge_bmb_rsp_ready;
  wire                system_periphBridge_bmb_rsp_payload_last;
  wire       [0:0]    system_periphBridge_bmb_rsp_payload_fragment_opcode;
  wire       [31:0]   system_periphBridge_bmb_rsp_payload_fragment_data;
  wire       [0:0]    system_periphBridge_bmb_rsp_payload_fragment_context;
  wire                system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_valid;
  wire                system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_ready;
  wire                system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_last;
  wire       [0:0]    system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_opcode;
  wire       [23:0]   system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_address;
  wire       [1:0]    system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_length;
  wire       [31:0]   system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_data;
  wire       [3:0]    system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_mask;
  wire       [0:0]    system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_context;
  wire                system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_rsp_valid;
  wire                system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_rsp_ready;
  wire                system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_last;
  wire       [0:0]    system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_opcode;
  wire       [31:0]   system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_data;
  wire       [0:0]    system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_context;
  wire                _zz_io_input_rsp_ready;
  wire                system_periphBridge_bmb_cmd_halfPipe_valid;
  wire                system_periphBridge_bmb_cmd_halfPipe_ready;
  wire                system_periphBridge_bmb_cmd_halfPipe_payload_last;
  wire       [0:0]    system_periphBridge_bmb_cmd_halfPipe_payload_fragment_opcode;
  wire       [23:0]   system_periphBridge_bmb_cmd_halfPipe_payload_fragment_address;
  wire       [1:0]    system_periphBridge_bmb_cmd_halfPipe_payload_fragment_length;
  wire       [31:0]   system_periphBridge_bmb_cmd_halfPipe_payload_fragment_data;
  wire       [3:0]    system_periphBridge_bmb_cmd_halfPipe_payload_fragment_mask;
  wire       [0:0]    system_periphBridge_bmb_cmd_halfPipe_payload_fragment_context;
  reg                 system_periphBridge_bmb_cmd_rValid;
  wire                system_periphBridge_bmb_cmd_halfPipe_fire;
  reg                 system_periphBridge_bmb_cmd_rData_last;
  reg        [0:0]    system_periphBridge_bmb_cmd_rData_fragment_opcode;
  reg        [23:0]   system_periphBridge_bmb_cmd_rData_fragment_address;
  reg        [1:0]    system_periphBridge_bmb_cmd_rData_fragment_length;
  reg        [31:0]   system_periphBridge_bmb_cmd_rData_fragment_data;
  reg        [3:0]    system_periphBridge_bmb_cmd_rData_fragment_mask;
  reg        [0:0]    system_periphBridge_bmb_cmd_rData_fragment_context;
  wire                _zz_system_periphBridge_bmb_rsp_valid;
  reg                 _zz_system_periphBridge_bmb_rsp_valid_1;
  reg                 _zz_system_periphBridge_bmb_rsp_payload_last;
  reg        [0:0]    _zz_system_periphBridge_bmb_rsp_payload_fragment_opcode;
  reg        [31:0]   _zz_system_periphBridge_bmb_rsp_payload_fragment_data;
  reg        [0:0]    _zz_system_periphBridge_bmb_rsp_payload_fragment_context;
  wire       [7:0]    _zz_io_gpio_read;
  wire       [7:0]    _zz_system_gpioA_gpio_8;
  wire       [7:0]    _zz_when_InOutWrapper_l65;
  wire                system_gpioA_interrupts_0;
  wire                system_gpioA_interrupts_1;
  wire                system_gpioA_interrupts_2;
  wire                system_gpioA_interrupts_3;
  wire                system_gpioA_interrupts_4;
  wire                system_gpioA_interrupts_5;
  wire                system_gpioA_interrupts_6;
  wire                system_gpioA_interrupts_7;
  wire                system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_valid;
  wire                system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_ready;
  wire                system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_last;
  wire       [0:0]    system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_opcode;
  wire       [15:0]   system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_address;
  wire       [1:0]    system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_length;
  wire       [31:0]   system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_data;
  wire       [0:0]    system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_context;
  wire                system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_valid;
  wire                system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_ready;
  wire                system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_last;
  wire       [0:0]    system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_opcode;
  wire       [31:0]   system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_data;
  wire       [0:0]    system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_context;
  wire                system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_valid;
  wire                system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_ready;
  wire                system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_last;
  wire       [0:0]    system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_opcode;
  wire       [7:0]    system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_address;
  wire       [1:0]    system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_length;
  wire       [31:0]   system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_data;
  wire       [0:0]    system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_context;
  wire                system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_valid;
  wire                system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_ready;
  wire                system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_last;
  wire       [0:0]    system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_opcode;
  wire       [31:0]   system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_data;
  wire       [0:0]    system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_context;
  wire                system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_valid;
  wire                system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_ready;
  wire                system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_last;
  wire       [0:0]    system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_opcode;
  wire       [5:0]    system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_address;
  wire       [1:0]    system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_length;
  wire       [31:0]   system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_data;
  wire       [0:0]    system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_context;
  wire                system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_valid;
  wire                system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_ready;
  wire                system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_last;
  wire       [0:0]    system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_opcode;
  wire       [31:0]   system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_data;
  wire       [0:0]    system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_context;
  wire                system_periphBridge_bmb_withoutMask_cmd_valid;
  wire                system_periphBridge_bmb_withoutMask_cmd_ready;
  wire                system_periphBridge_bmb_withoutMask_cmd_payload_last;
  wire       [0:0]    system_periphBridge_bmb_withoutMask_cmd_payload_fragment_opcode;
  wire       [23:0]   system_periphBridge_bmb_withoutMask_cmd_payload_fragment_address;
  wire       [1:0]    system_periphBridge_bmb_withoutMask_cmd_payload_fragment_length;
  wire       [31:0]   system_periphBridge_bmb_withoutMask_cmd_payload_fragment_data;
  wire       [0:0]    system_periphBridge_bmb_withoutMask_cmd_payload_fragment_context;
  wire                system_periphBridge_bmb_withoutMask_rsp_valid;
  wire                system_periphBridge_bmb_withoutMask_rsp_ready;
  wire                system_periphBridge_bmb_withoutMask_rsp_payload_last;
  wire       [0:0]    system_periphBridge_bmb_withoutMask_rsp_payload_fragment_opcode;
  wire       [31:0]   system_periphBridge_bmb_withoutMask_rsp_payload_fragment_data;
  wire       [0:0]    system_periphBridge_bmb_withoutMask_rsp_payload_fragment_context;
  wire                system_periphBridge_bmb_withoutMask_cmd_valid_1;
  wire                system_periphBridge_bmb_withoutMask_cmd_ready_1;
  wire                system_periphBridge_bmb_withoutMask_cmd_payload_last_1;
  wire       [0:0]    system_periphBridge_bmb_withoutMask_cmd_payload_fragment_opcode_1;
  wire       [23:0]   system_periphBridge_bmb_withoutMask_cmd_payload_fragment_address_1;
  wire       [1:0]    system_periphBridge_bmb_withoutMask_cmd_payload_fragment_length_1;
  wire       [31:0]   system_periphBridge_bmb_withoutMask_cmd_payload_fragment_data_1;
  wire       [0:0]    system_periphBridge_bmb_withoutMask_cmd_payload_fragment_context_1;
  wire                system_periphBridge_bmb_withoutMask_rsp_valid_1;
  wire                system_periphBridge_bmb_withoutMask_rsp_ready_1;
  wire                system_periphBridge_bmb_withoutMask_rsp_payload_last_1;
  wire       [0:0]    system_periphBridge_bmb_withoutMask_rsp_payload_fragment_opcode_1;
  wire       [31:0]   system_periphBridge_bmb_withoutMask_rsp_payload_fragment_data_1;
  wire       [0:0]    system_periphBridge_bmb_withoutMask_rsp_payload_fragment_context_1;
  wire                system_periphBridge_bmb_withoutMask_cmd_valid_2;
  wire                system_periphBridge_bmb_withoutMask_cmd_ready_2;
  wire                system_periphBridge_bmb_withoutMask_cmd_payload_last_2;
  wire       [0:0]    system_periphBridge_bmb_withoutMask_cmd_payload_fragment_opcode_2;
  wire       [23:0]   system_periphBridge_bmb_withoutMask_cmd_payload_fragment_address_2;
  wire       [1:0]    system_periphBridge_bmb_withoutMask_cmd_payload_fragment_length_2;
  wire       [31:0]   system_periphBridge_bmb_withoutMask_cmd_payload_fragment_data_2;
  wire       [0:0]    system_periphBridge_bmb_withoutMask_cmd_payload_fragment_context_2;
  wire                system_periphBridge_bmb_withoutMask_rsp_valid_2;
  wire                system_periphBridge_bmb_withoutMask_rsp_ready_2;
  wire                system_periphBridge_bmb_withoutMask_rsp_payload_last_2;
  wire       [0:0]    system_periphBridge_bmb_withoutMask_rsp_payload_fragment_opcode_2;
  wire       [31:0]   system_periphBridge_bmb_withoutMask_rsp_payload_fragment_data_2;
  wire       [0:0]    system_periphBridge_bmb_withoutMask_rsp_payload_fragment_context_2;
  wire                when_InOutWrapper_l65;
  wire                when_InOutWrapper_l65_1;
  wire                when_InOutWrapper_l65_2;
  wire                when_InOutWrapper_l65_3;
  wire                when_InOutWrapper_l65_4;
  wire                when_InOutWrapper_l65_5;
  wire                when_InOutWrapper_l65_6;
  wire                when_InOutWrapper_l65_7;

  BufferCC_2 bufferCC_5 (
    .io_dataIn                  (1'b0                      ), //i
    .io_dataOut                 (bufferCC_5_io_dataOut     ), //o
    .debugCdCtrl_external_clk   (debugCdCtrl_external_clk  ), //i
    .debugCdCtrl_external_reset (debugCdCtrl_external_reset)  //i
  );
  BufferCC_3 bufferCC_6 (
    .io_dataIn                     (1'b0                         ), //i
    .io_dataOut                    (bufferCC_6_io_dataOut        ), //o
    .debugCdCtrl_external_clk      (debugCdCtrl_external_clk     ), //i
    .debugCdCtrl_logic_outputReset (debugCdCtrl_logic_outputReset)  //i
  );
  VexRiscv system_cpu_logic_cpu (
    .iBus_cmd_valid                 (system_cpu_logic_cpu_iBus_cmd_valid                    ), //o
    .iBus_cmd_ready                 (system_cpu_iBus_cmd_ready                              ), //i
    .iBus_cmd_payload_pc            (system_cpu_logic_cpu_iBus_cmd_payload_pc[31:0]         ), //o
    .iBus_rsp_valid                 (system_cpu_iBus_rsp_valid                              ), //i
    .iBus_rsp_payload_error         (system_cpu_logic_cpu_iBus_rsp_payload_error            ), //i
    .iBus_rsp_payload_inst          (system_cpu_iBus_rsp_payload_fragment_data[31:0]        ), //i
    .debug_bus_cmd_valid            (systemDebugger_1_io_mem_cmd_valid                      ), //i
    .debug_bus_cmd_ready            (system_cpu_logic_cpu_debug_bus_cmd_ready               ), //o
    .debug_bus_cmd_payload_wr       (systemDebugger_1_io_mem_cmd_payload_wr                 ), //i
    .debug_bus_cmd_payload_address  (system_cpu_logic_cpu_debug_bus_cmd_payload_address[7:0]), //i
    .debug_bus_cmd_payload_data     (systemDebugger_1_io_mem_cmd_payload_data[31:0]         ), //i
    .debug_bus_rsp_data             (system_cpu_logic_cpu_debug_bus_rsp_data[31:0]          ), //o
    .debug_resetOut                 (system_cpu_logic_cpu_debug_resetOut                    ), //o
    .dBus_cmd_valid                 (system_cpu_logic_cpu_dBus_cmd_valid                    ), //o
    .dBus_cmd_ready                 (system_cpu_dBus_cmd_ready                              ), //i
    .dBus_cmd_payload_wr            (system_cpu_logic_cpu_dBus_cmd_payload_wr               ), //o
    .dBus_cmd_payload_address       (system_cpu_logic_cpu_dBus_cmd_payload_address[31:0]    ), //o
    .dBus_cmd_payload_data          (system_cpu_logic_cpu_dBus_cmd_payload_data[31:0]       ), //o
    .dBus_cmd_payload_size          (system_cpu_logic_cpu_dBus_cmd_payload_size[1:0]        ), //o
    .dBus_rsp_ready                 (system_cpu_logic_cpu_dBus_rsp_ready                    ), //i
    .dBus_rsp_error                 (system_cpu_logic_cpu_dBus_rsp_error                    ), //i
    .dBus_rsp_data                  (system_cpu_dBus_rsp_payload_fragment_data[31:0]        ), //i
    .debugCdCtrl_external_clk       (debugCdCtrl_external_clk                               ), //i
    .systemCdCtrl_logic_outputReset (systemCdCtrl_logic_outputReset                         ), //i
    .debugCdCtrl_logic_outputReset  (debugCdCtrl_logic_outputReset                          )  //i
  );
  JtagBridge jtagBridge_1 (
    .io_jtag_tms                    (system_cpu_jtag_tms                              ), //i
    .io_jtag_tdi                    (system_cpu_jtag_tdi                              ), //i
    .io_jtag_tdo                    (jtagBridge_1_io_jtag_tdo                         ), //o
    .io_jtag_tck                    (system_cpu_jtag_tck                              ), //i
    .io_remote_cmd_valid            (jtagBridge_1_io_remote_cmd_valid                 ), //o
    .io_remote_cmd_ready            (systemDebugger_1_io_remote_cmd_ready             ), //i
    .io_remote_cmd_payload_last     (jtagBridge_1_io_remote_cmd_payload_last          ), //o
    .io_remote_cmd_payload_fragment (jtagBridge_1_io_remote_cmd_payload_fragment      ), //o
    .io_remote_rsp_valid            (systemDebugger_1_io_remote_rsp_valid             ), //i
    .io_remote_rsp_ready            (jtagBridge_1_io_remote_rsp_ready                 ), //o
    .io_remote_rsp_payload_error    (systemDebugger_1_io_remote_rsp_payload_error     ), //i
    .io_remote_rsp_payload_data     (systemDebugger_1_io_remote_rsp_payload_data[31:0]), //i
    .debugCdCtrl_external_clk       (debugCdCtrl_external_clk                         ), //i
    .debugCdCtrl_logic_outputReset  (debugCdCtrl_logic_outputReset                    )  //i
  );
  SystemDebugger systemDebugger_1 (
    .io_remote_cmd_valid            (jtagBridge_1_io_remote_cmd_valid                 ), //i
    .io_remote_cmd_ready            (systemDebugger_1_io_remote_cmd_ready             ), //o
    .io_remote_cmd_payload_last     (jtagBridge_1_io_remote_cmd_payload_last          ), //i
    .io_remote_cmd_payload_fragment (jtagBridge_1_io_remote_cmd_payload_fragment      ), //i
    .io_remote_rsp_valid            (systemDebugger_1_io_remote_rsp_valid             ), //o
    .io_remote_rsp_ready            (jtagBridge_1_io_remote_rsp_ready                 ), //i
    .io_remote_rsp_payload_error    (systemDebugger_1_io_remote_rsp_payload_error     ), //o
    .io_remote_rsp_payload_data     (systemDebugger_1_io_remote_rsp_payload_data[31:0]), //o
    .io_mem_cmd_valid               (systemDebugger_1_io_mem_cmd_valid                ), //o
    .io_mem_cmd_ready               (system_cpu_logic_cpu_debug_bus_cmd_ready         ), //i
    .io_mem_cmd_payload_address     (systemDebugger_1_io_mem_cmd_payload_address[31:0]), //o
    .io_mem_cmd_payload_data        (systemDebugger_1_io_mem_cmd_payload_data[31:0]   ), //o
    .io_mem_cmd_payload_wr          (systemDebugger_1_io_mem_cmd_payload_wr           ), //o
    .io_mem_cmd_payload_size        (systemDebugger_1_io_mem_cmd_payload_size[1:0]    ), //o
    .io_mem_rsp_valid               (system_cpu_logic_cpu_debug_bus_cmd_fire_regNext  ), //i
    .io_mem_rsp_payload             (system_cpu_logic_cpu_debug_bus_rsp_data[31:0]    ), //i
    .debugCdCtrl_external_clk       (debugCdCtrl_external_clk                         ), //i
    .debugCdCtrl_logic_outputReset  (debugCdCtrl_logic_outputReset                    )  //i
  );
  BufferCC_4 bufferCC_7 (
    .io_dataIn                (1'b0                    ), //i
    .io_dataOut               (bufferCC_7_io_dataOut   ), //o
    .debugCdCtrl_external_clk (debugCdCtrl_external_clk), //i
    .system_cpu_debugReset    (system_cpu_debugReset   )  //i
  );
  BmbDecoder system_cpu_iBus_decoder (
    .io_input_cmd_valid                        (system_cpu_iBus_cmd_valid                                              ), //i
    .io_input_cmd_ready                        (system_cpu_iBus_decoder_io_input_cmd_ready                             ), //o
    .io_input_cmd_payload_last                 (system_cpu_iBus_cmd_payload_last                                       ), //i
    .io_input_cmd_payload_fragment_opcode      (system_cpu_iBus_cmd_payload_fragment_opcode                            ), //i
    .io_input_cmd_payload_fragment_address     (system_cpu_iBus_cmd_payload_fragment_address[31:0]                     ), //i
    .io_input_cmd_payload_fragment_length      (system_cpu_iBus_cmd_payload_fragment_length[1:0]                       ), //i
    .io_input_rsp_valid                        (system_cpu_iBus_decoder_io_input_rsp_valid                             ), //o
    .io_input_rsp_ready                        (system_cpu_iBus_rsp_ready                                              ), //i
    .io_input_rsp_payload_last                 (system_cpu_iBus_decoder_io_input_rsp_payload_last                      ), //o
    .io_input_rsp_payload_fragment_opcode      (system_cpu_iBus_decoder_io_input_rsp_payload_fragment_opcode           ), //o
    .io_input_rsp_payload_fragment_data        (system_cpu_iBus_decoder_io_input_rsp_payload_fragment_data[31:0]       ), //o
    .io_outputs_0_cmd_valid                    (system_cpu_iBus_decoder_io_outputs_0_cmd_valid                         ), //o
    .io_outputs_0_cmd_ready                    (system_ramA_ctrl_arbiter_io_inputs_1_cmd_ready                         ), //i
    .io_outputs_0_cmd_payload_last             (system_cpu_iBus_decoder_io_outputs_0_cmd_payload_last                  ), //o
    .io_outputs_0_cmd_payload_fragment_opcode  (system_cpu_iBus_decoder_io_outputs_0_cmd_payload_fragment_opcode       ), //o
    .io_outputs_0_cmd_payload_fragment_address (system_cpu_iBus_decoder_io_outputs_0_cmd_payload_fragment_address[31:0]), //o
    .io_outputs_0_cmd_payload_fragment_length  (system_cpu_iBus_decoder_io_outputs_0_cmd_payload_fragment_length[1:0]  ), //o
    .io_outputs_0_rsp_valid                    (system_ramA_ctrl_arbiter_io_inputs_1_rsp_valid                         ), //i
    .io_outputs_0_rsp_ready                    (system_cpu_iBus_decoder_io_outputs_0_rsp_ready                         ), //o
    .io_outputs_0_rsp_payload_last             (system_ramA_ctrl_arbiter_io_inputs_1_rsp_payload_last                  ), //i
    .io_outputs_0_rsp_payload_fragment_opcode  (system_ramA_ctrl_arbiter_io_inputs_1_rsp_payload_fragment_opcode       ), //i
    .io_outputs_0_rsp_payload_fragment_data    (system_ramA_ctrl_arbiter_io_inputs_1_rsp_payload_fragment_data[31:0]   ), //i
    .debugCdCtrl_external_clk                  (debugCdCtrl_external_clk                                               ), //i
    .systemCdCtrl_logic_outputReset            (systemCdCtrl_logic_outputReset                                         )  //i
  );
  BmbDecoder_1 system_cpu_dBus_decoder (
    .io_input_cmd_valid                        (system_cpu_dBus_cmd_m2sPipe_valid                                                             ), //i
    .io_input_cmd_ready                        (system_cpu_dBus_decoder_io_input_cmd_ready                                                    ), //o
    .io_input_cmd_payload_last                 (system_cpu_dBus_cmd_m2sPipe_payload_last                                                      ), //i
    .io_input_cmd_payload_fragment_opcode      (system_cpu_dBus_cmd_m2sPipe_payload_fragment_opcode                                           ), //i
    .io_input_cmd_payload_fragment_address     (system_cpu_dBus_cmd_m2sPipe_payload_fragment_address[31:0]                                    ), //i
    .io_input_cmd_payload_fragment_length      (system_cpu_dBus_cmd_m2sPipe_payload_fragment_length[1:0]                                      ), //i
    .io_input_cmd_payload_fragment_data        (system_cpu_dBus_cmd_m2sPipe_payload_fragment_data[31:0]                                       ), //i
    .io_input_cmd_payload_fragment_mask        (system_cpu_dBus_cmd_m2sPipe_payload_fragment_mask[3:0]                                        ), //i
    .io_input_cmd_payload_fragment_context     (system_cpu_dBus_cmd_m2sPipe_payload_fragment_context                                          ), //i
    .io_input_rsp_valid                        (system_cpu_dBus_decoder_io_input_rsp_valid                                                    ), //o
    .io_input_rsp_ready                        (system_cpu_dBus_rsp_ready                                                                     ), //i
    .io_input_rsp_payload_last                 (system_cpu_dBus_decoder_io_input_rsp_payload_last                                             ), //o
    .io_input_rsp_payload_fragment_opcode      (system_cpu_dBus_decoder_io_input_rsp_payload_fragment_opcode                                  ), //o
    .io_input_rsp_payload_fragment_data        (system_cpu_dBus_decoder_io_input_rsp_payload_fragment_data[31:0]                              ), //o
    .io_input_rsp_payload_fragment_context     (system_cpu_dBus_decoder_io_input_rsp_payload_fragment_context                                 ), //o
    .io_outputs_0_cmd_valid                    (system_cpu_dBus_decoder_io_outputs_0_cmd_valid                                                ), //o
    .io_outputs_0_cmd_ready                    (system_ramA_ctrl_arbiter_io_inputs_0_cmd_ready                                                ), //i
    .io_outputs_0_cmd_payload_last             (system_cpu_dBus_decoder_io_outputs_0_cmd_payload_last                                         ), //o
    .io_outputs_0_cmd_payload_fragment_opcode  (system_cpu_dBus_decoder_io_outputs_0_cmd_payload_fragment_opcode                              ), //o
    .io_outputs_0_cmd_payload_fragment_address (system_cpu_dBus_decoder_io_outputs_0_cmd_payload_fragment_address[31:0]                       ), //o
    .io_outputs_0_cmd_payload_fragment_length  (system_cpu_dBus_decoder_io_outputs_0_cmd_payload_fragment_length[1:0]                         ), //o
    .io_outputs_0_cmd_payload_fragment_data    (system_cpu_dBus_decoder_io_outputs_0_cmd_payload_fragment_data[31:0]                          ), //o
    .io_outputs_0_cmd_payload_fragment_mask    (system_cpu_dBus_decoder_io_outputs_0_cmd_payload_fragment_mask[3:0]                           ), //o
    .io_outputs_0_cmd_payload_fragment_context (system_cpu_dBus_decoder_io_outputs_0_cmd_payload_fragment_context                             ), //o
    .io_outputs_0_rsp_valid                    (system_ramA_ctrl_arbiter_io_inputs_0_rsp_valid                                                ), //i
    .io_outputs_0_rsp_ready                    (system_cpu_dBus_decoder_io_outputs_0_rsp_ready                                                ), //o
    .io_outputs_0_rsp_payload_last             (system_ramA_ctrl_arbiter_io_inputs_0_rsp_payload_last                                         ), //i
    .io_outputs_0_rsp_payload_fragment_opcode  (system_ramA_ctrl_arbiter_io_inputs_0_rsp_payload_fragment_opcode                              ), //i
    .io_outputs_0_rsp_payload_fragment_data    (system_ramA_ctrl_arbiter_io_inputs_0_rsp_payload_fragment_data[31:0]                          ), //i
    .io_outputs_0_rsp_payload_fragment_context (system_ramA_ctrl_arbiter_io_inputs_0_rsp_payload_fragment_context                             ), //i
    .io_outputs_1_cmd_valid                    (system_cpu_dBus_decoder_io_outputs_1_cmd_valid                                                ), //o
    .io_outputs_1_cmd_ready                    (system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_ready                      ), //i
    .io_outputs_1_cmd_payload_last             (system_cpu_dBus_decoder_io_outputs_1_cmd_payload_last                                         ), //o
    .io_outputs_1_cmd_payload_fragment_opcode  (system_cpu_dBus_decoder_io_outputs_1_cmd_payload_fragment_opcode                              ), //o
    .io_outputs_1_cmd_payload_fragment_address (system_cpu_dBus_decoder_io_outputs_1_cmd_payload_fragment_address[31:0]                       ), //o
    .io_outputs_1_cmd_payload_fragment_length  (system_cpu_dBus_decoder_io_outputs_1_cmd_payload_fragment_length[1:0]                         ), //o
    .io_outputs_1_cmd_payload_fragment_data    (system_cpu_dBus_decoder_io_outputs_1_cmd_payload_fragment_data[31:0]                          ), //o
    .io_outputs_1_cmd_payload_fragment_mask    (system_cpu_dBus_decoder_io_outputs_1_cmd_payload_fragment_mask[3:0]                           ), //o
    .io_outputs_1_cmd_payload_fragment_context (system_cpu_dBus_decoder_io_outputs_1_cmd_payload_fragment_context                             ), //o
    .io_outputs_1_rsp_valid                    (system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_rsp_valid                      ), //i
    .io_outputs_1_rsp_ready                    (system_cpu_dBus_decoder_io_outputs_1_rsp_ready                                                ), //o
    .io_outputs_1_rsp_payload_last             (system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_last               ), //i
    .io_outputs_1_rsp_payload_fragment_opcode  (system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_opcode    ), //i
    .io_outputs_1_rsp_payload_fragment_data    (system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_data[31:0]), //i
    .io_outputs_1_rsp_payload_fragment_context (system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_context   ), //i
    .debugCdCtrl_external_clk                  (debugCdCtrl_external_clk                                                                      ), //i
    .systemCdCtrl_logic_outputReset            (systemCdCtrl_logic_outputReset                                                                )  //i
  );
  BmbOnChipRam system_ramA_logic (
    .io_bus_cmd_valid                    (system_ramA_ctrl_arbiter_io_output_cmd_valid                         ), //i
    .io_bus_cmd_ready                    (system_ramA_logic_io_bus_cmd_ready                                   ), //o
    .io_bus_cmd_payload_last             (system_ramA_ctrl_arbiter_io_output_cmd_payload_last                  ), //i
    .io_bus_cmd_payload_fragment_source  (system_ramA_ctrl_arbiter_io_output_cmd_payload_fragment_source       ), //i
    .io_bus_cmd_payload_fragment_opcode  (system_ramA_ctrl_arbiter_io_output_cmd_payload_fragment_opcode       ), //i
    .io_bus_cmd_payload_fragment_address (system_ramA_ctrl_arbiter_io_output_cmd_payload_fragment_address[14:0]), //i
    .io_bus_cmd_payload_fragment_length  (system_ramA_ctrl_arbiter_io_output_cmd_payload_fragment_length[1:0]  ), //i
    .io_bus_cmd_payload_fragment_data    (system_ramA_ctrl_arbiter_io_output_cmd_payload_fragment_data[31:0]   ), //i
    .io_bus_cmd_payload_fragment_mask    (system_ramA_ctrl_arbiter_io_output_cmd_payload_fragment_mask[3:0]    ), //i
    .io_bus_cmd_payload_fragment_context (system_ramA_ctrl_arbiter_io_output_cmd_payload_fragment_context      ), //i
    .io_bus_rsp_valid                    (system_ramA_logic_io_bus_rsp_valid                                   ), //o
    .io_bus_rsp_ready                    (system_ramA_ctrl_arbiter_io_output_rsp_ready                         ), //i
    .io_bus_rsp_payload_last             (system_ramA_logic_io_bus_rsp_payload_last                            ), //o
    .io_bus_rsp_payload_fragment_source  (system_ramA_logic_io_bus_rsp_payload_fragment_source                 ), //o
    .io_bus_rsp_payload_fragment_opcode  (system_ramA_logic_io_bus_rsp_payload_fragment_opcode                 ), //o
    .io_bus_rsp_payload_fragment_data    (system_ramA_logic_io_bus_rsp_payload_fragment_data[31:0]             ), //o
    .io_bus_rsp_payload_fragment_context (system_ramA_logic_io_bus_rsp_payload_fragment_context                ), //o
    .debugCdCtrl_external_clk            (debugCdCtrl_external_clk                                             ), //i
    .systemCdCtrl_logic_outputReset      (systemCdCtrl_logic_outputReset                                       )  //i
  );
  BmbArbiter system_ramA_ctrl_arbiter (
    .io_inputs_0_cmd_valid                    (system_cpu_dBus_decoder_io_outputs_0_cmd_valid                         ), //i
    .io_inputs_0_cmd_ready                    (system_ramA_ctrl_arbiter_io_inputs_0_cmd_ready                         ), //o
    .io_inputs_0_cmd_payload_last             (system_cpu_dBus_decoder_io_outputs_0_cmd_payload_last                  ), //i
    .io_inputs_0_cmd_payload_fragment_opcode  (system_cpu_dBus_decoder_io_outputs_0_cmd_payload_fragment_opcode       ), //i
    .io_inputs_0_cmd_payload_fragment_address (system_ramA_ctrl_arbiter_io_inputs_0_cmd_payload_fragment_address[14:0]), //i
    .io_inputs_0_cmd_payload_fragment_length  (system_cpu_dBus_decoder_io_outputs_0_cmd_payload_fragment_length[1:0]  ), //i
    .io_inputs_0_cmd_payload_fragment_data    (system_cpu_dBus_decoder_io_outputs_0_cmd_payload_fragment_data[31:0]   ), //i
    .io_inputs_0_cmd_payload_fragment_mask    (system_cpu_dBus_decoder_io_outputs_0_cmd_payload_fragment_mask[3:0]    ), //i
    .io_inputs_0_cmd_payload_fragment_context (system_cpu_dBus_decoder_io_outputs_0_cmd_payload_fragment_context      ), //i
    .io_inputs_0_rsp_valid                    (system_ramA_ctrl_arbiter_io_inputs_0_rsp_valid                         ), //o
    .io_inputs_0_rsp_ready                    (system_cpu_dBus_decoder_io_outputs_0_rsp_ready                         ), //i
    .io_inputs_0_rsp_payload_last             (system_ramA_ctrl_arbiter_io_inputs_0_rsp_payload_last                  ), //o
    .io_inputs_0_rsp_payload_fragment_opcode  (system_ramA_ctrl_arbiter_io_inputs_0_rsp_payload_fragment_opcode       ), //o
    .io_inputs_0_rsp_payload_fragment_data    (system_ramA_ctrl_arbiter_io_inputs_0_rsp_payload_fragment_data[31:0]   ), //o
    .io_inputs_0_rsp_payload_fragment_context (system_ramA_ctrl_arbiter_io_inputs_0_rsp_payload_fragment_context      ), //o
    .io_inputs_1_cmd_valid                    (system_cpu_iBus_decoder_io_outputs_0_cmd_valid                         ), //i
    .io_inputs_1_cmd_ready                    (system_ramA_ctrl_arbiter_io_inputs_1_cmd_ready                         ), //o
    .io_inputs_1_cmd_payload_last             (system_cpu_iBus_decoder_io_outputs_0_cmd_payload_last                  ), //i
    .io_inputs_1_cmd_payload_fragment_opcode  (system_cpu_iBus_decoder_io_outputs_0_cmd_payload_fragment_opcode       ), //i
    .io_inputs_1_cmd_payload_fragment_address (system_ramA_ctrl_arbiter_io_inputs_1_cmd_payload_fragment_address[14:0]), //i
    .io_inputs_1_cmd_payload_fragment_length  (system_cpu_iBus_decoder_io_outputs_0_cmd_payload_fragment_length[1:0]  ), //i
    .io_inputs_1_cmd_payload_fragment_data    (32'bxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx                                   ), //i
    .io_inputs_1_cmd_payload_fragment_mask    (4'bxxxx                                                                ), //i
    .io_inputs_1_rsp_valid                    (system_ramA_ctrl_arbiter_io_inputs_1_rsp_valid                         ), //o
    .io_inputs_1_rsp_ready                    (system_cpu_iBus_decoder_io_outputs_0_rsp_ready                         ), //i
    .io_inputs_1_rsp_payload_last             (system_ramA_ctrl_arbiter_io_inputs_1_rsp_payload_last                  ), //o
    .io_inputs_1_rsp_payload_fragment_opcode  (system_ramA_ctrl_arbiter_io_inputs_1_rsp_payload_fragment_opcode       ), //o
    .io_inputs_1_rsp_payload_fragment_data    (system_ramA_ctrl_arbiter_io_inputs_1_rsp_payload_fragment_data[31:0]   ), //o
    .io_output_cmd_valid                      (system_ramA_ctrl_arbiter_io_output_cmd_valid                           ), //o
    .io_output_cmd_ready                      (system_ramA_logic_io_bus_cmd_ready                                     ), //i
    .io_output_cmd_payload_last               (system_ramA_ctrl_arbiter_io_output_cmd_payload_last                    ), //o
    .io_output_cmd_payload_fragment_source    (system_ramA_ctrl_arbiter_io_output_cmd_payload_fragment_source         ), //o
    .io_output_cmd_payload_fragment_opcode    (system_ramA_ctrl_arbiter_io_output_cmd_payload_fragment_opcode         ), //o
    .io_output_cmd_payload_fragment_address   (system_ramA_ctrl_arbiter_io_output_cmd_payload_fragment_address[14:0]  ), //o
    .io_output_cmd_payload_fragment_length    (system_ramA_ctrl_arbiter_io_output_cmd_payload_fragment_length[1:0]    ), //o
    .io_output_cmd_payload_fragment_data      (system_ramA_ctrl_arbiter_io_output_cmd_payload_fragment_data[31:0]     ), //o
    .io_output_cmd_payload_fragment_mask      (system_ramA_ctrl_arbiter_io_output_cmd_payload_fragment_mask[3:0]      ), //o
    .io_output_cmd_payload_fragment_context   (system_ramA_ctrl_arbiter_io_output_cmd_payload_fragment_context        ), //o
    .io_output_rsp_valid                      (system_ramA_logic_io_bus_rsp_valid                                     ), //i
    .io_output_rsp_ready                      (system_ramA_ctrl_arbiter_io_output_rsp_ready                           ), //o
    .io_output_rsp_payload_last               (system_ramA_logic_io_bus_rsp_payload_last                              ), //i
    .io_output_rsp_payload_fragment_source    (system_ramA_logic_io_bus_rsp_payload_fragment_source                   ), //i
    .io_output_rsp_payload_fragment_opcode    (system_ramA_logic_io_bus_rsp_payload_fragment_opcode                   ), //i
    .io_output_rsp_payload_fragment_data      (system_ramA_logic_io_bus_rsp_payload_fragment_data[31:0]               ), //i
    .io_output_rsp_payload_fragment_context   (system_ramA_logic_io_bus_rsp_payload_fragment_context                  ), //i
    .debugCdCtrl_external_clk                 (debugCdCtrl_external_clk                                               ), //i
    .systemCdCtrl_logic_outputReset           (systemCdCtrl_logic_outputReset                                         )  //i
  );
  BmbDecoder_2 system_periphBridge_bmb_decoder (
    .io_input_cmd_valid                        (system_periphBridge_bmb_cmd_halfPipe_valid                                     ), //i
    .io_input_cmd_ready                        (system_periphBridge_bmb_decoder_io_input_cmd_ready                             ), //o
    .io_input_cmd_payload_last                 (system_periphBridge_bmb_cmd_halfPipe_payload_last                              ), //i
    .io_input_cmd_payload_fragment_opcode      (system_periphBridge_bmb_cmd_halfPipe_payload_fragment_opcode                   ), //i
    .io_input_cmd_payload_fragment_address     (system_periphBridge_bmb_cmd_halfPipe_payload_fragment_address[23:0]            ), //i
    .io_input_cmd_payload_fragment_length      (system_periphBridge_bmb_cmd_halfPipe_payload_fragment_length[1:0]              ), //i
    .io_input_cmd_payload_fragment_data        (system_periphBridge_bmb_cmd_halfPipe_payload_fragment_data[31:0]               ), //i
    .io_input_cmd_payload_fragment_mask        (system_periphBridge_bmb_cmd_halfPipe_payload_fragment_mask[3:0]                ), //i
    .io_input_cmd_payload_fragment_context     (system_periphBridge_bmb_cmd_halfPipe_payload_fragment_context                  ), //i
    .io_input_rsp_valid                        (system_periphBridge_bmb_decoder_io_input_rsp_valid                             ), //o
    .io_input_rsp_ready                        (_zz_io_input_rsp_ready                                                         ), //i
    .io_input_rsp_payload_last                 (system_periphBridge_bmb_decoder_io_input_rsp_payload_last                      ), //o
    .io_input_rsp_payload_fragment_opcode      (system_periphBridge_bmb_decoder_io_input_rsp_payload_fragment_opcode           ), //o
    .io_input_rsp_payload_fragment_data        (system_periphBridge_bmb_decoder_io_input_rsp_payload_fragment_data[31:0]       ), //o
    .io_input_rsp_payload_fragment_context     (system_periphBridge_bmb_decoder_io_input_rsp_payload_fragment_context          ), //o
    .io_outputs_0_cmd_valid                    (system_periphBridge_bmb_decoder_io_outputs_0_cmd_valid                         ), //o
    .io_outputs_0_cmd_ready                    (system_periphBridge_bmb_withoutMask_cmd_ready                                  ), //i
    .io_outputs_0_cmd_payload_last             (system_periphBridge_bmb_decoder_io_outputs_0_cmd_payload_last                  ), //o
    .io_outputs_0_cmd_payload_fragment_opcode  (system_periphBridge_bmb_decoder_io_outputs_0_cmd_payload_fragment_opcode       ), //o
    .io_outputs_0_cmd_payload_fragment_address (system_periphBridge_bmb_decoder_io_outputs_0_cmd_payload_fragment_address[23:0]), //o
    .io_outputs_0_cmd_payload_fragment_length  (system_periphBridge_bmb_decoder_io_outputs_0_cmd_payload_fragment_length[1:0]  ), //o
    .io_outputs_0_cmd_payload_fragment_data    (system_periphBridge_bmb_decoder_io_outputs_0_cmd_payload_fragment_data[31:0]   ), //o
    .io_outputs_0_cmd_payload_fragment_mask    (system_periphBridge_bmb_decoder_io_outputs_0_cmd_payload_fragment_mask[3:0]    ), //o
    .io_outputs_0_cmd_payload_fragment_context (system_periphBridge_bmb_decoder_io_outputs_0_cmd_payload_fragment_context      ), //o
    .io_outputs_0_rsp_valid                    (system_periphBridge_bmb_withoutMask_rsp_valid                                  ), //i
    .io_outputs_0_rsp_ready                    (system_periphBridge_bmb_decoder_io_outputs_0_rsp_ready                         ), //o
    .io_outputs_0_rsp_payload_last             (system_periphBridge_bmb_withoutMask_rsp_payload_last                           ), //i
    .io_outputs_0_rsp_payload_fragment_opcode  (system_periphBridge_bmb_withoutMask_rsp_payload_fragment_opcode                ), //i
    .io_outputs_0_rsp_payload_fragment_data    (system_periphBridge_bmb_withoutMask_rsp_payload_fragment_data[31:0]            ), //i
    .io_outputs_0_rsp_payload_fragment_context (system_periphBridge_bmb_withoutMask_rsp_payload_fragment_context               ), //i
    .io_outputs_1_cmd_valid                    (system_periphBridge_bmb_decoder_io_outputs_1_cmd_valid                         ), //o
    .io_outputs_1_cmd_ready                    (system_periphBridge_bmb_withoutMask_cmd_ready_1                                ), //i
    .io_outputs_1_cmd_payload_last             (system_periphBridge_bmb_decoder_io_outputs_1_cmd_payload_last                  ), //o
    .io_outputs_1_cmd_payload_fragment_opcode  (system_periphBridge_bmb_decoder_io_outputs_1_cmd_payload_fragment_opcode       ), //o
    .io_outputs_1_cmd_payload_fragment_address (system_periphBridge_bmb_decoder_io_outputs_1_cmd_payload_fragment_address[23:0]), //o
    .io_outputs_1_cmd_payload_fragment_length  (system_periphBridge_bmb_decoder_io_outputs_1_cmd_payload_fragment_length[1:0]  ), //o
    .io_outputs_1_cmd_payload_fragment_data    (system_periphBridge_bmb_decoder_io_outputs_1_cmd_payload_fragment_data[31:0]   ), //o
    .io_outputs_1_cmd_payload_fragment_mask    (system_periphBridge_bmb_decoder_io_outputs_1_cmd_payload_fragment_mask[3:0]    ), //o
    .io_outputs_1_cmd_payload_fragment_context (system_periphBridge_bmb_decoder_io_outputs_1_cmd_payload_fragment_context      ), //o
    .io_outputs_1_rsp_valid                    (system_periphBridge_bmb_withoutMask_rsp_valid_1                                ), //i
    .io_outputs_1_rsp_ready                    (system_periphBridge_bmb_decoder_io_outputs_1_rsp_ready                         ), //o
    .io_outputs_1_rsp_payload_last             (system_periphBridge_bmb_withoutMask_rsp_payload_last_1                         ), //i
    .io_outputs_1_rsp_payload_fragment_opcode  (system_periphBridge_bmb_withoutMask_rsp_payload_fragment_opcode_1              ), //i
    .io_outputs_1_rsp_payload_fragment_data    (system_periphBridge_bmb_withoutMask_rsp_payload_fragment_data_1[31:0]          ), //i
    .io_outputs_1_rsp_payload_fragment_context (system_periphBridge_bmb_withoutMask_rsp_payload_fragment_context_1             ), //i
    .io_outputs_2_cmd_valid                    (system_periphBridge_bmb_decoder_io_outputs_2_cmd_valid                         ), //o
    .io_outputs_2_cmd_ready                    (system_periphBridge_bmb_withoutMask_cmd_ready_2                                ), //i
    .io_outputs_2_cmd_payload_last             (system_periphBridge_bmb_decoder_io_outputs_2_cmd_payload_last                  ), //o
    .io_outputs_2_cmd_payload_fragment_opcode  (system_periphBridge_bmb_decoder_io_outputs_2_cmd_payload_fragment_opcode       ), //o
    .io_outputs_2_cmd_payload_fragment_address (system_periphBridge_bmb_decoder_io_outputs_2_cmd_payload_fragment_address[23:0]), //o
    .io_outputs_2_cmd_payload_fragment_length  (system_periphBridge_bmb_decoder_io_outputs_2_cmd_payload_fragment_length[1:0]  ), //o
    .io_outputs_2_cmd_payload_fragment_data    (system_periphBridge_bmb_decoder_io_outputs_2_cmd_payload_fragment_data[31:0]   ), //o
    .io_outputs_2_cmd_payload_fragment_mask    (system_periphBridge_bmb_decoder_io_outputs_2_cmd_payload_fragment_mask[3:0]    ), //o
    .io_outputs_2_cmd_payload_fragment_context (system_periphBridge_bmb_decoder_io_outputs_2_cmd_payload_fragment_context      ), //o
    .io_outputs_2_rsp_valid                    (system_periphBridge_bmb_withoutMask_rsp_valid_2                                ), //i
    .io_outputs_2_rsp_ready                    (system_periphBridge_bmb_decoder_io_outputs_2_rsp_ready                         ), //o
    .io_outputs_2_rsp_payload_last             (system_periphBridge_bmb_withoutMask_rsp_payload_last_2                         ), //i
    .io_outputs_2_rsp_payload_fragment_opcode  (system_periphBridge_bmb_withoutMask_rsp_payload_fragment_opcode_2              ), //i
    .io_outputs_2_rsp_payload_fragment_data    (system_periphBridge_bmb_withoutMask_rsp_payload_fragment_data_2[31:0]          ), //i
    .io_outputs_2_rsp_payload_fragment_context (system_periphBridge_bmb_withoutMask_rsp_payload_fragment_context_2             ), //i
    .debugCdCtrl_external_clk                  (debugCdCtrl_external_clk                                                       ), //i
    .systemCdCtrl_logic_outputReset            (systemCdCtrl_logic_outputReset                                                 )  //i
  );
  BmbClint system_clint_logic (
    .io_bus_cmd_valid                    (system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_valid                         ), //i
    .io_bus_cmd_ready                    (system_clint_logic_io_bus_cmd_ready                                                        ), //o
    .io_bus_cmd_payload_last             (system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_last                  ), //i
    .io_bus_cmd_payload_fragment_opcode  (system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_opcode       ), //i
    .io_bus_cmd_payload_fragment_address (system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_address[15:0]), //i
    .io_bus_cmd_payload_fragment_length  (system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_length[1:0]  ), //i
    .io_bus_cmd_payload_fragment_data    (system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_data[31:0]   ), //i
    .io_bus_cmd_payload_fragment_context (system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_context      ), //i
    .io_bus_rsp_valid                    (system_clint_logic_io_bus_rsp_valid                                                        ), //o
    .io_bus_rsp_ready                    (system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_ready                         ), //i
    .io_bus_rsp_payload_last             (system_clint_logic_io_bus_rsp_payload_last                                                 ), //o
    .io_bus_rsp_payload_fragment_opcode  (system_clint_logic_io_bus_rsp_payload_fragment_opcode                                      ), //o
    .io_bus_rsp_payload_fragment_data    (system_clint_logic_io_bus_rsp_payload_fragment_data[31:0]                                  ), //o
    .io_bus_rsp_payload_fragment_context (system_clint_logic_io_bus_rsp_payload_fragment_context                                     ), //o
    .io_time                             (system_clint_logic_io_time[63:0]                                                           ), //o
    .debugCdCtrl_external_clk            (debugCdCtrl_external_clk                                                                   ), //i
    .systemCdCtrl_logic_outputReset      (systemCdCtrl_logic_outputReset                                                             )  //i
  );
  BmbGpio2 system_gpioA_logic (
    .io_gpio_read                        (_zz_io_gpio_read[7:0]                                                                     ), //i
    .io_gpio_write                       (system_gpioA_logic_io_gpio_write[7:0]                                                     ), //o
    .io_gpio_writeEnable                 (system_gpioA_logic_io_gpio_writeEnable[7:0]                                               ), //o
    .io_bus_cmd_valid                    (system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_valid                        ), //i
    .io_bus_cmd_ready                    (system_gpioA_logic_io_bus_cmd_ready                                                       ), //o
    .io_bus_cmd_payload_last             (system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_last                 ), //i
    .io_bus_cmd_payload_fragment_opcode  (system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_opcode      ), //i
    .io_bus_cmd_payload_fragment_address (system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_address[7:0]), //i
    .io_bus_cmd_payload_fragment_length  (system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_length[1:0] ), //i
    .io_bus_cmd_payload_fragment_data    (system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_data[31:0]  ), //i
    .io_bus_cmd_payload_fragment_context (system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_context     ), //i
    .io_bus_rsp_valid                    (system_gpioA_logic_io_bus_rsp_valid                                                       ), //o
    .io_bus_rsp_ready                    (system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_ready                        ), //i
    .io_bus_rsp_payload_last             (system_gpioA_logic_io_bus_rsp_payload_last                                                ), //o
    .io_bus_rsp_payload_fragment_opcode  (system_gpioA_logic_io_bus_rsp_payload_fragment_opcode                                     ), //o
    .io_bus_rsp_payload_fragment_data    (system_gpioA_logic_io_bus_rsp_payload_fragment_data[31:0]                                 ), //o
    .io_bus_rsp_payload_fragment_context (system_gpioA_logic_io_bus_rsp_payload_fragment_context                                    ), //o
    .io_interrupt                        (system_gpioA_logic_io_interrupt[7:0]                                                      ), //o
    .debugCdCtrl_external_clk            (debugCdCtrl_external_clk                                                                  ), //i
    .systemCdCtrl_logic_outputReset      (systemCdCtrl_logic_outputReset                                                            )  //i
  );
  BmbUartCtrl system_uartA_logic (
    .io_bus_cmd_valid                    (system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_valid                        ), //i
    .io_bus_cmd_ready                    (system_uartA_logic_io_bus_cmd_ready                                                       ), //o
    .io_bus_cmd_payload_last             (system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_last                 ), //i
    .io_bus_cmd_payload_fragment_opcode  (system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_opcode      ), //i
    .io_bus_cmd_payload_fragment_address (system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_address[5:0]), //i
    .io_bus_cmd_payload_fragment_length  (system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_length[1:0] ), //i
    .io_bus_cmd_payload_fragment_data    (system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_data[31:0]  ), //i
    .io_bus_cmd_payload_fragment_context (system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_context     ), //i
    .io_bus_rsp_valid                    (system_uartA_logic_io_bus_rsp_valid                                                       ), //o
    .io_bus_rsp_ready                    (system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_ready                        ), //i
    .io_bus_rsp_payload_last             (system_uartA_logic_io_bus_rsp_payload_last                                                ), //o
    .io_bus_rsp_payload_fragment_opcode  (system_uartA_logic_io_bus_rsp_payload_fragment_opcode                                     ), //o
    .io_bus_rsp_payload_fragment_data    (system_uartA_logic_io_bus_rsp_payload_fragment_data[31:0]                                 ), //o
    .io_bus_rsp_payload_fragment_context (system_uartA_logic_io_bus_rsp_payload_fragment_context                                    ), //o
    .io_uart_txd                         (system_uartA_logic_io_uart_txd                                                            ), //o
    .io_uart_rxd                         (system_uartA_uart_rxd                                                                     ), //i
    .io_interrupt                        (system_uartA_logic_io_interrupt                                                           ), //o
    .debugCdCtrl_external_clk            (debugCdCtrl_external_clk                                                                  ), //i
    .systemCdCtrl_logic_outputReset      (systemCdCtrl_logic_outputReset                                                            )  //i
  );
  initial begin
    debugCdCtrl_logic_holdingLogic_resetCounter = 12'h0;
    debugCdCtrl_logic_outputReset = 1'b1;
  end

  assign system_gpioA_gpio[0] = _zz_system_gpioA_gpio_7 ? _zz_system_gpioA_gpio_8[0] : 1'bz;
  assign system_gpioA_gpio[1] = _zz_system_gpioA_gpio_6 ? _zz_system_gpioA_gpio_8[1] : 1'bz;
  assign system_gpioA_gpio[2] = _zz_system_gpioA_gpio_5 ? _zz_system_gpioA_gpio_8[2] : 1'bz;
  assign system_gpioA_gpio[3] = _zz_system_gpioA_gpio_4 ? _zz_system_gpioA_gpio_8[3] : 1'bz;
  assign system_gpioA_gpio[4] = _zz_system_gpioA_gpio_3 ? _zz_system_gpioA_gpio_8[4] : 1'bz;
  assign system_gpioA_gpio[5] = _zz_system_gpioA_gpio_2 ? _zz_system_gpioA_gpio_8[5] : 1'bz;
  assign system_gpioA_gpio[6] = _zz_system_gpioA_gpio_1 ? _zz_system_gpioA_gpio_8[6] : 1'bz;
  assign system_gpioA_gpio[7] = _zz_system_gpioA_gpio ? _zz_system_gpioA_gpio_8[7] : 1'bz;
  always @(*) begin
    _zz_system_gpioA_gpio = 1'b0;
    if(when_InOutWrapper_l65_7) begin
      _zz_system_gpioA_gpio = 1'b1;
    end
  end

  always @(*) begin
    _zz_system_gpioA_gpio_1 = 1'b0;
    if(when_InOutWrapper_l65_6) begin
      _zz_system_gpioA_gpio_1 = 1'b1;
    end
  end

  always @(*) begin
    _zz_system_gpioA_gpio_2 = 1'b0;
    if(when_InOutWrapper_l65_5) begin
      _zz_system_gpioA_gpio_2 = 1'b1;
    end
  end

  always @(*) begin
    _zz_system_gpioA_gpio_3 = 1'b0;
    if(when_InOutWrapper_l65_4) begin
      _zz_system_gpioA_gpio_3 = 1'b1;
    end
  end

  always @(*) begin
    _zz_system_gpioA_gpio_4 = 1'b0;
    if(when_InOutWrapper_l65_3) begin
      _zz_system_gpioA_gpio_4 = 1'b1;
    end
  end

  always @(*) begin
    _zz_system_gpioA_gpio_5 = 1'b0;
    if(when_InOutWrapper_l65_2) begin
      _zz_system_gpioA_gpio_5 = 1'b1;
    end
  end

  always @(*) begin
    _zz_system_gpioA_gpio_6 = 1'b0;
    if(when_InOutWrapper_l65_1) begin
      _zz_system_gpioA_gpio_6 = 1'b1;
    end
  end

  always @(*) begin
    _zz_system_gpioA_gpio_7 = 1'b0;
    if(when_InOutWrapper_l65) begin
      _zz_system_gpioA_gpio_7 = 1'b1;
    end
  end

  always @(*) begin
    debugCdCtrl_logic_inputResetTrigger = 1'b0;
    if(debugCdCtrl_logic_inputResetAdapter_stuff_syncTrigger) begin
      debugCdCtrl_logic_inputResetTrigger = 1'b1;
    end
  end

  always @(*) begin
    debugCdCtrl_logic_outputResetUnbuffered = 1'b0;
    if(when_ClockDomainGenerator_l77) begin
      debugCdCtrl_logic_outputResetUnbuffered = 1'b1;
    end
  end

  assign when_ClockDomainGenerator_l77 = (debugCdCtrl_logic_holdingLogic_resetCounter != 12'hfff);
  assign debugCdCtrl_logic_inputResetAdapter_stuff_syncTrigger = bufferCC_5_io_dataOut;
  always @(*) begin
    systemCdCtrl_logic_inputResetTrigger = 1'b0;
    if(bufferCC_6_io_dataOut) begin
      systemCdCtrl_logic_inputResetTrigger = 1'b1;
    end
    if(bufferCC_7_io_dataOut) begin
      systemCdCtrl_logic_inputResetTrigger = 1'b1;
    end
  end

  always @(*) begin
    systemCdCtrl_logic_outputResetUnbuffered = 1'b0;
    if(when_ClockDomainGenerator_l77_1) begin
      systemCdCtrl_logic_outputResetUnbuffered = 1'b1;
    end
  end

  assign when_ClockDomainGenerator_l77_1 = (systemCdCtrl_logic_holdingLogic_resetCounter != 6'h3f);
  assign system_cpu_iBus_cmd_valid = system_cpu_logic_cpu_iBus_cmd_valid;
  assign system_cpu_iBus_cmd_payload_fragment_opcode = 1'b0;
  assign system_cpu_iBus_cmd_payload_fragment_address = system_cpu_logic_cpu_iBus_cmd_payload_pc;
  assign system_cpu_iBus_cmd_payload_fragment_length = 2'b11;
  assign system_cpu_iBus_cmd_payload_last = 1'b1;
  assign system_cpu_logic_cpu_iBus_rsp_payload_error = (system_cpu_iBus_rsp_payload_fragment_opcode == 1'b1);
  assign system_cpu_iBus_rsp_ready = 1'b1;
  assign system_cpu_dBus_cmd_valid = system_cpu_logic_cpu_dBus_cmd_valid;
  assign system_cpu_dBus_cmd_payload_last = 1'b1;
  assign system_cpu_dBus_cmd_payload_fragment_context[0] = system_cpu_logic_cpu_dBus_cmd_payload_wr;
  assign system_cpu_dBus_cmd_payload_fragment_opcode = (system_cpu_logic_cpu_dBus_cmd_payload_wr ? 1'b1 : 1'b0);
  assign system_cpu_dBus_cmd_payload_fragment_address = system_cpu_logic_cpu_dBus_cmd_payload_address;
  assign system_cpu_dBus_cmd_payload_fragment_data = system_cpu_logic_cpu_dBus_cmd_payload_data;
  always @(*) begin
    case(system_cpu_logic_cpu_dBus_cmd_payload_size)
      2'b00 : begin
        _zz_system_cpu_dBus_cmd_payload_fragment_length = 2'b00;
      end
      2'b01 : begin
        _zz_system_cpu_dBus_cmd_payload_fragment_length = 2'b01;
      end
      default : begin
        _zz_system_cpu_dBus_cmd_payload_fragment_length = 2'b11;
      end
    endcase
  end

  assign system_cpu_dBus_cmd_payload_fragment_length = _zz_system_cpu_dBus_cmd_payload_fragment_length;
  always @(*) begin
    case(system_cpu_logic_cpu_dBus_cmd_payload_size)
      2'b00 : begin
        _zz_system_cpu_dBus_cmd_payload_fragment_mask = 4'b0001;
      end
      2'b01 : begin
        _zz_system_cpu_dBus_cmd_payload_fragment_mask = 4'b0011;
      end
      default : begin
        _zz_system_cpu_dBus_cmd_payload_fragment_mask = 4'b1111;
      end
    endcase
  end

  assign system_cpu_dBus_cmd_payload_fragment_mask = (_zz_system_cpu_dBus_cmd_payload_fragment_mask <<< system_cpu_logic_cpu_dBus_cmd_payload_address[1 : 0]);
  assign system_cpu_logic_cpu_dBus_rsp_ready = (system_cpu_dBus_rsp_valid && (! system_cpu_dBus_rsp_payload_fragment_context[0]));
  assign system_cpu_logic_cpu_dBus_rsp_error = (system_cpu_dBus_rsp_payload_fragment_opcode == 1'b1);
  assign system_cpu_dBus_rsp_ready = 1'b1;
  assign system_cpu_logic_cpu_debug_bus_cmd_payload_address = systemDebugger_1_io_mem_cmd_payload_address[7:0];
  assign system_cpu_logic_cpu_debug_bus_cmd_fire = (systemDebugger_1_io_mem_cmd_valid && system_cpu_logic_cpu_debug_bus_cmd_ready);
  assign system_cpu_jtag_tdo = jtagBridge_1_io_jtag_tdo;
  assign system_cpu_iBus_cmd_ready = system_cpu_iBus_decoder_io_input_cmd_ready;
  assign system_cpu_iBus_rsp_valid = system_cpu_iBus_decoder_io_input_rsp_valid;
  assign system_cpu_iBus_rsp_payload_last = system_cpu_iBus_decoder_io_input_rsp_payload_last;
  assign system_cpu_iBus_rsp_payload_fragment_opcode = system_cpu_iBus_decoder_io_input_rsp_payload_fragment_opcode;
  assign system_cpu_iBus_rsp_payload_fragment_data = system_cpu_iBus_decoder_io_input_rsp_payload_fragment_data;
  always @(*) begin
    system_cpu_dBus_cmd_ready = system_cpu_dBus_cmd_m2sPipe_ready;
    if(when_Stream_l368) begin
      system_cpu_dBus_cmd_ready = 1'b1;
    end
  end

  assign when_Stream_l368 = (! system_cpu_dBus_cmd_m2sPipe_valid);
  assign system_cpu_dBus_cmd_m2sPipe_valid = system_cpu_dBus_cmd_rValid;
  assign system_cpu_dBus_cmd_m2sPipe_payload_last = system_cpu_dBus_cmd_rData_last;
  assign system_cpu_dBus_cmd_m2sPipe_payload_fragment_opcode = system_cpu_dBus_cmd_rData_fragment_opcode;
  assign system_cpu_dBus_cmd_m2sPipe_payload_fragment_address = system_cpu_dBus_cmd_rData_fragment_address;
  assign system_cpu_dBus_cmd_m2sPipe_payload_fragment_length = system_cpu_dBus_cmd_rData_fragment_length;
  assign system_cpu_dBus_cmd_m2sPipe_payload_fragment_data = system_cpu_dBus_cmd_rData_fragment_data;
  assign system_cpu_dBus_cmd_m2sPipe_payload_fragment_mask = system_cpu_dBus_cmd_rData_fragment_mask;
  assign system_cpu_dBus_cmd_m2sPipe_payload_fragment_context = system_cpu_dBus_cmd_rData_fragment_context;
  assign system_cpu_dBus_cmd_m2sPipe_ready = system_cpu_dBus_decoder_io_input_cmd_ready;
  assign system_cpu_dBus_rsp_valid = system_cpu_dBus_decoder_io_input_rsp_valid;
  assign system_cpu_dBus_rsp_payload_last = system_cpu_dBus_decoder_io_input_rsp_payload_last;
  assign system_cpu_dBus_rsp_payload_fragment_opcode = system_cpu_dBus_decoder_io_input_rsp_payload_fragment_opcode;
  assign system_cpu_dBus_rsp_payload_fragment_data = system_cpu_dBus_decoder_io_input_rsp_payload_fragment_data;
  assign system_cpu_dBus_rsp_payload_fragment_context = system_cpu_dBus_decoder_io_input_rsp_payload_fragment_context;
  assign system_periphBridge_bmb_cmd_valid = system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_valid;
  assign system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_ready = system_periphBridge_bmb_cmd_ready;
  assign system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_rsp_valid = system_periphBridge_bmb_rsp_valid;
  assign system_periphBridge_bmb_rsp_ready = system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_rsp_ready;
  assign system_periphBridge_bmb_cmd_payload_last = system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_last;
  assign system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_last = system_periphBridge_bmb_rsp_payload_last;
  assign system_periphBridge_bmb_cmd_payload_fragment_opcode = system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_opcode;
  assign system_periphBridge_bmb_cmd_payload_fragment_address = system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_address;
  assign system_periphBridge_bmb_cmd_payload_fragment_length = system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_length;
  assign system_periphBridge_bmb_cmd_payload_fragment_data = system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_data;
  assign system_periphBridge_bmb_cmd_payload_fragment_mask = system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_mask;
  assign system_periphBridge_bmb_cmd_payload_fragment_context = system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_context;
  assign system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_opcode = system_periphBridge_bmb_rsp_payload_fragment_opcode;
  assign system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_data = system_periphBridge_bmb_rsp_payload_fragment_data;
  assign system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_context = system_periphBridge_bmb_rsp_payload_fragment_context;
  assign system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_valid = system_cpu_dBus_decoder_io_outputs_1_cmd_valid;
  assign system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_rsp_ready = system_cpu_dBus_decoder_io_outputs_1_rsp_ready;
  assign system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_last = system_cpu_dBus_decoder_io_outputs_1_cmd_payload_last;
  assign system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_opcode = system_cpu_dBus_decoder_io_outputs_1_cmd_payload_fragment_opcode;
  assign system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_address = system_cpu_dBus_decoder_io_outputs_1_cmd_payload_fragment_address[23:0];
  assign system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_length = system_cpu_dBus_decoder_io_outputs_1_cmd_payload_fragment_length;
  assign system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_data = system_cpu_dBus_decoder_io_outputs_1_cmd_payload_fragment_data;
  assign system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_mask = system_cpu_dBus_decoder_io_outputs_1_cmd_payload_fragment_mask;
  assign system_periphBridge_bmb_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_context = system_cpu_dBus_decoder_io_outputs_1_cmd_payload_fragment_context;
  assign system_ramA_ctrl_arbiter_io_inputs_0_cmd_payload_fragment_address = system_cpu_dBus_decoder_io_outputs_0_cmd_payload_fragment_address[14:0];
  assign system_ramA_ctrl_arbiter_io_inputs_1_cmd_payload_fragment_address = system_cpu_iBus_decoder_io_outputs_0_cmd_payload_fragment_address[14:0];
  assign system_periphBridge_bmb_cmd_halfPipe_fire = (system_periphBridge_bmb_cmd_halfPipe_valid && system_periphBridge_bmb_cmd_halfPipe_ready);
  assign system_periphBridge_bmb_cmd_ready = (! system_periphBridge_bmb_cmd_rValid);
  assign system_periphBridge_bmb_cmd_halfPipe_valid = system_periphBridge_bmb_cmd_rValid;
  assign system_periphBridge_bmb_cmd_halfPipe_payload_last = system_periphBridge_bmb_cmd_rData_last;
  assign system_periphBridge_bmb_cmd_halfPipe_payload_fragment_opcode = system_periphBridge_bmb_cmd_rData_fragment_opcode;
  assign system_periphBridge_bmb_cmd_halfPipe_payload_fragment_address = system_periphBridge_bmb_cmd_rData_fragment_address;
  assign system_periphBridge_bmb_cmd_halfPipe_payload_fragment_length = system_periphBridge_bmb_cmd_rData_fragment_length;
  assign system_periphBridge_bmb_cmd_halfPipe_payload_fragment_data = system_periphBridge_bmb_cmd_rData_fragment_data;
  assign system_periphBridge_bmb_cmd_halfPipe_payload_fragment_mask = system_periphBridge_bmb_cmd_rData_fragment_mask;
  assign system_periphBridge_bmb_cmd_halfPipe_payload_fragment_context = system_periphBridge_bmb_cmd_rData_fragment_context;
  assign system_periphBridge_bmb_cmd_halfPipe_ready = system_periphBridge_bmb_decoder_io_input_cmd_ready;
  assign _zz_io_input_rsp_ready = (! _zz_system_periphBridge_bmb_rsp_valid_1);
  assign _zz_system_periphBridge_bmb_rsp_valid = _zz_system_periphBridge_bmb_rsp_valid_1;
  assign system_periphBridge_bmb_rsp_valid = _zz_system_periphBridge_bmb_rsp_valid;
  assign system_periphBridge_bmb_rsp_payload_last = _zz_system_periphBridge_bmb_rsp_payload_last;
  assign system_periphBridge_bmb_rsp_payload_fragment_opcode = _zz_system_periphBridge_bmb_rsp_payload_fragment_opcode;
  assign system_periphBridge_bmb_rsp_payload_fragment_data = _zz_system_periphBridge_bmb_rsp_payload_fragment_data;
  assign system_periphBridge_bmb_rsp_payload_fragment_context = _zz_system_periphBridge_bmb_rsp_payload_fragment_context;
  assign _zz_system_gpioA_gpio_8 = system_gpioA_logic_io_gpio_write;
  assign _zz_when_InOutWrapper_l65 = system_gpioA_logic_io_gpio_writeEnable;
  assign system_gpioA_interrupts_0 = system_gpioA_logic_io_interrupt[0];
  assign system_gpioA_interrupts_1 = system_gpioA_logic_io_interrupt[1];
  assign system_gpioA_interrupts_2 = system_gpioA_logic_io_interrupt[2];
  assign system_gpioA_interrupts_3 = system_gpioA_logic_io_interrupt[3];
  assign system_gpioA_interrupts_4 = system_gpioA_logic_io_interrupt[4];
  assign system_gpioA_interrupts_5 = system_gpioA_logic_io_interrupt[5];
  assign system_gpioA_interrupts_6 = system_gpioA_logic_io_interrupt[6];
  assign system_gpioA_interrupts_7 = system_gpioA_logic_io_interrupt[7];
  assign system_uartA_uart_txd = system_uartA_logic_io_uart_txd;
  assign system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_ready = system_clint_logic_io_bus_cmd_ready;
  assign system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_valid = system_clint_logic_io_bus_rsp_valid;
  assign system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_last = system_clint_logic_io_bus_rsp_payload_last;
  assign system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_opcode = system_clint_logic_io_bus_rsp_payload_fragment_opcode;
  assign system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_data = system_clint_logic_io_bus_rsp_payload_fragment_data;
  assign system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_context = system_clint_logic_io_bus_rsp_payload_fragment_context;
  assign system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_ready = system_gpioA_logic_io_bus_cmd_ready;
  assign system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_valid = system_gpioA_logic_io_bus_rsp_valid;
  assign system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_last = system_gpioA_logic_io_bus_rsp_payload_last;
  assign system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_opcode = system_gpioA_logic_io_bus_rsp_payload_fragment_opcode;
  assign system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_data = system_gpioA_logic_io_bus_rsp_payload_fragment_data;
  assign system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_context = system_gpioA_logic_io_bus_rsp_payload_fragment_context;
  assign system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_ready = system_uartA_logic_io_bus_cmd_ready;
  assign system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_valid = system_uartA_logic_io_bus_rsp_valid;
  assign system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_last = system_uartA_logic_io_bus_rsp_payload_last;
  assign system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_opcode = system_uartA_logic_io_bus_rsp_payload_fragment_opcode;
  assign system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_data = system_uartA_logic_io_bus_rsp_payload_fragment_data;
  assign system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_context = system_uartA_logic_io_bus_rsp_payload_fragment_context;
  assign system_periphBridge_bmb_withoutMask_cmd_valid = system_periphBridge_bmb_decoder_io_outputs_0_cmd_valid;
  assign system_periphBridge_bmb_withoutMask_rsp_ready = system_periphBridge_bmb_decoder_io_outputs_0_rsp_ready;
  assign system_periphBridge_bmb_withoutMask_cmd_payload_last = system_periphBridge_bmb_decoder_io_outputs_0_cmd_payload_last;
  assign system_periphBridge_bmb_withoutMask_cmd_payload_fragment_opcode = system_periphBridge_bmb_decoder_io_outputs_0_cmd_payload_fragment_opcode;
  assign system_periphBridge_bmb_withoutMask_cmd_payload_fragment_address = system_periphBridge_bmb_decoder_io_outputs_0_cmd_payload_fragment_address;
  assign system_periphBridge_bmb_withoutMask_cmd_payload_fragment_length = system_periphBridge_bmb_decoder_io_outputs_0_cmd_payload_fragment_length;
  assign system_periphBridge_bmb_withoutMask_cmd_payload_fragment_data = system_periphBridge_bmb_decoder_io_outputs_0_cmd_payload_fragment_data;
  assign system_periphBridge_bmb_withoutMask_cmd_payload_fragment_context = system_periphBridge_bmb_decoder_io_outputs_0_cmd_payload_fragment_context;
  assign system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_valid = system_periphBridge_bmb_withoutMask_cmd_valid;
  assign system_periphBridge_bmb_withoutMask_cmd_ready = system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_ready;
  assign system_periphBridge_bmb_withoutMask_rsp_valid = system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_valid;
  assign system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_ready = system_periphBridge_bmb_withoutMask_rsp_ready;
  assign system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_last = system_periphBridge_bmb_withoutMask_cmd_payload_last;
  assign system_periphBridge_bmb_withoutMask_rsp_payload_last = system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_last;
  assign system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_opcode = system_periphBridge_bmb_withoutMask_cmd_payload_fragment_opcode;
  assign system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_address = system_periphBridge_bmb_withoutMask_cmd_payload_fragment_address[15:0];
  assign system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_length = system_periphBridge_bmb_withoutMask_cmd_payload_fragment_length;
  assign system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_data = system_periphBridge_bmb_withoutMask_cmd_payload_fragment_data;
  assign system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_context = system_periphBridge_bmb_withoutMask_cmd_payload_fragment_context;
  assign system_periphBridge_bmb_withoutMask_rsp_payload_fragment_opcode = system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_opcode;
  assign system_periphBridge_bmb_withoutMask_rsp_payload_fragment_data = system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_data;
  assign system_periphBridge_bmb_withoutMask_rsp_payload_fragment_context = system_clint_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_context;
  assign system_periphBridge_bmb_withoutMask_cmd_valid_1 = system_periphBridge_bmb_decoder_io_outputs_1_cmd_valid;
  assign system_periphBridge_bmb_withoutMask_rsp_ready_1 = system_periphBridge_bmb_decoder_io_outputs_1_rsp_ready;
  assign system_periphBridge_bmb_withoutMask_cmd_payload_last_1 = system_periphBridge_bmb_decoder_io_outputs_1_cmd_payload_last;
  assign system_periphBridge_bmb_withoutMask_cmd_payload_fragment_opcode_1 = system_periphBridge_bmb_decoder_io_outputs_1_cmd_payload_fragment_opcode;
  assign system_periphBridge_bmb_withoutMask_cmd_payload_fragment_address_1 = system_periphBridge_bmb_decoder_io_outputs_1_cmd_payload_fragment_address;
  assign system_periphBridge_bmb_withoutMask_cmd_payload_fragment_length_1 = system_periphBridge_bmb_decoder_io_outputs_1_cmd_payload_fragment_length;
  assign system_periphBridge_bmb_withoutMask_cmd_payload_fragment_data_1 = system_periphBridge_bmb_decoder_io_outputs_1_cmd_payload_fragment_data;
  assign system_periphBridge_bmb_withoutMask_cmd_payload_fragment_context_1 = system_periphBridge_bmb_decoder_io_outputs_1_cmd_payload_fragment_context;
  assign system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_valid = system_periphBridge_bmb_withoutMask_cmd_valid_1;
  assign system_periphBridge_bmb_withoutMask_cmd_ready_1 = system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_ready;
  assign system_periphBridge_bmb_withoutMask_rsp_valid_1 = system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_valid;
  assign system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_ready = system_periphBridge_bmb_withoutMask_rsp_ready_1;
  assign system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_last = system_periphBridge_bmb_withoutMask_cmd_payload_last_1;
  assign system_periphBridge_bmb_withoutMask_rsp_payload_last_1 = system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_last;
  assign system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_opcode = system_periphBridge_bmb_withoutMask_cmd_payload_fragment_opcode_1;
  assign system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_address = system_periphBridge_bmb_withoutMask_cmd_payload_fragment_address_1[7:0];
  assign system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_length = system_periphBridge_bmb_withoutMask_cmd_payload_fragment_length_1;
  assign system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_data = system_periphBridge_bmb_withoutMask_cmd_payload_fragment_data_1;
  assign system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_context = system_periphBridge_bmb_withoutMask_cmd_payload_fragment_context_1;
  assign system_periphBridge_bmb_withoutMask_rsp_payload_fragment_opcode_1 = system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_opcode;
  assign system_periphBridge_bmb_withoutMask_rsp_payload_fragment_data_1 = system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_data;
  assign system_periphBridge_bmb_withoutMask_rsp_payload_fragment_context_1 = system_gpioA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_context;
  assign system_periphBridge_bmb_withoutMask_cmd_valid_2 = system_periphBridge_bmb_decoder_io_outputs_2_cmd_valid;
  assign system_periphBridge_bmb_withoutMask_rsp_ready_2 = system_periphBridge_bmb_decoder_io_outputs_2_rsp_ready;
  assign system_periphBridge_bmb_withoutMask_cmd_payload_last_2 = system_periphBridge_bmb_decoder_io_outputs_2_cmd_payload_last;
  assign system_periphBridge_bmb_withoutMask_cmd_payload_fragment_opcode_2 = system_periphBridge_bmb_decoder_io_outputs_2_cmd_payload_fragment_opcode;
  assign system_periphBridge_bmb_withoutMask_cmd_payload_fragment_address_2 = system_periphBridge_bmb_decoder_io_outputs_2_cmd_payload_fragment_address;
  assign system_periphBridge_bmb_withoutMask_cmd_payload_fragment_length_2 = system_periphBridge_bmb_decoder_io_outputs_2_cmd_payload_fragment_length;
  assign system_periphBridge_bmb_withoutMask_cmd_payload_fragment_data_2 = system_periphBridge_bmb_decoder_io_outputs_2_cmd_payload_fragment_data;
  assign system_periphBridge_bmb_withoutMask_cmd_payload_fragment_context_2 = system_periphBridge_bmb_decoder_io_outputs_2_cmd_payload_fragment_context;
  assign system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_valid = system_periphBridge_bmb_withoutMask_cmd_valid_2;
  assign system_periphBridge_bmb_withoutMask_cmd_ready_2 = system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_ready;
  assign system_periphBridge_bmb_withoutMask_rsp_valid_2 = system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_valid;
  assign system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_ready = system_periphBridge_bmb_withoutMask_rsp_ready_2;
  assign system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_last = system_periphBridge_bmb_withoutMask_cmd_payload_last_2;
  assign system_periphBridge_bmb_withoutMask_rsp_payload_last_2 = system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_last;
  assign system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_opcode = system_periphBridge_bmb_withoutMask_cmd_payload_fragment_opcode_2;
  assign system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_address = system_periphBridge_bmb_withoutMask_cmd_payload_fragment_address_2[5:0];
  assign system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_length = system_periphBridge_bmb_withoutMask_cmd_payload_fragment_length_2;
  assign system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_data = system_periphBridge_bmb_withoutMask_cmd_payload_fragment_data_2;
  assign system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_cmd_payload_fragment_context = system_periphBridge_bmb_withoutMask_cmd_payload_fragment_context_2;
  assign system_periphBridge_bmb_withoutMask_rsp_payload_fragment_opcode_2 = system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_opcode;
  assign system_periphBridge_bmb_withoutMask_rsp_payload_fragment_data_2 = system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_data;
  assign system_periphBridge_bmb_withoutMask_rsp_payload_fragment_context_2 = system_uartA_ctrl_slaveModel_arbiterGen_oneToOne_arbiter_rsp_payload_fragment_context;
  assign _zz_io_gpio_read = system_gpioA_gpio;
  assign when_InOutWrapper_l65 = _zz_when_InOutWrapper_l65[0];
  assign when_InOutWrapper_l65_1 = _zz_when_InOutWrapper_l65[1];
  assign when_InOutWrapper_l65_2 = _zz_when_InOutWrapper_l65[2];
  assign when_InOutWrapper_l65_3 = _zz_when_InOutWrapper_l65[3];
  assign when_InOutWrapper_l65_4 = _zz_when_InOutWrapper_l65[4];
  assign when_InOutWrapper_l65_5 = _zz_when_InOutWrapper_l65[5];
  assign when_InOutWrapper_l65_6 = _zz_when_InOutWrapper_l65[6];
  assign when_InOutWrapper_l65_7 = _zz_when_InOutWrapper_l65[7];
  always @(posedge debugCdCtrl_external_clk) begin
    if(when_ClockDomainGenerator_l77) begin
      debugCdCtrl_logic_holdingLogic_resetCounter <= (debugCdCtrl_logic_holdingLogic_resetCounter + 12'h001);
    end
    if(debugCdCtrl_logic_inputResetTrigger) begin
      debugCdCtrl_logic_holdingLogic_resetCounter <= 12'h0;
    end
    debugCdCtrl_logic_outputReset <= debugCdCtrl_logic_outputResetUnbuffered;
  end

  always @(posedge debugCdCtrl_external_clk) begin
    if(when_ClockDomainGenerator_l77_1) begin
      systemCdCtrl_logic_holdingLogic_resetCounter <= (systemCdCtrl_logic_holdingLogic_resetCounter + 6'h01);
    end
    if(systemCdCtrl_logic_inputResetTrigger) begin
      systemCdCtrl_logic_holdingLogic_resetCounter <= 6'h0;
    end
    systemCdCtrl_logic_outputReset <= systemCdCtrl_logic_outputResetUnbuffered;
  end

  always @(posedge debugCdCtrl_external_clk) begin
    system_cpu_debugReset <= system_cpu_logic_cpu_debug_resetOut;
  end

  always @(posedge debugCdCtrl_external_clk) begin
    if(debugCdCtrl_logic_outputReset) begin
      system_cpu_logic_cpu_debug_bus_cmd_fire_regNext <= 1'b0;
    end else begin
      system_cpu_logic_cpu_debug_bus_cmd_fire_regNext <= system_cpu_logic_cpu_debug_bus_cmd_fire;
    end
  end

  always @(posedge debugCdCtrl_external_clk) begin
    if(systemCdCtrl_logic_outputReset) begin
      system_cpu_dBus_cmd_rValid <= 1'b0;
      system_periphBridge_bmb_cmd_rValid <= 1'b0;
      _zz_system_periphBridge_bmb_rsp_valid_1 <= 1'b0;
    end else begin
      if(system_cpu_dBus_cmd_ready) begin
        system_cpu_dBus_cmd_rValid <= system_cpu_dBus_cmd_valid;
      end
      if(system_periphBridge_bmb_cmd_valid) begin
        system_periphBridge_bmb_cmd_rValid <= 1'b1;
      end
      if(system_periphBridge_bmb_cmd_halfPipe_fire) begin
        system_periphBridge_bmb_cmd_rValid <= 1'b0;
      end
      if(system_periphBridge_bmb_decoder_io_input_rsp_valid) begin
        _zz_system_periphBridge_bmb_rsp_valid_1 <= 1'b1;
      end
      if((_zz_system_periphBridge_bmb_rsp_valid && system_periphBridge_bmb_rsp_ready)) begin
        _zz_system_periphBridge_bmb_rsp_valid_1 <= 1'b0;
      end
    end
  end

  always @(posedge debugCdCtrl_external_clk) begin
    if(system_cpu_dBus_cmd_ready) begin
      system_cpu_dBus_cmd_rData_last <= system_cpu_dBus_cmd_payload_last;
      system_cpu_dBus_cmd_rData_fragment_opcode <= system_cpu_dBus_cmd_payload_fragment_opcode;
      system_cpu_dBus_cmd_rData_fragment_address <= system_cpu_dBus_cmd_payload_fragment_address;
      system_cpu_dBus_cmd_rData_fragment_length <= system_cpu_dBus_cmd_payload_fragment_length;
      system_cpu_dBus_cmd_rData_fragment_data <= system_cpu_dBus_cmd_payload_fragment_data;
      system_cpu_dBus_cmd_rData_fragment_mask <= system_cpu_dBus_cmd_payload_fragment_mask;
      system_cpu_dBus_cmd_rData_fragment_context <= system_cpu_dBus_cmd_payload_fragment_context;
    end
    if(system_periphBridge_bmb_cmd_ready) begin
      system_periphBridge_bmb_cmd_rData_last <= system_periphBridge_bmb_cmd_payload_last;
      system_periphBridge_bmb_cmd_rData_fragment_opcode <= system_periphBridge_bmb_cmd_payload_fragment_opcode;
      system_periphBridge_bmb_cmd_rData_fragment_address <= system_periphBridge_bmb_cmd_payload_fragment_address;
      system_periphBridge_bmb_cmd_rData_fragment_length <= system_periphBridge_bmb_cmd_payload_fragment_length;
      system_periphBridge_bmb_cmd_rData_fragment_data <= system_periphBridge_bmb_cmd_payload_fragment_data;
      system_periphBridge_bmb_cmd_rData_fragment_mask <= system_periphBridge_bmb_cmd_payload_fragment_mask;
      system_periphBridge_bmb_cmd_rData_fragment_context <= system_periphBridge_bmb_cmd_payload_fragment_context;
    end
    if(_zz_io_input_rsp_ready) begin
      _zz_system_periphBridge_bmb_rsp_payload_last <= system_periphBridge_bmb_decoder_io_input_rsp_payload_last;
      _zz_system_periphBridge_bmb_rsp_payload_fragment_opcode <= system_periphBridge_bmb_decoder_io_input_rsp_payload_fragment_opcode;
      _zz_system_periphBridge_bmb_rsp_payload_fragment_data <= system_periphBridge_bmb_decoder_io_input_rsp_payload_fragment_data;
      _zz_system_periphBridge_bmb_rsp_payload_fragment_context <= system_periphBridge_bmb_decoder_io_input_rsp_payload_fragment_context;
    end
  end


endmodule

module BmbUartCtrl (
  input               io_bus_cmd_valid,
  output              io_bus_cmd_ready,
  input               io_bus_cmd_payload_last,
  input      [0:0]    io_bus_cmd_payload_fragment_opcode,
  input      [5:0]    io_bus_cmd_payload_fragment_address,
  input      [1:0]    io_bus_cmd_payload_fragment_length,
  input      [31:0]   io_bus_cmd_payload_fragment_data,
  input      [0:0]    io_bus_cmd_payload_fragment_context,
  output              io_bus_rsp_valid,
  input               io_bus_rsp_ready,
  output              io_bus_rsp_payload_last,
  output     [0:0]    io_bus_rsp_payload_fragment_opcode,
  output     [31:0]   io_bus_rsp_payload_fragment_data,
  output     [0:0]    io_bus_rsp_payload_fragment_context,
  output              io_uart_txd,
  input               io_uart_rxd,
  output              io_interrupt,
  input               debugCdCtrl_external_clk,
  input               systemCdCtrl_logic_outputReset
);
  localparam UartStopType_ONE = 1'd0;
  localparam UartStopType_TWO = 1'd1;
  localparam UartParityType_NONE = 2'd0;
  localparam UartParityType_EVEN = 2'd1;
  localparam UartParityType_ODD = 2'd2;

  reg                 uartCtrl_1_io_read_queueWithOccupancy_io_pop_ready;
  wire                uartCtrl_1_io_write_ready;
  wire                uartCtrl_1_io_read_valid;
  wire       [7:0]    uartCtrl_1_io_read_payload;
  wire                uartCtrl_1_io_uart_txd;
  wire                uartCtrl_1_io_readError;
  wire                uartCtrl_1_io_readBreak;
  wire                bridge_write_streamUnbuffered_queueWithOccupancy_io_push_ready;
  wire                bridge_write_streamUnbuffered_queueWithOccupancy_io_pop_valid;
  wire       [7:0]    bridge_write_streamUnbuffered_queueWithOccupancy_io_pop_payload;
  wire       [0:0]    bridge_write_streamUnbuffered_queueWithOccupancy_io_occupancy;
  wire       [0:0]    bridge_write_streamUnbuffered_queueWithOccupancy_io_availability;
  wire                uartCtrl_1_io_read_queueWithOccupancy_io_push_ready;
  wire                uartCtrl_1_io_read_queueWithOccupancy_io_pop_valid;
  wire       [7:0]    uartCtrl_1_io_read_queueWithOccupancy_io_pop_payload;
  wire       [0:0]    uartCtrl_1_io_read_queueWithOccupancy_io_occupancy;
  wire       [0:0]    uartCtrl_1_io_read_queueWithOccupancy_io_availability;
  wire       [0:0]    _zz_bridge_misc_readError;
  wire       [0:0]    _zz_bridge_misc_readOverflowError;
  wire       [0:0]    _zz_bridge_misc_breakDetected;
  wire       [0:0]    _zz_bridge_misc_doBreak;
  wire       [0:0]    _zz_bridge_misc_doBreak_1;
  wire       [0:0]    _zz_busCtrl_rsp_payload_fragment_data;
  wire                busCtrl_readHaltTrigger;
  wire                busCtrl_writeHaltTrigger;
  wire                busCtrl_rsp_valid;
  wire                busCtrl_rsp_ready;
  wire                busCtrl_rsp_payload_last;
  wire       [0:0]    busCtrl_rsp_payload_fragment_opcode;
  reg        [31:0]   busCtrl_rsp_payload_fragment_data;
  wire       [0:0]    busCtrl_rsp_payload_fragment_context;
  wire                _zz_io_bus_rsp_valid;
  reg                 _zz_busCtrl_rsp_ready;
  wire                _zz_io_bus_rsp_valid_1;
  reg                 _zz_io_bus_rsp_valid_2;
  reg                 _zz_io_bus_rsp_payload_last;
  reg        [0:0]    _zz_io_bus_rsp_payload_fragment_opcode;
  reg        [31:0]   _zz_io_bus_rsp_payload_fragment_data;
  reg        [0:0]    _zz_io_bus_rsp_payload_fragment_context;
  wire                when_Stream_l368;
  wire                busCtrl_askWrite;
  wire                busCtrl_askRead;
  wire                io_bus_cmd_fire;
  wire                busCtrl_doWrite;
  wire                io_bus_cmd_fire_1;
  wire                busCtrl_doRead;
  wire       [2:0]    bridge_uartConfigReg_frame_dataLength;
  wire       [0:0]    bridge_uartConfigReg_frame_stop;
  wire       [1:0]    bridge_uartConfigReg_frame_parity;
  reg        [11:0]   bridge_uartConfigReg_clockDivider;
  reg                 _zz_bridge_write_streamUnbuffered_valid;
  wire                bridge_write_streamUnbuffered_valid;
  wire                bridge_write_streamUnbuffered_ready;
  wire       [7:0]    bridge_write_streamUnbuffered_payload;
  reg                 bridge_read_streamBreaked_valid;
  reg                 bridge_read_streamBreaked_ready;
  wire       [7:0]    bridge_read_streamBreaked_payload;
  reg                 bridge_interruptCtrl_writeIntEnable;
  reg                 bridge_interruptCtrl_readIntEnable;
  wire                bridge_interruptCtrl_readInt;
  wire                bridge_interruptCtrl_writeInt;
  wire                bridge_interruptCtrl_interrupt;
  reg                 bridge_misc_readError;
  reg                 when_BusSlaveFactory_l335;
  wire                when_BusSlaveFactory_l341;
  reg                 bridge_misc_readOverflowError;
  reg                 when_BusSlaveFactory_l335_1;
  wire                when_BusSlaveFactory_l341_1;
  wire                uartCtrl_1_io_read_isStall;
  reg                 bridge_misc_breakDetected;
  reg                 uartCtrl_1_io_readBreak_regNext;
  wire                when_UartCtrl_l155;
  reg                 when_BusSlaveFactory_l335_2;
  wire                when_BusSlaveFactory_l341_2;
  reg                 bridge_misc_doBreak;
  reg                 when_BusSlaveFactory_l371;
  wire                when_BusSlaveFactory_l373;
  reg                 when_BusSlaveFactory_l335_3;
  wire                when_BusSlaveFactory_l341_3;
  `ifndef SYNTHESIS
  reg [23:0] bridge_uartConfigReg_frame_stop_string;
  reg [31:0] bridge_uartConfigReg_frame_parity_string;
  `endif

  function [11:0] zz_bridge_uartConfigReg_clockDivider(input dummy);
    begin
      zz_bridge_uartConfigReg_clockDivider = 12'h0;
      zz_bridge_uartConfigReg_clockDivider = 12'h02a;
    end
  endfunction
  wire [11:0] _zz_1;

  assign _zz_bridge_misc_readError = 1'b0;
  assign _zz_bridge_misc_readOverflowError = 1'b0;
  assign _zz_bridge_misc_breakDetected = 1'b0;
  assign _zz_bridge_misc_doBreak = 1'b1;
  assign _zz_bridge_misc_doBreak_1 = 1'b0;
  assign _zz_busCtrl_rsp_payload_fragment_data = (1'b1 - bridge_write_streamUnbuffered_queueWithOccupancy_io_occupancy);
  UartCtrl uartCtrl_1 (
    .io_config_frame_dataLength     (bridge_uartConfigReg_frame_dataLength[2:0]                          ), //i
    .io_config_frame_stop           (bridge_uartConfigReg_frame_stop                                     ), //i
    .io_config_frame_parity         (bridge_uartConfigReg_frame_parity[1:0]                              ), //i
    .io_config_clockDivider         (bridge_uartConfigReg_clockDivider[11:0]                             ), //i
    .io_write_valid                 (bridge_write_streamUnbuffered_queueWithOccupancy_io_pop_valid       ), //i
    .io_write_ready                 (uartCtrl_1_io_write_ready                                           ), //o
    .io_write_payload               (bridge_write_streamUnbuffered_queueWithOccupancy_io_pop_payload[7:0]), //i
    .io_read_valid                  (uartCtrl_1_io_read_valid                                            ), //o
    .io_read_ready                  (uartCtrl_1_io_read_queueWithOccupancy_io_push_ready                 ), //i
    .io_read_payload                (uartCtrl_1_io_read_payload[7:0]                                     ), //o
    .io_uart_txd                    (uartCtrl_1_io_uart_txd                                              ), //o
    .io_uart_rxd                    (io_uart_rxd                                                         ), //i
    .io_readError                   (uartCtrl_1_io_readError                                             ), //o
    .io_writeBreak                  (bridge_misc_doBreak                                                 ), //i
    .io_readBreak                   (uartCtrl_1_io_readBreak                                             ), //o
    .debugCdCtrl_external_clk       (debugCdCtrl_external_clk                                            ), //i
    .systemCdCtrl_logic_outputReset (systemCdCtrl_logic_outputReset                                      )  //i
  );
  StreamFifo bridge_write_streamUnbuffered_queueWithOccupancy (
    .io_push_valid                  (bridge_write_streamUnbuffered_valid                                 ), //i
    .io_push_ready                  (bridge_write_streamUnbuffered_queueWithOccupancy_io_push_ready      ), //o
    .io_push_payload                (bridge_write_streamUnbuffered_payload[7:0]                          ), //i
    .io_pop_valid                   (bridge_write_streamUnbuffered_queueWithOccupancy_io_pop_valid       ), //o
    .io_pop_ready                   (uartCtrl_1_io_write_ready                                           ), //i
    .io_pop_payload                 (bridge_write_streamUnbuffered_queueWithOccupancy_io_pop_payload[7:0]), //o
    .io_flush                       (1'b0                                                                ), //i
    .io_occupancy                   (bridge_write_streamUnbuffered_queueWithOccupancy_io_occupancy       ), //o
    .io_availability                (bridge_write_streamUnbuffered_queueWithOccupancy_io_availability    ), //o
    .debugCdCtrl_external_clk       (debugCdCtrl_external_clk                                            ), //i
    .systemCdCtrl_logic_outputReset (systemCdCtrl_logic_outputReset                                      )  //i
  );
  StreamFifo uartCtrl_1_io_read_queueWithOccupancy (
    .io_push_valid                  (uartCtrl_1_io_read_valid                                 ), //i
    .io_push_ready                  (uartCtrl_1_io_read_queueWithOccupancy_io_push_ready      ), //o
    .io_push_payload                (uartCtrl_1_io_read_payload[7:0]                          ), //i
    .io_pop_valid                   (uartCtrl_1_io_read_queueWithOccupancy_io_pop_valid       ), //o
    .io_pop_ready                   (uartCtrl_1_io_read_queueWithOccupancy_io_pop_ready       ), //i
    .io_pop_payload                 (uartCtrl_1_io_read_queueWithOccupancy_io_pop_payload[7:0]), //o
    .io_flush                       (1'b0                                                     ), //i
    .io_occupancy                   (uartCtrl_1_io_read_queueWithOccupancy_io_occupancy       ), //o
    .io_availability                (uartCtrl_1_io_read_queueWithOccupancy_io_availability    ), //o
    .debugCdCtrl_external_clk       (debugCdCtrl_external_clk                                 ), //i
    .systemCdCtrl_logic_outputReset (systemCdCtrl_logic_outputReset                           )  //i
  );
  `ifndef SYNTHESIS
  always @(*) begin
    case(bridge_uartConfigReg_frame_stop)
      UartStopType_ONE : bridge_uartConfigReg_frame_stop_string = "ONE";
      UartStopType_TWO : bridge_uartConfigReg_frame_stop_string = "TWO";
      default : bridge_uartConfigReg_frame_stop_string = "???";
    endcase
  end
  always @(*) begin
    case(bridge_uartConfigReg_frame_parity)
      UartParityType_NONE : bridge_uartConfigReg_frame_parity_string = "NONE";
      UartParityType_EVEN : bridge_uartConfigReg_frame_parity_string = "EVEN";
      UartParityType_ODD : bridge_uartConfigReg_frame_parity_string = "ODD ";
      default : bridge_uartConfigReg_frame_parity_string = "????";
    endcase
  end
  `endif

  assign io_uart_txd = uartCtrl_1_io_uart_txd;
  assign busCtrl_readHaltTrigger = 1'b0;
  assign busCtrl_writeHaltTrigger = 1'b0;
  assign _zz_io_bus_rsp_valid = (! (busCtrl_readHaltTrigger || busCtrl_writeHaltTrigger));
  assign busCtrl_rsp_ready = (_zz_busCtrl_rsp_ready && _zz_io_bus_rsp_valid);
  always @(*) begin
    _zz_busCtrl_rsp_ready = io_bus_rsp_ready;
    if(when_Stream_l368) begin
      _zz_busCtrl_rsp_ready = 1'b1;
    end
  end

  assign when_Stream_l368 = (! _zz_io_bus_rsp_valid_1);
  assign _zz_io_bus_rsp_valid_1 = _zz_io_bus_rsp_valid_2;
  assign io_bus_rsp_valid = _zz_io_bus_rsp_valid_1;
  assign io_bus_rsp_payload_last = _zz_io_bus_rsp_payload_last;
  assign io_bus_rsp_payload_fragment_opcode = _zz_io_bus_rsp_payload_fragment_opcode;
  assign io_bus_rsp_payload_fragment_data = _zz_io_bus_rsp_payload_fragment_data;
  assign io_bus_rsp_payload_fragment_context = _zz_io_bus_rsp_payload_fragment_context;
  assign busCtrl_askWrite = (io_bus_cmd_valid && (io_bus_cmd_payload_fragment_opcode == 1'b1));
  assign busCtrl_askRead = (io_bus_cmd_valid && (io_bus_cmd_payload_fragment_opcode == 1'b0));
  assign io_bus_cmd_fire = (io_bus_cmd_valid && io_bus_cmd_ready);
  assign busCtrl_doWrite = (io_bus_cmd_fire && (io_bus_cmd_payload_fragment_opcode == 1'b1));
  assign io_bus_cmd_fire_1 = (io_bus_cmd_valid && io_bus_cmd_ready);
  assign busCtrl_doRead = (io_bus_cmd_fire_1 && (io_bus_cmd_payload_fragment_opcode == 1'b0));
  assign busCtrl_rsp_valid = io_bus_cmd_valid;
  assign io_bus_cmd_ready = busCtrl_rsp_ready;
  assign busCtrl_rsp_payload_last = 1'b1;
  assign busCtrl_rsp_payload_fragment_opcode = 1'b0;
  always @(*) begin
    busCtrl_rsp_payload_fragment_data = 32'h0;
    case(io_bus_cmd_payload_fragment_address)
      6'h0 : begin
        busCtrl_rsp_payload_fragment_data[16 : 16] = (bridge_read_streamBreaked_valid ^ 1'b0);
        busCtrl_rsp_payload_fragment_data[7 : 0] = bridge_read_streamBreaked_payload;
      end
      6'h04 : begin
        busCtrl_rsp_payload_fragment_data[16 : 16] = _zz_busCtrl_rsp_payload_fragment_data;
        busCtrl_rsp_payload_fragment_data[15 : 15] = bridge_write_streamUnbuffered_queueWithOccupancy_io_pop_valid;
        busCtrl_rsp_payload_fragment_data[24 : 24] = uartCtrl_1_io_read_queueWithOccupancy_io_occupancy;
        busCtrl_rsp_payload_fragment_data[0 : 0] = bridge_interruptCtrl_writeIntEnable;
        busCtrl_rsp_payload_fragment_data[1 : 1] = bridge_interruptCtrl_readIntEnable;
        busCtrl_rsp_payload_fragment_data[8 : 8] = bridge_interruptCtrl_writeInt;
        busCtrl_rsp_payload_fragment_data[9 : 9] = bridge_interruptCtrl_readInt;
      end
      6'h10 : begin
        busCtrl_rsp_payload_fragment_data[0 : 0] = bridge_misc_readError;
        busCtrl_rsp_payload_fragment_data[1 : 1] = bridge_misc_readOverflowError;
        busCtrl_rsp_payload_fragment_data[8 : 8] = uartCtrl_1_io_readBreak;
        busCtrl_rsp_payload_fragment_data[9 : 9] = bridge_misc_breakDetected;
      end
      default : begin
      end
    endcase
  end

  assign busCtrl_rsp_payload_fragment_context = io_bus_cmd_payload_fragment_context;
  assign _zz_1 = zz_bridge_uartConfigReg_clockDivider(1'b0);
  always @(*) bridge_uartConfigReg_clockDivider = _zz_1;
  assign bridge_uartConfigReg_frame_dataLength = 3'b111;
  assign bridge_uartConfigReg_frame_parity = UartParityType_NONE;
  assign bridge_uartConfigReg_frame_stop = UartStopType_ONE;
  always @(*) begin
    _zz_bridge_write_streamUnbuffered_valid = 1'b0;
    case(io_bus_cmd_payload_fragment_address)
      6'h0 : begin
        if(busCtrl_doWrite) begin
          _zz_bridge_write_streamUnbuffered_valid = 1'b1;
        end
      end
      default : begin
      end
    endcase
  end

  assign bridge_write_streamUnbuffered_valid = _zz_bridge_write_streamUnbuffered_valid;
  assign bridge_write_streamUnbuffered_payload = io_bus_cmd_payload_fragment_data[7 : 0];
  assign bridge_write_streamUnbuffered_ready = bridge_write_streamUnbuffered_queueWithOccupancy_io_push_ready;
  always @(*) begin
    bridge_read_streamBreaked_valid = uartCtrl_1_io_read_queueWithOccupancy_io_pop_valid;
    if(uartCtrl_1_io_readBreak) begin
      bridge_read_streamBreaked_valid = 1'b0;
    end
  end

  always @(*) begin
    uartCtrl_1_io_read_queueWithOccupancy_io_pop_ready = bridge_read_streamBreaked_ready;
    if(uartCtrl_1_io_readBreak) begin
      uartCtrl_1_io_read_queueWithOccupancy_io_pop_ready = 1'b1;
    end
  end

  assign bridge_read_streamBreaked_payload = uartCtrl_1_io_read_queueWithOccupancy_io_pop_payload;
  always @(*) begin
    bridge_read_streamBreaked_ready = 1'b0;
    case(io_bus_cmd_payload_fragment_address)
      6'h0 : begin
        if(busCtrl_doRead) begin
          bridge_read_streamBreaked_ready = 1'b1;
        end
      end
      default : begin
      end
    endcase
  end

  assign bridge_interruptCtrl_readInt = (bridge_interruptCtrl_readIntEnable && bridge_read_streamBreaked_valid);
  assign bridge_interruptCtrl_writeInt = (bridge_interruptCtrl_writeIntEnable && (! bridge_write_streamUnbuffered_queueWithOccupancy_io_pop_valid));
  assign bridge_interruptCtrl_interrupt = (bridge_interruptCtrl_readInt || bridge_interruptCtrl_writeInt);
  always @(*) begin
    when_BusSlaveFactory_l335 = 1'b0;
    case(io_bus_cmd_payload_fragment_address)
      6'h10 : begin
        if(busCtrl_doWrite) begin
          when_BusSlaveFactory_l335 = 1'b1;
        end
      end
      default : begin
      end
    endcase
  end

  assign when_BusSlaveFactory_l341 = io_bus_cmd_payload_fragment_data[0];
  always @(*) begin
    when_BusSlaveFactory_l335_1 = 1'b0;
    case(io_bus_cmd_payload_fragment_address)
      6'h10 : begin
        if(busCtrl_doWrite) begin
          when_BusSlaveFactory_l335_1 = 1'b1;
        end
      end
      default : begin
      end
    endcase
  end

  assign when_BusSlaveFactory_l341_1 = io_bus_cmd_payload_fragment_data[1];
  assign uartCtrl_1_io_read_isStall = (uartCtrl_1_io_read_valid && (! uartCtrl_1_io_read_queueWithOccupancy_io_push_ready));
  assign when_UartCtrl_l155 = (uartCtrl_1_io_readBreak && (! uartCtrl_1_io_readBreak_regNext));
  always @(*) begin
    when_BusSlaveFactory_l335_2 = 1'b0;
    case(io_bus_cmd_payload_fragment_address)
      6'h10 : begin
        if(busCtrl_doWrite) begin
          when_BusSlaveFactory_l335_2 = 1'b1;
        end
      end
      default : begin
      end
    endcase
  end

  assign when_BusSlaveFactory_l341_2 = io_bus_cmd_payload_fragment_data[9];
  always @(*) begin
    when_BusSlaveFactory_l371 = 1'b0;
    case(io_bus_cmd_payload_fragment_address)
      6'h10 : begin
        if(busCtrl_doWrite) begin
          when_BusSlaveFactory_l371 = 1'b1;
        end
      end
      default : begin
      end
    endcase
  end

  assign when_BusSlaveFactory_l373 = io_bus_cmd_payload_fragment_data[10];
  always @(*) begin
    when_BusSlaveFactory_l335_3 = 1'b0;
    case(io_bus_cmd_payload_fragment_address)
      6'h10 : begin
        if(busCtrl_doWrite) begin
          when_BusSlaveFactory_l335_3 = 1'b1;
        end
      end
      default : begin
      end
    endcase
  end

  assign when_BusSlaveFactory_l341_3 = io_bus_cmd_payload_fragment_data[11];
  assign io_interrupt = bridge_interruptCtrl_interrupt;
  always @(posedge debugCdCtrl_external_clk) begin
    if(systemCdCtrl_logic_outputReset) begin
      _zz_io_bus_rsp_valid_2 <= 1'b0;
      bridge_interruptCtrl_writeIntEnable <= 1'b0;
      bridge_interruptCtrl_readIntEnable <= 1'b0;
      bridge_misc_readError <= 1'b0;
      bridge_misc_readOverflowError <= 1'b0;
      bridge_misc_breakDetected <= 1'b0;
      bridge_misc_doBreak <= 1'b0;
    end else begin
      if(_zz_busCtrl_rsp_ready) begin
        _zz_io_bus_rsp_valid_2 <= (busCtrl_rsp_valid && _zz_io_bus_rsp_valid);
      end
      if(when_BusSlaveFactory_l335) begin
        if(when_BusSlaveFactory_l341) begin
          bridge_misc_readError <= _zz_bridge_misc_readError[0];
        end
      end
      if(uartCtrl_1_io_readError) begin
        bridge_misc_readError <= 1'b1;
      end
      if(when_BusSlaveFactory_l335_1) begin
        if(when_BusSlaveFactory_l341_1) begin
          bridge_misc_readOverflowError <= _zz_bridge_misc_readOverflowError[0];
        end
      end
      if(uartCtrl_1_io_read_isStall) begin
        bridge_misc_readOverflowError <= 1'b1;
      end
      if(when_UartCtrl_l155) begin
        bridge_misc_breakDetected <= 1'b1;
      end
      if(when_BusSlaveFactory_l335_2) begin
        if(when_BusSlaveFactory_l341_2) begin
          bridge_misc_breakDetected <= _zz_bridge_misc_breakDetected[0];
        end
      end
      if(when_BusSlaveFactory_l371) begin
        if(when_BusSlaveFactory_l373) begin
          bridge_misc_doBreak <= _zz_bridge_misc_doBreak[0];
        end
      end
      if(when_BusSlaveFactory_l335_3) begin
        if(when_BusSlaveFactory_l341_3) begin
          bridge_misc_doBreak <= _zz_bridge_misc_doBreak_1[0];
        end
      end
      case(io_bus_cmd_payload_fragment_address)
        6'h04 : begin
          if(busCtrl_doWrite) begin
            bridge_interruptCtrl_writeIntEnable <= io_bus_cmd_payload_fragment_data[0];
            bridge_interruptCtrl_readIntEnable <= io_bus_cmd_payload_fragment_data[1];
          end
        end
        default : begin
        end
      endcase
    end
  end

  always @(posedge debugCdCtrl_external_clk) begin
    if(_zz_busCtrl_rsp_ready) begin
      _zz_io_bus_rsp_payload_last <= busCtrl_rsp_payload_last;
      _zz_io_bus_rsp_payload_fragment_opcode <= busCtrl_rsp_payload_fragment_opcode;
      _zz_io_bus_rsp_payload_fragment_data <= busCtrl_rsp_payload_fragment_data;
      _zz_io_bus_rsp_payload_fragment_context <= busCtrl_rsp_payload_fragment_context;
    end
    uartCtrl_1_io_readBreak_regNext <= uartCtrl_1_io_readBreak;
  end


endmodule

module BmbGpio2 (
  input      [7:0]    io_gpio_read,
  output reg [7:0]    io_gpio_write,
  output reg [7:0]    io_gpio_writeEnable,
  input               io_bus_cmd_valid,
  output              io_bus_cmd_ready,
  input               io_bus_cmd_payload_last,
  input      [0:0]    io_bus_cmd_payload_fragment_opcode,
  input      [7:0]    io_bus_cmd_payload_fragment_address,
  input      [1:0]    io_bus_cmd_payload_fragment_length,
  input      [31:0]   io_bus_cmd_payload_fragment_data,
  input      [0:0]    io_bus_cmd_payload_fragment_context,
  output              io_bus_rsp_valid,
  input               io_bus_rsp_ready,
  output              io_bus_rsp_payload_last,
  output     [0:0]    io_bus_rsp_payload_fragment_opcode,
  output     [31:0]   io_bus_rsp_payload_fragment_data,
  output     [0:0]    io_bus_rsp_payload_fragment_context,
  output reg [7:0]    io_interrupt,
  input               debugCdCtrl_external_clk,
  input               systemCdCtrl_logic_outputReset
);

  wire                mapper_readHaltTrigger;
  wire                mapper_writeHaltTrigger;
  wire                mapper_rsp_valid;
  wire                mapper_rsp_ready;
  wire                mapper_rsp_payload_last;
  wire       [0:0]    mapper_rsp_payload_fragment_opcode;
  reg        [31:0]   mapper_rsp_payload_fragment_data;
  wire       [0:0]    mapper_rsp_payload_fragment_context;
  wire                _zz_io_bus_rsp_valid;
  reg                 _zz_mapper_rsp_ready;
  wire                _zz_io_bus_rsp_valid_1;
  reg                 _zz_io_bus_rsp_valid_2;
  reg                 _zz_io_bus_rsp_payload_last;
  reg        [0:0]    _zz_io_bus_rsp_payload_fragment_opcode;
  reg        [31:0]   _zz_io_bus_rsp_payload_fragment_data;
  reg        [0:0]    _zz_io_bus_rsp_payload_fragment_context;
  wire                when_Stream_l368;
  wire                mapper_askWrite;
  wire                mapper_askRead;
  wire                io_bus_cmd_fire;
  wire                mapper_doWrite;
  wire                io_bus_cmd_fire_1;
  wire                mapper_doRead;
  reg        [7:0]    io_gpio_read_delay_1;
  reg        [7:0]    syncronized;
  reg        [7:0]    last;
  reg                 _zz_io_gpio_write;
  reg                 _zz_io_gpio_writeEnable;
  reg                 _zz_io_gpio_write_1;
  reg                 _zz_io_gpio_writeEnable_1;
  reg                 _zz_io_gpio_write_2;
  reg                 _zz_io_gpio_writeEnable_2;
  reg                 _zz_io_gpio_write_3;
  reg                 _zz_io_gpio_writeEnable_3;
  reg                 _zz_io_gpio_write_4;
  reg                 _zz_io_gpio_writeEnable_4;
  reg                 _zz_io_gpio_write_5;
  reg                 _zz_io_gpio_writeEnable_5;
  reg                 _zz_io_gpio_write_6;
  reg                 _zz_io_gpio_writeEnable_6;
  reg                 _zz_io_gpio_write_7;
  reg                 _zz_io_gpio_writeEnable_7;
  reg        [7:0]    interrupt_enable_high;
  reg        [7:0]    interrupt_enable_low;
  reg        [7:0]    interrupt_enable_rise;
  reg        [7:0]    interrupt_enable_fall;
  wire       [7:0]    interrupt_valid;
  function [7:0] zz_io_interrupt(input dummy);
    begin
      zz_io_interrupt[0] = 1'b0;
      zz_io_interrupt[1] = 1'b0;
      zz_io_interrupt[2] = 1'b0;
      zz_io_interrupt[3] = 1'b0;
      zz_io_interrupt[4] = 1'b0;
      zz_io_interrupt[5] = 1'b0;
      zz_io_interrupt[6] = 1'b0;
      zz_io_interrupt[7] = 1'b0;
    end
  endfunction
  wire [7:0] _zz_1;
  function [7:0] zz_interrupt_enable_rise(input dummy);
    begin
      zz_interrupt_enable_rise[0] = 1'b0;
      zz_interrupt_enable_rise[1] = 1'b0;
      zz_interrupt_enable_rise[2] = 1'b0;
      zz_interrupt_enable_rise[3] = 1'b0;
      zz_interrupt_enable_rise[4] = 1'b0;
      zz_interrupt_enable_rise[5] = 1'b0;
      zz_interrupt_enable_rise[6] = 1'b0;
      zz_interrupt_enable_rise[7] = 1'b0;
    end
  endfunction
  wire [7:0] _zz_2;
  function [7:0] zz_interrupt_enable_fall(input dummy);
    begin
      zz_interrupt_enable_fall[0] = 1'b0;
      zz_interrupt_enable_fall[1] = 1'b0;
      zz_interrupt_enable_fall[2] = 1'b0;
      zz_interrupt_enable_fall[3] = 1'b0;
      zz_interrupt_enable_fall[4] = 1'b0;
      zz_interrupt_enable_fall[5] = 1'b0;
      zz_interrupt_enable_fall[6] = 1'b0;
      zz_interrupt_enable_fall[7] = 1'b0;
    end
  endfunction
  wire [7:0] _zz_3;
  function [7:0] zz_interrupt_enable_high(input dummy);
    begin
      zz_interrupt_enable_high[0] = 1'b0;
      zz_interrupt_enable_high[1] = 1'b0;
      zz_interrupt_enable_high[2] = 1'b0;
      zz_interrupt_enable_high[3] = 1'b0;
      zz_interrupt_enable_high[4] = 1'b0;
      zz_interrupt_enable_high[5] = 1'b0;
      zz_interrupt_enable_high[6] = 1'b0;
      zz_interrupt_enable_high[7] = 1'b0;
    end
  endfunction
  wire [7:0] _zz_4;
  function [7:0] zz_interrupt_enable_low(input dummy);
    begin
      zz_interrupt_enable_low[0] = 1'b0;
      zz_interrupt_enable_low[1] = 1'b0;
      zz_interrupt_enable_low[2] = 1'b0;
      zz_interrupt_enable_low[3] = 1'b0;
      zz_interrupt_enable_low[4] = 1'b0;
      zz_interrupt_enable_low[5] = 1'b0;
      zz_interrupt_enable_low[6] = 1'b0;
      zz_interrupt_enable_low[7] = 1'b0;
    end
  endfunction
  wire [7:0] _zz_5;

  assign mapper_readHaltTrigger = 1'b0;
  assign mapper_writeHaltTrigger = 1'b0;
  assign _zz_io_bus_rsp_valid = (! (mapper_readHaltTrigger || mapper_writeHaltTrigger));
  assign mapper_rsp_ready = (_zz_mapper_rsp_ready && _zz_io_bus_rsp_valid);
  always @(*) begin
    _zz_mapper_rsp_ready = io_bus_rsp_ready;
    if(when_Stream_l368) begin
      _zz_mapper_rsp_ready = 1'b1;
    end
  end

  assign when_Stream_l368 = (! _zz_io_bus_rsp_valid_1);
  assign _zz_io_bus_rsp_valid_1 = _zz_io_bus_rsp_valid_2;
  assign io_bus_rsp_valid = _zz_io_bus_rsp_valid_1;
  assign io_bus_rsp_payload_last = _zz_io_bus_rsp_payload_last;
  assign io_bus_rsp_payload_fragment_opcode = _zz_io_bus_rsp_payload_fragment_opcode;
  assign io_bus_rsp_payload_fragment_data = _zz_io_bus_rsp_payload_fragment_data;
  assign io_bus_rsp_payload_fragment_context = _zz_io_bus_rsp_payload_fragment_context;
  assign mapper_askWrite = (io_bus_cmd_valid && (io_bus_cmd_payload_fragment_opcode == 1'b1));
  assign mapper_askRead = (io_bus_cmd_valid && (io_bus_cmd_payload_fragment_opcode == 1'b0));
  assign io_bus_cmd_fire = (io_bus_cmd_valid && io_bus_cmd_ready);
  assign mapper_doWrite = (io_bus_cmd_fire && (io_bus_cmd_payload_fragment_opcode == 1'b1));
  assign io_bus_cmd_fire_1 = (io_bus_cmd_valid && io_bus_cmd_ready);
  assign mapper_doRead = (io_bus_cmd_fire_1 && (io_bus_cmd_payload_fragment_opcode == 1'b0));
  assign mapper_rsp_valid = io_bus_cmd_valid;
  assign io_bus_cmd_ready = mapper_rsp_ready;
  assign mapper_rsp_payload_last = 1'b1;
  assign mapper_rsp_payload_fragment_opcode = 1'b0;
  always @(*) begin
    mapper_rsp_payload_fragment_data = 32'h0;
    case(io_bus_cmd_payload_fragment_address)
      8'h0 : begin
        mapper_rsp_payload_fragment_data[0 : 0] = syncronized[0];
        mapper_rsp_payload_fragment_data[1 : 1] = syncronized[1];
        mapper_rsp_payload_fragment_data[2 : 2] = syncronized[2];
        mapper_rsp_payload_fragment_data[3 : 3] = syncronized[3];
        mapper_rsp_payload_fragment_data[4 : 4] = syncronized[4];
        mapper_rsp_payload_fragment_data[5 : 5] = syncronized[5];
        mapper_rsp_payload_fragment_data[6 : 6] = syncronized[6];
        mapper_rsp_payload_fragment_data[7 : 7] = syncronized[7];
      end
      8'h04 : begin
        mapper_rsp_payload_fragment_data[0 : 0] = _zz_io_gpio_write;
        mapper_rsp_payload_fragment_data[1 : 1] = _zz_io_gpio_write_1;
        mapper_rsp_payload_fragment_data[2 : 2] = _zz_io_gpio_write_2;
        mapper_rsp_payload_fragment_data[3 : 3] = _zz_io_gpio_write_3;
        mapper_rsp_payload_fragment_data[4 : 4] = _zz_io_gpio_write_4;
        mapper_rsp_payload_fragment_data[5 : 5] = _zz_io_gpio_write_5;
        mapper_rsp_payload_fragment_data[6 : 6] = _zz_io_gpio_write_6;
        mapper_rsp_payload_fragment_data[7 : 7] = _zz_io_gpio_write_7;
      end
      8'h08 : begin
        mapper_rsp_payload_fragment_data[0 : 0] = _zz_io_gpio_writeEnable;
        mapper_rsp_payload_fragment_data[1 : 1] = _zz_io_gpio_writeEnable_1;
        mapper_rsp_payload_fragment_data[2 : 2] = _zz_io_gpio_writeEnable_2;
        mapper_rsp_payload_fragment_data[3 : 3] = _zz_io_gpio_writeEnable_3;
        mapper_rsp_payload_fragment_data[4 : 4] = _zz_io_gpio_writeEnable_4;
        mapper_rsp_payload_fragment_data[5 : 5] = _zz_io_gpio_writeEnable_5;
        mapper_rsp_payload_fragment_data[6 : 6] = _zz_io_gpio_writeEnable_6;
        mapper_rsp_payload_fragment_data[7 : 7] = _zz_io_gpio_writeEnable_7;
      end
      default : begin
      end
    endcase
  end

  assign mapper_rsp_payload_fragment_context = io_bus_cmd_payload_fragment_context;
  always @(*) begin
    io_gpio_write[0] = _zz_io_gpio_write;
    io_gpio_write[1] = _zz_io_gpio_write_1;
    io_gpio_write[2] = _zz_io_gpio_write_2;
    io_gpio_write[3] = _zz_io_gpio_write_3;
    io_gpio_write[4] = _zz_io_gpio_write_4;
    io_gpio_write[5] = _zz_io_gpio_write_5;
    io_gpio_write[6] = _zz_io_gpio_write_6;
    io_gpio_write[7] = _zz_io_gpio_write_7;
  end

  always @(*) begin
    io_gpio_writeEnable[0] = _zz_io_gpio_writeEnable;
    io_gpio_writeEnable[1] = _zz_io_gpio_writeEnable_1;
    io_gpio_writeEnable[2] = _zz_io_gpio_writeEnable_2;
    io_gpio_writeEnable[3] = _zz_io_gpio_writeEnable_3;
    io_gpio_writeEnable[4] = _zz_io_gpio_writeEnable_4;
    io_gpio_writeEnable[5] = _zz_io_gpio_writeEnable_5;
    io_gpio_writeEnable[6] = _zz_io_gpio_writeEnable_6;
    io_gpio_writeEnable[7] = _zz_io_gpio_writeEnable_7;
  end

  assign interrupt_valid = ((((interrupt_enable_high & syncronized) | (interrupt_enable_low & (~ syncronized))) | (interrupt_enable_rise & (syncronized & (~ last)))) | (interrupt_enable_fall & ((~ syncronized) & last)));
  assign _zz_1 = zz_io_interrupt(1'b0);
  always @(*) io_interrupt = _zz_1;
  assign _zz_2 = zz_interrupt_enable_rise(1'b0);
  always @(*) interrupt_enable_rise = _zz_2;
  assign _zz_3 = zz_interrupt_enable_fall(1'b0);
  always @(*) interrupt_enable_fall = _zz_3;
  assign _zz_4 = zz_interrupt_enable_high(1'b0);
  always @(*) interrupt_enable_high = _zz_4;
  assign _zz_5 = zz_interrupt_enable_low(1'b0);
  always @(*) interrupt_enable_low = _zz_5;
  always @(posedge debugCdCtrl_external_clk) begin
    if(systemCdCtrl_logic_outputReset) begin
      _zz_io_bus_rsp_valid_2 <= 1'b0;
      _zz_io_gpio_writeEnable <= 1'b0;
      _zz_io_gpio_writeEnable_1 <= 1'b0;
      _zz_io_gpio_writeEnable_2 <= 1'b0;
      _zz_io_gpio_writeEnable_3 <= 1'b0;
      _zz_io_gpio_writeEnable_4 <= 1'b0;
      _zz_io_gpio_writeEnable_5 <= 1'b0;
      _zz_io_gpio_writeEnable_6 <= 1'b0;
      _zz_io_gpio_writeEnable_7 <= 1'b0;
    end else begin
      if(_zz_mapper_rsp_ready) begin
        _zz_io_bus_rsp_valid_2 <= (mapper_rsp_valid && _zz_io_bus_rsp_valid);
      end
      case(io_bus_cmd_payload_fragment_address)
        8'h08 : begin
          if(mapper_doWrite) begin
            _zz_io_gpio_writeEnable <= io_bus_cmd_payload_fragment_data[0];
            _zz_io_gpio_writeEnable_1 <= io_bus_cmd_payload_fragment_data[1];
            _zz_io_gpio_writeEnable_2 <= io_bus_cmd_payload_fragment_data[2];
            _zz_io_gpio_writeEnable_3 <= io_bus_cmd_payload_fragment_data[3];
            _zz_io_gpio_writeEnable_4 <= io_bus_cmd_payload_fragment_data[4];
            _zz_io_gpio_writeEnable_5 <= io_bus_cmd_payload_fragment_data[5];
            _zz_io_gpio_writeEnable_6 <= io_bus_cmd_payload_fragment_data[6];
            _zz_io_gpio_writeEnable_7 <= io_bus_cmd_payload_fragment_data[7];
          end
        end
        default : begin
        end
      endcase
    end
  end

  always @(posedge debugCdCtrl_external_clk) begin
    if(_zz_mapper_rsp_ready) begin
      _zz_io_bus_rsp_payload_last <= mapper_rsp_payload_last;
      _zz_io_bus_rsp_payload_fragment_opcode <= mapper_rsp_payload_fragment_opcode;
      _zz_io_bus_rsp_payload_fragment_data <= mapper_rsp_payload_fragment_data;
      _zz_io_bus_rsp_payload_fragment_context <= mapper_rsp_payload_fragment_context;
    end
    io_gpio_read_delay_1 <= io_gpio_read;
    syncronized <= io_gpio_read_delay_1;
    last <= syncronized;
    case(io_bus_cmd_payload_fragment_address)
      8'h04 : begin
        if(mapper_doWrite) begin
          _zz_io_gpio_write <= io_bus_cmd_payload_fragment_data[0];
          _zz_io_gpio_write_1 <= io_bus_cmd_payload_fragment_data[1];
          _zz_io_gpio_write_2 <= io_bus_cmd_payload_fragment_data[2];
          _zz_io_gpio_write_3 <= io_bus_cmd_payload_fragment_data[3];
          _zz_io_gpio_write_4 <= io_bus_cmd_payload_fragment_data[4];
          _zz_io_gpio_write_5 <= io_bus_cmd_payload_fragment_data[5];
          _zz_io_gpio_write_6 <= io_bus_cmd_payload_fragment_data[6];
          _zz_io_gpio_write_7 <= io_bus_cmd_payload_fragment_data[7];
        end
      end
      default : begin
      end
    endcase
  end


endmodule

module BmbClint (
  input               io_bus_cmd_valid,
  output              io_bus_cmd_ready,
  input               io_bus_cmd_payload_last,
  input      [0:0]    io_bus_cmd_payload_fragment_opcode,
  input      [15:0]   io_bus_cmd_payload_fragment_address,
  input      [1:0]    io_bus_cmd_payload_fragment_length,
  input      [31:0]   io_bus_cmd_payload_fragment_data,
  input      [0:0]    io_bus_cmd_payload_fragment_context,
  output              io_bus_rsp_valid,
  input               io_bus_rsp_ready,
  output              io_bus_rsp_payload_last,
  output     [0:0]    io_bus_rsp_payload_fragment_opcode,
  output     [31:0]   io_bus_rsp_payload_fragment_data,
  output     [0:0]    io_bus_rsp_payload_fragment_context,
  output     [63:0]   io_time,
  input               debugCdCtrl_external_clk,
  input               systemCdCtrl_logic_outputReset
);

  wire                factory_readHaltTrigger;
  wire                factory_writeHaltTrigger;
  wire                factory_rsp_valid;
  wire                factory_rsp_ready;
  wire                factory_rsp_payload_last;
  wire       [0:0]    factory_rsp_payload_fragment_opcode;
  reg        [31:0]   factory_rsp_payload_fragment_data;
  wire       [0:0]    factory_rsp_payload_fragment_context;
  wire                _zz_io_bus_rsp_valid;
  reg                 _zz_factory_rsp_ready;
  wire                _zz_io_bus_rsp_valid_1;
  reg                 _zz_io_bus_rsp_valid_2;
  reg                 _zz_io_bus_rsp_payload_last;
  reg        [0:0]    _zz_io_bus_rsp_payload_fragment_opcode;
  reg        [31:0]   _zz_io_bus_rsp_payload_fragment_data;
  reg        [0:0]    _zz_io_bus_rsp_payload_fragment_context;
  wire                when_Stream_l368;
  wire                factory_askWrite;
  wire                factory_askRead;
  wire                io_bus_cmd_fire;
  wire                factory_doWrite;
  wire                io_bus_cmd_fire_1;
  wire                factory_doRead;
  reg        [63:0]   logic_time;
  wire       [63:0]   _zz_factory_rsp_payload_fragment_data;
  wire                when_BmbSlaveFactory_l71;
  wire                when_BmbSlaveFactory_l71_1;

  assign factory_readHaltTrigger = 1'b0;
  assign factory_writeHaltTrigger = 1'b0;
  assign _zz_io_bus_rsp_valid = (! (factory_readHaltTrigger || factory_writeHaltTrigger));
  assign factory_rsp_ready = (_zz_factory_rsp_ready && _zz_io_bus_rsp_valid);
  always @(*) begin
    _zz_factory_rsp_ready = io_bus_rsp_ready;
    if(when_Stream_l368) begin
      _zz_factory_rsp_ready = 1'b1;
    end
  end

  assign when_Stream_l368 = (! _zz_io_bus_rsp_valid_1);
  assign _zz_io_bus_rsp_valid_1 = _zz_io_bus_rsp_valid_2;
  assign io_bus_rsp_valid = _zz_io_bus_rsp_valid_1;
  assign io_bus_rsp_payload_last = _zz_io_bus_rsp_payload_last;
  assign io_bus_rsp_payload_fragment_opcode = _zz_io_bus_rsp_payload_fragment_opcode;
  assign io_bus_rsp_payload_fragment_data = _zz_io_bus_rsp_payload_fragment_data;
  assign io_bus_rsp_payload_fragment_context = _zz_io_bus_rsp_payload_fragment_context;
  assign factory_askWrite = (io_bus_cmd_valid && (io_bus_cmd_payload_fragment_opcode == 1'b1));
  assign factory_askRead = (io_bus_cmd_valid && (io_bus_cmd_payload_fragment_opcode == 1'b0));
  assign io_bus_cmd_fire = (io_bus_cmd_valid && io_bus_cmd_ready);
  assign factory_doWrite = (io_bus_cmd_fire && (io_bus_cmd_payload_fragment_opcode == 1'b1));
  assign io_bus_cmd_fire_1 = (io_bus_cmd_valid && io_bus_cmd_ready);
  assign factory_doRead = (io_bus_cmd_fire_1 && (io_bus_cmd_payload_fragment_opcode == 1'b0));
  assign factory_rsp_valid = io_bus_cmd_valid;
  assign io_bus_cmd_ready = factory_rsp_ready;
  assign factory_rsp_payload_last = 1'b1;
  assign factory_rsp_payload_fragment_opcode = 1'b0;
  always @(*) begin
    factory_rsp_payload_fragment_data = 32'h0;
    if(when_BmbSlaveFactory_l71) begin
      factory_rsp_payload_fragment_data[31 : 0] = _zz_factory_rsp_payload_fragment_data[31 : 0];
    end
    if(when_BmbSlaveFactory_l71_1) begin
      factory_rsp_payload_fragment_data[31 : 0] = _zz_factory_rsp_payload_fragment_data[63 : 32];
    end
  end

  assign factory_rsp_payload_fragment_context = io_bus_cmd_payload_fragment_context;
  assign _zz_factory_rsp_payload_fragment_data = logic_time;
  assign io_time = logic_time;
  assign when_BmbSlaveFactory_l71 = ((io_bus_cmd_payload_fragment_address & (~ 16'h0003)) == 16'hbff8);
  assign when_BmbSlaveFactory_l71_1 = ((io_bus_cmd_payload_fragment_address & (~ 16'h0003)) == 16'hbffc);
  always @(posedge debugCdCtrl_external_clk) begin
    if(systemCdCtrl_logic_outputReset) begin
      _zz_io_bus_rsp_valid_2 <= 1'b0;
      logic_time <= 64'h0;
    end else begin
      if(_zz_factory_rsp_ready) begin
        _zz_io_bus_rsp_valid_2 <= (factory_rsp_valid && _zz_io_bus_rsp_valid);
      end
      logic_time <= (logic_time + 64'h0000000000000001);
    end
  end

  always @(posedge debugCdCtrl_external_clk) begin
    if(_zz_factory_rsp_ready) begin
      _zz_io_bus_rsp_payload_last <= factory_rsp_payload_last;
      _zz_io_bus_rsp_payload_fragment_opcode <= factory_rsp_payload_fragment_opcode;
      _zz_io_bus_rsp_payload_fragment_data <= factory_rsp_payload_fragment_data;
      _zz_io_bus_rsp_payload_fragment_context <= factory_rsp_payload_fragment_context;
    end
  end


endmodule

module BmbDecoder_2 (
  input               io_input_cmd_valid,
  output              io_input_cmd_ready,
  input               io_input_cmd_payload_last,
  input      [0:0]    io_input_cmd_payload_fragment_opcode,
  input      [23:0]   io_input_cmd_payload_fragment_address,
  input      [1:0]    io_input_cmd_payload_fragment_length,
  input      [31:0]   io_input_cmd_payload_fragment_data,
  input      [3:0]    io_input_cmd_payload_fragment_mask,
  input      [0:0]    io_input_cmd_payload_fragment_context,
  output reg          io_input_rsp_valid,
  input               io_input_rsp_ready,
  output reg          io_input_rsp_payload_last,
  output reg [0:0]    io_input_rsp_payload_fragment_opcode,
  output     [31:0]   io_input_rsp_payload_fragment_data,
  output reg [0:0]    io_input_rsp_payload_fragment_context,
  output reg          io_outputs_0_cmd_valid,
  input               io_outputs_0_cmd_ready,
  output              io_outputs_0_cmd_payload_last,
  output     [0:0]    io_outputs_0_cmd_payload_fragment_opcode,
  output     [23:0]   io_outputs_0_cmd_payload_fragment_address,
  output     [1:0]    io_outputs_0_cmd_payload_fragment_length,
  output     [31:0]   io_outputs_0_cmd_payload_fragment_data,
  output     [3:0]    io_outputs_0_cmd_payload_fragment_mask,
  output     [0:0]    io_outputs_0_cmd_payload_fragment_context,
  input               io_outputs_0_rsp_valid,
  output              io_outputs_0_rsp_ready,
  input               io_outputs_0_rsp_payload_last,
  input      [0:0]    io_outputs_0_rsp_payload_fragment_opcode,
  input      [31:0]   io_outputs_0_rsp_payload_fragment_data,
  input      [0:0]    io_outputs_0_rsp_payload_fragment_context,
  output reg          io_outputs_1_cmd_valid,
  input               io_outputs_1_cmd_ready,
  output              io_outputs_1_cmd_payload_last,
  output     [0:0]    io_outputs_1_cmd_payload_fragment_opcode,
  output     [23:0]   io_outputs_1_cmd_payload_fragment_address,
  output     [1:0]    io_outputs_1_cmd_payload_fragment_length,
  output     [31:0]   io_outputs_1_cmd_payload_fragment_data,
  output     [3:0]    io_outputs_1_cmd_payload_fragment_mask,
  output     [0:0]    io_outputs_1_cmd_payload_fragment_context,
  input               io_outputs_1_rsp_valid,
  output              io_outputs_1_rsp_ready,
  input               io_outputs_1_rsp_payload_last,
  input      [0:0]    io_outputs_1_rsp_payload_fragment_opcode,
  input      [31:0]   io_outputs_1_rsp_payload_fragment_data,
  input      [0:0]    io_outputs_1_rsp_payload_fragment_context,
  output reg          io_outputs_2_cmd_valid,
  input               io_outputs_2_cmd_ready,
  output              io_outputs_2_cmd_payload_last,
  output     [0:0]    io_outputs_2_cmd_payload_fragment_opcode,
  output     [23:0]   io_outputs_2_cmd_payload_fragment_address,
  output     [1:0]    io_outputs_2_cmd_payload_fragment_length,
  output     [31:0]   io_outputs_2_cmd_payload_fragment_data,
  output     [3:0]    io_outputs_2_cmd_payload_fragment_mask,
  output     [0:0]    io_outputs_2_cmd_payload_fragment_context,
  input               io_outputs_2_rsp_valid,
  output              io_outputs_2_rsp_ready,
  input               io_outputs_2_rsp_payload_last,
  input      [0:0]    io_outputs_2_rsp_payload_fragment_opcode,
  input      [31:0]   io_outputs_2_rsp_payload_fragment_data,
  input      [0:0]    io_outputs_2_rsp_payload_fragment_context,
  input               debugCdCtrl_external_clk,
  input               systemCdCtrl_logic_outputReset
);

  wire       [3:0]    _zz_logic_rspPendingCounter;
  wire       [3:0]    _zz_logic_rspPendingCounter_1;
  wire       [0:0]    _zz_logic_rspPendingCounter_2;
  wire       [3:0]    _zz_logic_rspPendingCounter_3;
  wire       [0:0]    _zz_logic_rspPendingCounter_4;
  reg                 _zz_io_input_rsp_payload_last_1;
  reg        [0:0]    _zz_io_input_rsp_payload_fragment_opcode;
  reg        [31:0]   _zz_io_input_rsp_payload_fragment_data;
  reg        [0:0]    _zz_io_input_rsp_payload_fragment_context;
  wire                logic_input_valid;
  reg                 logic_input_ready;
  wire                logic_input_payload_last;
  wire       [0:0]    logic_input_payload_fragment_opcode;
  wire       [23:0]   logic_input_payload_fragment_address;
  wire       [1:0]    logic_input_payload_fragment_length;
  wire       [31:0]   logic_input_payload_fragment_data;
  wire       [3:0]    logic_input_payload_fragment_mask;
  wire       [0:0]    logic_input_payload_fragment_context;
  reg                 io_input_cmd_rValid;
  wire                logic_input_fire;
  reg                 io_input_cmd_rData_last;
  reg        [0:0]    io_input_cmd_rData_fragment_opcode;
  reg        [23:0]   io_input_cmd_rData_fragment_address;
  reg        [1:0]    io_input_cmd_rData_fragment_length;
  reg        [31:0]   io_input_cmd_rData_fragment_data;
  reg        [3:0]    io_input_cmd_rData_fragment_mask;
  reg        [0:0]    io_input_cmd_rData_fragment_context;
  wire                logic_hitsS0_0;
  wire                logic_hitsS0_1;
  wire                logic_hitsS0_2;
  wire                logic_noHitS0;
  wire                io_input_cmd_fire;
  reg                 logic_hitsS1_0;
  reg                 logic_hitsS1_1;
  reg                 logic_hitsS1_2;
  wire                io_input_cmd_fire_1;
  reg                 logic_noHitS1;
  wire                _zz_io_outputs_0_cmd_payload_last;
  wire                _zz_io_outputs_1_cmd_payload_last;
  wire                _zz_io_outputs_2_cmd_payload_last;
  reg        [3:0]    logic_rspPendingCounter;
  wire                logic_input_fire_1;
  wire                io_input_rsp_fire;
  wire                logic_cmdWait;
  wire                when_BmbDecoder_l56;
  reg                 logic_rspHits_0;
  reg                 logic_rspHits_1;
  reg                 logic_rspHits_2;
  wire                logic_rspPending;
  wire                logic_rspNoHitValid;
  reg                 logic_rspNoHit_doIt;
  wire                io_input_rsp_fire_1;
  wire                when_BmbDecoder_l60;
  wire                logic_input_fire_2;
  wire                when_BmbDecoder_l60_1;
  wire                logic_input_fire_3;
  reg                 logic_rspNoHit_singleBeatRsp;
  wire                logic_input_fire_4;
  wire                logic_input_fire_5;
  reg        [0:0]    logic_rspNoHit_context;
  wire                logic_input_fire_6;
  wire       [1:0]    _zz_io_input_rsp_payload_last;

  assign _zz_logic_rspPendingCounter = (logic_rspPendingCounter + _zz_logic_rspPendingCounter_1);
  assign _zz_logic_rspPendingCounter_2 = (logic_input_fire_1 && logic_input_payload_last);
  assign _zz_logic_rspPendingCounter_1 = {3'd0, _zz_logic_rspPendingCounter_2};
  assign _zz_logic_rspPendingCounter_4 = (io_input_rsp_fire && io_input_rsp_payload_last);
  assign _zz_logic_rspPendingCounter_3 = {3'd0, _zz_logic_rspPendingCounter_4};
  always @(*) begin
    case(_zz_io_input_rsp_payload_last)
      2'b00 : begin
        _zz_io_input_rsp_payload_last_1 = io_outputs_0_rsp_payload_last;
        _zz_io_input_rsp_payload_fragment_opcode = io_outputs_0_rsp_payload_fragment_opcode;
        _zz_io_input_rsp_payload_fragment_data = io_outputs_0_rsp_payload_fragment_data;
        _zz_io_input_rsp_payload_fragment_context = io_outputs_0_rsp_payload_fragment_context;
      end
      2'b01 : begin
        _zz_io_input_rsp_payload_last_1 = io_outputs_1_rsp_payload_last;
        _zz_io_input_rsp_payload_fragment_opcode = io_outputs_1_rsp_payload_fragment_opcode;
        _zz_io_input_rsp_payload_fragment_data = io_outputs_1_rsp_payload_fragment_data;
        _zz_io_input_rsp_payload_fragment_context = io_outputs_1_rsp_payload_fragment_context;
      end
      default : begin
        _zz_io_input_rsp_payload_last_1 = io_outputs_2_rsp_payload_last;
        _zz_io_input_rsp_payload_fragment_opcode = io_outputs_2_rsp_payload_fragment_opcode;
        _zz_io_input_rsp_payload_fragment_data = io_outputs_2_rsp_payload_fragment_data;
        _zz_io_input_rsp_payload_fragment_context = io_outputs_2_rsp_payload_fragment_context;
      end
    endcase
  end

  assign logic_input_fire = (logic_input_valid && logic_input_ready);
  assign io_input_cmd_ready = (! io_input_cmd_rValid);
  assign logic_input_valid = io_input_cmd_rValid;
  assign logic_input_payload_last = io_input_cmd_rData_last;
  assign logic_input_payload_fragment_opcode = io_input_cmd_rData_fragment_opcode;
  assign logic_input_payload_fragment_address = io_input_cmd_rData_fragment_address;
  assign logic_input_payload_fragment_length = io_input_cmd_rData_fragment_length;
  assign logic_input_payload_fragment_data = io_input_cmd_rData_fragment_data;
  assign logic_input_payload_fragment_mask = io_input_cmd_rData_fragment_mask;
  assign logic_input_payload_fragment_context = io_input_cmd_rData_fragment_context;
  assign logic_noHitS0 = (! ({logic_hitsS0_2,{logic_hitsS0_1,logic_hitsS0_0}} != 3'b000));
  assign io_input_cmd_fire = (io_input_cmd_valid && io_input_cmd_ready);
  assign io_input_cmd_fire_1 = (io_input_cmd_valid && io_input_cmd_ready);
  assign logic_hitsS0_0 = ((io_input_cmd_payload_fragment_address & (~ 24'h00ffff)) == 24'hb00000);
  always @(*) begin
    io_outputs_0_cmd_valid = (logic_input_valid && logic_hitsS1_0);
    if(logic_cmdWait) begin
      io_outputs_0_cmd_valid = 1'b0;
    end
  end

  assign _zz_io_outputs_0_cmd_payload_last = logic_input_payload_last;
  assign io_outputs_0_cmd_payload_last = _zz_io_outputs_0_cmd_payload_last;
  assign io_outputs_0_cmd_payload_fragment_opcode = logic_input_payload_fragment_opcode;
  assign io_outputs_0_cmd_payload_fragment_address = logic_input_payload_fragment_address;
  assign io_outputs_0_cmd_payload_fragment_length = logic_input_payload_fragment_length;
  assign io_outputs_0_cmd_payload_fragment_data = logic_input_payload_fragment_data;
  assign io_outputs_0_cmd_payload_fragment_mask = logic_input_payload_fragment_mask;
  assign io_outputs_0_cmd_payload_fragment_context = logic_input_payload_fragment_context;
  assign logic_hitsS0_1 = ((io_input_cmd_payload_fragment_address & (~ 24'h0000ff)) == 24'h0);
  always @(*) begin
    io_outputs_1_cmd_valid = (logic_input_valid && logic_hitsS1_1);
    if(logic_cmdWait) begin
      io_outputs_1_cmd_valid = 1'b0;
    end
  end

  assign _zz_io_outputs_1_cmd_payload_last = logic_input_payload_last;
  assign io_outputs_1_cmd_payload_last = _zz_io_outputs_1_cmd_payload_last;
  assign io_outputs_1_cmd_payload_fragment_opcode = logic_input_payload_fragment_opcode;
  assign io_outputs_1_cmd_payload_fragment_address = logic_input_payload_fragment_address;
  assign io_outputs_1_cmd_payload_fragment_length = logic_input_payload_fragment_length;
  assign io_outputs_1_cmd_payload_fragment_data = logic_input_payload_fragment_data;
  assign io_outputs_1_cmd_payload_fragment_mask = logic_input_payload_fragment_mask;
  assign io_outputs_1_cmd_payload_fragment_context = logic_input_payload_fragment_context;
  assign logic_hitsS0_2 = ((io_input_cmd_payload_fragment_address & (~ 24'h00003f)) == 24'h010000);
  always @(*) begin
    io_outputs_2_cmd_valid = (logic_input_valid && logic_hitsS1_2);
    if(logic_cmdWait) begin
      io_outputs_2_cmd_valid = 1'b0;
    end
  end

  assign _zz_io_outputs_2_cmd_payload_last = logic_input_payload_last;
  assign io_outputs_2_cmd_payload_last = _zz_io_outputs_2_cmd_payload_last;
  assign io_outputs_2_cmd_payload_fragment_opcode = logic_input_payload_fragment_opcode;
  assign io_outputs_2_cmd_payload_fragment_address = logic_input_payload_fragment_address;
  assign io_outputs_2_cmd_payload_fragment_length = logic_input_payload_fragment_length;
  assign io_outputs_2_cmd_payload_fragment_data = logic_input_payload_fragment_data;
  assign io_outputs_2_cmd_payload_fragment_mask = logic_input_payload_fragment_mask;
  assign io_outputs_2_cmd_payload_fragment_context = logic_input_payload_fragment_context;
  always @(*) begin
    logic_input_ready = (({(logic_hitsS1_2 && io_outputs_2_cmd_ready),{(logic_hitsS1_1 && io_outputs_1_cmd_ready),(logic_hitsS1_0 && io_outputs_0_cmd_ready)}} != 3'b000) || logic_noHitS1);
    if(logic_cmdWait) begin
      logic_input_ready = 1'b0;
    end
  end

  assign logic_input_fire_1 = (logic_input_valid && logic_input_ready);
  assign io_input_rsp_fire = (io_input_rsp_valid && io_input_rsp_ready);
  assign when_BmbDecoder_l56 = (logic_input_valid && (! logic_cmdWait));
  assign logic_rspPending = (logic_rspPendingCounter != 4'b0000);
  assign logic_rspNoHitValid = (! ({logic_rspHits_2,{logic_rspHits_1,logic_rspHits_0}} != 3'b000));
  assign io_input_rsp_fire_1 = (io_input_rsp_valid && io_input_rsp_ready);
  assign when_BmbDecoder_l60 = (io_input_rsp_fire_1 && io_input_rsp_payload_last);
  assign logic_input_fire_2 = (logic_input_valid && logic_input_ready);
  assign when_BmbDecoder_l60_1 = ((logic_input_fire_2 && logic_noHitS1) && logic_input_payload_last);
  assign logic_input_fire_3 = (logic_input_valid && logic_input_ready);
  assign logic_input_fire_4 = (logic_input_valid && logic_input_ready);
  assign logic_input_fire_5 = (logic_input_valid && logic_input_ready);
  assign logic_input_fire_6 = (logic_input_valid && logic_input_ready);
  always @(*) begin
    io_input_rsp_valid = (({io_outputs_2_rsp_valid,{io_outputs_1_rsp_valid,io_outputs_0_rsp_valid}} != 3'b000) || (logic_rspPending && logic_rspNoHitValid));
    if(logic_rspNoHit_doIt) begin
      io_input_rsp_valid = 1'b1;
    end
  end

  assign _zz_io_input_rsp_payload_last = {logic_rspHits_2,logic_rspHits_1};
  always @(*) begin
    io_input_rsp_payload_last = _zz_io_input_rsp_payload_last_1;
    if(logic_rspNoHit_doIt) begin
      io_input_rsp_payload_last = 1'b1;
    end
  end

  always @(*) begin
    io_input_rsp_payload_fragment_opcode = _zz_io_input_rsp_payload_fragment_opcode;
    if(logic_rspNoHit_doIt) begin
      io_input_rsp_payload_fragment_opcode = 1'b1;
    end
  end

  assign io_input_rsp_payload_fragment_data = _zz_io_input_rsp_payload_fragment_data;
  always @(*) begin
    io_input_rsp_payload_fragment_context = _zz_io_input_rsp_payload_fragment_context;
    if(logic_rspNoHit_doIt) begin
      io_input_rsp_payload_fragment_context = logic_rspNoHit_context;
    end
  end

  assign io_outputs_0_rsp_ready = io_input_rsp_ready;
  assign io_outputs_1_rsp_ready = io_input_rsp_ready;
  assign io_outputs_2_rsp_ready = io_input_rsp_ready;
  assign logic_cmdWait = ((logic_rspPending && ((((logic_hitsS1_0 != logic_rspHits_0) || (logic_hitsS1_1 != logic_rspHits_1)) || (logic_hitsS1_2 != logic_rspHits_2)) || logic_rspNoHitValid)) || (logic_rspPendingCounter == 4'b1000));
  always @(posedge debugCdCtrl_external_clk) begin
    if(systemCdCtrl_logic_outputReset) begin
      io_input_cmd_rValid <= 1'b0;
      logic_rspPendingCounter <= 4'b0000;
      logic_rspNoHit_doIt <= 1'b0;
    end else begin
      if(io_input_cmd_valid) begin
        io_input_cmd_rValid <= 1'b1;
      end
      if(logic_input_fire) begin
        io_input_cmd_rValid <= 1'b0;
      end
      logic_rspPendingCounter <= (_zz_logic_rspPendingCounter - _zz_logic_rspPendingCounter_3);
      if(when_BmbDecoder_l60) begin
        logic_rspNoHit_doIt <= 1'b0;
      end
      if(when_BmbDecoder_l60_1) begin
        logic_rspNoHit_doIt <= 1'b1;
      end
    end
  end

  always @(posedge debugCdCtrl_external_clk) begin
    if(io_input_cmd_ready) begin
      io_input_cmd_rData_last <= io_input_cmd_payload_last;
      io_input_cmd_rData_fragment_opcode <= io_input_cmd_payload_fragment_opcode;
      io_input_cmd_rData_fragment_address <= io_input_cmd_payload_fragment_address;
      io_input_cmd_rData_fragment_length <= io_input_cmd_payload_fragment_length;
      io_input_cmd_rData_fragment_data <= io_input_cmd_payload_fragment_data;
      io_input_cmd_rData_fragment_mask <= io_input_cmd_payload_fragment_mask;
      io_input_cmd_rData_fragment_context <= io_input_cmd_payload_fragment_context;
    end
    if(io_input_cmd_fire) begin
      logic_hitsS1_0 <= logic_hitsS0_0;
      logic_hitsS1_1 <= logic_hitsS0_1;
      logic_hitsS1_2 <= logic_hitsS0_2;
    end
    if(io_input_cmd_fire_1) begin
      logic_noHitS1 <= logic_noHitS0;
    end
    if(when_BmbDecoder_l56) begin
      logic_rspHits_0 <= logic_hitsS1_0;
      logic_rspHits_1 <= logic_hitsS1_1;
      logic_rspHits_2 <= logic_hitsS1_2;
    end
    if(logic_input_fire_3) begin
      logic_rspNoHit_singleBeatRsp <= (logic_input_payload_fragment_opcode == 1'b1);
    end
    if(logic_input_fire_5) begin
      logic_rspNoHit_context <= logic_input_payload_fragment_context;
    end
  end


endmodule

module BmbArbiter (
  input               io_inputs_0_cmd_valid,
  output              io_inputs_0_cmd_ready,
  input               io_inputs_0_cmd_payload_last,
  input      [0:0]    io_inputs_0_cmd_payload_fragment_opcode,
  input      [14:0]   io_inputs_0_cmd_payload_fragment_address,
  input      [1:0]    io_inputs_0_cmd_payload_fragment_length,
  input      [31:0]   io_inputs_0_cmd_payload_fragment_data,
  input      [3:0]    io_inputs_0_cmd_payload_fragment_mask,
  input      [0:0]    io_inputs_0_cmd_payload_fragment_context,
  output              io_inputs_0_rsp_valid,
  input               io_inputs_0_rsp_ready,
  output              io_inputs_0_rsp_payload_last,
  output     [0:0]    io_inputs_0_rsp_payload_fragment_opcode,
  output     [31:0]   io_inputs_0_rsp_payload_fragment_data,
  output     [0:0]    io_inputs_0_rsp_payload_fragment_context,
  input               io_inputs_1_cmd_valid,
  output              io_inputs_1_cmd_ready,
  input               io_inputs_1_cmd_payload_last,
  input      [0:0]    io_inputs_1_cmd_payload_fragment_opcode,
  input      [14:0]   io_inputs_1_cmd_payload_fragment_address,
  input      [1:0]    io_inputs_1_cmd_payload_fragment_length,
  input      [31:0]   io_inputs_1_cmd_payload_fragment_data,
  input      [3:0]    io_inputs_1_cmd_payload_fragment_mask,
  output              io_inputs_1_rsp_valid,
  input               io_inputs_1_rsp_ready,
  output              io_inputs_1_rsp_payload_last,
  output     [0:0]    io_inputs_1_rsp_payload_fragment_opcode,
  output     [31:0]   io_inputs_1_rsp_payload_fragment_data,
  output              io_output_cmd_valid,
  input               io_output_cmd_ready,
  output              io_output_cmd_payload_last,
  output     [0:0]    io_output_cmd_payload_fragment_source,
  output     [0:0]    io_output_cmd_payload_fragment_opcode,
  output     [14:0]   io_output_cmd_payload_fragment_address,
  output     [1:0]    io_output_cmd_payload_fragment_length,
  output     [31:0]   io_output_cmd_payload_fragment_data,
  output     [3:0]    io_output_cmd_payload_fragment_mask,
  output     [0:0]    io_output_cmd_payload_fragment_context,
  input               io_output_rsp_valid,
  output              io_output_rsp_ready,
  input               io_output_rsp_payload_last,
  input      [0:0]    io_output_rsp_payload_fragment_source,
  input      [0:0]    io_output_rsp_payload_fragment_opcode,
  input      [31:0]   io_output_rsp_payload_fragment_data,
  input      [0:0]    io_output_rsp_payload_fragment_context,
  input               debugCdCtrl_external_clk,
  input               systemCdCtrl_logic_outputReset
);

  wire                memory_arbiter_io_inputs_0_ready;
  wire                memory_arbiter_io_inputs_1_ready;
  wire                memory_arbiter_io_output_valid;
  wire                memory_arbiter_io_output_payload_last;
  wire       [0:0]    memory_arbiter_io_output_payload_fragment_source;
  wire       [0:0]    memory_arbiter_io_output_payload_fragment_opcode;
  wire       [14:0]   memory_arbiter_io_output_payload_fragment_address;
  wire       [1:0]    memory_arbiter_io_output_payload_fragment_length;
  wire       [31:0]   memory_arbiter_io_output_payload_fragment_data;
  wire       [3:0]    memory_arbiter_io_output_payload_fragment_mask;
  wire       [0:0]    memory_arbiter_io_output_payload_fragment_context;
  wire       [0:0]    memory_arbiter_io_chosen;
  wire       [1:0]    memory_arbiter_io_chosenOH;
  wire       [1:0]    _zz_io_output_cmd_payload_fragment_source;
  reg                 _zz_io_output_rsp_ready;
  wire       [0:0]    memory_rspSel;

  assign _zz_io_output_cmd_payload_fragment_source = {memory_arbiter_io_output_payload_fragment_source,memory_arbiter_io_chosen};
  StreamArbiter memory_arbiter (
    .io_inputs_0_valid                    (io_inputs_0_cmd_valid                                  ), //i
    .io_inputs_0_ready                    (memory_arbiter_io_inputs_0_ready                       ), //o
    .io_inputs_0_payload_last             (io_inputs_0_cmd_payload_last                           ), //i
    .io_inputs_0_payload_fragment_source  (1'b0                                                   ), //i
    .io_inputs_0_payload_fragment_opcode  (io_inputs_0_cmd_payload_fragment_opcode                ), //i
    .io_inputs_0_payload_fragment_address (io_inputs_0_cmd_payload_fragment_address[14:0]         ), //i
    .io_inputs_0_payload_fragment_length  (io_inputs_0_cmd_payload_fragment_length[1:0]           ), //i
    .io_inputs_0_payload_fragment_data    (io_inputs_0_cmd_payload_fragment_data[31:0]            ), //i
    .io_inputs_0_payload_fragment_mask    (io_inputs_0_cmd_payload_fragment_mask[3:0]             ), //i
    .io_inputs_0_payload_fragment_context (io_inputs_0_cmd_payload_fragment_context               ), //i
    .io_inputs_1_valid                    (io_inputs_1_cmd_valid                                  ), //i
    .io_inputs_1_ready                    (memory_arbiter_io_inputs_1_ready                       ), //o
    .io_inputs_1_payload_last             (io_inputs_1_cmd_payload_last                           ), //i
    .io_inputs_1_payload_fragment_source  (1'b0                                                   ), //i
    .io_inputs_1_payload_fragment_opcode  (io_inputs_1_cmd_payload_fragment_opcode                ), //i
    .io_inputs_1_payload_fragment_address (io_inputs_1_cmd_payload_fragment_address[14:0]         ), //i
    .io_inputs_1_payload_fragment_length  (io_inputs_1_cmd_payload_fragment_length[1:0]           ), //i
    .io_inputs_1_payload_fragment_data    (io_inputs_1_cmd_payload_fragment_data[31:0]            ), //i
    .io_inputs_1_payload_fragment_mask    (io_inputs_1_cmd_payload_fragment_mask[3:0]             ), //i
    .io_inputs_1_payload_fragment_context (1'b0                                                   ), //i
    .io_output_valid                      (memory_arbiter_io_output_valid                         ), //o
    .io_output_ready                      (io_output_cmd_ready                                    ), //i
    .io_output_payload_last               (memory_arbiter_io_output_payload_last                  ), //o
    .io_output_payload_fragment_source    (memory_arbiter_io_output_payload_fragment_source       ), //o
    .io_output_payload_fragment_opcode    (memory_arbiter_io_output_payload_fragment_opcode       ), //o
    .io_output_payload_fragment_address   (memory_arbiter_io_output_payload_fragment_address[14:0]), //o
    .io_output_payload_fragment_length    (memory_arbiter_io_output_payload_fragment_length[1:0]  ), //o
    .io_output_payload_fragment_data      (memory_arbiter_io_output_payload_fragment_data[31:0]   ), //o
    .io_output_payload_fragment_mask      (memory_arbiter_io_output_payload_fragment_mask[3:0]    ), //o
    .io_output_payload_fragment_context   (memory_arbiter_io_output_payload_fragment_context      ), //o
    .io_chosen                            (memory_arbiter_io_chosen                               ), //o
    .io_chosenOH                          (memory_arbiter_io_chosenOH[1:0]                        ), //o
    .debugCdCtrl_external_clk             (debugCdCtrl_external_clk                               ), //i
    .systemCdCtrl_logic_outputReset       (systemCdCtrl_logic_outputReset                         )  //i
  );
  always @(*) begin
    case(memory_rspSel)
      1'b0 : _zz_io_output_rsp_ready = io_inputs_0_rsp_ready;
      default : _zz_io_output_rsp_ready = io_inputs_1_rsp_ready;
    endcase
  end

  assign io_inputs_0_cmd_ready = memory_arbiter_io_inputs_0_ready;
  assign io_inputs_1_cmd_ready = memory_arbiter_io_inputs_1_ready;
  assign io_output_cmd_valid = memory_arbiter_io_output_valid;
  assign io_output_cmd_payload_last = memory_arbiter_io_output_payload_last;
  assign io_output_cmd_payload_fragment_opcode = memory_arbiter_io_output_payload_fragment_opcode;
  assign io_output_cmd_payload_fragment_address = memory_arbiter_io_output_payload_fragment_address;
  assign io_output_cmd_payload_fragment_length = memory_arbiter_io_output_payload_fragment_length;
  assign io_output_cmd_payload_fragment_data = memory_arbiter_io_output_payload_fragment_data;
  assign io_output_cmd_payload_fragment_mask = memory_arbiter_io_output_payload_fragment_mask;
  assign io_output_cmd_payload_fragment_context = memory_arbiter_io_output_payload_fragment_context;
  assign io_output_cmd_payload_fragment_source = _zz_io_output_cmd_payload_fragment_source[0:0];
  assign memory_rspSel = io_output_rsp_payload_fragment_source[0 : 0];
  assign io_inputs_0_rsp_valid = (io_output_rsp_valid && (memory_rspSel == 1'b0));
  assign io_inputs_0_rsp_payload_last = io_output_rsp_payload_last;
  assign io_inputs_0_rsp_payload_fragment_opcode = io_output_rsp_payload_fragment_opcode;
  assign io_inputs_0_rsp_payload_fragment_data = io_output_rsp_payload_fragment_data;
  assign io_inputs_0_rsp_payload_fragment_context = io_output_rsp_payload_fragment_context;
  assign io_inputs_1_rsp_valid = (io_output_rsp_valid && (memory_rspSel == 1'b1));
  assign io_inputs_1_rsp_payload_last = io_output_rsp_payload_last;
  assign io_inputs_1_rsp_payload_fragment_opcode = io_output_rsp_payload_fragment_opcode;
  assign io_inputs_1_rsp_payload_fragment_data = io_output_rsp_payload_fragment_data;
  assign io_output_rsp_ready = _zz_io_output_rsp_ready;

endmodule

module BmbOnChipRam (
  input               io_bus_cmd_valid,
  output              io_bus_cmd_ready,
  input               io_bus_cmd_payload_last,
  input      [0:0]    io_bus_cmd_payload_fragment_source,
  input      [0:0]    io_bus_cmd_payload_fragment_opcode,
  input      [14:0]   io_bus_cmd_payload_fragment_address,
  input      [1:0]    io_bus_cmd_payload_fragment_length,
  input      [31:0]   io_bus_cmd_payload_fragment_data,
  input      [3:0]    io_bus_cmd_payload_fragment_mask,
  input      [0:0]    io_bus_cmd_payload_fragment_context,
  output              io_bus_rsp_valid,
  input               io_bus_rsp_ready,
  output              io_bus_rsp_payload_last,
  output     [0:0]    io_bus_rsp_payload_fragment_source,
  output     [0:0]    io_bus_rsp_payload_fragment_opcode,
  output     [31:0]   io_bus_rsp_payload_fragment_data,
  output     [0:0]    io_bus_rsp_payload_fragment_context,
  input               debugCdCtrl_external_clk,
  input               systemCdCtrl_logic_outputReset
);

  reg        [31:0]   _zz_ram_port0;
  wire                io_bus_rsp_isStall;
  reg                 io_bus_cmd_valid_regNextWhen;
  reg        [0:0]    io_bus_cmd_payload_fragment_source_regNextWhen;
  reg        [0:0]    io_bus_cmd_payload_fragment_context_regNextWhen;
  wire       [12:0]   _zz_io_bus_rsp_payload_fragment_data;
  wire                io_bus_cmd_fire;
  wire                _zz_io_bus_rsp_payload_fragment_data_1;
  wire       [31:0]   _zz_io_bus_rsp_payload_fragment_data_2;
  reg [7:0] ram_symbol0 [0:8191];
  reg [7:0] ram_symbol1 [0:8191];
  reg [7:0] ram_symbol2 [0:8191];
  reg [7:0] ram_symbol3 [0:8191];
  reg [7:0] _zz_ramsymbol_read;
  reg [7:0] _zz_ramsymbol_read_1;
  reg [7:0] _zz_ramsymbol_read_2;
  reg [7:0] _zz_ramsymbol_read_3;

  always @(*) begin
    _zz_ram_port0 = {_zz_ramsymbol_read_3, _zz_ramsymbol_read_2, _zz_ramsymbol_read_1, _zz_ramsymbol_read};
  end
  always @(posedge debugCdCtrl_external_clk) begin
    if(io_bus_cmd_fire) begin
      _zz_ramsymbol_read <= ram_symbol0[_zz_io_bus_rsp_payload_fragment_data];
      _zz_ramsymbol_read_1 <= ram_symbol1[_zz_io_bus_rsp_payload_fragment_data];
      _zz_ramsymbol_read_2 <= ram_symbol2[_zz_io_bus_rsp_payload_fragment_data];
      _zz_ramsymbol_read_3 <= ram_symbol3[_zz_io_bus_rsp_payload_fragment_data];
    end
  end

  always @(posedge debugCdCtrl_external_clk) begin
    if(io_bus_cmd_payload_fragment_mask[0] && io_bus_cmd_fire && _zz_io_bus_rsp_payload_fragment_data_1 ) begin
      ram_symbol0[_zz_io_bus_rsp_payload_fragment_data] <= _zz_io_bus_rsp_payload_fragment_data_2[7 : 0];
    end
    if(io_bus_cmd_payload_fragment_mask[1] && io_bus_cmd_fire && _zz_io_bus_rsp_payload_fragment_data_1 ) begin
      ram_symbol1[_zz_io_bus_rsp_payload_fragment_data] <= _zz_io_bus_rsp_payload_fragment_data_2[15 : 8];
    end
    if(io_bus_cmd_payload_fragment_mask[2] && io_bus_cmd_fire && _zz_io_bus_rsp_payload_fragment_data_1 ) begin
      ram_symbol2[_zz_io_bus_rsp_payload_fragment_data] <= _zz_io_bus_rsp_payload_fragment_data_2[23 : 16];
    end
    if(io_bus_cmd_payload_fragment_mask[3] && io_bus_cmd_fire && _zz_io_bus_rsp_payload_fragment_data_1 ) begin
      ram_symbol3[_zz_io_bus_rsp_payload_fragment_data] <= _zz_io_bus_rsp_payload_fragment_data_2[31 : 24];
    end
  end

  assign io_bus_rsp_isStall = (io_bus_rsp_valid && (! io_bus_rsp_ready));
  assign io_bus_cmd_ready = (! io_bus_rsp_isStall);
  assign io_bus_rsp_valid = io_bus_cmd_valid_regNextWhen;
  assign io_bus_rsp_payload_fragment_source = io_bus_cmd_payload_fragment_source_regNextWhen;
  assign io_bus_rsp_payload_fragment_context = io_bus_cmd_payload_fragment_context_regNextWhen;
  assign _zz_io_bus_rsp_payload_fragment_data = (io_bus_cmd_payload_fragment_address >>> 2);
  assign io_bus_cmd_fire = (io_bus_cmd_valid && io_bus_cmd_ready);
  assign _zz_io_bus_rsp_payload_fragment_data_1 = (io_bus_cmd_payload_fragment_opcode == 1'b1);
  assign _zz_io_bus_rsp_payload_fragment_data_2 = io_bus_cmd_payload_fragment_data;
  assign io_bus_rsp_payload_fragment_data = _zz_ram_port0;
  assign io_bus_rsp_payload_fragment_opcode = 1'b0;
  assign io_bus_rsp_payload_last = 1'b1;
  always @(posedge debugCdCtrl_external_clk) begin
    if(systemCdCtrl_logic_outputReset) begin
      io_bus_cmd_valid_regNextWhen <= 1'b0;
    end else begin
      if(io_bus_cmd_ready) begin
        io_bus_cmd_valid_regNextWhen <= io_bus_cmd_valid;
      end
    end
  end

  always @(posedge debugCdCtrl_external_clk) begin
    if(io_bus_cmd_ready) begin
      io_bus_cmd_payload_fragment_source_regNextWhen <= io_bus_cmd_payload_fragment_source;
    end
    if(io_bus_cmd_ready) begin
      io_bus_cmd_payload_fragment_context_regNextWhen <= io_bus_cmd_payload_fragment_context;
    end
  end


endmodule

module BmbDecoder_1 (
  input               io_input_cmd_valid,
  output              io_input_cmd_ready,
  input               io_input_cmd_payload_last,
  input      [0:0]    io_input_cmd_payload_fragment_opcode,
  input      [31:0]   io_input_cmd_payload_fragment_address,
  input      [1:0]    io_input_cmd_payload_fragment_length,
  input      [31:0]   io_input_cmd_payload_fragment_data,
  input      [3:0]    io_input_cmd_payload_fragment_mask,
  input      [0:0]    io_input_cmd_payload_fragment_context,
  output reg          io_input_rsp_valid,
  input               io_input_rsp_ready,
  output reg          io_input_rsp_payload_last,
  output reg [0:0]    io_input_rsp_payload_fragment_opcode,
  output     [31:0]   io_input_rsp_payload_fragment_data,
  output reg [0:0]    io_input_rsp_payload_fragment_context,
  output reg          io_outputs_0_cmd_valid,
  input               io_outputs_0_cmd_ready,
  output              io_outputs_0_cmd_payload_last,
  output     [0:0]    io_outputs_0_cmd_payload_fragment_opcode,
  output     [31:0]   io_outputs_0_cmd_payload_fragment_address,
  output     [1:0]    io_outputs_0_cmd_payload_fragment_length,
  output     [31:0]   io_outputs_0_cmd_payload_fragment_data,
  output     [3:0]    io_outputs_0_cmd_payload_fragment_mask,
  output     [0:0]    io_outputs_0_cmd_payload_fragment_context,
  input               io_outputs_0_rsp_valid,
  output              io_outputs_0_rsp_ready,
  input               io_outputs_0_rsp_payload_last,
  input      [0:0]    io_outputs_0_rsp_payload_fragment_opcode,
  input      [31:0]   io_outputs_0_rsp_payload_fragment_data,
  input      [0:0]    io_outputs_0_rsp_payload_fragment_context,
  output reg          io_outputs_1_cmd_valid,
  input               io_outputs_1_cmd_ready,
  output              io_outputs_1_cmd_payload_last,
  output     [0:0]    io_outputs_1_cmd_payload_fragment_opcode,
  output     [31:0]   io_outputs_1_cmd_payload_fragment_address,
  output     [1:0]    io_outputs_1_cmd_payload_fragment_length,
  output     [31:0]   io_outputs_1_cmd_payload_fragment_data,
  output     [3:0]    io_outputs_1_cmd_payload_fragment_mask,
  output     [0:0]    io_outputs_1_cmd_payload_fragment_context,
  input               io_outputs_1_rsp_valid,
  output              io_outputs_1_rsp_ready,
  input               io_outputs_1_rsp_payload_last,
  input      [0:0]    io_outputs_1_rsp_payload_fragment_opcode,
  input      [31:0]   io_outputs_1_rsp_payload_fragment_data,
  input      [0:0]    io_outputs_1_rsp_payload_fragment_context,
  input               debugCdCtrl_external_clk,
  input               systemCdCtrl_logic_outputReset
);

  wire       [6:0]    _zz_logic_rspPendingCounter;
  wire       [6:0]    _zz_logic_rspPendingCounter_1;
  wire       [0:0]    _zz_logic_rspPendingCounter_2;
  wire       [6:0]    _zz_logic_rspPendingCounter_3;
  wire       [0:0]    _zz_logic_rspPendingCounter_4;
  reg                 _zz_io_input_rsp_payload_last_1;
  reg        [0:0]    _zz_io_input_rsp_payload_fragment_opcode;
  reg        [31:0]   _zz_io_input_rsp_payload_fragment_data;
  reg        [0:0]    _zz_io_input_rsp_payload_fragment_context;
  wire                logic_input_valid;
  reg                 logic_input_ready;
  wire                logic_input_payload_last;
  wire       [0:0]    logic_input_payload_fragment_opcode;
  wire       [31:0]   logic_input_payload_fragment_address;
  wire       [1:0]    logic_input_payload_fragment_length;
  wire       [31:0]   logic_input_payload_fragment_data;
  wire       [3:0]    logic_input_payload_fragment_mask;
  wire       [0:0]    logic_input_payload_fragment_context;
  wire                logic_hitsS0_0;
  wire                logic_hitsS0_1;
  wire                logic_noHitS0;
  wire                _zz_io_outputs_0_cmd_payload_last;
  wire                _zz_io_outputs_1_cmd_payload_last;
  reg        [6:0]    logic_rspPendingCounter;
  wire                logic_input_fire;
  wire                io_input_rsp_fire;
  wire                logic_cmdWait;
  wire                when_BmbDecoder_l56;
  reg                 logic_rspHits_0;
  reg                 logic_rspHits_1;
  wire                logic_rspPending;
  wire                logic_rspNoHitValid;
  reg                 logic_rspNoHit_doIt;
  wire                io_input_rsp_fire_1;
  wire                when_BmbDecoder_l60;
  wire                logic_input_fire_1;
  wire                when_BmbDecoder_l60_1;
  wire                logic_input_fire_2;
  reg                 logic_rspNoHit_singleBeatRsp;
  wire                logic_input_fire_3;
  wire                logic_input_fire_4;
  reg        [0:0]    logic_rspNoHit_context;
  wire                logic_input_fire_5;
  wire       [0:0]    _zz_io_input_rsp_payload_last;

  assign _zz_logic_rspPendingCounter = (logic_rspPendingCounter + _zz_logic_rspPendingCounter_1);
  assign _zz_logic_rspPendingCounter_2 = (logic_input_fire && logic_input_payload_last);
  assign _zz_logic_rspPendingCounter_1 = {6'd0, _zz_logic_rspPendingCounter_2};
  assign _zz_logic_rspPendingCounter_4 = (io_input_rsp_fire && io_input_rsp_payload_last);
  assign _zz_logic_rspPendingCounter_3 = {6'd0, _zz_logic_rspPendingCounter_4};
  always @(*) begin
    case(_zz_io_input_rsp_payload_last)
      1'b0 : begin
        _zz_io_input_rsp_payload_last_1 = io_outputs_0_rsp_payload_last;
        _zz_io_input_rsp_payload_fragment_opcode = io_outputs_0_rsp_payload_fragment_opcode;
        _zz_io_input_rsp_payload_fragment_data = io_outputs_0_rsp_payload_fragment_data;
        _zz_io_input_rsp_payload_fragment_context = io_outputs_0_rsp_payload_fragment_context;
      end
      default : begin
        _zz_io_input_rsp_payload_last_1 = io_outputs_1_rsp_payload_last;
        _zz_io_input_rsp_payload_fragment_opcode = io_outputs_1_rsp_payload_fragment_opcode;
        _zz_io_input_rsp_payload_fragment_data = io_outputs_1_rsp_payload_fragment_data;
        _zz_io_input_rsp_payload_fragment_context = io_outputs_1_rsp_payload_fragment_context;
      end
    endcase
  end

  assign logic_input_valid = io_input_cmd_valid;
  assign io_input_cmd_ready = logic_input_ready;
  assign logic_input_payload_last = io_input_cmd_payload_last;
  assign logic_input_payload_fragment_opcode = io_input_cmd_payload_fragment_opcode;
  assign logic_input_payload_fragment_address = io_input_cmd_payload_fragment_address;
  assign logic_input_payload_fragment_length = io_input_cmd_payload_fragment_length;
  assign logic_input_payload_fragment_data = io_input_cmd_payload_fragment_data;
  assign logic_input_payload_fragment_mask = io_input_cmd_payload_fragment_mask;
  assign logic_input_payload_fragment_context = io_input_cmd_payload_fragment_context;
  assign logic_noHitS0 = (! ({logic_hitsS0_1,logic_hitsS0_0} != 2'b00));
  assign logic_hitsS0_0 = ((io_input_cmd_payload_fragment_address & (~ 32'h00007fff)) == 32'h80000000);
  always @(*) begin
    io_outputs_0_cmd_valid = (logic_input_valid && logic_hitsS0_0);
    if(logic_cmdWait) begin
      io_outputs_0_cmd_valid = 1'b0;
    end
  end

  assign _zz_io_outputs_0_cmd_payload_last = logic_input_payload_last;
  assign io_outputs_0_cmd_payload_last = _zz_io_outputs_0_cmd_payload_last;
  assign io_outputs_0_cmd_payload_fragment_opcode = logic_input_payload_fragment_opcode;
  assign io_outputs_0_cmd_payload_fragment_address = logic_input_payload_fragment_address;
  assign io_outputs_0_cmd_payload_fragment_length = logic_input_payload_fragment_length;
  assign io_outputs_0_cmd_payload_fragment_data = logic_input_payload_fragment_data;
  assign io_outputs_0_cmd_payload_fragment_mask = logic_input_payload_fragment_mask;
  assign io_outputs_0_cmd_payload_fragment_context = logic_input_payload_fragment_context;
  assign logic_hitsS0_1 = ((io_input_cmd_payload_fragment_address & (~ 32'h00ffffff)) == 32'h10000000);
  always @(*) begin
    io_outputs_1_cmd_valid = (logic_input_valid && logic_hitsS0_1);
    if(logic_cmdWait) begin
      io_outputs_1_cmd_valid = 1'b0;
    end
  end

  assign _zz_io_outputs_1_cmd_payload_last = logic_input_payload_last;
  assign io_outputs_1_cmd_payload_last = _zz_io_outputs_1_cmd_payload_last;
  assign io_outputs_1_cmd_payload_fragment_opcode = logic_input_payload_fragment_opcode;
  assign io_outputs_1_cmd_payload_fragment_address = logic_input_payload_fragment_address;
  assign io_outputs_1_cmd_payload_fragment_length = logic_input_payload_fragment_length;
  assign io_outputs_1_cmd_payload_fragment_data = logic_input_payload_fragment_data;
  assign io_outputs_1_cmd_payload_fragment_mask = logic_input_payload_fragment_mask;
  assign io_outputs_1_cmd_payload_fragment_context = logic_input_payload_fragment_context;
  always @(*) begin
    logic_input_ready = (({(logic_hitsS0_1 && io_outputs_1_cmd_ready),(logic_hitsS0_0 && io_outputs_0_cmd_ready)} != 2'b00) || logic_noHitS0);
    if(logic_cmdWait) begin
      logic_input_ready = 1'b0;
    end
  end

  assign logic_input_fire = (logic_input_valid && logic_input_ready);
  assign io_input_rsp_fire = (io_input_rsp_valid && io_input_rsp_ready);
  assign when_BmbDecoder_l56 = (logic_input_valid && (! logic_cmdWait));
  assign logic_rspPending = (logic_rspPendingCounter != 7'h0);
  assign logic_rspNoHitValid = (! ({logic_rspHits_1,logic_rspHits_0} != 2'b00));
  assign io_input_rsp_fire_1 = (io_input_rsp_valid && io_input_rsp_ready);
  assign when_BmbDecoder_l60 = (io_input_rsp_fire_1 && io_input_rsp_payload_last);
  assign logic_input_fire_1 = (logic_input_valid && logic_input_ready);
  assign when_BmbDecoder_l60_1 = ((logic_input_fire_1 && logic_noHitS0) && logic_input_payload_last);
  assign logic_input_fire_2 = (logic_input_valid && logic_input_ready);
  assign logic_input_fire_3 = (logic_input_valid && logic_input_ready);
  assign logic_input_fire_4 = (logic_input_valid && logic_input_ready);
  assign logic_input_fire_5 = (logic_input_valid && logic_input_ready);
  always @(*) begin
    io_input_rsp_valid = (({io_outputs_1_rsp_valid,io_outputs_0_rsp_valid} != 2'b00) || (logic_rspPending && logic_rspNoHitValid));
    if(logic_rspNoHit_doIt) begin
      io_input_rsp_valid = 1'b1;
    end
  end

  assign _zz_io_input_rsp_payload_last = logic_rspHits_1;
  always @(*) begin
    io_input_rsp_payload_last = _zz_io_input_rsp_payload_last_1;
    if(logic_rspNoHit_doIt) begin
      io_input_rsp_payload_last = 1'b1;
    end
  end

  always @(*) begin
    io_input_rsp_payload_fragment_opcode = _zz_io_input_rsp_payload_fragment_opcode;
    if(logic_rspNoHit_doIt) begin
      io_input_rsp_payload_fragment_opcode = 1'b1;
    end
  end

  assign io_input_rsp_payload_fragment_data = _zz_io_input_rsp_payload_fragment_data;
  always @(*) begin
    io_input_rsp_payload_fragment_context = _zz_io_input_rsp_payload_fragment_context;
    if(logic_rspNoHit_doIt) begin
      io_input_rsp_payload_fragment_context = logic_rspNoHit_context;
    end
  end

  assign io_outputs_0_rsp_ready = io_input_rsp_ready;
  assign io_outputs_1_rsp_ready = io_input_rsp_ready;
  assign logic_cmdWait = ((logic_rspPending && (((logic_hitsS0_0 != logic_rspHits_0) || (logic_hitsS0_1 != logic_rspHits_1)) || logic_rspNoHitValid)) || (logic_rspPendingCounter == 7'h40));
  always @(posedge debugCdCtrl_external_clk) begin
    if(systemCdCtrl_logic_outputReset) begin
      logic_rspPendingCounter <= 7'h0;
      logic_rspNoHit_doIt <= 1'b0;
    end else begin
      logic_rspPendingCounter <= (_zz_logic_rspPendingCounter - _zz_logic_rspPendingCounter_3);
      if(when_BmbDecoder_l60) begin
        logic_rspNoHit_doIt <= 1'b0;
      end
      if(when_BmbDecoder_l60_1) begin
        logic_rspNoHit_doIt <= 1'b1;
      end
    end
  end

  always @(posedge debugCdCtrl_external_clk) begin
    if(when_BmbDecoder_l56) begin
      logic_rspHits_0 <= logic_hitsS0_0;
      logic_rspHits_1 <= logic_hitsS0_1;
    end
    if(logic_input_fire_2) begin
      logic_rspNoHit_singleBeatRsp <= (logic_input_payload_fragment_opcode == 1'b1);
    end
    if(logic_input_fire_4) begin
      logic_rspNoHit_context <= logic_input_payload_fragment_context;
    end
  end


endmodule

module BmbDecoder (
  input               io_input_cmd_valid,
  output              io_input_cmd_ready,
  input               io_input_cmd_payload_last,
  input      [0:0]    io_input_cmd_payload_fragment_opcode,
  input      [31:0]   io_input_cmd_payload_fragment_address,
  input      [1:0]    io_input_cmd_payload_fragment_length,
  output reg          io_input_rsp_valid,
  input               io_input_rsp_ready,
  output reg          io_input_rsp_payload_last,
  output reg [0:0]    io_input_rsp_payload_fragment_opcode,
  output     [31:0]   io_input_rsp_payload_fragment_data,
  output reg          io_outputs_0_cmd_valid,
  input               io_outputs_0_cmd_ready,
  output              io_outputs_0_cmd_payload_last,
  output     [0:0]    io_outputs_0_cmd_payload_fragment_opcode,
  output     [31:0]   io_outputs_0_cmd_payload_fragment_address,
  output     [1:0]    io_outputs_0_cmd_payload_fragment_length,
  input               io_outputs_0_rsp_valid,
  output              io_outputs_0_rsp_ready,
  input               io_outputs_0_rsp_payload_last,
  input      [0:0]    io_outputs_0_rsp_payload_fragment_opcode,
  input      [31:0]   io_outputs_0_rsp_payload_fragment_data,
  input               debugCdCtrl_external_clk,
  input               systemCdCtrl_logic_outputReset
);

  wire       [6:0]    _zz_logic_rspPendingCounter;
  wire       [6:0]    _zz_logic_rspPendingCounter_1;
  wire       [0:0]    _zz_logic_rspPendingCounter_2;
  wire       [6:0]    _zz_logic_rspPendingCounter_3;
  wire       [0:0]    _zz_logic_rspPendingCounter_4;
  wire                logic_input_valid;
  reg                 logic_input_ready;
  wire                logic_input_payload_last;
  wire       [0:0]    logic_input_payload_fragment_opcode;
  wire       [31:0]   logic_input_payload_fragment_address;
  wire       [1:0]    logic_input_payload_fragment_length;
  wire                logic_hitsS0_0;
  wire                logic_noHitS0;
  wire                _zz_io_outputs_0_cmd_payload_last;
  reg        [6:0]    logic_rspPendingCounter;
  wire                logic_input_fire;
  wire                io_input_rsp_fire;
  wire                logic_cmdWait;
  wire                when_BmbDecoder_l56;
  reg                 logic_rspHits_0;
  wire                logic_rspPending;
  wire                logic_rspNoHitValid;
  reg                 logic_rspNoHit_doIt;
  wire                io_input_rsp_fire_1;
  wire                when_BmbDecoder_l60;
  wire                logic_input_fire_1;
  wire                when_BmbDecoder_l60_1;
  wire                logic_input_fire_2;
  reg                 logic_rspNoHit_singleBeatRsp;
  wire                logic_input_fire_3;
  wire                logic_input_fire_4;
  wire                logic_input_fire_5;

  assign _zz_logic_rspPendingCounter = (logic_rspPendingCounter + _zz_logic_rspPendingCounter_1);
  assign _zz_logic_rspPendingCounter_2 = (logic_input_fire && logic_input_payload_last);
  assign _zz_logic_rspPendingCounter_1 = {6'd0, _zz_logic_rspPendingCounter_2};
  assign _zz_logic_rspPendingCounter_4 = (io_input_rsp_fire && io_input_rsp_payload_last);
  assign _zz_logic_rspPendingCounter_3 = {6'd0, _zz_logic_rspPendingCounter_4};
  assign logic_input_valid = io_input_cmd_valid;
  assign io_input_cmd_ready = logic_input_ready;
  assign logic_input_payload_last = io_input_cmd_payload_last;
  assign logic_input_payload_fragment_opcode = io_input_cmd_payload_fragment_opcode;
  assign logic_input_payload_fragment_address = io_input_cmd_payload_fragment_address;
  assign logic_input_payload_fragment_length = io_input_cmd_payload_fragment_length;
  assign logic_noHitS0 = (! (logic_hitsS0_0 != 1'b0));
  assign logic_hitsS0_0 = ((io_input_cmd_payload_fragment_address & (~ 32'h00007fff)) == 32'h80000000);
  always @(*) begin
    io_outputs_0_cmd_valid = (logic_input_valid && logic_hitsS0_0);
    if(logic_cmdWait) begin
      io_outputs_0_cmd_valid = 1'b0;
    end
  end

  assign _zz_io_outputs_0_cmd_payload_last = logic_input_payload_last;
  assign io_outputs_0_cmd_payload_last = _zz_io_outputs_0_cmd_payload_last;
  assign io_outputs_0_cmd_payload_fragment_opcode = logic_input_payload_fragment_opcode;
  assign io_outputs_0_cmd_payload_fragment_address = logic_input_payload_fragment_address;
  assign io_outputs_0_cmd_payload_fragment_length = logic_input_payload_fragment_length;
  always @(*) begin
    logic_input_ready = (((logic_hitsS0_0 && io_outputs_0_cmd_ready) != 1'b0) || logic_noHitS0);
    if(logic_cmdWait) begin
      logic_input_ready = 1'b0;
    end
  end

  assign logic_input_fire = (logic_input_valid && logic_input_ready);
  assign io_input_rsp_fire = (io_input_rsp_valid && io_input_rsp_ready);
  assign when_BmbDecoder_l56 = (logic_input_valid && (! logic_cmdWait));
  assign logic_rspPending = (logic_rspPendingCounter != 7'h0);
  assign logic_rspNoHitValid = (! (logic_rspHits_0 != 1'b0));
  assign io_input_rsp_fire_1 = (io_input_rsp_valid && io_input_rsp_ready);
  assign when_BmbDecoder_l60 = (io_input_rsp_fire_1 && io_input_rsp_payload_last);
  assign logic_input_fire_1 = (logic_input_valid && logic_input_ready);
  assign when_BmbDecoder_l60_1 = ((logic_input_fire_1 && logic_noHitS0) && logic_input_payload_last);
  assign logic_input_fire_2 = (logic_input_valid && logic_input_ready);
  assign logic_input_fire_3 = (logic_input_valid && logic_input_ready);
  assign logic_input_fire_4 = (logic_input_valid && logic_input_ready);
  assign logic_input_fire_5 = (logic_input_valid && logic_input_ready);
  always @(*) begin
    io_input_rsp_valid = ((io_outputs_0_rsp_valid != 1'b0) || (logic_rspPending && logic_rspNoHitValid));
    if(logic_rspNoHit_doIt) begin
      io_input_rsp_valid = 1'b1;
    end
  end

  always @(*) begin
    io_input_rsp_payload_last = io_outputs_0_rsp_payload_last;
    if(logic_rspNoHit_doIt) begin
      io_input_rsp_payload_last = 1'b1;
    end
  end

  always @(*) begin
    io_input_rsp_payload_fragment_opcode = io_outputs_0_rsp_payload_fragment_opcode;
    if(logic_rspNoHit_doIt) begin
      io_input_rsp_payload_fragment_opcode = 1'b1;
    end
  end

  assign io_input_rsp_payload_fragment_data = io_outputs_0_rsp_payload_fragment_data;
  assign io_outputs_0_rsp_ready = io_input_rsp_ready;
  assign logic_cmdWait = ((logic_rspPending && ((logic_hitsS0_0 != logic_rspHits_0) || logic_rspNoHitValid)) || (logic_rspPendingCounter == 7'h40));
  always @(posedge debugCdCtrl_external_clk) begin
    if(systemCdCtrl_logic_outputReset) begin
      logic_rspPendingCounter <= 7'h0;
      logic_rspNoHit_doIt <= 1'b0;
    end else begin
      logic_rspPendingCounter <= (_zz_logic_rspPendingCounter - _zz_logic_rspPendingCounter_3);
      if(when_BmbDecoder_l60) begin
        logic_rspNoHit_doIt <= 1'b0;
      end
      if(when_BmbDecoder_l60_1) begin
        logic_rspNoHit_doIt <= 1'b1;
      end
    end
  end

  always @(posedge debugCdCtrl_external_clk) begin
    if(when_BmbDecoder_l56) begin
      logic_rspHits_0 <= logic_hitsS0_0;
    end
    if(logic_input_fire_2) begin
      logic_rspNoHit_singleBeatRsp <= (logic_input_payload_fragment_opcode == 1'b1);
    end
  end


endmodule

module BufferCC_4 (
  input               io_dataIn,
  output              io_dataOut,
  input               debugCdCtrl_external_clk,
  input               system_cpu_debugReset
);

  (* async_reg = "true" *) reg                 buffers_0;
  (* async_reg = "true" *) reg                 buffers_1;

  assign io_dataOut = buffers_1;
  always @(posedge debugCdCtrl_external_clk or posedge system_cpu_debugReset) begin
    if(system_cpu_debugReset) begin
      buffers_0 <= 1'b1;
      buffers_1 <= 1'b1;
    end else begin
      buffers_0 <= io_dataIn;
      buffers_1 <= buffers_0;
    end
  end


endmodule

module SystemDebugger (
  input               io_remote_cmd_valid,
  output              io_remote_cmd_ready,
  input               io_remote_cmd_payload_last,
  input      [0:0]    io_remote_cmd_payload_fragment,
  output              io_remote_rsp_valid,
  input               io_remote_rsp_ready,
  output              io_remote_rsp_payload_error,
  output     [31:0]   io_remote_rsp_payload_data,
  output              io_mem_cmd_valid,
  input               io_mem_cmd_ready,
  output     [31:0]   io_mem_cmd_payload_address,
  output     [31:0]   io_mem_cmd_payload_data,
  output              io_mem_cmd_payload_wr,
  output     [1:0]    io_mem_cmd_payload_size,
  input               io_mem_rsp_valid,
  input      [31:0]   io_mem_rsp_payload,
  input               debugCdCtrl_external_clk,
  input               debugCdCtrl_logic_outputReset
);

  reg        [66:0]   dispatcher_dataShifter;
  reg                 dispatcher_dataLoaded;
  reg        [7:0]    dispatcher_headerShifter;
  wire       [7:0]    dispatcher_header;
  reg                 dispatcher_headerLoaded;
  reg        [2:0]    dispatcher_counter;
  wire                when_Fragment_l346;
  wire                when_Fragment_l349;
  wire       [66:0]   _zz_io_mem_cmd_payload_address;
  wire                io_mem_cmd_isStall;
  wire                when_Fragment_l372;

  assign dispatcher_header = dispatcher_headerShifter[7 : 0];
  assign when_Fragment_l346 = (dispatcher_headerLoaded == 1'b0);
  assign when_Fragment_l349 = (dispatcher_counter == 3'b111);
  assign io_remote_cmd_ready = (! dispatcher_dataLoaded);
  assign _zz_io_mem_cmd_payload_address = dispatcher_dataShifter[66 : 0];
  assign io_mem_cmd_payload_address = _zz_io_mem_cmd_payload_address[31 : 0];
  assign io_mem_cmd_payload_data = _zz_io_mem_cmd_payload_address[63 : 32];
  assign io_mem_cmd_payload_wr = _zz_io_mem_cmd_payload_address[64];
  assign io_mem_cmd_payload_size = _zz_io_mem_cmd_payload_address[66 : 65];
  assign io_mem_cmd_valid = (dispatcher_dataLoaded && (dispatcher_header == 8'h0));
  assign io_mem_cmd_isStall = (io_mem_cmd_valid && (! io_mem_cmd_ready));
  assign when_Fragment_l372 = ((dispatcher_headerLoaded && dispatcher_dataLoaded) && (! io_mem_cmd_isStall));
  assign io_remote_rsp_valid = io_mem_rsp_valid;
  assign io_remote_rsp_payload_error = 1'b0;
  assign io_remote_rsp_payload_data = io_mem_rsp_payload;
  always @(posedge debugCdCtrl_external_clk) begin
    if(debugCdCtrl_logic_outputReset) begin
      dispatcher_dataLoaded <= 1'b0;
      dispatcher_headerLoaded <= 1'b0;
      dispatcher_counter <= 3'b000;
    end else begin
      if(io_remote_cmd_valid) begin
        if(when_Fragment_l346) begin
          dispatcher_counter <= (dispatcher_counter + 3'b001);
          if(when_Fragment_l349) begin
            dispatcher_headerLoaded <= 1'b1;
          end
        end
        if(io_remote_cmd_payload_last) begin
          dispatcher_headerLoaded <= 1'b1;
          dispatcher_dataLoaded <= 1'b1;
          dispatcher_counter <= 3'b000;
        end
      end
      if(when_Fragment_l372) begin
        dispatcher_headerLoaded <= 1'b0;
        dispatcher_dataLoaded <= 1'b0;
      end
    end
  end

  always @(posedge debugCdCtrl_external_clk) begin
    if(io_remote_cmd_valid) begin
      if(when_Fragment_l346) begin
        dispatcher_headerShifter <= ({io_remote_cmd_payload_fragment,dispatcher_headerShifter} >>> 1);
      end else begin
        dispatcher_dataShifter <= ({io_remote_cmd_payload_fragment,dispatcher_dataShifter} >>> 1);
      end
    end
  end


endmodule

module JtagBridge (
  input               io_jtag_tms,
  input               io_jtag_tdi,
  output              io_jtag_tdo,
  input               io_jtag_tck,
  output              io_remote_cmd_valid,
  input               io_remote_cmd_ready,
  output              io_remote_cmd_payload_last,
  output     [0:0]    io_remote_cmd_payload_fragment,
  input               io_remote_rsp_valid,
  output              io_remote_rsp_ready,
  input               io_remote_rsp_payload_error,
  input      [31:0]   io_remote_rsp_payload_data,
  input               debugCdCtrl_external_clk,
  input               debugCdCtrl_logic_outputReset
);
  localparam JtagState_RESET = 4'd0;
  localparam JtagState_IDLE = 4'd1;
  localparam JtagState_IR_SELECT = 4'd2;
  localparam JtagState_IR_CAPTURE = 4'd3;
  localparam JtagState_IR_SHIFT = 4'd4;
  localparam JtagState_IR_EXIT1 = 4'd5;
  localparam JtagState_IR_PAUSE = 4'd6;
  localparam JtagState_IR_EXIT2 = 4'd7;
  localparam JtagState_IR_UPDATE = 4'd8;
  localparam JtagState_DR_SELECT = 4'd9;
  localparam JtagState_DR_CAPTURE = 4'd10;
  localparam JtagState_DR_SHIFT = 4'd11;
  localparam JtagState_DR_EXIT1 = 4'd12;
  localparam JtagState_DR_PAUSE = 4'd13;
  localparam JtagState_DR_EXIT2 = 4'd14;
  localparam JtagState_DR_UPDATE = 4'd15;

  wire                flowCCByToggle_1_io_output_valid;
  wire                flowCCByToggle_1_io_output_payload_last;
  wire       [0:0]    flowCCByToggle_1_io_output_payload_fragment;
  wire       [3:0]    _zz_jtag_tap_isBypass;
  wire       [3:0]    _zz_jtag_tap_isBypass_1;
  wire       [1:0]    _zz_jtag_tap_instructionShift;
  wire                system_cmd_valid;
  wire                system_cmd_payload_last;
  wire       [0:0]    system_cmd_payload_fragment;
  wire                system_cmd_toStream_valid;
  wire                system_cmd_toStream_ready;
  wire                system_cmd_toStream_payload_last;
  wire       [0:0]    system_cmd_toStream_payload_fragment;
  (* async_reg = "true" *) reg                 system_rsp_valid;
  (* async_reg = "true" *) reg                 system_rsp_payload_error;
  (* async_reg = "true" *) reg        [31:0]   system_rsp_payload_data;
  wire                io_remote_rsp_fire;
  wire       [3:0]    jtag_tap_fsm_stateNext;
  reg        [3:0]    jtag_tap_fsm_state;
  reg        [3:0]    _zz_jtag_tap_fsm_stateNext;
  reg        [3:0]    jtag_tap_instruction;
  reg        [3:0]    jtag_tap_instructionShift;
  reg                 jtag_tap_bypass;
  reg                 jtag_tap_tdoUnbufferd;
  reg                 jtag_tap_tdoDr;
  wire                jtag_tap_tdoIr;
  wire                jtag_tap_isBypass;
  reg                 jtag_tap_tdoUnbufferd_regNext;
  wire                jtag_idcodeArea_ctrl_tdi;
  wire                jtag_idcodeArea_ctrl_enable;
  wire                jtag_idcodeArea_ctrl_capture;
  wire                jtag_idcodeArea_ctrl_shift;
  wire                jtag_idcodeArea_ctrl_update;
  wire                jtag_idcodeArea_ctrl_reset;
  wire                jtag_idcodeArea_ctrl_tdo;
  reg        [31:0]   jtag_idcodeArea_shifter;
  wire                when_JtagTap_l120;
  wire                jtag_writeArea_ctrl_tdi;
  wire                jtag_writeArea_ctrl_enable;
  wire                jtag_writeArea_ctrl_capture;
  wire                jtag_writeArea_ctrl_shift;
  wire                jtag_writeArea_ctrl_update;
  wire                jtag_writeArea_ctrl_reset;
  wire                jtag_writeArea_ctrl_tdo;
  wire                jtag_writeArea_source_valid;
  wire                jtag_writeArea_source_payload_last;
  wire       [0:0]    jtag_writeArea_source_payload_fragment;
  reg                 jtag_writeArea_valid;
  reg                 jtag_writeArea_data;
  wire                jtag_readArea_ctrl_tdi;
  wire                jtag_readArea_ctrl_enable;
  wire                jtag_readArea_ctrl_capture;
  wire                jtag_readArea_ctrl_shift;
  wire                jtag_readArea_ctrl_update;
  wire                jtag_readArea_ctrl_reset;
  wire                jtag_readArea_ctrl_tdo;
  reg        [33:0]   jtag_readArea_full_shifter;
  `ifndef SYNTHESIS
  reg [79:0] jtag_tap_fsm_stateNext_string;
  reg [79:0] jtag_tap_fsm_state_string;
  reg [79:0] _zz_jtag_tap_fsm_stateNext_string;
  `endif


  assign _zz_jtag_tap_isBypass = jtag_tap_instruction;
  assign _zz_jtag_tap_isBypass_1 = 4'b1111;
  assign _zz_jtag_tap_instructionShift = 2'b01;
  FlowCCByToggle flowCCByToggle_1 (
    .io_input_valid                (jtag_writeArea_source_valid                ), //i
    .io_input_payload_last         (jtag_writeArea_source_payload_last         ), //i
    .io_input_payload_fragment     (jtag_writeArea_source_payload_fragment     ), //i
    .io_output_valid               (flowCCByToggle_1_io_output_valid           ), //o
    .io_output_payload_last        (flowCCByToggle_1_io_output_payload_last    ), //o
    .io_output_payload_fragment    (flowCCByToggle_1_io_output_payload_fragment), //o
    .io_jtag_tck                   (io_jtag_tck                                ), //i
    .debugCdCtrl_external_clk      (debugCdCtrl_external_clk                   ), //i
    .debugCdCtrl_logic_outputReset (debugCdCtrl_logic_outputReset              )  //i
  );
  initial begin
  `ifndef SYNTHESIS
    jtag_tap_fsm_state = {1{$urandom}};
  `endif
  end

  `ifndef SYNTHESIS
  always @(*) begin
    case(jtag_tap_fsm_stateNext)
      JtagState_RESET : jtag_tap_fsm_stateNext_string = "RESET     ";
      JtagState_IDLE : jtag_tap_fsm_stateNext_string = "IDLE      ";
      JtagState_IR_SELECT : jtag_tap_fsm_stateNext_string = "IR_SELECT ";
      JtagState_IR_CAPTURE : jtag_tap_fsm_stateNext_string = "IR_CAPTURE";
      JtagState_IR_SHIFT : jtag_tap_fsm_stateNext_string = "IR_SHIFT  ";
      JtagState_IR_EXIT1 : jtag_tap_fsm_stateNext_string = "IR_EXIT1  ";
      JtagState_IR_PAUSE : jtag_tap_fsm_stateNext_string = "IR_PAUSE  ";
      JtagState_IR_EXIT2 : jtag_tap_fsm_stateNext_string = "IR_EXIT2  ";
      JtagState_IR_UPDATE : jtag_tap_fsm_stateNext_string = "IR_UPDATE ";
      JtagState_DR_SELECT : jtag_tap_fsm_stateNext_string = "DR_SELECT ";
      JtagState_DR_CAPTURE : jtag_tap_fsm_stateNext_string = "DR_CAPTURE";
      JtagState_DR_SHIFT : jtag_tap_fsm_stateNext_string = "DR_SHIFT  ";
      JtagState_DR_EXIT1 : jtag_tap_fsm_stateNext_string = "DR_EXIT1  ";
      JtagState_DR_PAUSE : jtag_tap_fsm_stateNext_string = "DR_PAUSE  ";
      JtagState_DR_EXIT2 : jtag_tap_fsm_stateNext_string = "DR_EXIT2  ";
      JtagState_DR_UPDATE : jtag_tap_fsm_stateNext_string = "DR_UPDATE ";
      default : jtag_tap_fsm_stateNext_string = "??????????";
    endcase
  end
  always @(*) begin
    case(jtag_tap_fsm_state)
      JtagState_RESET : jtag_tap_fsm_state_string = "RESET     ";
      JtagState_IDLE : jtag_tap_fsm_state_string = "IDLE      ";
      JtagState_IR_SELECT : jtag_tap_fsm_state_string = "IR_SELECT ";
      JtagState_IR_CAPTURE : jtag_tap_fsm_state_string = "IR_CAPTURE";
      JtagState_IR_SHIFT : jtag_tap_fsm_state_string = "IR_SHIFT  ";
      JtagState_IR_EXIT1 : jtag_tap_fsm_state_string = "IR_EXIT1  ";
      JtagState_IR_PAUSE : jtag_tap_fsm_state_string = "IR_PAUSE  ";
      JtagState_IR_EXIT2 : jtag_tap_fsm_state_string = "IR_EXIT2  ";
      JtagState_IR_UPDATE : jtag_tap_fsm_state_string = "IR_UPDATE ";
      JtagState_DR_SELECT : jtag_tap_fsm_state_string = "DR_SELECT ";
      JtagState_DR_CAPTURE : jtag_tap_fsm_state_string = "DR_CAPTURE";
      JtagState_DR_SHIFT : jtag_tap_fsm_state_string = "DR_SHIFT  ";
      JtagState_DR_EXIT1 : jtag_tap_fsm_state_string = "DR_EXIT1  ";
      JtagState_DR_PAUSE : jtag_tap_fsm_state_string = "DR_PAUSE  ";
      JtagState_DR_EXIT2 : jtag_tap_fsm_state_string = "DR_EXIT2  ";
      JtagState_DR_UPDATE : jtag_tap_fsm_state_string = "DR_UPDATE ";
      default : jtag_tap_fsm_state_string = "??????????";
    endcase
  end
  always @(*) begin
    case(_zz_jtag_tap_fsm_stateNext)
      JtagState_RESET : _zz_jtag_tap_fsm_stateNext_string = "RESET     ";
      JtagState_IDLE : _zz_jtag_tap_fsm_stateNext_string = "IDLE      ";
      JtagState_IR_SELECT : _zz_jtag_tap_fsm_stateNext_string = "IR_SELECT ";
      JtagState_IR_CAPTURE : _zz_jtag_tap_fsm_stateNext_string = "IR_CAPTURE";
      JtagState_IR_SHIFT : _zz_jtag_tap_fsm_stateNext_string = "IR_SHIFT  ";
      JtagState_IR_EXIT1 : _zz_jtag_tap_fsm_stateNext_string = "IR_EXIT1  ";
      JtagState_IR_PAUSE : _zz_jtag_tap_fsm_stateNext_string = "IR_PAUSE  ";
      JtagState_IR_EXIT2 : _zz_jtag_tap_fsm_stateNext_string = "IR_EXIT2  ";
      JtagState_IR_UPDATE : _zz_jtag_tap_fsm_stateNext_string = "IR_UPDATE ";
      JtagState_DR_SELECT : _zz_jtag_tap_fsm_stateNext_string = "DR_SELECT ";
      JtagState_DR_CAPTURE : _zz_jtag_tap_fsm_stateNext_string = "DR_CAPTURE";
      JtagState_DR_SHIFT : _zz_jtag_tap_fsm_stateNext_string = "DR_SHIFT  ";
      JtagState_DR_EXIT1 : _zz_jtag_tap_fsm_stateNext_string = "DR_EXIT1  ";
      JtagState_DR_PAUSE : _zz_jtag_tap_fsm_stateNext_string = "DR_PAUSE  ";
      JtagState_DR_EXIT2 : _zz_jtag_tap_fsm_stateNext_string = "DR_EXIT2  ";
      JtagState_DR_UPDATE : _zz_jtag_tap_fsm_stateNext_string = "DR_UPDATE ";
      default : _zz_jtag_tap_fsm_stateNext_string = "??????????";
    endcase
  end
  `endif

  assign system_cmd_toStream_valid = system_cmd_valid;
  assign system_cmd_toStream_payload_last = system_cmd_payload_last;
  assign system_cmd_toStream_payload_fragment = system_cmd_payload_fragment;
  assign io_remote_cmd_valid = system_cmd_toStream_valid;
  assign system_cmd_toStream_ready = io_remote_cmd_ready;
  assign io_remote_cmd_payload_last = system_cmd_toStream_payload_last;
  assign io_remote_cmd_payload_fragment = system_cmd_toStream_payload_fragment;
  assign io_remote_rsp_fire = (io_remote_rsp_valid && io_remote_rsp_ready);
  assign io_remote_rsp_ready = 1'b1;
  always @(*) begin
    case(jtag_tap_fsm_state)
      JtagState_IDLE : begin
        _zz_jtag_tap_fsm_stateNext = (io_jtag_tms ? JtagState_DR_SELECT : JtagState_IDLE);
      end
      JtagState_IR_SELECT : begin
        _zz_jtag_tap_fsm_stateNext = (io_jtag_tms ? JtagState_RESET : JtagState_IR_CAPTURE);
      end
      JtagState_IR_CAPTURE : begin
        _zz_jtag_tap_fsm_stateNext = (io_jtag_tms ? JtagState_IR_EXIT1 : JtagState_IR_SHIFT);
      end
      JtagState_IR_SHIFT : begin
        _zz_jtag_tap_fsm_stateNext = (io_jtag_tms ? JtagState_IR_EXIT1 : JtagState_IR_SHIFT);
      end
      JtagState_IR_EXIT1 : begin
        _zz_jtag_tap_fsm_stateNext = (io_jtag_tms ? JtagState_IR_UPDATE : JtagState_IR_PAUSE);
      end
      JtagState_IR_PAUSE : begin
        _zz_jtag_tap_fsm_stateNext = (io_jtag_tms ? JtagState_IR_EXIT2 : JtagState_IR_PAUSE);
      end
      JtagState_IR_EXIT2 : begin
        _zz_jtag_tap_fsm_stateNext = (io_jtag_tms ? JtagState_IR_UPDATE : JtagState_IR_SHIFT);
      end
      JtagState_IR_UPDATE : begin
        _zz_jtag_tap_fsm_stateNext = (io_jtag_tms ? JtagState_DR_SELECT : JtagState_IDLE);
      end
      JtagState_DR_SELECT : begin
        _zz_jtag_tap_fsm_stateNext = (io_jtag_tms ? JtagState_IR_SELECT : JtagState_DR_CAPTURE);
      end
      JtagState_DR_CAPTURE : begin
        _zz_jtag_tap_fsm_stateNext = (io_jtag_tms ? JtagState_DR_EXIT1 : JtagState_DR_SHIFT);
      end
      JtagState_DR_SHIFT : begin
        _zz_jtag_tap_fsm_stateNext = (io_jtag_tms ? JtagState_DR_EXIT1 : JtagState_DR_SHIFT);
      end
      JtagState_DR_EXIT1 : begin
        _zz_jtag_tap_fsm_stateNext = (io_jtag_tms ? JtagState_DR_UPDATE : JtagState_DR_PAUSE);
      end
      JtagState_DR_PAUSE : begin
        _zz_jtag_tap_fsm_stateNext = (io_jtag_tms ? JtagState_DR_EXIT2 : JtagState_DR_PAUSE);
      end
      JtagState_DR_EXIT2 : begin
        _zz_jtag_tap_fsm_stateNext = (io_jtag_tms ? JtagState_DR_UPDATE : JtagState_DR_SHIFT);
      end
      JtagState_DR_UPDATE : begin
        _zz_jtag_tap_fsm_stateNext = (io_jtag_tms ? JtagState_DR_SELECT : JtagState_IDLE);
      end
      default : begin
        _zz_jtag_tap_fsm_stateNext = (io_jtag_tms ? JtagState_RESET : JtagState_IDLE);
      end
    endcase
  end

  assign jtag_tap_fsm_stateNext = _zz_jtag_tap_fsm_stateNext;
  always @(*) begin
    jtag_tap_tdoUnbufferd = jtag_tap_bypass;
    case(jtag_tap_fsm_state)
      JtagState_IR_SHIFT : begin
        jtag_tap_tdoUnbufferd = jtag_tap_tdoIr;
      end
      JtagState_DR_SHIFT : begin
        if(jtag_tap_isBypass) begin
          jtag_tap_tdoUnbufferd = jtag_tap_bypass;
        end else begin
          jtag_tap_tdoUnbufferd = jtag_tap_tdoDr;
        end
      end
      default : begin
      end
    endcase
  end

  always @(*) begin
    jtag_tap_tdoDr = 1'b0;
    if(jtag_idcodeArea_ctrl_enable) begin
      jtag_tap_tdoDr = jtag_idcodeArea_ctrl_tdo;
    end
    if(jtag_writeArea_ctrl_enable) begin
      jtag_tap_tdoDr = jtag_writeArea_ctrl_tdo;
    end
    if(jtag_readArea_ctrl_enable) begin
      jtag_tap_tdoDr = jtag_readArea_ctrl_tdo;
    end
  end

  assign jtag_tap_tdoIr = jtag_tap_instructionShift[0];
  assign jtag_tap_isBypass = ($signed(_zz_jtag_tap_isBypass) == $signed(_zz_jtag_tap_isBypass_1));
  assign io_jtag_tdo = jtag_tap_tdoUnbufferd_regNext;
  assign jtag_idcodeArea_ctrl_tdo = jtag_idcodeArea_shifter[0];
  assign jtag_idcodeArea_ctrl_tdi = io_jtag_tdi;
  assign jtag_idcodeArea_ctrl_enable = (jtag_tap_instruction == 4'b0001);
  assign jtag_idcodeArea_ctrl_capture = (jtag_tap_fsm_state == JtagState_DR_CAPTURE);
  assign jtag_idcodeArea_ctrl_shift = (jtag_tap_fsm_state == JtagState_DR_SHIFT);
  assign jtag_idcodeArea_ctrl_update = (jtag_tap_fsm_state == JtagState_DR_UPDATE);
  assign jtag_idcodeArea_ctrl_reset = (jtag_tap_fsm_state == JtagState_RESET);
  assign when_JtagTap_l120 = (jtag_tap_fsm_state == JtagState_RESET);
  assign jtag_writeArea_source_valid = jtag_writeArea_valid;
  assign jtag_writeArea_source_payload_last = (! (jtag_writeArea_ctrl_enable && jtag_writeArea_ctrl_shift));
  assign jtag_writeArea_source_payload_fragment[0] = jtag_writeArea_data;
  assign system_cmd_valid = flowCCByToggle_1_io_output_valid;
  assign system_cmd_payload_last = flowCCByToggle_1_io_output_payload_last;
  assign system_cmd_payload_fragment = flowCCByToggle_1_io_output_payload_fragment;
  assign jtag_writeArea_ctrl_tdo = 1'b0;
  assign jtag_writeArea_ctrl_tdi = io_jtag_tdi;
  assign jtag_writeArea_ctrl_enable = (jtag_tap_instruction == 4'b0010);
  assign jtag_writeArea_ctrl_capture = (jtag_tap_fsm_state == JtagState_DR_CAPTURE);
  assign jtag_writeArea_ctrl_shift = (jtag_tap_fsm_state == JtagState_DR_SHIFT);
  assign jtag_writeArea_ctrl_update = (jtag_tap_fsm_state == JtagState_DR_UPDATE);
  assign jtag_writeArea_ctrl_reset = (jtag_tap_fsm_state == JtagState_RESET);
  assign jtag_readArea_ctrl_tdo = jtag_readArea_full_shifter[0];
  assign jtag_readArea_ctrl_tdi = io_jtag_tdi;
  assign jtag_readArea_ctrl_enable = (jtag_tap_instruction == 4'b0011);
  assign jtag_readArea_ctrl_capture = (jtag_tap_fsm_state == JtagState_DR_CAPTURE);
  assign jtag_readArea_ctrl_shift = (jtag_tap_fsm_state == JtagState_DR_SHIFT);
  assign jtag_readArea_ctrl_update = (jtag_tap_fsm_state == JtagState_DR_UPDATE);
  assign jtag_readArea_ctrl_reset = (jtag_tap_fsm_state == JtagState_RESET);
  always @(posedge debugCdCtrl_external_clk) begin
    if(io_remote_cmd_valid) begin
      system_rsp_valid <= 1'b0;
    end
    if(io_remote_rsp_fire) begin
      system_rsp_valid <= 1'b1;
      system_rsp_payload_error <= io_remote_rsp_payload_error;
      system_rsp_payload_data <= io_remote_rsp_payload_data;
    end
  end

  always @(posedge io_jtag_tck) begin
    jtag_tap_fsm_state <= jtag_tap_fsm_stateNext;
    jtag_tap_bypass <= io_jtag_tdi;
    case(jtag_tap_fsm_state)
      JtagState_IR_CAPTURE : begin
        jtag_tap_instructionShift <= {2'd0, _zz_jtag_tap_instructionShift};
      end
      JtagState_IR_SHIFT : begin
        jtag_tap_instructionShift <= ({io_jtag_tdi,jtag_tap_instructionShift} >>> 1);
      end
      JtagState_IR_UPDATE : begin
        jtag_tap_instruction <= jtag_tap_instructionShift;
      end
      JtagState_DR_SHIFT : begin
        jtag_tap_instructionShift <= ({io_jtag_tdi,jtag_tap_instructionShift} >>> 1);
      end
      default : begin
      end
    endcase
    if(jtag_idcodeArea_ctrl_enable) begin
      if(jtag_idcodeArea_ctrl_shift) begin
        jtag_idcodeArea_shifter <= ({jtag_idcodeArea_ctrl_tdi,jtag_idcodeArea_shifter} >>> 1);
      end
    end
    if(jtag_idcodeArea_ctrl_capture) begin
      jtag_idcodeArea_shifter <= 32'h10001fff;
    end
    if(when_JtagTap_l120) begin
      jtag_tap_instruction <= 4'b0001;
    end
    jtag_writeArea_valid <= (jtag_writeArea_ctrl_enable && jtag_writeArea_ctrl_shift);
    jtag_writeArea_data <= jtag_writeArea_ctrl_tdi;
    if(jtag_readArea_ctrl_enable) begin
      if(jtag_readArea_ctrl_capture) begin
        jtag_readArea_full_shifter <= {{system_rsp_payload_data,system_rsp_payload_error},system_rsp_valid};
      end
      if(jtag_readArea_ctrl_shift) begin
        jtag_readArea_full_shifter <= ({jtag_readArea_ctrl_tdi,jtag_readArea_full_shifter} >>> 1);
      end
    end
  end

  always @(negedge io_jtag_tck) begin
    jtag_tap_tdoUnbufferd_regNext <= jtag_tap_tdoUnbufferd;
  end


endmodule

module VexRiscv (
  output              iBus_cmd_valid,
  input               iBus_cmd_ready,
  output     [31:0]   iBus_cmd_payload_pc,
  input               iBus_rsp_valid,
  input               iBus_rsp_payload_error,
  input      [31:0]   iBus_rsp_payload_inst,
  input               debug_bus_cmd_valid,
  output reg          debug_bus_cmd_ready,
  input               debug_bus_cmd_payload_wr,
  input      [7:0]    debug_bus_cmd_payload_address,
  input      [31:0]   debug_bus_cmd_payload_data,
  output reg [31:0]   debug_bus_rsp_data,
  output              debug_resetOut,
  output              dBus_cmd_valid,
  input               dBus_cmd_ready,
  output              dBus_cmd_payload_wr,
  output     [31:0]   dBus_cmd_payload_address,
  output     [31:0]   dBus_cmd_payload_data,
  output     [1:0]    dBus_cmd_payload_size,
  input               dBus_rsp_ready,
  input               dBus_rsp_error,
  input      [31:0]   dBus_rsp_data,
  input               debugCdCtrl_external_clk,
  input               systemCdCtrl_logic_outputReset,
  input               debugCdCtrl_logic_outputReset
);
  localparam BranchCtrlEnum_INC = 2'd0;
  localparam BranchCtrlEnum_B = 2'd1;
  localparam BranchCtrlEnum_JAL = 2'd2;
  localparam BranchCtrlEnum_JALR = 2'd3;
  localparam ShiftCtrlEnum_DISABLE_1 = 2'd0;
  localparam ShiftCtrlEnum_SLL_1 = 2'd1;
  localparam ShiftCtrlEnum_SRL_1 = 2'd2;
  localparam ShiftCtrlEnum_SRA_1 = 2'd3;
  localparam AluBitwiseCtrlEnum_XOR_1 = 2'd0;
  localparam AluBitwiseCtrlEnum_OR_1 = 2'd1;
  localparam AluBitwiseCtrlEnum_AND_1 = 2'd2;
  localparam AluCtrlEnum_ADD_SUB = 2'd0;
  localparam AluCtrlEnum_SLT_SLTU = 2'd1;
  localparam AluCtrlEnum_BITWISE = 2'd2;
  localparam Src2CtrlEnum_RS = 2'd0;
  localparam Src2CtrlEnum_IMI = 2'd1;
  localparam Src2CtrlEnum_IMS = 2'd2;
  localparam Src2CtrlEnum_PC = 2'd3;
  localparam Src1CtrlEnum_RS = 2'd0;
  localparam Src1CtrlEnum_IMU = 2'd1;
  localparam Src1CtrlEnum_PC_INCREMENT = 2'd2;
  localparam Src1CtrlEnum_URS1 = 2'd3;

  wire                IBusSimplePlugin_rspJoin_rspBuffer_c_io_pop_ready;
  reg        [31:0]   _zz_RegFilePlugin_regFile_port0;
  reg        [31:0]   _zz_RegFilePlugin_regFile_port1;
  wire                IBusSimplePlugin_rspJoin_rspBuffer_c_io_push_ready;
  wire                IBusSimplePlugin_rspJoin_rspBuffer_c_io_pop_valid;
  wire                IBusSimplePlugin_rspJoin_rspBuffer_c_io_pop_payload_error;
  wire       [31:0]   IBusSimplePlugin_rspJoin_rspBuffer_c_io_pop_payload_inst;
  wire       [0:0]    IBusSimplePlugin_rspJoin_rspBuffer_c_io_occupancy;
  wire       [31:0]   _zz_IBusSimplePlugin_fetchPc_pc;
  wire       [2:0]    _zz_IBusSimplePlugin_fetchPc_pc_1;
  wire       [2:0]    _zz_IBusSimplePlugin_pending_next;
  wire       [2:0]    _zz_IBusSimplePlugin_pending_next_1;
  wire       [0:0]    _zz_IBusSimplePlugin_pending_next_2;
  wire       [2:0]    _zz_IBusSimplePlugin_pending_next_3;
  wire       [0:0]    _zz_IBusSimplePlugin_pending_next_4;
  wire       [2:0]    _zz_IBusSimplePlugin_rspJoin_rspBuffer_discardCounter;
  wire       [0:0]    _zz_IBusSimplePlugin_rspJoin_rspBuffer_discardCounter_1;
  wire       [2:0]    _zz_IBusSimplePlugin_rspJoin_rspBuffer_discardCounter_2;
  wire       [0:0]    _zz_IBusSimplePlugin_rspJoin_rspBuffer_discardCounter_3;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2;
  wire       [0:0]    _zz__zz_decode_BRANCH_CTRL_2_1;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_2;
  wire       [0:0]    _zz__zz_decode_BRANCH_CTRL_2_3;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_4;
  wire                _zz__zz_decode_BRANCH_CTRL_2_5;
  wire       [0:0]    _zz__zz_decode_BRANCH_CTRL_2_6;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_7;
  wire                _zz__zz_decode_BRANCH_CTRL_2_8;
  wire                _zz__zz_decode_BRANCH_CTRL_2_9;
  wire                _zz__zz_decode_BRANCH_CTRL_2_10;
  wire       [0:0]    _zz__zz_decode_BRANCH_CTRL_2_11;
  wire       [0:0]    _zz__zz_decode_BRANCH_CTRL_2_12;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_13;
  wire       [0:0]    _zz__zz_decode_BRANCH_CTRL_2_14;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_15;
  wire       [17:0]   _zz__zz_decode_BRANCH_CTRL_2_16;
  wire       [0:0]    _zz__zz_decode_BRANCH_CTRL_2_17;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_18;
  wire                _zz__zz_decode_BRANCH_CTRL_2_19;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_20;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_21;
  wire       [0:0]    _zz__zz_decode_BRANCH_CTRL_2_22;
  wire       [0:0]    _zz__zz_decode_BRANCH_CTRL_2_23;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_24;
  wire       [0:0]    _zz__zz_decode_BRANCH_CTRL_2_25;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_26;
  wire       [14:0]   _zz__zz_decode_BRANCH_CTRL_2_27;
  wire       [1:0]    _zz__zz_decode_BRANCH_CTRL_2_28;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_29;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_30;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_31;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_32;
  wire                _zz__zz_decode_BRANCH_CTRL_2_33;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_34;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_35;
  wire       [0:0]    _zz__zz_decode_BRANCH_CTRL_2_36;
  wire       [0:0]    _zz__zz_decode_BRANCH_CTRL_2_37;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_38;
  wire       [0:0]    _zz__zz_decode_BRANCH_CTRL_2_39;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_40;
  wire       [11:0]   _zz__zz_decode_BRANCH_CTRL_2_41;
  wire       [2:0]    _zz__zz_decode_BRANCH_CTRL_2_42;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_43;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_44;
  wire                _zz__zz_decode_BRANCH_CTRL_2_45;
  wire                _zz__zz_decode_BRANCH_CTRL_2_46;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_47;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_48;
  wire       [0:0]    _zz__zz_decode_BRANCH_CTRL_2_49;
  wire       [8:0]    _zz__zz_decode_BRANCH_CTRL_2_50;
  wire       [0:0]    _zz__zz_decode_BRANCH_CTRL_2_51;
  wire                _zz__zz_decode_BRANCH_CTRL_2_52;
  wire                _zz__zz_decode_BRANCH_CTRL_2_53;
  wire       [0:0]    _zz__zz_decode_BRANCH_CTRL_2_54;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_55;
  wire       [0:0]    _zz__zz_decode_BRANCH_CTRL_2_56;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_57;
  wire       [0:0]    _zz__zz_decode_BRANCH_CTRL_2_58;
  wire                _zz__zz_decode_BRANCH_CTRL_2_59;
  wire       [5:0]    _zz__zz_decode_BRANCH_CTRL_2_60;
  wire       [0:0]    _zz__zz_decode_BRANCH_CTRL_2_61;
  wire       [0:0]    _zz__zz_decode_BRANCH_CTRL_2_62;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_63;
  wire       [1:0]    _zz__zz_decode_BRANCH_CTRL_2_64;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_65;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_66;
  wire                _zz__zz_decode_BRANCH_CTRL_2_67;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_68;
  wire       [31:0]   _zz__zz_decode_BRANCH_CTRL_2_69;
  wire       [0:0]    _zz__zz_decode_BRANCH_CTRL_2_70;
  wire       [0:0]    _zz__zz_decode_BRANCH_CTRL_2_71;
  wire       [1:0]    _zz__zz_decode_BRANCH_CTRL_2_72;
  wire       [1:0]    _zz__zz_decode_BRANCH_CTRL_2_73;
  wire       [0:0]    _zz__zz_decode_BRANCH_CTRL_2_74;
  wire       [0:0]    _zz__zz_decode_BRANCH_CTRL_2_75;
  wire                _zz_RegFilePlugin_regFile_port;
  wire                _zz_execute_RegFilePlugin_rs1Data;
  wire                _zz_RegFilePlugin_regFile_port_1;
  wire                _zz_execute_RegFilePlugin_rs2Data;
  wire       [0:0]    _zz__zz_execute_REGFILE_WRITE_DATA;
  wire       [2:0]    _zz__zz_execute_SRC1;
  wire       [4:0]    _zz__zz_execute_SRC1_1;
  wire       [11:0]   _zz__zz_execute_SRC2_2;
  wire       [31:0]   _zz_execute_SrcPlugin_addSub;
  wire       [31:0]   _zz_execute_SrcPlugin_addSub_1;
  wire       [31:0]   _zz_execute_SrcPlugin_addSub_2;
  wire       [31:0]   _zz_execute_SrcPlugin_addSub_3;
  wire       [31:0]   _zz_execute_SrcPlugin_addSub_4;
  wire       [31:0]   _zz_execute_SrcPlugin_addSub_5;
  wire       [31:0]   _zz_execute_SrcPlugin_addSub_6;
  wire       [31:0]   _zz__zz_lastStageRegFileWrite_payload_data_1;
  wire       [32:0]   _zz__zz_lastStageRegFileWrite_payload_data_1_1;
  wire       [19:0]   _zz__zz_execute_BranchPlugin_branch_src2;
  wire       [11:0]   _zz__zz_execute_BranchPlugin_branch_src2_4;
  wire       [31:0]   execute_REGFILE_WRITE_DATA;
  wire                decode_DO_EBREAK;
  wire                decode_SRC2_FORCE_ZERO;
  wire       [1:0]    decode_BRANCH_CTRL;
  wire       [1:0]    _zz_decode_BRANCH_CTRL;
  wire       [1:0]    _zz_decode_to_execute_BRANCH_CTRL;
  wire       [1:0]    _zz_decode_to_execute_BRANCH_CTRL_1;
  wire       [1:0]    decode_SHIFT_CTRL;
  wire       [1:0]    _zz_decode_SHIFT_CTRL;
  wire       [1:0]    _zz_decode_to_execute_SHIFT_CTRL;
  wire       [1:0]    _zz_decode_to_execute_SHIFT_CTRL_1;
  wire       [1:0]    decode_ALU_BITWISE_CTRL;
  wire       [1:0]    _zz_decode_ALU_BITWISE_CTRL;
  wire       [1:0]    _zz_decode_to_execute_ALU_BITWISE_CTRL;
  wire       [1:0]    _zz_decode_to_execute_ALU_BITWISE_CTRL_1;
  wire                decode_SRC_LESS_UNSIGNED;
  wire       [1:0]    decode_ALU_CTRL;
  wire       [1:0]    _zz_decode_ALU_CTRL;
  wire       [1:0]    _zz_decode_to_execute_ALU_CTRL;
  wire       [1:0]    _zz_decode_to_execute_ALU_CTRL_1;
  wire                decode_RS2_USE;
  wire                decode_MEMORY_STORE;
  wire                execute_REGFILE_WRITE_VALID;
  wire       [1:0]    decode_SRC2_CTRL;
  wire       [1:0]    _zz_decode_SRC2_CTRL;
  wire       [1:0]    _zz_decode_to_execute_SRC2_CTRL;
  wire       [1:0]    _zz_decode_to_execute_SRC2_CTRL_1;
  wire                decode_RS1_USE;
  wire                decode_MEMORY_ENABLE;
  wire       [1:0]    decode_SRC1_CTRL;
  wire       [1:0]    _zz_decode_SRC1_CTRL;
  wire       [1:0]    _zz_decode_to_execute_SRC1_CTRL;
  wire       [1:0]    _zz_decode_to_execute_SRC1_CTRL_1;
  wire       [31:0]   execute_FORMAL_PC_NEXT;
  wire       [31:0]   decode_FORMAL_PC_NEXT;
  wire                execute_DO_EBREAK;
  wire                decode_IS_EBREAK;
  wire                execute_RS2_USE;
  wire                execute_RS1_USE;
  wire                execute_IS_FENCEI;
  reg        [31:0]   _zz_decode_to_execute_INSTRUCTION;
  wire                decode_IS_FENCEI;
  wire       [31:0]   execute_BRANCH_CALC;
  wire                execute_BRANCH_DO;
  wire       [31:0]   execute_RS1;
  wire       [1:0]    execute_BRANCH_CTRL;
  wire       [1:0]    _zz_execute_BRANCH_CTRL;
  wire       [1:0]    execute_SHIFT_CTRL;
  wire       [1:0]    _zz_execute_SHIFT_CTRL;
  wire                execute_SRC_LESS_UNSIGNED;
  wire                execute_SRC2_FORCE_ZERO;
  wire                execute_SRC_USE_SUB_LESS;
  wire       [1:0]    execute_SRC2_CTRL;
  wire       [1:0]    _zz_execute_SRC2_CTRL;
  wire       [1:0]    execute_SRC1_CTRL;
  wire       [1:0]    _zz_execute_SRC1_CTRL;
  wire                decode_SRC_USE_SUB_LESS;
  wire                decode_SRC_ADD_ZERO;
  wire       [31:0]   execute_SRC_ADD_SUB;
  wire                execute_SRC_LESS;
  wire       [1:0]    execute_ALU_CTRL;
  wire       [1:0]    _zz_execute_ALU_CTRL;
  wire       [31:0]   execute_SRC2;
  wire       [31:0]   execute_SRC1;
  wire       [1:0]    execute_ALU_BITWISE_CTRL;
  wire       [1:0]    _zz_execute_ALU_BITWISE_CTRL;
  wire       [31:0]   _zz_lastStageRegFileWrite_payload_address;
  wire                _zz_lastStageRegFileWrite_valid;
  reg                 _zz_1;
  reg                 decode_REGFILE_WRITE_VALID;
  wire       [1:0]    _zz_decode_BRANCH_CTRL_1;
  wire       [1:0]    _zz_decode_SHIFT_CTRL_1;
  wire       [1:0]    _zz_decode_ALU_BITWISE_CTRL_1;
  wire       [1:0]    _zz_decode_ALU_CTRL_1;
  wire       [1:0]    _zz_decode_SRC2_CTRL_1;
  wire       [1:0]    _zz_decode_SRC1_CTRL_1;
  reg        [31:0]   _zz_lastStageRegFileWrite_payload_data;
  wire       [1:0]    execute_MEMORY_ADDRESS_LOW;
  wire       [31:0]   execute_MEMORY_READ_DATA;
  wire       [31:0]   execute_SRC_ADD;
  wire       [31:0]   execute_RS2;
  wire                execute_MEMORY_STORE;
  wire                execute_MEMORY_ENABLE;
  wire                execute_ALIGNEMENT_FAULT;
  wire       [31:0]   decode_PC;
  wire       [31:0]   decode_INSTRUCTION;
  wire       [31:0]   execute_PC;
  wire       [31:0]   execute_INSTRUCTION;
  reg                 decode_arbitration_haltItself;
  wire                decode_arbitration_haltByOther;
  reg                 decode_arbitration_removeIt;
  wire                decode_arbitration_flushIt;
  wire                decode_arbitration_flushNext;
  reg                 decode_arbitration_isValid;
  wire                decode_arbitration_isStuck;
  wire                decode_arbitration_isStuckByOthers;
  wire                decode_arbitration_isFlushed;
  wire                decode_arbitration_isMoving;
  wire                decode_arbitration_isFiring;
  reg                 execute_arbitration_haltItself;
  reg                 execute_arbitration_haltByOther;
  reg                 execute_arbitration_removeIt;
  reg                 execute_arbitration_flushIt;
  reg                 execute_arbitration_flushNext;
  reg                 execute_arbitration_isValid;
  wire                execute_arbitration_isStuck;
  wire                execute_arbitration_isStuckByOthers;
  wire                execute_arbitration_isFlushed;
  wire                execute_arbitration_isMoving;
  wire                execute_arbitration_isFiring;
  wire       [31:0]   lastStageInstruction /* verilator public */ ;
  wire       [31:0]   lastStagePc /* verilator public */ ;
  wire                lastStageIsValid /* verilator public */ ;
  wire                lastStageIsFiring /* verilator public */ ;
  reg                 IBusSimplePlugin_fetcherHalt;
  wire                IBusSimplePlugin_forceNoDecodeCond;
  reg                 IBusSimplePlugin_incomingInstruction;
  wire                IBusSimplePlugin_pcValids_0;
  wire                IBusSimplePlugin_pcValids_1;
  wire                BranchPlugin_jumpInterface_valid;
  wire       [31:0]   BranchPlugin_jumpInterface_payload;
  reg                 BranchPlugin_inDebugNoFetchFlag;
  reg                 IBusSimplePlugin_injectionPort_valid;
  reg                 IBusSimplePlugin_injectionPort_ready;
  wire       [31:0]   IBusSimplePlugin_injectionPort_payload;
  wire                IBusSimplePlugin_externalFlush;
  wire                IBusSimplePlugin_jump_pcLoad_valid;
  wire       [31:0]   IBusSimplePlugin_jump_pcLoad_payload;
  wire                IBusSimplePlugin_fetchPc_output_valid;
  wire                IBusSimplePlugin_fetchPc_output_ready;
  wire       [31:0]   IBusSimplePlugin_fetchPc_output_payload;
  reg        [31:0]   IBusSimplePlugin_fetchPc_pcReg /* verilator public */ ;
  reg                 IBusSimplePlugin_fetchPc_correction;
  reg                 IBusSimplePlugin_fetchPc_correctionReg;
  wire                IBusSimplePlugin_fetchPc_output_fire;
  wire                IBusSimplePlugin_fetchPc_corrected;
  reg                 IBusSimplePlugin_fetchPc_pcRegPropagate;
  reg                 IBusSimplePlugin_fetchPc_booted;
  reg                 IBusSimplePlugin_fetchPc_inc;
  wire                when_Fetcher_l134;
  wire                IBusSimplePlugin_fetchPc_output_fire_1;
  wire                when_Fetcher_l134_1;
  reg        [31:0]   IBusSimplePlugin_fetchPc_pc;
  reg                 IBusSimplePlugin_fetchPc_flushed;
  wire                when_Fetcher_l161;
  wire                IBusSimplePlugin_iBusRsp_redoFetch;
  wire                IBusSimplePlugin_iBusRsp_stages_0_input_valid;
  wire                IBusSimplePlugin_iBusRsp_stages_0_input_ready;
  wire       [31:0]   IBusSimplePlugin_iBusRsp_stages_0_input_payload;
  wire                IBusSimplePlugin_iBusRsp_stages_0_output_valid;
  wire                IBusSimplePlugin_iBusRsp_stages_0_output_ready;
  wire       [31:0]   IBusSimplePlugin_iBusRsp_stages_0_output_payload;
  reg                 IBusSimplePlugin_iBusRsp_stages_0_halt;
  wire                IBusSimplePlugin_iBusRsp_stages_1_input_valid;
  wire                IBusSimplePlugin_iBusRsp_stages_1_input_ready;
  wire       [31:0]   IBusSimplePlugin_iBusRsp_stages_1_input_payload;
  wire                IBusSimplePlugin_iBusRsp_stages_1_output_valid;
  wire                IBusSimplePlugin_iBusRsp_stages_1_output_ready;
  wire       [31:0]   IBusSimplePlugin_iBusRsp_stages_1_output_payload;
  wire                IBusSimplePlugin_iBusRsp_stages_1_halt;
  wire                _zz_IBusSimplePlugin_iBusRsp_stages_0_input_ready;
  wire                _zz_IBusSimplePlugin_iBusRsp_stages_1_input_ready;
  wire                IBusSimplePlugin_iBusRsp_flush;
  wire                _zz_IBusSimplePlugin_iBusRsp_stages_0_output_ready;
  wire                _zz_IBusSimplePlugin_iBusRsp_stages_0_output_ready_1;
  reg                 _zz_IBusSimplePlugin_iBusRsp_stages_0_output_ready_2;
  reg                 IBusSimplePlugin_iBusRsp_readyForError;
  wire                IBusSimplePlugin_iBusRsp_output_valid;
  wire                IBusSimplePlugin_iBusRsp_output_ready;
  wire       [31:0]   IBusSimplePlugin_iBusRsp_output_payload_pc;
  wire                IBusSimplePlugin_iBusRsp_output_payload_rsp_error;
  wire       [31:0]   IBusSimplePlugin_iBusRsp_output_payload_rsp_inst;
  wire                IBusSimplePlugin_iBusRsp_output_payload_isRvc;
  wire                IBusSimplePlugin_injector_decodeInput_valid;
  wire                IBusSimplePlugin_injector_decodeInput_ready;
  wire       [31:0]   IBusSimplePlugin_injector_decodeInput_payload_pc;
  wire                IBusSimplePlugin_injector_decodeInput_payload_rsp_error;
  wire       [31:0]   IBusSimplePlugin_injector_decodeInput_payload_rsp_inst;
  wire                IBusSimplePlugin_injector_decodeInput_payload_isRvc;
  reg                 _zz_IBusSimplePlugin_injector_decodeInput_valid;
  reg        [31:0]   _zz_IBusSimplePlugin_injector_decodeInput_payload_pc;
  reg                 _zz_IBusSimplePlugin_injector_decodeInput_payload_rsp_error;
  reg        [31:0]   _zz_IBusSimplePlugin_injector_decodeInput_payload_rsp_inst;
  reg                 _zz_IBusSimplePlugin_injector_decodeInput_payload_isRvc;
  wire                when_Fetcher_l323;
  reg                 IBusSimplePlugin_injector_nextPcCalc_valids_0;
  wire                when_Fetcher_l332;
  reg                 IBusSimplePlugin_injector_nextPcCalc_valids_1;
  wire                when_Fetcher_l332_1;
  reg                 IBusSimplePlugin_injector_nextPcCalc_valids_2;
  wire                when_Fetcher_l332_2;
  reg        [31:0]   IBusSimplePlugin_injector_formal_rawInDecode;
  wire                IBusSimplePlugin_cmd_valid;
  wire                IBusSimplePlugin_cmd_ready;
  wire       [31:0]   IBusSimplePlugin_cmd_payload_pc;
  wire                IBusSimplePlugin_cmd_s2mPipe_valid;
  wire                IBusSimplePlugin_cmd_s2mPipe_ready;
  wire       [31:0]   IBusSimplePlugin_cmd_s2mPipe_payload_pc;
  reg                 IBusSimplePlugin_cmd_rValid;
  reg        [31:0]   IBusSimplePlugin_cmd_rData_pc;
  wire                IBusSimplePlugin_pending_inc;
  wire                IBusSimplePlugin_pending_dec;
  reg        [2:0]    IBusSimplePlugin_pending_value;
  wire       [2:0]    IBusSimplePlugin_pending_next;
  wire                IBusSimplePlugin_cmdFork_canEmit;
  wire                when_IBusSimplePlugin_l305;
  wire                IBusSimplePlugin_cmd_fire;
  wire                IBusSimplePlugin_rspJoin_rspBuffer_output_valid;
  wire                IBusSimplePlugin_rspJoin_rspBuffer_output_ready;
  wire                IBusSimplePlugin_rspJoin_rspBuffer_output_payload_error;
  wire       [31:0]   IBusSimplePlugin_rspJoin_rspBuffer_output_payload_inst;
  reg        [2:0]    IBusSimplePlugin_rspJoin_rspBuffer_discardCounter;
  wire                iBus_rsp_toStream_valid;
  wire                iBus_rsp_toStream_ready;
  wire                iBus_rsp_toStream_payload_error;
  wire       [31:0]   iBus_rsp_toStream_payload_inst;
  wire                IBusSimplePlugin_rspJoin_rspBuffer_flush;
  wire                IBusSimplePlugin_rspJoin_rspBuffer_c_io_pop_fire;
  wire       [31:0]   IBusSimplePlugin_rspJoin_fetchRsp_pc;
  reg                 IBusSimplePlugin_rspJoin_fetchRsp_rsp_error;
  wire       [31:0]   IBusSimplePlugin_rspJoin_fetchRsp_rsp_inst;
  wire                IBusSimplePlugin_rspJoin_fetchRsp_isRvc;
  wire                when_IBusSimplePlugin_l376;
  wire                IBusSimplePlugin_rspJoin_join_valid;
  wire                IBusSimplePlugin_rspJoin_join_ready;
  wire       [31:0]   IBusSimplePlugin_rspJoin_join_payload_pc;
  wire                IBusSimplePlugin_rspJoin_join_payload_rsp_error;
  wire       [31:0]   IBusSimplePlugin_rspJoin_join_payload_rsp_inst;
  wire                IBusSimplePlugin_rspJoin_join_payload_isRvc;
  wire                IBusSimplePlugin_rspJoin_exceptionDetected;
  wire                IBusSimplePlugin_rspJoin_join_fire;
  wire                IBusSimplePlugin_rspJoin_join_fire_1;
  wire                _zz_IBusSimplePlugin_iBusRsp_output_valid;
  reg                 _zz_dBus_cmd_valid;
  wire                dBus_cmd_fire;
  wire                when_DBusSimplePlugin_l404;
  reg                 execute_DBusSimplePlugin_skipCmd;
  reg        [31:0]   _zz_dBus_cmd_payload_data;
  wire                when_DBusSimplePlugin_l428;
  reg        [3:0]    _zz_execute_DBusSimplePlugin_formalMask;
  wire       [3:0]    execute_DBusSimplePlugin_formalMask;
  wire                when_DBusSimplePlugin_l482;
  reg        [31:0]   execute_DBusSimplePlugin_rspShifted;
  wire       [1:0]    switch_Misc_l210;
  wire                _zz_execute_DBusSimplePlugin_rspFormated;
  reg        [31:0]   _zz_execute_DBusSimplePlugin_rspFormated_1;
  wire                _zz_execute_DBusSimplePlugin_rspFormated_2;
  reg        [31:0]   _zz_execute_DBusSimplePlugin_rspFormated_3;
  reg        [31:0]   execute_DBusSimplePlugin_rspFormated;
  wire                when_DBusSimplePlugin_l558;
  wire       [24:0]   _zz_decode_BRANCH_CTRL_2;
  wire                _zz_decode_BRANCH_CTRL_3;
  wire                _zz_decode_BRANCH_CTRL_4;
  wire                _zz_decode_BRANCH_CTRL_5;
  wire                _zz_decode_BRANCH_CTRL_6;
  wire       [1:0]    _zz_decode_SRC1_CTRL_2;
  wire       [1:0]    _zz_decode_SRC2_CTRL_2;
  wire       [1:0]    _zz_decode_ALU_CTRL_2;
  wire       [1:0]    _zz_decode_ALU_BITWISE_CTRL_2;
  wire       [1:0]    _zz_decode_SHIFT_CTRL_2;
  wire       [1:0]    _zz_decode_BRANCH_CTRL_7;
  wire                when_RegFilePlugin_l63;
  wire       [31:0]   execute_RegFilePlugin_srcInstruction;
  wire       [4:0]    execute_RegFilePlugin_regFileReadAddress1;
  wire       [4:0]    execute_RegFilePlugin_regFileReadAddress2;
  wire       [31:0]   execute_RegFilePlugin_rs1Data;
  wire       [31:0]   execute_RegFilePlugin_rs2Data;
  reg                 lastStageRegFileWrite_valid /* verilator public */ ;
  wire       [4:0]    lastStageRegFileWrite_payload_address /* verilator public */ ;
  wire       [31:0]   lastStageRegFileWrite_payload_data /* verilator public */ ;
  wire                when_RegFilePlugin_l107;
  reg        [31:0]   execute_IntAluPlugin_bitwise;
  reg        [31:0]   _zz_execute_REGFILE_WRITE_DATA;
  reg        [31:0]   _zz_execute_SRC1;
  wire                _zz_execute_SRC2;
  reg        [19:0]   _zz_execute_SRC2_1;
  wire                _zz_execute_SRC2_2;
  reg        [19:0]   _zz_execute_SRC2_3;
  reg        [31:0]   _zz_execute_SRC2_4;
  reg        [31:0]   execute_SrcPlugin_addSub;
  wire                execute_SrcPlugin_less;
  reg                 execute_LightShifterPlugin_isActive;
  wire                execute_LightShifterPlugin_isShift;
  reg        [4:0]    execute_LightShifterPlugin_amplitudeReg;
  wire       [4:0]    execute_LightShifterPlugin_amplitude;
  wire                when_ShiftPlugins_l163;
  reg        [31:0]   execute_LightShifterPlugin_shiftReg;
  wire       [31:0]   execute_LightShifterPlugin_shiftInput;
  wire                execute_LightShifterPlugin_done;
  wire                when_ShiftPlugins_l169;
  reg        [31:0]   _zz_lastStageRegFileWrite_payload_data_1;
  wire                when_ShiftPlugins_l175;
  wire                when_ShiftPlugins_l184;
  wire                execute_BranchPlugin_eq;
  wire       [2:0]    switch_Misc_l210_1;
  reg                 _zz_execute_BRANCH_DO;
  reg                 _zz_execute_BRANCH_DO_1;
  wire       [31:0]   execute_BranchPlugin_branch_src1;
  wire                _zz_execute_BranchPlugin_branch_src2;
  reg        [10:0]   _zz_execute_BranchPlugin_branch_src2_1;
  wire                _zz_execute_BranchPlugin_branch_src2_2;
  reg        [19:0]   _zz_execute_BranchPlugin_branch_src2_3;
  wire                _zz_execute_BranchPlugin_branch_src2_4;
  reg        [18:0]   _zz_execute_BranchPlugin_branch_src2_5;
  reg        [31:0]   _zz_execute_BranchPlugin_branch_src2_6;
  wire       [31:0]   execute_BranchPlugin_branch_src2;
  wire       [31:0]   execute_BranchPlugin_branchAdder;
  wire                when_BranchPlugin_l171;
  reg                 HazardSimplePlugin_src0Hazard;
  reg                 HazardSimplePlugin_src1Hazard;
  wire                HazardSimplePlugin_writeBackWrites_valid;
  wire       [4:0]    HazardSimplePlugin_writeBackWrites_payload_address;
  wire       [31:0]   HazardSimplePlugin_writeBackWrites_payload_data;
  reg                 HazardSimplePlugin_writeBackBuffer_valid;
  reg        [4:0]    HazardSimplePlugin_writeBackBuffer_payload_address;
  reg        [31:0]   HazardSimplePlugin_writeBackBuffer_payload_data;
  wire                HazardSimplePlugin_addr0Match;
  wire                HazardSimplePlugin_addr1Match;
  wire                when_HazardSimplePlugin_l105;
  wire                when_HazardSimplePlugin_l108;
  wire                when_HazardSimplePlugin_l113;
  reg                 DebugPlugin_firstCycle;
  reg                 DebugPlugin_secondCycle;
  reg                 DebugPlugin_resetIt;
  reg                 DebugPlugin_haltIt;
  reg                 DebugPlugin_stepIt;
  reg                 DebugPlugin_isPipBusy;
  reg                 DebugPlugin_godmode;
  wire                when_DebugPlugin_l225;
  reg                 DebugPlugin_haltedByBreak;
  reg                 DebugPlugin_debugUsed /* verilator public */ ;
  reg                 DebugPlugin_disableEbreak;
  wire                DebugPlugin_allowEBreak;
  reg        [31:0]   DebugPlugin_busReadDataReg;
  reg                 _zz_when_DebugPlugin_l244;
  wire                when_DebugPlugin_l244;
  wire       [5:0]    switch_DebugPlugin_l267;
  wire                when_DebugPlugin_l271;
  wire                when_DebugPlugin_l271_1;
  wire                when_DebugPlugin_l272;
  wire                when_DebugPlugin_l272_1;
  wire                when_DebugPlugin_l273;
  wire                when_DebugPlugin_l274;
  wire                when_DebugPlugin_l275;
  wire                when_DebugPlugin_l275_1;
  wire                when_DebugPlugin_l295;
  wire                when_DebugPlugin_l298;
  wire                when_DebugPlugin_l311;
  reg                 DebugPlugin_resetIt_regNext;
  wire                when_Pipeline_l124;
  reg        [31:0]   decode_to_execute_PC;
  wire                when_Pipeline_l124_1;
  reg        [31:0]   decode_to_execute_INSTRUCTION;
  wire                when_Pipeline_l124_2;
  reg        [31:0]   decode_to_execute_FORMAL_PC_NEXT;
  wire                when_Pipeline_l124_3;
  reg        [1:0]    decode_to_execute_SRC1_CTRL;
  wire                when_Pipeline_l124_4;
  reg                 decode_to_execute_SRC_USE_SUB_LESS;
  wire                when_Pipeline_l124_5;
  reg                 decode_to_execute_MEMORY_ENABLE;
  wire                when_Pipeline_l124_6;
  reg                 decode_to_execute_RS1_USE;
  wire                when_Pipeline_l124_7;
  reg        [1:0]    decode_to_execute_SRC2_CTRL;
  wire                when_Pipeline_l124_8;
  reg                 decode_to_execute_REGFILE_WRITE_VALID;
  wire                when_Pipeline_l124_9;
  reg                 decode_to_execute_MEMORY_STORE;
  wire                when_Pipeline_l124_10;
  reg                 decode_to_execute_RS2_USE;
  wire                when_Pipeline_l124_11;
  reg        [1:0]    decode_to_execute_ALU_CTRL;
  wire                when_Pipeline_l124_12;
  reg                 decode_to_execute_SRC_LESS_UNSIGNED;
  wire                when_Pipeline_l124_13;
  reg        [1:0]    decode_to_execute_ALU_BITWISE_CTRL;
  wire                when_Pipeline_l124_14;
  reg        [1:0]    decode_to_execute_SHIFT_CTRL;
  wire                when_Pipeline_l124_15;
  reg        [1:0]    decode_to_execute_BRANCH_CTRL;
  wire                when_Pipeline_l124_16;
  reg                 decode_to_execute_IS_FENCEI;
  wire                when_Pipeline_l124_17;
  reg                 decode_to_execute_SRC2_FORCE_ZERO;
  wire                when_Pipeline_l124_18;
  reg                 decode_to_execute_DO_EBREAK;
  wire                when_Pipeline_l151;
  wire                when_Pipeline_l154;
  reg        [2:0]    switch_Fetcher_l365;
  wire                when_Fetcher_l381;
  wire                when_Fetcher_l401;
  `ifndef SYNTHESIS
  reg [31:0] decode_BRANCH_CTRL_string;
  reg [31:0] _zz_decode_BRANCH_CTRL_string;
  reg [31:0] _zz_decode_to_execute_BRANCH_CTRL_string;
  reg [31:0] _zz_decode_to_execute_BRANCH_CTRL_1_string;
  reg [71:0] decode_SHIFT_CTRL_string;
  reg [71:0] _zz_decode_SHIFT_CTRL_string;
  reg [71:0] _zz_decode_to_execute_SHIFT_CTRL_string;
  reg [71:0] _zz_decode_to_execute_SHIFT_CTRL_1_string;
  reg [39:0] decode_ALU_BITWISE_CTRL_string;
  reg [39:0] _zz_decode_ALU_BITWISE_CTRL_string;
  reg [39:0] _zz_decode_to_execute_ALU_BITWISE_CTRL_string;
  reg [39:0] _zz_decode_to_execute_ALU_BITWISE_CTRL_1_string;
  reg [63:0] decode_ALU_CTRL_string;
  reg [63:0] _zz_decode_ALU_CTRL_string;
  reg [63:0] _zz_decode_to_execute_ALU_CTRL_string;
  reg [63:0] _zz_decode_to_execute_ALU_CTRL_1_string;
  reg [23:0] decode_SRC2_CTRL_string;
  reg [23:0] _zz_decode_SRC2_CTRL_string;
  reg [23:0] _zz_decode_to_execute_SRC2_CTRL_string;
  reg [23:0] _zz_decode_to_execute_SRC2_CTRL_1_string;
  reg [95:0] decode_SRC1_CTRL_string;
  reg [95:0] _zz_decode_SRC1_CTRL_string;
  reg [95:0] _zz_decode_to_execute_SRC1_CTRL_string;
  reg [95:0] _zz_decode_to_execute_SRC1_CTRL_1_string;
  reg [31:0] execute_BRANCH_CTRL_string;
  reg [31:0] _zz_execute_BRANCH_CTRL_string;
  reg [71:0] execute_SHIFT_CTRL_string;
  reg [71:0] _zz_execute_SHIFT_CTRL_string;
  reg [23:0] execute_SRC2_CTRL_string;
  reg [23:0] _zz_execute_SRC2_CTRL_string;
  reg [95:0] execute_SRC1_CTRL_string;
  reg [95:0] _zz_execute_SRC1_CTRL_string;
  reg [63:0] execute_ALU_CTRL_string;
  reg [63:0] _zz_execute_ALU_CTRL_string;
  reg [39:0] execute_ALU_BITWISE_CTRL_string;
  reg [39:0] _zz_execute_ALU_BITWISE_CTRL_string;
  reg [31:0] _zz_decode_BRANCH_CTRL_1_string;
  reg [71:0] _zz_decode_SHIFT_CTRL_1_string;
  reg [39:0] _zz_decode_ALU_BITWISE_CTRL_1_string;
  reg [63:0] _zz_decode_ALU_CTRL_1_string;
  reg [23:0] _zz_decode_SRC2_CTRL_1_string;
  reg [95:0] _zz_decode_SRC1_CTRL_1_string;
  reg [95:0] _zz_decode_SRC1_CTRL_2_string;
  reg [23:0] _zz_decode_SRC2_CTRL_2_string;
  reg [63:0] _zz_decode_ALU_CTRL_2_string;
  reg [39:0] _zz_decode_ALU_BITWISE_CTRL_2_string;
  reg [71:0] _zz_decode_SHIFT_CTRL_2_string;
  reg [31:0] _zz_decode_BRANCH_CTRL_7_string;
  reg [95:0] decode_to_execute_SRC1_CTRL_string;
  reg [23:0] decode_to_execute_SRC2_CTRL_string;
  reg [63:0] decode_to_execute_ALU_CTRL_string;
  reg [39:0] decode_to_execute_ALU_BITWISE_CTRL_string;
  reg [71:0] decode_to_execute_SHIFT_CTRL_string;
  reg [31:0] decode_to_execute_BRANCH_CTRL_string;
  `endif

  reg [31:0] RegFilePlugin_regFile [0:31] /* verilator public */ ;

  assign _zz_IBusSimplePlugin_fetchPc_pc_1 = {IBusSimplePlugin_fetchPc_inc,2'b00};
  assign _zz_IBusSimplePlugin_fetchPc_pc = {29'd0, _zz_IBusSimplePlugin_fetchPc_pc_1};
  assign _zz_IBusSimplePlugin_pending_next = (IBusSimplePlugin_pending_value + _zz_IBusSimplePlugin_pending_next_1);
  assign _zz_IBusSimplePlugin_pending_next_2 = IBusSimplePlugin_pending_inc;
  assign _zz_IBusSimplePlugin_pending_next_1 = {2'd0, _zz_IBusSimplePlugin_pending_next_2};
  assign _zz_IBusSimplePlugin_pending_next_4 = IBusSimplePlugin_pending_dec;
  assign _zz_IBusSimplePlugin_pending_next_3 = {2'd0, _zz_IBusSimplePlugin_pending_next_4};
  assign _zz_IBusSimplePlugin_rspJoin_rspBuffer_discardCounter_1 = (IBusSimplePlugin_rspJoin_rspBuffer_c_io_pop_valid && (IBusSimplePlugin_rspJoin_rspBuffer_discardCounter != 3'b000));
  assign _zz_IBusSimplePlugin_rspJoin_rspBuffer_discardCounter = {2'd0, _zz_IBusSimplePlugin_rspJoin_rspBuffer_discardCounter_1};
  assign _zz_IBusSimplePlugin_rspJoin_rspBuffer_discardCounter_3 = IBusSimplePlugin_pending_dec;
  assign _zz_IBusSimplePlugin_rspJoin_rspBuffer_discardCounter_2 = {2'd0, _zz_IBusSimplePlugin_rspJoin_rspBuffer_discardCounter_3};
  assign _zz__zz_execute_REGFILE_WRITE_DATA = execute_SRC_LESS;
  assign _zz__zz_execute_SRC1 = 3'b100;
  assign _zz__zz_execute_SRC1_1 = execute_INSTRUCTION[19 : 15];
  assign _zz__zz_execute_SRC2_2 = {execute_INSTRUCTION[31 : 25],execute_INSTRUCTION[11 : 7]};
  assign _zz_execute_SrcPlugin_addSub = ($signed(_zz_execute_SrcPlugin_addSub_1) + $signed(_zz_execute_SrcPlugin_addSub_4));
  assign _zz_execute_SrcPlugin_addSub_1 = ($signed(_zz_execute_SrcPlugin_addSub_2) + $signed(_zz_execute_SrcPlugin_addSub_3));
  assign _zz_execute_SrcPlugin_addSub_2 = execute_SRC1;
  assign _zz_execute_SrcPlugin_addSub_3 = (execute_SRC_USE_SUB_LESS ? (~ execute_SRC2) : execute_SRC2);
  assign _zz_execute_SrcPlugin_addSub_4 = (execute_SRC_USE_SUB_LESS ? _zz_execute_SrcPlugin_addSub_5 : _zz_execute_SrcPlugin_addSub_6);
  assign _zz_execute_SrcPlugin_addSub_5 = 32'h00000001;
  assign _zz_execute_SrcPlugin_addSub_6 = 32'h0;
  assign _zz__zz_lastStageRegFileWrite_payload_data_1 = (_zz__zz_lastStageRegFileWrite_payload_data_1_1 >>> 1);
  assign _zz__zz_lastStageRegFileWrite_payload_data_1_1 = {((execute_SHIFT_CTRL == ShiftCtrlEnum_SRA_1) && execute_LightShifterPlugin_shiftInput[31]),execute_LightShifterPlugin_shiftInput};
  assign _zz__zz_execute_BranchPlugin_branch_src2 = {{{execute_INSTRUCTION[31],execute_INSTRUCTION[19 : 12]},execute_INSTRUCTION[20]},execute_INSTRUCTION[30 : 21]};
  assign _zz__zz_execute_BranchPlugin_branch_src2_4 = {{{execute_INSTRUCTION[31],execute_INSTRUCTION[7]},execute_INSTRUCTION[30 : 25]},execute_INSTRUCTION[11 : 8]};
  assign _zz_execute_RegFilePlugin_rs1Data = 1'b1;
  assign _zz_execute_RegFilePlugin_rs2Data = 1'b1;
  assign _zz__zz_decode_BRANCH_CTRL_2 = 32'hffff9fc8;
  assign _zz__zz_decode_BRANCH_CTRL_2_1 = ((decode_INSTRUCTION & _zz__zz_decode_BRANCH_CTRL_2_2) == 32'h00000004);
  assign _zz__zz_decode_BRANCH_CTRL_2_3 = ((decode_INSTRUCTION & _zz__zz_decode_BRANCH_CTRL_2_4) == 32'h00001008);
  assign _zz__zz_decode_BRANCH_CTRL_2_5 = ((decode_INSTRUCTION & 32'h00000058) == 32'h00000040);
  assign _zz__zz_decode_BRANCH_CTRL_2_6 = ((decode_INSTRUCTION & _zz__zz_decode_BRANCH_CTRL_2_7) == 32'h00005010);
  assign _zz__zz_decode_BRANCH_CTRL_2_8 = (|{_zz__zz_decode_BRANCH_CTRL_2_9,_zz__zz_decode_BRANCH_CTRL_2_10});
  assign _zz__zz_decode_BRANCH_CTRL_2_11 = (|{_zz__zz_decode_BRANCH_CTRL_2_12,_zz__zz_decode_BRANCH_CTRL_2_14});
  assign _zz__zz_decode_BRANCH_CTRL_2_16 = {(|_zz__zz_decode_BRANCH_CTRL_2_17),{_zz__zz_decode_BRANCH_CTRL_2_19,{_zz__zz_decode_BRANCH_CTRL_2_22,_zz__zz_decode_BRANCH_CTRL_2_27}}};
  assign _zz__zz_decode_BRANCH_CTRL_2_2 = 32'h0000001c;
  assign _zz__zz_decode_BRANCH_CTRL_2_4 = 32'hffff9f88;
  assign _zz__zz_decode_BRANCH_CTRL_2_7 = 32'h00007014;
  assign _zz__zz_decode_BRANCH_CTRL_2_9 = ((decode_INSTRUCTION & 32'h40003014) == 32'h40001010);
  assign _zz__zz_decode_BRANCH_CTRL_2_10 = ((decode_INSTRUCTION & 32'h00007014) == 32'h00001010);
  assign _zz__zz_decode_BRANCH_CTRL_2_12 = ((decode_INSTRUCTION & _zz__zz_decode_BRANCH_CTRL_2_13) == 32'h00000024);
  assign _zz__zz_decode_BRANCH_CTRL_2_14 = ((decode_INSTRUCTION & _zz__zz_decode_BRANCH_CTRL_2_15) == 32'h00001010);
  assign _zz__zz_decode_BRANCH_CTRL_2_17 = ((decode_INSTRUCTION & _zz__zz_decode_BRANCH_CTRL_2_18) == 32'h00001000);
  assign _zz__zz_decode_BRANCH_CTRL_2_19 = (|(_zz__zz_decode_BRANCH_CTRL_2_20 == _zz__zz_decode_BRANCH_CTRL_2_21));
  assign _zz__zz_decode_BRANCH_CTRL_2_22 = (|{_zz__zz_decode_BRANCH_CTRL_2_23,_zz__zz_decode_BRANCH_CTRL_2_25});
  assign _zz__zz_decode_BRANCH_CTRL_2_27 = {(|_zz__zz_decode_BRANCH_CTRL_2_28),{_zz__zz_decode_BRANCH_CTRL_2_33,{_zz__zz_decode_BRANCH_CTRL_2_36,_zz__zz_decode_BRANCH_CTRL_2_41}}};
  assign _zz__zz_decode_BRANCH_CTRL_2_13 = 32'h00000064;
  assign _zz__zz_decode_BRANCH_CTRL_2_15 = 32'h00003014;
  assign _zz__zz_decode_BRANCH_CTRL_2_18 = 32'h00001000;
  assign _zz__zz_decode_BRANCH_CTRL_2_20 = (decode_INSTRUCTION & 32'h00003000);
  assign _zz__zz_decode_BRANCH_CTRL_2_21 = 32'h00002000;
  assign _zz__zz_decode_BRANCH_CTRL_2_23 = ((decode_INSTRUCTION & _zz__zz_decode_BRANCH_CTRL_2_24) == 32'h00002000);
  assign _zz__zz_decode_BRANCH_CTRL_2_25 = ((decode_INSTRUCTION & _zz__zz_decode_BRANCH_CTRL_2_26) == 32'h00001000);
  assign _zz__zz_decode_BRANCH_CTRL_2_28 = {(_zz__zz_decode_BRANCH_CTRL_2_29 == _zz__zz_decode_BRANCH_CTRL_2_30),(_zz__zz_decode_BRANCH_CTRL_2_31 == _zz__zz_decode_BRANCH_CTRL_2_32)};
  assign _zz__zz_decode_BRANCH_CTRL_2_33 = (|(_zz__zz_decode_BRANCH_CTRL_2_34 == _zz__zz_decode_BRANCH_CTRL_2_35));
  assign _zz__zz_decode_BRANCH_CTRL_2_36 = (|{_zz__zz_decode_BRANCH_CTRL_2_37,_zz__zz_decode_BRANCH_CTRL_2_39});
  assign _zz__zz_decode_BRANCH_CTRL_2_41 = {(|_zz__zz_decode_BRANCH_CTRL_2_42),{_zz__zz_decode_BRANCH_CTRL_2_46,{_zz__zz_decode_BRANCH_CTRL_2_49,_zz__zz_decode_BRANCH_CTRL_2_50}}};
  assign _zz__zz_decode_BRANCH_CTRL_2_24 = 32'h00002010;
  assign _zz__zz_decode_BRANCH_CTRL_2_26 = 32'h00005000;
  assign _zz__zz_decode_BRANCH_CTRL_2_29 = (decode_INSTRUCTION & 32'h00006004);
  assign _zz__zz_decode_BRANCH_CTRL_2_30 = 32'h00006000;
  assign _zz__zz_decode_BRANCH_CTRL_2_31 = (decode_INSTRUCTION & 32'h00005004);
  assign _zz__zz_decode_BRANCH_CTRL_2_32 = 32'h00004000;
  assign _zz__zz_decode_BRANCH_CTRL_2_34 = (decode_INSTRUCTION & 32'h00006004);
  assign _zz__zz_decode_BRANCH_CTRL_2_35 = 32'h00002000;
  assign _zz__zz_decode_BRANCH_CTRL_2_37 = ((decode_INSTRUCTION & _zz__zz_decode_BRANCH_CTRL_2_38) == 32'h00000020);
  assign _zz__zz_decode_BRANCH_CTRL_2_39 = ((decode_INSTRUCTION & _zz__zz_decode_BRANCH_CTRL_2_40) == 32'h00000020);
  assign _zz__zz_decode_BRANCH_CTRL_2_42 = {(_zz__zz_decode_BRANCH_CTRL_2_43 == _zz__zz_decode_BRANCH_CTRL_2_44),{_zz_decode_BRANCH_CTRL_3,_zz__zz_decode_BRANCH_CTRL_2_45}};
  assign _zz__zz_decode_BRANCH_CTRL_2_46 = (|(_zz__zz_decode_BRANCH_CTRL_2_47 == _zz__zz_decode_BRANCH_CTRL_2_48));
  assign _zz__zz_decode_BRANCH_CTRL_2_49 = (|_zz_decode_BRANCH_CTRL_6);
  assign _zz__zz_decode_BRANCH_CTRL_2_50 = {(|_zz__zz_decode_BRANCH_CTRL_2_51),{_zz__zz_decode_BRANCH_CTRL_2_52,{_zz__zz_decode_BRANCH_CTRL_2_58,_zz__zz_decode_BRANCH_CTRL_2_60}}};
  assign _zz__zz_decode_BRANCH_CTRL_2_38 = 32'h00000034;
  assign _zz__zz_decode_BRANCH_CTRL_2_40 = 32'h00000064;
  assign _zz__zz_decode_BRANCH_CTRL_2_43 = (decode_INSTRUCTION & 32'h00000050);
  assign _zz__zz_decode_BRANCH_CTRL_2_44 = 32'h00000040;
  assign _zz__zz_decode_BRANCH_CTRL_2_45 = ((decode_INSTRUCTION & 32'hffff9f90) == 32'h00001000);
  assign _zz__zz_decode_BRANCH_CTRL_2_47 = (decode_INSTRUCTION & 32'h00000020);
  assign _zz__zz_decode_BRANCH_CTRL_2_48 = 32'h00000020;
  assign _zz__zz_decode_BRANCH_CTRL_2_51 = _zz_decode_BRANCH_CTRL_6;
  assign _zz__zz_decode_BRANCH_CTRL_2_52 = (|{_zz_decode_BRANCH_CTRL_5,{_zz__zz_decode_BRANCH_CTRL_2_53,{_zz__zz_decode_BRANCH_CTRL_2_54,_zz__zz_decode_BRANCH_CTRL_2_56}}});
  assign _zz__zz_decode_BRANCH_CTRL_2_58 = (|{_zz_decode_BRANCH_CTRL_4,_zz__zz_decode_BRANCH_CTRL_2_59});
  assign _zz__zz_decode_BRANCH_CTRL_2_60 = {(|{_zz__zz_decode_BRANCH_CTRL_2_61,_zz__zz_decode_BRANCH_CTRL_2_62}),{(|_zz__zz_decode_BRANCH_CTRL_2_64),{_zz__zz_decode_BRANCH_CTRL_2_67,{_zz__zz_decode_BRANCH_CTRL_2_70,_zz__zz_decode_BRANCH_CTRL_2_73}}}};
  assign _zz__zz_decode_BRANCH_CTRL_2_53 = ((decode_INSTRUCTION & 32'h00000050) == 32'h00000010);
  assign _zz__zz_decode_BRANCH_CTRL_2_54 = ((decode_INSTRUCTION & _zz__zz_decode_BRANCH_CTRL_2_55) == 32'h00000004);
  assign _zz__zz_decode_BRANCH_CTRL_2_56 = ((decode_INSTRUCTION & _zz__zz_decode_BRANCH_CTRL_2_57) == 32'h0);
  assign _zz__zz_decode_BRANCH_CTRL_2_59 = ((decode_INSTRUCTION & 32'h00000070) == 32'h00000020);
  assign _zz__zz_decode_BRANCH_CTRL_2_61 = _zz_decode_BRANCH_CTRL_4;
  assign _zz__zz_decode_BRANCH_CTRL_2_62 = ((decode_INSTRUCTION & _zz__zz_decode_BRANCH_CTRL_2_63) == 32'h0);
  assign _zz__zz_decode_BRANCH_CTRL_2_64 = {(_zz__zz_decode_BRANCH_CTRL_2_65 == _zz__zz_decode_BRANCH_CTRL_2_66),_zz_decode_BRANCH_CTRL_3};
  assign _zz__zz_decode_BRANCH_CTRL_2_67 = (|(_zz__zz_decode_BRANCH_CTRL_2_68 == _zz__zz_decode_BRANCH_CTRL_2_69));
  assign _zz__zz_decode_BRANCH_CTRL_2_70 = (|{_zz__zz_decode_BRANCH_CTRL_2_71,_zz__zz_decode_BRANCH_CTRL_2_72});
  assign _zz__zz_decode_BRANCH_CTRL_2_73 = {(|_zz__zz_decode_BRANCH_CTRL_2_74),(|_zz__zz_decode_BRANCH_CTRL_2_75)};
  assign _zz__zz_decode_BRANCH_CTRL_2_55 = 32'h0000000c;
  assign _zz__zz_decode_BRANCH_CTRL_2_57 = 32'h00000028;
  assign _zz__zz_decode_BRANCH_CTRL_2_63 = 32'h00000020;
  assign _zz__zz_decode_BRANCH_CTRL_2_65 = (decode_INSTRUCTION & 32'h00000044);
  assign _zz__zz_decode_BRANCH_CTRL_2_66 = 32'h0;
  assign _zz__zz_decode_BRANCH_CTRL_2_68 = (decode_INSTRUCTION & 32'h00000058);
  assign _zz__zz_decode_BRANCH_CTRL_2_69 = 32'h0;
  assign _zz__zz_decode_BRANCH_CTRL_2_71 = ((decode_INSTRUCTION & 32'h00000044) == 32'h00000040);
  assign _zz__zz_decode_BRANCH_CTRL_2_72 = {((decode_INSTRUCTION & 32'h00002014) == 32'h00002010),((decode_INSTRUCTION & 32'h40004034) == 32'h40000030)};
  assign _zz__zz_decode_BRANCH_CTRL_2_74 = ((decode_INSTRUCTION & 32'h00000014) == 32'h00000004);
  assign _zz__zz_decode_BRANCH_CTRL_2_75 = ((decode_INSTRUCTION & 32'h00000044) == 32'h00000004);
  initial begin
    $readmemb("SoC.v_toplevel_system_cpu_logic_cpu_RegFilePlugin_regFile.bin",RegFilePlugin_regFile);
  end
  always @(posedge debugCdCtrl_external_clk) begin
    if(_zz_execute_RegFilePlugin_rs1Data) begin
      _zz_RegFilePlugin_regFile_port0 <= RegFilePlugin_regFile[execute_RegFilePlugin_regFileReadAddress1];
    end
  end

  always @(posedge debugCdCtrl_external_clk) begin
    if(_zz_execute_RegFilePlugin_rs2Data) begin
      _zz_RegFilePlugin_regFile_port1 <= RegFilePlugin_regFile[execute_RegFilePlugin_regFileReadAddress2];
    end
  end

  always @(posedge debugCdCtrl_external_clk) begin
    if(_zz_1) begin
      RegFilePlugin_regFile[lastStageRegFileWrite_payload_address] <= lastStageRegFileWrite_payload_data;
    end
  end

  StreamFifoLowLatency IBusSimplePlugin_rspJoin_rspBuffer_c (
    .io_push_valid                  (iBus_rsp_toStream_valid                                       ), //i
    .io_push_ready                  (IBusSimplePlugin_rspJoin_rspBuffer_c_io_push_ready            ), //o
    .io_push_payload_error          (iBus_rsp_toStream_payload_error                               ), //i
    .io_push_payload_inst           (iBus_rsp_toStream_payload_inst[31:0]                          ), //i
    .io_pop_valid                   (IBusSimplePlugin_rspJoin_rspBuffer_c_io_pop_valid             ), //o
    .io_pop_ready                   (IBusSimplePlugin_rspJoin_rspBuffer_c_io_pop_ready             ), //i
    .io_pop_payload_error           (IBusSimplePlugin_rspJoin_rspBuffer_c_io_pop_payload_error     ), //o
    .io_pop_payload_inst            (IBusSimplePlugin_rspJoin_rspBuffer_c_io_pop_payload_inst[31:0]), //o
    .io_flush                       (1'b0                                                          ), //i
    .io_occupancy                   (IBusSimplePlugin_rspJoin_rspBuffer_c_io_occupancy             ), //o
    .debugCdCtrl_external_clk       (debugCdCtrl_external_clk                                      ), //i
    .systemCdCtrl_logic_outputReset (systemCdCtrl_logic_outputReset                                )  //i
  );
  `ifndef SYNTHESIS
  always @(*) begin
    case(decode_BRANCH_CTRL)
      BranchCtrlEnum_INC : decode_BRANCH_CTRL_string = "INC ";
      BranchCtrlEnum_B : decode_BRANCH_CTRL_string = "B   ";
      BranchCtrlEnum_JAL : decode_BRANCH_CTRL_string = "JAL ";
      BranchCtrlEnum_JALR : decode_BRANCH_CTRL_string = "JALR";
      default : decode_BRANCH_CTRL_string = "????";
    endcase
  end
  always @(*) begin
    case(_zz_decode_BRANCH_CTRL)
      BranchCtrlEnum_INC : _zz_decode_BRANCH_CTRL_string = "INC ";
      BranchCtrlEnum_B : _zz_decode_BRANCH_CTRL_string = "B   ";
      BranchCtrlEnum_JAL : _zz_decode_BRANCH_CTRL_string = "JAL ";
      BranchCtrlEnum_JALR : _zz_decode_BRANCH_CTRL_string = "JALR";
      default : _zz_decode_BRANCH_CTRL_string = "????";
    endcase
  end
  always @(*) begin
    case(_zz_decode_to_execute_BRANCH_CTRL)
      BranchCtrlEnum_INC : _zz_decode_to_execute_BRANCH_CTRL_string = "INC ";
      BranchCtrlEnum_B : _zz_decode_to_execute_BRANCH_CTRL_string = "B   ";
      BranchCtrlEnum_JAL : _zz_decode_to_execute_BRANCH_CTRL_string = "JAL ";
      BranchCtrlEnum_JALR : _zz_decode_to_execute_BRANCH_CTRL_string = "JALR";
      default : _zz_decode_to_execute_BRANCH_CTRL_string = "????";
    endcase
  end
  always @(*) begin
    case(_zz_decode_to_execute_BRANCH_CTRL_1)
      BranchCtrlEnum_INC : _zz_decode_to_execute_BRANCH_CTRL_1_string = "INC ";
      BranchCtrlEnum_B : _zz_decode_to_execute_BRANCH_CTRL_1_string = "B   ";
      BranchCtrlEnum_JAL : _zz_decode_to_execute_BRANCH_CTRL_1_string = "JAL ";
      BranchCtrlEnum_JALR : _zz_decode_to_execute_BRANCH_CTRL_1_string = "JALR";
      default : _zz_decode_to_execute_BRANCH_CTRL_1_string = "????";
    endcase
  end
  always @(*) begin
    case(decode_SHIFT_CTRL)
      ShiftCtrlEnum_DISABLE_1 : decode_SHIFT_CTRL_string = "DISABLE_1";
      ShiftCtrlEnum_SLL_1 : decode_SHIFT_CTRL_string = "SLL_1    ";
      ShiftCtrlEnum_SRL_1 : decode_SHIFT_CTRL_string = "SRL_1    ";
      ShiftCtrlEnum_SRA_1 : decode_SHIFT_CTRL_string = "SRA_1    ";
      default : decode_SHIFT_CTRL_string = "?????????";
    endcase
  end
  always @(*) begin
    case(_zz_decode_SHIFT_CTRL)
      ShiftCtrlEnum_DISABLE_1 : _zz_decode_SHIFT_CTRL_string = "DISABLE_1";
      ShiftCtrlEnum_SLL_1 : _zz_decode_SHIFT_CTRL_string = "SLL_1    ";
      ShiftCtrlEnum_SRL_1 : _zz_decode_SHIFT_CTRL_string = "SRL_1    ";
      ShiftCtrlEnum_SRA_1 : _zz_decode_SHIFT_CTRL_string = "SRA_1    ";
      default : _zz_decode_SHIFT_CTRL_string = "?????????";
    endcase
  end
  always @(*) begin
    case(_zz_decode_to_execute_SHIFT_CTRL)
      ShiftCtrlEnum_DISABLE_1 : _zz_decode_to_execute_SHIFT_CTRL_string = "DISABLE_1";
      ShiftCtrlEnum_SLL_1 : _zz_decode_to_execute_SHIFT_CTRL_string = "SLL_1    ";
      ShiftCtrlEnum_SRL_1 : _zz_decode_to_execute_SHIFT_CTRL_string = "SRL_1    ";
      ShiftCtrlEnum_SRA_1 : _zz_decode_to_execute_SHIFT_CTRL_string = "SRA_1    ";
      default : _zz_decode_to_execute_SHIFT_CTRL_string = "?????????";
    endcase
  end
  always @(*) begin
    case(_zz_decode_to_execute_SHIFT_CTRL_1)
      ShiftCtrlEnum_DISABLE_1 : _zz_decode_to_execute_SHIFT_CTRL_1_string = "DISABLE_1";
      ShiftCtrlEnum_SLL_1 : _zz_decode_to_execute_SHIFT_CTRL_1_string = "SLL_1    ";
      ShiftCtrlEnum_SRL_1 : _zz_decode_to_execute_SHIFT_CTRL_1_string = "SRL_1    ";
      ShiftCtrlEnum_SRA_1 : _zz_decode_to_execute_SHIFT_CTRL_1_string = "SRA_1    ";
      default : _zz_decode_to_execute_SHIFT_CTRL_1_string = "?????????";
    endcase
  end
  always @(*) begin
    case(decode_ALU_BITWISE_CTRL)
      AluBitwiseCtrlEnum_XOR_1 : decode_ALU_BITWISE_CTRL_string = "XOR_1";
      AluBitwiseCtrlEnum_OR_1 : decode_ALU_BITWISE_CTRL_string = "OR_1 ";
      AluBitwiseCtrlEnum_AND_1 : decode_ALU_BITWISE_CTRL_string = "AND_1";
      default : decode_ALU_BITWISE_CTRL_string = "?????";
    endcase
  end
  always @(*) begin
    case(_zz_decode_ALU_BITWISE_CTRL)
      AluBitwiseCtrlEnum_XOR_1 : _zz_decode_ALU_BITWISE_CTRL_string = "XOR_1";
      AluBitwiseCtrlEnum_OR_1 : _zz_decode_ALU_BITWISE_CTRL_string = "OR_1 ";
      AluBitwiseCtrlEnum_AND_1 : _zz_decode_ALU_BITWISE_CTRL_string = "AND_1";
      default : _zz_decode_ALU_BITWISE_CTRL_string = "?????";
    endcase
  end
  always @(*) begin
    case(_zz_decode_to_execute_ALU_BITWISE_CTRL)
      AluBitwiseCtrlEnum_XOR_1 : _zz_decode_to_execute_ALU_BITWISE_CTRL_string = "XOR_1";
      AluBitwiseCtrlEnum_OR_1 : _zz_decode_to_execute_ALU_BITWISE_CTRL_string = "OR_1 ";
      AluBitwiseCtrlEnum_AND_1 : _zz_decode_to_execute_ALU_BITWISE_CTRL_string = "AND_1";
      default : _zz_decode_to_execute_ALU_BITWISE_CTRL_string = "?????";
    endcase
  end
  always @(*) begin
    case(_zz_decode_to_execute_ALU_BITWISE_CTRL_1)
      AluBitwiseCtrlEnum_XOR_1 : _zz_decode_to_execute_ALU_BITWISE_CTRL_1_string = "XOR_1";
      AluBitwiseCtrlEnum_OR_1 : _zz_decode_to_execute_ALU_BITWISE_CTRL_1_string = "OR_1 ";
      AluBitwiseCtrlEnum_AND_1 : _zz_decode_to_execute_ALU_BITWISE_CTRL_1_string = "AND_1";
      default : _zz_decode_to_execute_ALU_BITWISE_CTRL_1_string = "?????";
    endcase
  end
  always @(*) begin
    case(decode_ALU_CTRL)
      AluCtrlEnum_ADD_SUB : decode_ALU_CTRL_string = "ADD_SUB ";
      AluCtrlEnum_SLT_SLTU : decode_ALU_CTRL_string = "SLT_SLTU";
      AluCtrlEnum_BITWISE : decode_ALU_CTRL_string = "BITWISE ";
      default : decode_ALU_CTRL_string = "????????";
    endcase
  end
  always @(*) begin
    case(_zz_decode_ALU_CTRL)
      AluCtrlEnum_ADD_SUB : _zz_decode_ALU_CTRL_string = "ADD_SUB ";
      AluCtrlEnum_SLT_SLTU : _zz_decode_ALU_CTRL_string = "SLT_SLTU";
      AluCtrlEnum_BITWISE : _zz_decode_ALU_CTRL_string = "BITWISE ";
      default : _zz_decode_ALU_CTRL_string = "????????";
    endcase
  end
  always @(*) begin
    case(_zz_decode_to_execute_ALU_CTRL)
      AluCtrlEnum_ADD_SUB : _zz_decode_to_execute_ALU_CTRL_string = "ADD_SUB ";
      AluCtrlEnum_SLT_SLTU : _zz_decode_to_execute_ALU_CTRL_string = "SLT_SLTU";
      AluCtrlEnum_BITWISE : _zz_decode_to_execute_ALU_CTRL_string = "BITWISE ";
      default : _zz_decode_to_execute_ALU_CTRL_string = "????????";
    endcase
  end
  always @(*) begin
    case(_zz_decode_to_execute_ALU_CTRL_1)
      AluCtrlEnum_ADD_SUB : _zz_decode_to_execute_ALU_CTRL_1_string = "ADD_SUB ";
      AluCtrlEnum_SLT_SLTU : _zz_decode_to_execute_ALU_CTRL_1_string = "SLT_SLTU";
      AluCtrlEnum_BITWISE : _zz_decode_to_execute_ALU_CTRL_1_string = "BITWISE ";
      default : _zz_decode_to_execute_ALU_CTRL_1_string = "????????";
    endcase
  end
  always @(*) begin
    case(decode_SRC2_CTRL)
      Src2CtrlEnum_RS : decode_SRC2_CTRL_string = "RS ";
      Src2CtrlEnum_IMI : decode_SRC2_CTRL_string = "IMI";
      Src2CtrlEnum_IMS : decode_SRC2_CTRL_string = "IMS";
      Src2CtrlEnum_PC : decode_SRC2_CTRL_string = "PC ";
      default : decode_SRC2_CTRL_string = "???";
    endcase
  end
  always @(*) begin
    case(_zz_decode_SRC2_CTRL)
      Src2CtrlEnum_RS : _zz_decode_SRC2_CTRL_string = "RS ";
      Src2CtrlEnum_IMI : _zz_decode_SRC2_CTRL_string = "IMI";
      Src2CtrlEnum_IMS : _zz_decode_SRC2_CTRL_string = "IMS";
      Src2CtrlEnum_PC : _zz_decode_SRC2_CTRL_string = "PC ";
      default : _zz_decode_SRC2_CTRL_string = "???";
    endcase
  end
  always @(*) begin
    case(_zz_decode_to_execute_SRC2_CTRL)
      Src2CtrlEnum_RS : _zz_decode_to_execute_SRC2_CTRL_string = "RS ";
      Src2CtrlEnum_IMI : _zz_decode_to_execute_SRC2_CTRL_string = "IMI";
      Src2CtrlEnum_IMS : _zz_decode_to_execute_SRC2_CTRL_string = "IMS";
      Src2CtrlEnum_PC : _zz_decode_to_execute_SRC2_CTRL_string = "PC ";
      default : _zz_decode_to_execute_SRC2_CTRL_string = "???";
    endcase
  end
  always @(*) begin
    case(_zz_decode_to_execute_SRC2_CTRL_1)
      Src2CtrlEnum_RS : _zz_decode_to_execute_SRC2_CTRL_1_string = "RS ";
      Src2CtrlEnum_IMI : _zz_decode_to_execute_SRC2_CTRL_1_string = "IMI";
      Src2CtrlEnum_IMS : _zz_decode_to_execute_SRC2_CTRL_1_string = "IMS";
      Src2CtrlEnum_PC : _zz_decode_to_execute_SRC2_CTRL_1_string = "PC ";
      default : _zz_decode_to_execute_SRC2_CTRL_1_string = "???";
    endcase
  end
  always @(*) begin
    case(decode_SRC1_CTRL)
      Src1CtrlEnum_RS : decode_SRC1_CTRL_string = "RS          ";
      Src1CtrlEnum_IMU : decode_SRC1_CTRL_string = "IMU         ";
      Src1CtrlEnum_PC_INCREMENT : decode_SRC1_CTRL_string = "PC_INCREMENT";
      Src1CtrlEnum_URS1 : decode_SRC1_CTRL_string = "URS1        ";
      default : decode_SRC1_CTRL_string = "????????????";
    endcase
  end
  always @(*) begin
    case(_zz_decode_SRC1_CTRL)
      Src1CtrlEnum_RS : _zz_decode_SRC1_CTRL_string = "RS          ";
      Src1CtrlEnum_IMU : _zz_decode_SRC1_CTRL_string = "IMU         ";
      Src1CtrlEnum_PC_INCREMENT : _zz_decode_SRC1_CTRL_string = "PC_INCREMENT";
      Src1CtrlEnum_URS1 : _zz_decode_SRC1_CTRL_string = "URS1        ";
      default : _zz_decode_SRC1_CTRL_string = "????????????";
    endcase
  end
  always @(*) begin
    case(_zz_decode_to_execute_SRC1_CTRL)
      Src1CtrlEnum_RS : _zz_decode_to_execute_SRC1_CTRL_string = "RS          ";
      Src1CtrlEnum_IMU : _zz_decode_to_execute_SRC1_CTRL_string = "IMU         ";
      Src1CtrlEnum_PC_INCREMENT : _zz_decode_to_execute_SRC1_CTRL_string = "PC_INCREMENT";
      Src1CtrlEnum_URS1 : _zz_decode_to_execute_SRC1_CTRL_string = "URS1        ";
      default : _zz_decode_to_execute_SRC1_CTRL_string = "????????????";
    endcase
  end
  always @(*) begin
    case(_zz_decode_to_execute_SRC1_CTRL_1)
      Src1CtrlEnum_RS : _zz_decode_to_execute_SRC1_CTRL_1_string = "RS          ";
      Src1CtrlEnum_IMU : _zz_decode_to_execute_SRC1_CTRL_1_string = "IMU         ";
      Src1CtrlEnum_PC_INCREMENT : _zz_decode_to_execute_SRC1_CTRL_1_string = "PC_INCREMENT";
      Src1CtrlEnum_URS1 : _zz_decode_to_execute_SRC1_CTRL_1_string = "URS1        ";
      default : _zz_decode_to_execute_SRC1_CTRL_1_string = "????????????";
    endcase
  end
  always @(*) begin
    case(execute_BRANCH_CTRL)
      BranchCtrlEnum_INC : execute_BRANCH_CTRL_string = "INC ";
      BranchCtrlEnum_B : execute_BRANCH_CTRL_string = "B   ";
      BranchCtrlEnum_JAL : execute_BRANCH_CTRL_string = "JAL ";
      BranchCtrlEnum_JALR : execute_BRANCH_CTRL_string = "JALR";
      default : execute_BRANCH_CTRL_string = "????";
    endcase
  end
  always @(*) begin
    case(_zz_execute_BRANCH_CTRL)
      BranchCtrlEnum_INC : _zz_execute_BRANCH_CTRL_string = "INC ";
      BranchCtrlEnum_B : _zz_execute_BRANCH_CTRL_string = "B   ";
      BranchCtrlEnum_JAL : _zz_execute_BRANCH_CTRL_string = "JAL ";
      BranchCtrlEnum_JALR : _zz_execute_BRANCH_CTRL_string = "JALR";
      default : _zz_execute_BRANCH_CTRL_string = "????";
    endcase
  end
  always @(*) begin
    case(execute_SHIFT_CTRL)
      ShiftCtrlEnum_DISABLE_1 : execute_SHIFT_CTRL_string = "DISABLE_1";
      ShiftCtrlEnum_SLL_1 : execute_SHIFT_CTRL_string = "SLL_1    ";
      ShiftCtrlEnum_SRL_1 : execute_SHIFT_CTRL_string = "SRL_1    ";
      ShiftCtrlEnum_SRA_1 : execute_SHIFT_CTRL_string = "SRA_1    ";
      default : execute_SHIFT_CTRL_string = "?????????";
    endcase
  end
  always @(*) begin
    case(_zz_execute_SHIFT_CTRL)
      ShiftCtrlEnum_DISABLE_1 : _zz_execute_SHIFT_CTRL_string = "DISABLE_1";
      ShiftCtrlEnum_SLL_1 : _zz_execute_SHIFT_CTRL_string = "SLL_1    ";
      ShiftCtrlEnum_SRL_1 : _zz_execute_SHIFT_CTRL_string = "SRL_1    ";
      ShiftCtrlEnum_SRA_1 : _zz_execute_SHIFT_CTRL_string = "SRA_1    ";
      default : _zz_execute_SHIFT_CTRL_string = "?????????";
    endcase
  end
  always @(*) begin
    case(execute_SRC2_CTRL)
      Src2CtrlEnum_RS : execute_SRC2_CTRL_string = "RS ";
      Src2CtrlEnum_IMI : execute_SRC2_CTRL_string = "IMI";
      Src2CtrlEnum_IMS : execute_SRC2_CTRL_string = "IMS";
      Src2CtrlEnum_PC : execute_SRC2_CTRL_string = "PC ";
      default : execute_SRC2_CTRL_string = "???";
    endcase
  end
  always @(*) begin
    case(_zz_execute_SRC2_CTRL)
      Src2CtrlEnum_RS : _zz_execute_SRC2_CTRL_string = "RS ";
      Src2CtrlEnum_IMI : _zz_execute_SRC2_CTRL_string = "IMI";
      Src2CtrlEnum_IMS : _zz_execute_SRC2_CTRL_string = "IMS";
      Src2CtrlEnum_PC : _zz_execute_SRC2_CTRL_string = "PC ";
      default : _zz_execute_SRC2_CTRL_string = "???";
    endcase
  end
  always @(*) begin
    case(execute_SRC1_CTRL)
      Src1CtrlEnum_RS : execute_SRC1_CTRL_string = "RS          ";
      Src1CtrlEnum_IMU : execute_SRC1_CTRL_string = "IMU         ";
      Src1CtrlEnum_PC_INCREMENT : execute_SRC1_CTRL_string = "PC_INCREMENT";
      Src1CtrlEnum_URS1 : execute_SRC1_CTRL_string = "URS1        ";
      default : execute_SRC1_CTRL_string = "????????????";
    endcase
  end
  always @(*) begin
    case(_zz_execute_SRC1_CTRL)
      Src1CtrlEnum_RS : _zz_execute_SRC1_CTRL_string = "RS          ";
      Src1CtrlEnum_IMU : _zz_execute_SRC1_CTRL_string = "IMU         ";
      Src1CtrlEnum_PC_INCREMENT : _zz_execute_SRC1_CTRL_string = "PC_INCREMENT";
      Src1CtrlEnum_URS1 : _zz_execute_SRC1_CTRL_string = "URS1        ";
      default : _zz_execute_SRC1_CTRL_string = "????????????";
    endcase
  end
  always @(*) begin
    case(execute_ALU_CTRL)
      AluCtrlEnum_ADD_SUB : execute_ALU_CTRL_string = "ADD_SUB ";
      AluCtrlEnum_SLT_SLTU : execute_ALU_CTRL_string = "SLT_SLTU";
      AluCtrlEnum_BITWISE : execute_ALU_CTRL_string = "BITWISE ";
      default : execute_ALU_CTRL_string = "????????";
    endcase
  end
  always @(*) begin
    case(_zz_execute_ALU_CTRL)
      AluCtrlEnum_ADD_SUB : _zz_execute_ALU_CTRL_string = "ADD_SUB ";
      AluCtrlEnum_SLT_SLTU : _zz_execute_ALU_CTRL_string = "SLT_SLTU";
      AluCtrlEnum_BITWISE : _zz_execute_ALU_CTRL_string = "BITWISE ";
      default : _zz_execute_ALU_CTRL_string = "????????";
    endcase
  end
  always @(*) begin
    case(execute_ALU_BITWISE_CTRL)
      AluBitwiseCtrlEnum_XOR_1 : execute_ALU_BITWISE_CTRL_string = "XOR_1";
      AluBitwiseCtrlEnum_OR_1 : execute_ALU_BITWISE_CTRL_string = "OR_1 ";
      AluBitwiseCtrlEnum_AND_1 : execute_ALU_BITWISE_CTRL_string = "AND_1";
      default : execute_ALU_BITWISE_CTRL_string = "?????";
    endcase
  end
  always @(*) begin
    case(_zz_execute_ALU_BITWISE_CTRL)
      AluBitwiseCtrlEnum_XOR_1 : _zz_execute_ALU_BITWISE_CTRL_string = "XOR_1";
      AluBitwiseCtrlEnum_OR_1 : _zz_execute_ALU_BITWISE_CTRL_string = "OR_1 ";
      AluBitwiseCtrlEnum_AND_1 : _zz_execute_ALU_BITWISE_CTRL_string = "AND_1";
      default : _zz_execute_ALU_BITWISE_CTRL_string = "?????";
    endcase
  end
  always @(*) begin
    case(_zz_decode_BRANCH_CTRL_1)
      BranchCtrlEnum_INC : _zz_decode_BRANCH_CTRL_1_string = "INC ";
      BranchCtrlEnum_B : _zz_decode_BRANCH_CTRL_1_string = "B   ";
      BranchCtrlEnum_JAL : _zz_decode_BRANCH_CTRL_1_string = "JAL ";
      BranchCtrlEnum_JALR : _zz_decode_BRANCH_CTRL_1_string = "JALR";
      default : _zz_decode_BRANCH_CTRL_1_string = "????";
    endcase
  end
  always @(*) begin
    case(_zz_decode_SHIFT_CTRL_1)
      ShiftCtrlEnum_DISABLE_1 : _zz_decode_SHIFT_CTRL_1_string = "DISABLE_1";
      ShiftCtrlEnum_SLL_1 : _zz_decode_SHIFT_CTRL_1_string = "SLL_1    ";
      ShiftCtrlEnum_SRL_1 : _zz_decode_SHIFT_CTRL_1_string = "SRL_1    ";
      ShiftCtrlEnum_SRA_1 : _zz_decode_SHIFT_CTRL_1_string = "SRA_1    ";
      default : _zz_decode_SHIFT_CTRL_1_string = "?????????";
    endcase
  end
  always @(*) begin
    case(_zz_decode_ALU_BITWISE_CTRL_1)
      AluBitwiseCtrlEnum_XOR_1 : _zz_decode_ALU_BITWISE_CTRL_1_string = "XOR_1";
      AluBitwiseCtrlEnum_OR_1 : _zz_decode_ALU_BITWISE_CTRL_1_string = "OR_1 ";
      AluBitwiseCtrlEnum_AND_1 : _zz_decode_ALU_BITWISE_CTRL_1_string = "AND_1";
      default : _zz_decode_ALU_BITWISE_CTRL_1_string = "?????";
    endcase
  end
  always @(*) begin
    case(_zz_decode_ALU_CTRL_1)
      AluCtrlEnum_ADD_SUB : _zz_decode_ALU_CTRL_1_string = "ADD_SUB ";
      AluCtrlEnum_SLT_SLTU : _zz_decode_ALU_CTRL_1_string = "SLT_SLTU";
      AluCtrlEnum_BITWISE : _zz_decode_ALU_CTRL_1_string = "BITWISE ";
      default : _zz_decode_ALU_CTRL_1_string = "????????";
    endcase
  end
  always @(*) begin
    case(_zz_decode_SRC2_CTRL_1)
      Src2CtrlEnum_RS : _zz_decode_SRC2_CTRL_1_string = "RS ";
      Src2CtrlEnum_IMI : _zz_decode_SRC2_CTRL_1_string = "IMI";
      Src2CtrlEnum_IMS : _zz_decode_SRC2_CTRL_1_string = "IMS";
      Src2CtrlEnum_PC : _zz_decode_SRC2_CTRL_1_string = "PC ";
      default : _zz_decode_SRC2_CTRL_1_string = "???";
    endcase
  end
  always @(*) begin
    case(_zz_decode_SRC1_CTRL_1)
      Src1CtrlEnum_RS : _zz_decode_SRC1_CTRL_1_string = "RS          ";
      Src1CtrlEnum_IMU : _zz_decode_SRC1_CTRL_1_string = "IMU         ";
      Src1CtrlEnum_PC_INCREMENT : _zz_decode_SRC1_CTRL_1_string = "PC_INCREMENT";
      Src1CtrlEnum_URS1 : _zz_decode_SRC1_CTRL_1_string = "URS1        ";
      default : _zz_decode_SRC1_CTRL_1_string = "????????????";
    endcase
  end
  always @(*) begin
    case(_zz_decode_SRC1_CTRL_2)
      Src1CtrlEnum_RS : _zz_decode_SRC1_CTRL_2_string = "RS          ";
      Src1CtrlEnum_IMU : _zz_decode_SRC1_CTRL_2_string = "IMU         ";
      Src1CtrlEnum_PC_INCREMENT : _zz_decode_SRC1_CTRL_2_string = "PC_INCREMENT";
      Src1CtrlEnum_URS1 : _zz_decode_SRC1_CTRL_2_string = "URS1        ";
      default : _zz_decode_SRC1_CTRL_2_string = "????????????";
    endcase
  end
  always @(*) begin
    case(_zz_decode_SRC2_CTRL_2)
      Src2CtrlEnum_RS : _zz_decode_SRC2_CTRL_2_string = "RS ";
      Src2CtrlEnum_IMI : _zz_decode_SRC2_CTRL_2_string = "IMI";
      Src2CtrlEnum_IMS : _zz_decode_SRC2_CTRL_2_string = "IMS";
      Src2CtrlEnum_PC : _zz_decode_SRC2_CTRL_2_string = "PC ";
      default : _zz_decode_SRC2_CTRL_2_string = "???";
    endcase
  end
  always @(*) begin
    case(_zz_decode_ALU_CTRL_2)
      AluCtrlEnum_ADD_SUB : _zz_decode_ALU_CTRL_2_string = "ADD_SUB ";
      AluCtrlEnum_SLT_SLTU : _zz_decode_ALU_CTRL_2_string = "SLT_SLTU";
      AluCtrlEnum_BITWISE : _zz_decode_ALU_CTRL_2_string = "BITWISE ";
      default : _zz_decode_ALU_CTRL_2_string = "????????";
    endcase
  end
  always @(*) begin
    case(_zz_decode_ALU_BITWISE_CTRL_2)
      AluBitwiseCtrlEnum_XOR_1 : _zz_decode_ALU_BITWISE_CTRL_2_string = "XOR_1";
      AluBitwiseCtrlEnum_OR_1 : _zz_decode_ALU_BITWISE_CTRL_2_string = "OR_1 ";
      AluBitwiseCtrlEnum_AND_1 : _zz_decode_ALU_BITWISE_CTRL_2_string = "AND_1";
      default : _zz_decode_ALU_BITWISE_CTRL_2_string = "?????";
    endcase
  end
  always @(*) begin
    case(_zz_decode_SHIFT_CTRL_2)
      ShiftCtrlEnum_DISABLE_1 : _zz_decode_SHIFT_CTRL_2_string = "DISABLE_1";
      ShiftCtrlEnum_SLL_1 : _zz_decode_SHIFT_CTRL_2_string = "SLL_1    ";
      ShiftCtrlEnum_SRL_1 : _zz_decode_SHIFT_CTRL_2_string = "SRL_1    ";
      ShiftCtrlEnum_SRA_1 : _zz_decode_SHIFT_CTRL_2_string = "SRA_1    ";
      default : _zz_decode_SHIFT_CTRL_2_string = "?????????";
    endcase
  end
  always @(*) begin
    case(_zz_decode_BRANCH_CTRL_7)
      BranchCtrlEnum_INC : _zz_decode_BRANCH_CTRL_7_string = "INC ";
      BranchCtrlEnum_B : _zz_decode_BRANCH_CTRL_7_string = "B   ";
      BranchCtrlEnum_JAL : _zz_decode_BRANCH_CTRL_7_string = "JAL ";
      BranchCtrlEnum_JALR : _zz_decode_BRANCH_CTRL_7_string = "JALR";
      default : _zz_decode_BRANCH_CTRL_7_string = "????";
    endcase
  end
  always @(*) begin
    case(decode_to_execute_SRC1_CTRL)
      Src1CtrlEnum_RS : decode_to_execute_SRC1_CTRL_string = "RS          ";
      Src1CtrlEnum_IMU : decode_to_execute_SRC1_CTRL_string = "IMU         ";
      Src1CtrlEnum_PC_INCREMENT : decode_to_execute_SRC1_CTRL_string = "PC_INCREMENT";
      Src1CtrlEnum_URS1 : decode_to_execute_SRC1_CTRL_string = "URS1        ";
      default : decode_to_execute_SRC1_CTRL_string = "????????????";
    endcase
  end
  always @(*) begin
    case(decode_to_execute_SRC2_CTRL)
      Src2CtrlEnum_RS : decode_to_execute_SRC2_CTRL_string = "RS ";
      Src2CtrlEnum_IMI : decode_to_execute_SRC2_CTRL_string = "IMI";
      Src2CtrlEnum_IMS : decode_to_execute_SRC2_CTRL_string = "IMS";
      Src2CtrlEnum_PC : decode_to_execute_SRC2_CTRL_string = "PC ";
      default : decode_to_execute_SRC2_CTRL_string = "???";
    endcase
  end
  always @(*) begin
    case(decode_to_execute_ALU_CTRL)
      AluCtrlEnum_ADD_SUB : decode_to_execute_ALU_CTRL_string = "ADD_SUB ";
      AluCtrlEnum_SLT_SLTU : decode_to_execute_ALU_CTRL_string = "SLT_SLTU";
      AluCtrlEnum_BITWISE : decode_to_execute_ALU_CTRL_string = "BITWISE ";
      default : decode_to_execute_ALU_CTRL_string = "????????";
    endcase
  end
  always @(*) begin
    case(decode_to_execute_ALU_BITWISE_CTRL)
      AluBitwiseCtrlEnum_XOR_1 : decode_to_execute_ALU_BITWISE_CTRL_string = "XOR_1";
      AluBitwiseCtrlEnum_OR_1 : decode_to_execute_ALU_BITWISE_CTRL_string = "OR_1 ";
      AluBitwiseCtrlEnum_AND_1 : decode_to_execute_ALU_BITWISE_CTRL_string = "AND_1";
      default : decode_to_execute_ALU_BITWISE_CTRL_string = "?????";
    endcase
  end
  always @(*) begin
    case(decode_to_execute_SHIFT_CTRL)
      ShiftCtrlEnum_DISABLE_1 : decode_to_execute_SHIFT_CTRL_string = "DISABLE_1";
      ShiftCtrlEnum_SLL_1 : decode_to_execute_SHIFT_CTRL_string = "SLL_1    ";
      ShiftCtrlEnum_SRL_1 : decode_to_execute_SHIFT_CTRL_string = "SRL_1    ";
      ShiftCtrlEnum_SRA_1 : decode_to_execute_SHIFT_CTRL_string = "SRA_1    ";
      default : decode_to_execute_SHIFT_CTRL_string = "?????????";
    endcase
  end
  always @(*) begin
    case(decode_to_execute_BRANCH_CTRL)
      BranchCtrlEnum_INC : decode_to_execute_BRANCH_CTRL_string = "INC ";
      BranchCtrlEnum_B : decode_to_execute_BRANCH_CTRL_string = "B   ";
      BranchCtrlEnum_JAL : decode_to_execute_BRANCH_CTRL_string = "JAL ";
      BranchCtrlEnum_JALR : decode_to_execute_BRANCH_CTRL_string = "JALR";
      default : decode_to_execute_BRANCH_CTRL_string = "????";
    endcase
  end
  `endif

  assign execute_REGFILE_WRITE_DATA = _zz_execute_REGFILE_WRITE_DATA;
  assign decode_DO_EBREAK = (((! DebugPlugin_haltIt) && (decode_IS_EBREAK || 1'b0)) && DebugPlugin_allowEBreak);
  assign decode_SRC2_FORCE_ZERO = (decode_SRC_ADD_ZERO && (! decode_SRC_USE_SUB_LESS));
  assign decode_BRANCH_CTRL = _zz_decode_BRANCH_CTRL;
  assign _zz_decode_to_execute_BRANCH_CTRL = _zz_decode_to_execute_BRANCH_CTRL_1;
  assign decode_SHIFT_CTRL = _zz_decode_SHIFT_CTRL;
  assign _zz_decode_to_execute_SHIFT_CTRL = _zz_decode_to_execute_SHIFT_CTRL_1;
  assign decode_ALU_BITWISE_CTRL = _zz_decode_ALU_BITWISE_CTRL;
  assign _zz_decode_to_execute_ALU_BITWISE_CTRL = _zz_decode_to_execute_ALU_BITWISE_CTRL_1;
  assign decode_SRC_LESS_UNSIGNED = _zz_decode_BRANCH_CTRL_2[15];
  assign decode_ALU_CTRL = _zz_decode_ALU_CTRL;
  assign _zz_decode_to_execute_ALU_CTRL = _zz_decode_to_execute_ALU_CTRL_1;
  assign decode_RS2_USE = _zz_decode_BRANCH_CTRL_2[12];
  assign decode_MEMORY_STORE = _zz_decode_BRANCH_CTRL_2[10];
  assign execute_REGFILE_WRITE_VALID = decode_to_execute_REGFILE_WRITE_VALID;
  assign decode_SRC2_CTRL = _zz_decode_SRC2_CTRL;
  assign _zz_decode_to_execute_SRC2_CTRL = _zz_decode_to_execute_SRC2_CTRL_1;
  assign decode_RS1_USE = _zz_decode_BRANCH_CTRL_2[4];
  assign decode_MEMORY_ENABLE = _zz_decode_BRANCH_CTRL_2[3];
  assign decode_SRC1_CTRL = _zz_decode_SRC1_CTRL;
  assign _zz_decode_to_execute_SRC1_CTRL = _zz_decode_to_execute_SRC1_CTRL_1;
  assign execute_FORMAL_PC_NEXT = decode_to_execute_FORMAL_PC_NEXT;
  assign decode_FORMAL_PC_NEXT = (decode_PC + 32'h00000004);
  assign execute_DO_EBREAK = decode_to_execute_DO_EBREAK;
  assign decode_IS_EBREAK = _zz_decode_BRANCH_CTRL_2[24];
  assign execute_RS2_USE = decode_to_execute_RS2_USE;
  assign execute_RS1_USE = decode_to_execute_RS1_USE;
  assign execute_IS_FENCEI = decode_to_execute_IS_FENCEI;
  always @(*) begin
    _zz_decode_to_execute_INSTRUCTION = decode_INSTRUCTION;
    if(decode_IS_FENCEI) begin
      _zz_decode_to_execute_INSTRUCTION[12] = 1'b0;
      _zz_decode_to_execute_INSTRUCTION[22] = 1'b1;
    end
  end

  assign decode_IS_FENCEI = _zz_decode_BRANCH_CTRL_2[23];
  assign execute_BRANCH_CALC = {execute_BranchPlugin_branchAdder[31 : 1],1'b0};
  assign execute_BRANCH_DO = _zz_execute_BRANCH_DO_1;
  assign execute_RS1 = execute_RegFilePlugin_rs1Data;
  assign execute_BRANCH_CTRL = _zz_execute_BRANCH_CTRL;
  assign execute_SHIFT_CTRL = _zz_execute_SHIFT_CTRL;
  assign execute_SRC_LESS_UNSIGNED = decode_to_execute_SRC_LESS_UNSIGNED;
  assign execute_SRC2_FORCE_ZERO = decode_to_execute_SRC2_FORCE_ZERO;
  assign execute_SRC_USE_SUB_LESS = decode_to_execute_SRC_USE_SUB_LESS;
  assign execute_SRC2_CTRL = _zz_execute_SRC2_CTRL;
  assign execute_SRC1_CTRL = _zz_execute_SRC1_CTRL;
  assign decode_SRC_USE_SUB_LESS = _zz_decode_BRANCH_CTRL_2[2];
  assign decode_SRC_ADD_ZERO = _zz_decode_BRANCH_CTRL_2[18];
  assign execute_SRC_ADD_SUB = execute_SrcPlugin_addSub;
  assign execute_SRC_LESS = execute_SrcPlugin_less;
  assign execute_ALU_CTRL = _zz_execute_ALU_CTRL;
  assign execute_SRC2 = _zz_execute_SRC2_4;
  assign execute_SRC1 = _zz_execute_SRC1;
  assign execute_ALU_BITWISE_CTRL = _zz_execute_ALU_BITWISE_CTRL;
  assign _zz_lastStageRegFileWrite_payload_address = execute_INSTRUCTION;
  assign _zz_lastStageRegFileWrite_valid = execute_REGFILE_WRITE_VALID;
  always @(*) begin
    _zz_1 = 1'b0;
    if(lastStageRegFileWrite_valid) begin
      _zz_1 = 1'b1;
    end
  end

  always @(*) begin
    decode_REGFILE_WRITE_VALID = _zz_decode_BRANCH_CTRL_2[7];
    if(when_RegFilePlugin_l63) begin
      decode_REGFILE_WRITE_VALID = 1'b0;
    end
  end

  always @(*) begin
    _zz_lastStageRegFileWrite_payload_data = execute_REGFILE_WRITE_DATA;
    if(when_DBusSimplePlugin_l558) begin
      _zz_lastStageRegFileWrite_payload_data = execute_DBusSimplePlugin_rspFormated;
    end
    if(when_ShiftPlugins_l169) begin
      _zz_lastStageRegFileWrite_payload_data = _zz_lastStageRegFileWrite_payload_data_1;
    end
  end

  assign execute_MEMORY_ADDRESS_LOW = dBus_cmd_payload_address[1 : 0];
  assign execute_MEMORY_READ_DATA = dBus_rsp_data;
  assign execute_SRC_ADD = execute_SrcPlugin_addSub;
  assign execute_RS2 = execute_RegFilePlugin_rs2Data;
  assign execute_MEMORY_STORE = decode_to_execute_MEMORY_STORE;
  assign execute_MEMORY_ENABLE = decode_to_execute_MEMORY_ENABLE;
  assign execute_ALIGNEMENT_FAULT = 1'b0;
  assign decode_PC = IBusSimplePlugin_injector_decodeInput_payload_pc;
  assign decode_INSTRUCTION = IBusSimplePlugin_injector_decodeInput_payload_rsp_inst;
  assign execute_PC = decode_to_execute_PC;
  assign execute_INSTRUCTION = decode_to_execute_INSTRUCTION;
  always @(*) begin
    decode_arbitration_haltItself = 1'b0;
    case(switch_Fetcher_l365)
      3'b010 : begin
        decode_arbitration_haltItself = 1'b1;
      end
      default : begin
      end
    endcase
  end

  assign decode_arbitration_haltByOther = 1'b0;
  always @(*) begin
    decode_arbitration_removeIt = 1'b0;
    if(decode_arbitration_isFlushed) begin
      decode_arbitration_removeIt = 1'b1;
    end
  end

  assign decode_arbitration_flushIt = 1'b0;
  assign decode_arbitration_flushNext = 1'b0;
  always @(*) begin
    execute_arbitration_haltItself = 1'b0;
    if(when_DBusSimplePlugin_l428) begin
      execute_arbitration_haltItself = 1'b1;
    end
    if(when_DBusSimplePlugin_l482) begin
      execute_arbitration_haltItself = 1'b1;
    end
    if(when_ShiftPlugins_l169) begin
      if(when_ShiftPlugins_l184) begin
        execute_arbitration_haltItself = 1'b1;
      end
    end
  end

  always @(*) begin
    execute_arbitration_haltByOther = 1'b0;
    if(when_BranchPlugin_l171) begin
      execute_arbitration_haltByOther = 1'b1;
    end
    if(when_HazardSimplePlugin_l113) begin
      execute_arbitration_haltByOther = 1'b1;
    end
    if(when_DebugPlugin_l295) begin
      execute_arbitration_haltByOther = 1'b1;
    end
  end

  always @(*) begin
    execute_arbitration_removeIt = 1'b0;
    if(execute_arbitration_isFlushed) begin
      execute_arbitration_removeIt = 1'b1;
    end
  end

  always @(*) begin
    execute_arbitration_flushIt = 1'b0;
    if(when_DebugPlugin_l295) begin
      if(when_DebugPlugin_l298) begin
        execute_arbitration_flushIt = 1'b1;
      end
    end
  end

  always @(*) begin
    execute_arbitration_flushNext = 1'b0;
    if(BranchPlugin_jumpInterface_valid) begin
      execute_arbitration_flushNext = 1'b1;
    end
    if(when_DebugPlugin_l295) begin
      if(when_DebugPlugin_l298) begin
        execute_arbitration_flushNext = 1'b1;
      end
    end
  end

  assign lastStageInstruction = execute_INSTRUCTION;
  assign lastStagePc = execute_PC;
  assign lastStageIsValid = execute_arbitration_isValid;
  assign lastStageIsFiring = execute_arbitration_isFiring;
  always @(*) begin
    IBusSimplePlugin_fetcherHalt = 1'b0;
    if(when_DebugPlugin_l295) begin
      if(when_DebugPlugin_l298) begin
        IBusSimplePlugin_fetcherHalt = 1'b1;
      end
    end
    if(DebugPlugin_haltIt) begin
      IBusSimplePlugin_fetcherHalt = 1'b1;
    end
    if(when_DebugPlugin_l311) begin
      IBusSimplePlugin_fetcherHalt = 1'b1;
    end
  end

  assign IBusSimplePlugin_forceNoDecodeCond = 1'b0;
  always @(*) begin
    IBusSimplePlugin_incomingInstruction = 1'b0;
    if(IBusSimplePlugin_iBusRsp_stages_1_input_valid) begin
      IBusSimplePlugin_incomingInstruction = 1'b1;
    end
    if(IBusSimplePlugin_injector_decodeInput_valid) begin
      IBusSimplePlugin_incomingInstruction = 1'b1;
    end
  end

  always @(*) begin
    BranchPlugin_inDebugNoFetchFlag = 1'b0;
    if(DebugPlugin_godmode) begin
      BranchPlugin_inDebugNoFetchFlag = 1'b1;
    end
  end

  assign IBusSimplePlugin_externalFlush = ({execute_arbitration_flushNext,decode_arbitration_flushNext} != 2'b00);
  assign IBusSimplePlugin_jump_pcLoad_valid = (BranchPlugin_jumpInterface_valid != 1'b0);
  assign IBusSimplePlugin_jump_pcLoad_payload = BranchPlugin_jumpInterface_payload;
  always @(*) begin
    IBusSimplePlugin_fetchPc_correction = 1'b0;
    if(IBusSimplePlugin_jump_pcLoad_valid) begin
      IBusSimplePlugin_fetchPc_correction = 1'b1;
    end
  end

  assign IBusSimplePlugin_fetchPc_output_fire = (IBusSimplePlugin_fetchPc_output_valid && IBusSimplePlugin_fetchPc_output_ready);
  assign IBusSimplePlugin_fetchPc_corrected = (IBusSimplePlugin_fetchPc_correction || IBusSimplePlugin_fetchPc_correctionReg);
  always @(*) begin
    IBusSimplePlugin_fetchPc_pcRegPropagate = 1'b0;
    if(IBusSimplePlugin_iBusRsp_stages_1_input_ready) begin
      IBusSimplePlugin_fetchPc_pcRegPropagate = 1'b1;
    end
  end

  assign when_Fetcher_l134 = (IBusSimplePlugin_fetchPc_correction || IBusSimplePlugin_fetchPc_pcRegPropagate);
  assign IBusSimplePlugin_fetchPc_output_fire_1 = (IBusSimplePlugin_fetchPc_output_valid && IBusSimplePlugin_fetchPc_output_ready);
  assign when_Fetcher_l134_1 = ((! IBusSimplePlugin_fetchPc_output_valid) && IBusSimplePlugin_fetchPc_output_ready);
  always @(*) begin
    IBusSimplePlugin_fetchPc_pc = (IBusSimplePlugin_fetchPc_pcReg + _zz_IBusSimplePlugin_fetchPc_pc);
    if(IBusSimplePlugin_jump_pcLoad_valid) begin
      IBusSimplePlugin_fetchPc_pc = IBusSimplePlugin_jump_pcLoad_payload;
    end
    IBusSimplePlugin_fetchPc_pc[0] = 1'b0;
    IBusSimplePlugin_fetchPc_pc[1] = 1'b0;
  end

  always @(*) begin
    IBusSimplePlugin_fetchPc_flushed = 1'b0;
    if(IBusSimplePlugin_jump_pcLoad_valid) begin
      IBusSimplePlugin_fetchPc_flushed = 1'b1;
    end
  end

  assign when_Fetcher_l161 = (IBusSimplePlugin_fetchPc_booted && ((IBusSimplePlugin_fetchPc_output_ready || IBusSimplePlugin_fetchPc_correction) || IBusSimplePlugin_fetchPc_pcRegPropagate));
  assign IBusSimplePlugin_fetchPc_output_valid = ((! IBusSimplePlugin_fetcherHalt) && IBusSimplePlugin_fetchPc_booted);
  assign IBusSimplePlugin_fetchPc_output_payload = IBusSimplePlugin_fetchPc_pc;
  assign IBusSimplePlugin_iBusRsp_redoFetch = 1'b0;
  assign IBusSimplePlugin_iBusRsp_stages_0_input_valid = IBusSimplePlugin_fetchPc_output_valid;
  assign IBusSimplePlugin_fetchPc_output_ready = IBusSimplePlugin_iBusRsp_stages_0_input_ready;
  assign IBusSimplePlugin_iBusRsp_stages_0_input_payload = IBusSimplePlugin_fetchPc_output_payload;
  always @(*) begin
    IBusSimplePlugin_iBusRsp_stages_0_halt = 1'b0;
    if(when_IBusSimplePlugin_l305) begin
      IBusSimplePlugin_iBusRsp_stages_0_halt = 1'b1;
    end
  end

  assign _zz_IBusSimplePlugin_iBusRsp_stages_0_input_ready = (! IBusSimplePlugin_iBusRsp_stages_0_halt);
  assign IBusSimplePlugin_iBusRsp_stages_0_input_ready = (IBusSimplePlugin_iBusRsp_stages_0_output_ready && _zz_IBusSimplePlugin_iBusRsp_stages_0_input_ready);
  assign IBusSimplePlugin_iBusRsp_stages_0_output_valid = (IBusSimplePlugin_iBusRsp_stages_0_input_valid && _zz_IBusSimplePlugin_iBusRsp_stages_0_input_ready);
  assign IBusSimplePlugin_iBusRsp_stages_0_output_payload = IBusSimplePlugin_iBusRsp_stages_0_input_payload;
  assign IBusSimplePlugin_iBusRsp_stages_1_halt = 1'b0;
  assign _zz_IBusSimplePlugin_iBusRsp_stages_1_input_ready = (! IBusSimplePlugin_iBusRsp_stages_1_halt);
  assign IBusSimplePlugin_iBusRsp_stages_1_input_ready = (IBusSimplePlugin_iBusRsp_stages_1_output_ready && _zz_IBusSimplePlugin_iBusRsp_stages_1_input_ready);
  assign IBusSimplePlugin_iBusRsp_stages_1_output_valid = (IBusSimplePlugin_iBusRsp_stages_1_input_valid && _zz_IBusSimplePlugin_iBusRsp_stages_1_input_ready);
  assign IBusSimplePlugin_iBusRsp_stages_1_output_payload = IBusSimplePlugin_iBusRsp_stages_1_input_payload;
  assign IBusSimplePlugin_iBusRsp_flush = (IBusSimplePlugin_externalFlush || IBusSimplePlugin_iBusRsp_redoFetch);
  assign IBusSimplePlugin_iBusRsp_stages_0_output_ready = _zz_IBusSimplePlugin_iBusRsp_stages_0_output_ready;
  assign _zz_IBusSimplePlugin_iBusRsp_stages_0_output_ready = ((1'b0 && (! _zz_IBusSimplePlugin_iBusRsp_stages_0_output_ready_1)) || IBusSimplePlugin_iBusRsp_stages_1_input_ready);
  assign _zz_IBusSimplePlugin_iBusRsp_stages_0_output_ready_1 = _zz_IBusSimplePlugin_iBusRsp_stages_0_output_ready_2;
  assign IBusSimplePlugin_iBusRsp_stages_1_input_valid = _zz_IBusSimplePlugin_iBusRsp_stages_0_output_ready_1;
  assign IBusSimplePlugin_iBusRsp_stages_1_input_payload = IBusSimplePlugin_fetchPc_pcReg;
  always @(*) begin
    IBusSimplePlugin_iBusRsp_readyForError = 1'b1;
    if(IBusSimplePlugin_injector_decodeInput_valid) begin
      IBusSimplePlugin_iBusRsp_readyForError = 1'b0;
    end
    if(when_Fetcher_l323) begin
      IBusSimplePlugin_iBusRsp_readyForError = 1'b0;
    end
  end

  assign IBusSimplePlugin_iBusRsp_output_ready = ((1'b0 && (! IBusSimplePlugin_injector_decodeInput_valid)) || IBusSimplePlugin_injector_decodeInput_ready);
  assign IBusSimplePlugin_injector_decodeInput_valid = _zz_IBusSimplePlugin_injector_decodeInput_valid;
  assign IBusSimplePlugin_injector_decodeInput_payload_pc = _zz_IBusSimplePlugin_injector_decodeInput_payload_pc;
  assign IBusSimplePlugin_injector_decodeInput_payload_rsp_error = _zz_IBusSimplePlugin_injector_decodeInput_payload_rsp_error;
  assign IBusSimplePlugin_injector_decodeInput_payload_rsp_inst = _zz_IBusSimplePlugin_injector_decodeInput_payload_rsp_inst;
  assign IBusSimplePlugin_injector_decodeInput_payload_isRvc = _zz_IBusSimplePlugin_injector_decodeInput_payload_isRvc;
  assign when_Fetcher_l323 = (! IBusSimplePlugin_pcValids_0);
  assign when_Fetcher_l332 = (! (! IBusSimplePlugin_iBusRsp_stages_1_input_ready));
  assign when_Fetcher_l332_1 = (! (! IBusSimplePlugin_injector_decodeInput_ready));
  assign when_Fetcher_l332_2 = (! execute_arbitration_isStuck);
  assign IBusSimplePlugin_pcValids_0 = IBusSimplePlugin_injector_nextPcCalc_valids_1;
  assign IBusSimplePlugin_pcValids_1 = IBusSimplePlugin_injector_nextPcCalc_valids_2;
  assign IBusSimplePlugin_injector_decodeInput_ready = (! decode_arbitration_isStuck);
  always @(*) begin
    decode_arbitration_isValid = IBusSimplePlugin_injector_decodeInput_valid;
    case(switch_Fetcher_l365)
      3'b010 : begin
        decode_arbitration_isValid = 1'b1;
      end
      3'b011 : begin
        decode_arbitration_isValid = 1'b1;
      end
      default : begin
      end
    endcase
    if(IBusSimplePlugin_forceNoDecodeCond) begin
      decode_arbitration_isValid = 1'b0;
    end
  end

  assign IBusSimplePlugin_cmd_ready = (! IBusSimplePlugin_cmd_rValid);
  assign IBusSimplePlugin_cmd_s2mPipe_valid = (IBusSimplePlugin_cmd_valid || IBusSimplePlugin_cmd_rValid);
  assign IBusSimplePlugin_cmd_s2mPipe_payload_pc = (IBusSimplePlugin_cmd_rValid ? IBusSimplePlugin_cmd_rData_pc : IBusSimplePlugin_cmd_payload_pc);
  assign iBus_cmd_valid = IBusSimplePlugin_cmd_s2mPipe_valid;
  assign IBusSimplePlugin_cmd_s2mPipe_ready = iBus_cmd_ready;
  assign iBus_cmd_payload_pc = IBusSimplePlugin_cmd_s2mPipe_payload_pc;
  assign IBusSimplePlugin_pending_next = (_zz_IBusSimplePlugin_pending_next - _zz_IBusSimplePlugin_pending_next_3);
  assign IBusSimplePlugin_cmdFork_canEmit = (IBusSimplePlugin_iBusRsp_stages_0_output_ready && (IBusSimplePlugin_pending_value != 3'b111));
  assign when_IBusSimplePlugin_l305 = (IBusSimplePlugin_iBusRsp_stages_0_input_valid && ((! IBusSimplePlugin_cmdFork_canEmit) || (! IBusSimplePlugin_cmd_ready)));
  assign IBusSimplePlugin_cmd_valid = (IBusSimplePlugin_iBusRsp_stages_0_input_valid && IBusSimplePlugin_cmdFork_canEmit);
  assign IBusSimplePlugin_cmd_fire = (IBusSimplePlugin_cmd_valid && IBusSimplePlugin_cmd_ready);
  assign IBusSimplePlugin_pending_inc = IBusSimplePlugin_cmd_fire;
  assign IBusSimplePlugin_cmd_payload_pc = {IBusSimplePlugin_iBusRsp_stages_0_input_payload[31 : 2],2'b00};
  assign iBus_rsp_toStream_valid = iBus_rsp_valid;
  assign iBus_rsp_toStream_payload_error = iBus_rsp_payload_error;
  assign iBus_rsp_toStream_payload_inst = iBus_rsp_payload_inst;
  assign iBus_rsp_toStream_ready = IBusSimplePlugin_rspJoin_rspBuffer_c_io_push_ready;
  assign IBusSimplePlugin_rspJoin_rspBuffer_flush = ((IBusSimplePlugin_rspJoin_rspBuffer_discardCounter != 3'b000) || IBusSimplePlugin_iBusRsp_flush);
  assign IBusSimplePlugin_rspJoin_rspBuffer_output_valid = (IBusSimplePlugin_rspJoin_rspBuffer_c_io_pop_valid && (IBusSimplePlugin_rspJoin_rspBuffer_discardCounter == 3'b000));
  assign IBusSimplePlugin_rspJoin_rspBuffer_output_payload_error = IBusSimplePlugin_rspJoin_rspBuffer_c_io_pop_payload_error;
  assign IBusSimplePlugin_rspJoin_rspBuffer_output_payload_inst = IBusSimplePlugin_rspJoin_rspBuffer_c_io_pop_payload_inst;
  assign IBusSimplePlugin_rspJoin_rspBuffer_c_io_pop_ready = (IBusSimplePlugin_rspJoin_rspBuffer_output_ready || IBusSimplePlugin_rspJoin_rspBuffer_flush);
  assign IBusSimplePlugin_rspJoin_rspBuffer_c_io_pop_fire = (IBusSimplePlugin_rspJoin_rspBuffer_c_io_pop_valid && IBusSimplePlugin_rspJoin_rspBuffer_c_io_pop_ready);
  assign IBusSimplePlugin_pending_dec = IBusSimplePlugin_rspJoin_rspBuffer_c_io_pop_fire;
  assign IBusSimplePlugin_rspJoin_fetchRsp_pc = IBusSimplePlugin_iBusRsp_stages_1_output_payload;
  always @(*) begin
    IBusSimplePlugin_rspJoin_fetchRsp_rsp_error = IBusSimplePlugin_rspJoin_rspBuffer_output_payload_error;
    if(when_IBusSimplePlugin_l376) begin
      IBusSimplePlugin_rspJoin_fetchRsp_rsp_error = 1'b0;
    end
  end

  assign IBusSimplePlugin_rspJoin_fetchRsp_rsp_inst = IBusSimplePlugin_rspJoin_rspBuffer_output_payload_inst;
  assign when_IBusSimplePlugin_l376 = (! IBusSimplePlugin_rspJoin_rspBuffer_output_valid);
  assign IBusSimplePlugin_rspJoin_exceptionDetected = 1'b0;
  assign IBusSimplePlugin_rspJoin_join_valid = (IBusSimplePlugin_iBusRsp_stages_1_output_valid && IBusSimplePlugin_rspJoin_rspBuffer_output_valid);
  assign IBusSimplePlugin_rspJoin_join_payload_pc = IBusSimplePlugin_rspJoin_fetchRsp_pc;
  assign IBusSimplePlugin_rspJoin_join_payload_rsp_error = IBusSimplePlugin_rspJoin_fetchRsp_rsp_error;
  assign IBusSimplePlugin_rspJoin_join_payload_rsp_inst = IBusSimplePlugin_rspJoin_fetchRsp_rsp_inst;
  assign IBusSimplePlugin_rspJoin_join_payload_isRvc = IBusSimplePlugin_rspJoin_fetchRsp_isRvc;
  assign IBusSimplePlugin_rspJoin_join_fire = (IBusSimplePlugin_rspJoin_join_valid && IBusSimplePlugin_rspJoin_join_ready);
  assign IBusSimplePlugin_iBusRsp_stages_1_output_ready = (IBusSimplePlugin_iBusRsp_stages_1_output_valid ? IBusSimplePlugin_rspJoin_join_fire : IBusSimplePlugin_rspJoin_join_ready);
  assign IBusSimplePlugin_rspJoin_join_fire_1 = (IBusSimplePlugin_rspJoin_join_valid && IBusSimplePlugin_rspJoin_join_ready);
  assign IBusSimplePlugin_rspJoin_rspBuffer_output_ready = IBusSimplePlugin_rspJoin_join_fire_1;
  assign _zz_IBusSimplePlugin_iBusRsp_output_valid = (! IBusSimplePlugin_rspJoin_exceptionDetected);
  assign IBusSimplePlugin_rspJoin_join_ready = (IBusSimplePlugin_iBusRsp_output_ready && _zz_IBusSimplePlugin_iBusRsp_output_valid);
  assign IBusSimplePlugin_iBusRsp_output_valid = (IBusSimplePlugin_rspJoin_join_valid && _zz_IBusSimplePlugin_iBusRsp_output_valid);
  assign IBusSimplePlugin_iBusRsp_output_payload_pc = IBusSimplePlugin_rspJoin_join_payload_pc;
  assign IBusSimplePlugin_iBusRsp_output_payload_rsp_error = IBusSimplePlugin_rspJoin_join_payload_rsp_error;
  assign IBusSimplePlugin_iBusRsp_output_payload_rsp_inst = IBusSimplePlugin_rspJoin_join_payload_rsp_inst;
  assign IBusSimplePlugin_iBusRsp_output_payload_isRvc = IBusSimplePlugin_rspJoin_join_payload_isRvc;
  assign dBus_cmd_fire = (dBus_cmd_valid && dBus_cmd_ready);
  assign when_DBusSimplePlugin_l404 = (! execute_arbitration_isStuck);
  always @(*) begin
    execute_DBusSimplePlugin_skipCmd = 1'b0;
    if(execute_ALIGNEMENT_FAULT) begin
      execute_DBusSimplePlugin_skipCmd = 1'b1;
    end
  end

  assign dBus_cmd_valid = (((((execute_arbitration_isValid && execute_MEMORY_ENABLE) && (! execute_arbitration_isStuckByOthers)) && (! execute_arbitration_isFlushed)) && (! execute_DBusSimplePlugin_skipCmd)) && (! _zz_dBus_cmd_valid));
  assign dBus_cmd_payload_wr = execute_MEMORY_STORE;
  assign dBus_cmd_payload_size = execute_INSTRUCTION[13 : 12];
  always @(*) begin
    case(dBus_cmd_payload_size)
      2'b00 : begin
        _zz_dBus_cmd_payload_data = {{{execute_RS2[7 : 0],execute_RS2[7 : 0]},execute_RS2[7 : 0]},execute_RS2[7 : 0]};
      end
      2'b01 : begin
        _zz_dBus_cmd_payload_data = {execute_RS2[15 : 0],execute_RS2[15 : 0]};
      end
      default : begin
        _zz_dBus_cmd_payload_data = execute_RS2[31 : 0];
      end
    endcase
  end

  assign dBus_cmd_payload_data = _zz_dBus_cmd_payload_data;
  assign when_DBusSimplePlugin_l428 = ((((execute_arbitration_isValid && execute_MEMORY_ENABLE) && (! dBus_cmd_ready)) && (! execute_DBusSimplePlugin_skipCmd)) && (! _zz_dBus_cmd_valid));
  always @(*) begin
    case(dBus_cmd_payload_size)
      2'b00 : begin
        _zz_execute_DBusSimplePlugin_formalMask = 4'b0001;
      end
      2'b01 : begin
        _zz_execute_DBusSimplePlugin_formalMask = 4'b0011;
      end
      default : begin
        _zz_execute_DBusSimplePlugin_formalMask = 4'b1111;
      end
    endcase
  end

  assign execute_DBusSimplePlugin_formalMask = (_zz_execute_DBusSimplePlugin_formalMask <<< dBus_cmd_payload_address[1 : 0]);
  assign dBus_cmd_payload_address = execute_SRC_ADD;
  assign when_DBusSimplePlugin_l482 = (((execute_arbitration_isValid && execute_MEMORY_ENABLE) && (! execute_MEMORY_STORE)) && ((! dBus_rsp_ready) || (! _zz_dBus_cmd_valid)));
  always @(*) begin
    execute_DBusSimplePlugin_rspShifted = execute_MEMORY_READ_DATA;
    case(execute_MEMORY_ADDRESS_LOW)
      2'b01 : begin
        execute_DBusSimplePlugin_rspShifted[7 : 0] = execute_MEMORY_READ_DATA[15 : 8];
      end
      2'b10 : begin
        execute_DBusSimplePlugin_rspShifted[15 : 0] = execute_MEMORY_READ_DATA[31 : 16];
      end
      2'b11 : begin
        execute_DBusSimplePlugin_rspShifted[7 : 0] = execute_MEMORY_READ_DATA[31 : 24];
      end
      default : begin
      end
    endcase
  end

  assign switch_Misc_l210 = execute_INSTRUCTION[13 : 12];
  assign _zz_execute_DBusSimplePlugin_rspFormated = (execute_DBusSimplePlugin_rspShifted[7] && (! execute_INSTRUCTION[14]));
  always @(*) begin
    _zz_execute_DBusSimplePlugin_rspFormated_1[31] = _zz_execute_DBusSimplePlugin_rspFormated;
    _zz_execute_DBusSimplePlugin_rspFormated_1[30] = _zz_execute_DBusSimplePlugin_rspFormated;
    _zz_execute_DBusSimplePlugin_rspFormated_1[29] = _zz_execute_DBusSimplePlugin_rspFormated;
    _zz_execute_DBusSimplePlugin_rspFormated_1[28] = _zz_execute_DBusSimplePlugin_rspFormated;
    _zz_execute_DBusSimplePlugin_rspFormated_1[27] = _zz_execute_DBusSimplePlugin_rspFormated;
    _zz_execute_DBusSimplePlugin_rspFormated_1[26] = _zz_execute_DBusSimplePlugin_rspFormated;
    _zz_execute_DBusSimplePlugin_rspFormated_1[25] = _zz_execute_DBusSimplePlugin_rspFormated;
    _zz_execute_DBusSimplePlugin_rspFormated_1[24] = _zz_execute_DBusSimplePlugin_rspFormated;
    _zz_execute_DBusSimplePlugin_rspFormated_1[23] = _zz_execute_DBusSimplePlugin_rspFormated;
    _zz_execute_DBusSimplePlugin_rspFormated_1[22] = _zz_execute_DBusSimplePlugin_rspFormated;
    _zz_execute_DBusSimplePlugin_rspFormated_1[21] = _zz_execute_DBusSimplePlugin_rspFormated;
    _zz_execute_DBusSimplePlugin_rspFormated_1[20] = _zz_execute_DBusSimplePlugin_rspFormated;
    _zz_execute_DBusSimplePlugin_rspFormated_1[19] = _zz_execute_DBusSimplePlugin_rspFormated;
    _zz_execute_DBusSimplePlugin_rspFormated_1[18] = _zz_execute_DBusSimplePlugin_rspFormated;
    _zz_execute_DBusSimplePlugin_rspFormated_1[17] = _zz_execute_DBusSimplePlugin_rspFormated;
    _zz_execute_DBusSimplePlugin_rspFormated_1[16] = _zz_execute_DBusSimplePlugin_rspFormated;
    _zz_execute_DBusSimplePlugin_rspFormated_1[15] = _zz_execute_DBusSimplePlugin_rspFormated;
    _zz_execute_DBusSimplePlugin_rspFormated_1[14] = _zz_execute_DBusSimplePlugin_rspFormated;
    _zz_execute_DBusSimplePlugin_rspFormated_1[13] = _zz_execute_DBusSimplePlugin_rspFormated;
    _zz_execute_DBusSimplePlugin_rspFormated_1[12] = _zz_execute_DBusSimplePlugin_rspFormated;
    _zz_execute_DBusSimplePlugin_rspFormated_1[11] = _zz_execute_DBusSimplePlugin_rspFormated;
    _zz_execute_DBusSimplePlugin_rspFormated_1[10] = _zz_execute_DBusSimplePlugin_rspFormated;
    _zz_execute_DBusSimplePlugin_rspFormated_1[9] = _zz_execute_DBusSimplePlugin_rspFormated;
    _zz_execute_DBusSimplePlugin_rspFormated_1[8] = _zz_execute_DBusSimplePlugin_rspFormated;
    _zz_execute_DBusSimplePlugin_rspFormated_1[7 : 0] = execute_DBusSimplePlugin_rspShifted[7 : 0];
  end

  assign _zz_execute_DBusSimplePlugin_rspFormated_2 = (execute_DBusSimplePlugin_rspShifted[15] && (! execute_INSTRUCTION[14]));
  always @(*) begin
    _zz_execute_DBusSimplePlugin_rspFormated_3[31] = _zz_execute_DBusSimplePlugin_rspFormated_2;
    _zz_execute_DBusSimplePlugin_rspFormated_3[30] = _zz_execute_DBusSimplePlugin_rspFormated_2;
    _zz_execute_DBusSimplePlugin_rspFormated_3[29] = _zz_execute_DBusSimplePlugin_rspFormated_2;
    _zz_execute_DBusSimplePlugin_rspFormated_3[28] = _zz_execute_DBusSimplePlugin_rspFormated_2;
    _zz_execute_DBusSimplePlugin_rspFormated_3[27] = _zz_execute_DBusSimplePlugin_rspFormated_2;
    _zz_execute_DBusSimplePlugin_rspFormated_3[26] = _zz_execute_DBusSimplePlugin_rspFormated_2;
    _zz_execute_DBusSimplePlugin_rspFormated_3[25] = _zz_execute_DBusSimplePlugin_rspFormated_2;
    _zz_execute_DBusSimplePlugin_rspFormated_3[24] = _zz_execute_DBusSimplePlugin_rspFormated_2;
    _zz_execute_DBusSimplePlugin_rspFormated_3[23] = _zz_execute_DBusSimplePlugin_rspFormated_2;
    _zz_execute_DBusSimplePlugin_rspFormated_3[22] = _zz_execute_DBusSimplePlugin_rspFormated_2;
    _zz_execute_DBusSimplePlugin_rspFormated_3[21] = _zz_execute_DBusSimplePlugin_rspFormated_2;
    _zz_execute_DBusSimplePlugin_rspFormated_3[20] = _zz_execute_DBusSimplePlugin_rspFormated_2;
    _zz_execute_DBusSimplePlugin_rspFormated_3[19] = _zz_execute_DBusSimplePlugin_rspFormated_2;
    _zz_execute_DBusSimplePlugin_rspFormated_3[18] = _zz_execute_DBusSimplePlugin_rspFormated_2;
    _zz_execute_DBusSimplePlugin_rspFormated_3[17] = _zz_execute_DBusSimplePlugin_rspFormated_2;
    _zz_execute_DBusSimplePlugin_rspFormated_3[16] = _zz_execute_DBusSimplePlugin_rspFormated_2;
    _zz_execute_DBusSimplePlugin_rspFormated_3[15 : 0] = execute_DBusSimplePlugin_rspShifted[15 : 0];
  end

  always @(*) begin
    case(switch_Misc_l210)
      2'b00 : begin
        execute_DBusSimplePlugin_rspFormated = _zz_execute_DBusSimplePlugin_rspFormated_1;
      end
      2'b01 : begin
        execute_DBusSimplePlugin_rspFormated = _zz_execute_DBusSimplePlugin_rspFormated_3;
      end
      default : begin
        execute_DBusSimplePlugin_rspFormated = execute_DBusSimplePlugin_rspShifted;
      end
    endcase
  end

  assign when_DBusSimplePlugin_l558 = (execute_arbitration_isValid && execute_MEMORY_ENABLE);
  assign _zz_decode_BRANCH_CTRL_3 = ((decode_INSTRUCTION & 32'h00000018) == 32'h0);
  assign _zz_decode_BRANCH_CTRL_4 = ((decode_INSTRUCTION & 32'h00000004) == 32'h00000004);
  assign _zz_decode_BRANCH_CTRL_5 = ((decode_INSTRUCTION & 32'h00000048) == 32'h00000048);
  assign _zz_decode_BRANCH_CTRL_6 = ((decode_INSTRUCTION & 32'h00000010) == 32'h00000010);
  assign _zz_decode_BRANCH_CTRL_2 = {(|((decode_INSTRUCTION & 32'h00000050) == 32'h00000050)),{(|((decode_INSTRUCTION & _zz__zz_decode_BRANCH_CTRL_2) == 32'h00001008)),{(|{_zz_decode_BRANCH_CTRL_5,{_zz__zz_decode_BRANCH_CTRL_2_1,_zz__zz_decode_BRANCH_CTRL_2_3}}),{(|_zz__zz_decode_BRANCH_CTRL_2_5),{(|_zz__zz_decode_BRANCH_CTRL_2_6),{_zz__zz_decode_BRANCH_CTRL_2_8,{_zz__zz_decode_BRANCH_CTRL_2_11,_zz__zz_decode_BRANCH_CTRL_2_16}}}}}}};
  assign _zz_decode_SRC1_CTRL_2 = _zz_decode_BRANCH_CTRL_2[1 : 0];
  assign _zz_decode_SRC1_CTRL_1 = _zz_decode_SRC1_CTRL_2;
  assign _zz_decode_SRC2_CTRL_2 = _zz_decode_BRANCH_CTRL_2[6 : 5];
  assign _zz_decode_SRC2_CTRL_1 = _zz_decode_SRC2_CTRL_2;
  assign _zz_decode_ALU_CTRL_2 = _zz_decode_BRANCH_CTRL_2[14 : 13];
  assign _zz_decode_ALU_CTRL_1 = _zz_decode_ALU_CTRL_2;
  assign _zz_decode_ALU_BITWISE_CTRL_2 = _zz_decode_BRANCH_CTRL_2[17 : 16];
  assign _zz_decode_ALU_BITWISE_CTRL_1 = _zz_decode_ALU_BITWISE_CTRL_2;
  assign _zz_decode_SHIFT_CTRL_2 = _zz_decode_BRANCH_CTRL_2[20 : 19];
  assign _zz_decode_SHIFT_CTRL_1 = _zz_decode_SHIFT_CTRL_2;
  assign _zz_decode_BRANCH_CTRL_7 = _zz_decode_BRANCH_CTRL_2[22 : 21];
  assign _zz_decode_BRANCH_CTRL_1 = _zz_decode_BRANCH_CTRL_7;
  assign when_RegFilePlugin_l63 = (decode_INSTRUCTION[11 : 7] == 5'h0);
  assign execute_RegFilePlugin_srcInstruction = (execute_arbitration_isStuck ? execute_INSTRUCTION : decode_INSTRUCTION);
  assign execute_RegFilePlugin_regFileReadAddress1 = execute_RegFilePlugin_srcInstruction[19 : 15];
  assign execute_RegFilePlugin_regFileReadAddress2 = execute_RegFilePlugin_srcInstruction[24 : 20];
  assign execute_RegFilePlugin_rs1Data = _zz_RegFilePlugin_regFile_port0;
  assign execute_RegFilePlugin_rs2Data = _zz_RegFilePlugin_regFile_port1;
  always @(*) begin
    lastStageRegFileWrite_valid = (_zz_lastStageRegFileWrite_valid && execute_arbitration_isFiring);
    if(when_RegFilePlugin_l107) begin
      lastStageRegFileWrite_valid = 1'b0;
    end
  end

  assign lastStageRegFileWrite_payload_address = _zz_lastStageRegFileWrite_payload_address[11 : 7];
  assign lastStageRegFileWrite_payload_data = _zz_lastStageRegFileWrite_payload_data;
  assign when_RegFilePlugin_l107 = (lastStageRegFileWrite_payload_address == 5'h0);
  always @(*) begin
    case(execute_ALU_BITWISE_CTRL)
      AluBitwiseCtrlEnum_AND_1 : begin
        execute_IntAluPlugin_bitwise = (execute_SRC1 & execute_SRC2);
      end
      AluBitwiseCtrlEnum_OR_1 : begin
        execute_IntAluPlugin_bitwise = (execute_SRC1 | execute_SRC2);
      end
      default : begin
        execute_IntAluPlugin_bitwise = (execute_SRC1 ^ execute_SRC2);
      end
    endcase
  end

  always @(*) begin
    case(execute_ALU_CTRL)
      AluCtrlEnum_BITWISE : begin
        _zz_execute_REGFILE_WRITE_DATA = execute_IntAluPlugin_bitwise;
      end
      AluCtrlEnum_SLT_SLTU : begin
        _zz_execute_REGFILE_WRITE_DATA = {31'd0, _zz__zz_execute_REGFILE_WRITE_DATA};
      end
      default : begin
        _zz_execute_REGFILE_WRITE_DATA = execute_SRC_ADD_SUB;
      end
    endcase
  end

  always @(*) begin
    case(execute_SRC1_CTRL)
      Src1CtrlEnum_RS : begin
        _zz_execute_SRC1 = execute_RS1;
      end
      Src1CtrlEnum_PC_INCREMENT : begin
        _zz_execute_SRC1 = {29'd0, _zz__zz_execute_SRC1};
      end
      Src1CtrlEnum_IMU : begin
        _zz_execute_SRC1 = {execute_INSTRUCTION[31 : 12],12'h0};
      end
      default : begin
        _zz_execute_SRC1 = {27'd0, _zz__zz_execute_SRC1_1};
      end
    endcase
  end

  assign _zz_execute_SRC2 = execute_INSTRUCTION[31];
  always @(*) begin
    _zz_execute_SRC2_1[19] = _zz_execute_SRC2;
    _zz_execute_SRC2_1[18] = _zz_execute_SRC2;
    _zz_execute_SRC2_1[17] = _zz_execute_SRC2;
    _zz_execute_SRC2_1[16] = _zz_execute_SRC2;
    _zz_execute_SRC2_1[15] = _zz_execute_SRC2;
    _zz_execute_SRC2_1[14] = _zz_execute_SRC2;
    _zz_execute_SRC2_1[13] = _zz_execute_SRC2;
    _zz_execute_SRC2_1[12] = _zz_execute_SRC2;
    _zz_execute_SRC2_1[11] = _zz_execute_SRC2;
    _zz_execute_SRC2_1[10] = _zz_execute_SRC2;
    _zz_execute_SRC2_1[9] = _zz_execute_SRC2;
    _zz_execute_SRC2_1[8] = _zz_execute_SRC2;
    _zz_execute_SRC2_1[7] = _zz_execute_SRC2;
    _zz_execute_SRC2_1[6] = _zz_execute_SRC2;
    _zz_execute_SRC2_1[5] = _zz_execute_SRC2;
    _zz_execute_SRC2_1[4] = _zz_execute_SRC2;
    _zz_execute_SRC2_1[3] = _zz_execute_SRC2;
    _zz_execute_SRC2_1[2] = _zz_execute_SRC2;
    _zz_execute_SRC2_1[1] = _zz_execute_SRC2;
    _zz_execute_SRC2_1[0] = _zz_execute_SRC2;
  end

  assign _zz_execute_SRC2_2 = _zz__zz_execute_SRC2_2[11];
  always @(*) begin
    _zz_execute_SRC2_3[19] = _zz_execute_SRC2_2;
    _zz_execute_SRC2_3[18] = _zz_execute_SRC2_2;
    _zz_execute_SRC2_3[17] = _zz_execute_SRC2_2;
    _zz_execute_SRC2_3[16] = _zz_execute_SRC2_2;
    _zz_execute_SRC2_3[15] = _zz_execute_SRC2_2;
    _zz_execute_SRC2_3[14] = _zz_execute_SRC2_2;
    _zz_execute_SRC2_3[13] = _zz_execute_SRC2_2;
    _zz_execute_SRC2_3[12] = _zz_execute_SRC2_2;
    _zz_execute_SRC2_3[11] = _zz_execute_SRC2_2;
    _zz_execute_SRC2_3[10] = _zz_execute_SRC2_2;
    _zz_execute_SRC2_3[9] = _zz_execute_SRC2_2;
    _zz_execute_SRC2_3[8] = _zz_execute_SRC2_2;
    _zz_execute_SRC2_3[7] = _zz_execute_SRC2_2;
    _zz_execute_SRC2_3[6] = _zz_execute_SRC2_2;
    _zz_execute_SRC2_3[5] = _zz_execute_SRC2_2;
    _zz_execute_SRC2_3[4] = _zz_execute_SRC2_2;
    _zz_execute_SRC2_3[3] = _zz_execute_SRC2_2;
    _zz_execute_SRC2_3[2] = _zz_execute_SRC2_2;
    _zz_execute_SRC2_3[1] = _zz_execute_SRC2_2;
    _zz_execute_SRC2_3[0] = _zz_execute_SRC2_2;
  end

  always @(*) begin
    case(execute_SRC2_CTRL)
      Src2CtrlEnum_RS : begin
        _zz_execute_SRC2_4 = execute_RS2;
      end
      Src2CtrlEnum_IMI : begin
        _zz_execute_SRC2_4 = {_zz_execute_SRC2_1,execute_INSTRUCTION[31 : 20]};
      end
      Src2CtrlEnum_IMS : begin
        _zz_execute_SRC2_4 = {_zz_execute_SRC2_3,{execute_INSTRUCTION[31 : 25],execute_INSTRUCTION[11 : 7]}};
      end
      default : begin
        _zz_execute_SRC2_4 = execute_PC;
      end
    endcase
  end

  always @(*) begin
    execute_SrcPlugin_addSub = _zz_execute_SrcPlugin_addSub;
    if(execute_SRC2_FORCE_ZERO) begin
      execute_SrcPlugin_addSub = execute_SRC1;
    end
  end

  assign execute_SrcPlugin_less = ((execute_SRC1[31] == execute_SRC2[31]) ? execute_SrcPlugin_addSub[31] : (execute_SRC_LESS_UNSIGNED ? execute_SRC2[31] : execute_SRC1[31]));
  assign execute_LightShifterPlugin_isShift = (execute_SHIFT_CTRL != ShiftCtrlEnum_DISABLE_1);
  assign execute_LightShifterPlugin_amplitude = (execute_LightShifterPlugin_isActive ? execute_LightShifterPlugin_amplitudeReg : execute_SRC2[4 : 0]);
  assign when_ShiftPlugins_l163 = (! execute_arbitration_isStuckByOthers);
  assign execute_LightShifterPlugin_shiftInput = (execute_LightShifterPlugin_isActive ? execute_LightShifterPlugin_shiftReg : execute_SRC1);
  assign execute_LightShifterPlugin_done = (execute_LightShifterPlugin_amplitude[4 : 1] == 4'b0000);
  assign when_ShiftPlugins_l169 = ((execute_arbitration_isValid && execute_LightShifterPlugin_isShift) && (execute_SRC2[4 : 0] != 5'h0));
  always @(*) begin
    case(execute_SHIFT_CTRL)
      ShiftCtrlEnum_SLL_1 : begin
        _zz_lastStageRegFileWrite_payload_data_1 = (execute_LightShifterPlugin_shiftInput <<< 1);
      end
      default : begin
        _zz_lastStageRegFileWrite_payload_data_1 = _zz__zz_lastStageRegFileWrite_payload_data_1;
      end
    endcase
  end

  assign when_ShiftPlugins_l175 = (! execute_arbitration_isStuckByOthers);
  assign when_ShiftPlugins_l184 = (! execute_LightShifterPlugin_done);
  assign execute_BranchPlugin_eq = (execute_SRC1 == execute_SRC2);
  assign switch_Misc_l210_1 = execute_INSTRUCTION[14 : 12];
  always @(*) begin
    casez(switch_Misc_l210_1)
      3'b000 : begin
        _zz_execute_BRANCH_DO = execute_BranchPlugin_eq;
      end
      3'b001 : begin
        _zz_execute_BRANCH_DO = (! execute_BranchPlugin_eq);
      end
      3'b1?1 : begin
        _zz_execute_BRANCH_DO = (! execute_SRC_LESS);
      end
      default : begin
        _zz_execute_BRANCH_DO = execute_SRC_LESS;
      end
    endcase
  end

  always @(*) begin
    case(execute_BRANCH_CTRL)
      BranchCtrlEnum_INC : begin
        _zz_execute_BRANCH_DO_1 = 1'b0;
      end
      BranchCtrlEnum_JAL : begin
        _zz_execute_BRANCH_DO_1 = 1'b1;
      end
      BranchCtrlEnum_JALR : begin
        _zz_execute_BRANCH_DO_1 = 1'b1;
      end
      default : begin
        _zz_execute_BRANCH_DO_1 = _zz_execute_BRANCH_DO;
      end
    endcase
  end

  assign execute_BranchPlugin_branch_src1 = ((execute_BRANCH_CTRL == BranchCtrlEnum_JALR) ? execute_RS1 : execute_PC);
  assign _zz_execute_BranchPlugin_branch_src2 = _zz__zz_execute_BranchPlugin_branch_src2[19];
  always @(*) begin
    _zz_execute_BranchPlugin_branch_src2_1[10] = _zz_execute_BranchPlugin_branch_src2;
    _zz_execute_BranchPlugin_branch_src2_1[9] = _zz_execute_BranchPlugin_branch_src2;
    _zz_execute_BranchPlugin_branch_src2_1[8] = _zz_execute_BranchPlugin_branch_src2;
    _zz_execute_BranchPlugin_branch_src2_1[7] = _zz_execute_BranchPlugin_branch_src2;
    _zz_execute_BranchPlugin_branch_src2_1[6] = _zz_execute_BranchPlugin_branch_src2;
    _zz_execute_BranchPlugin_branch_src2_1[5] = _zz_execute_BranchPlugin_branch_src2;
    _zz_execute_BranchPlugin_branch_src2_1[4] = _zz_execute_BranchPlugin_branch_src2;
    _zz_execute_BranchPlugin_branch_src2_1[3] = _zz_execute_BranchPlugin_branch_src2;
    _zz_execute_BranchPlugin_branch_src2_1[2] = _zz_execute_BranchPlugin_branch_src2;
    _zz_execute_BranchPlugin_branch_src2_1[1] = _zz_execute_BranchPlugin_branch_src2;
    _zz_execute_BranchPlugin_branch_src2_1[0] = _zz_execute_BranchPlugin_branch_src2;
  end

  assign _zz_execute_BranchPlugin_branch_src2_2 = execute_INSTRUCTION[31];
  always @(*) begin
    _zz_execute_BranchPlugin_branch_src2_3[19] = _zz_execute_BranchPlugin_branch_src2_2;
    _zz_execute_BranchPlugin_branch_src2_3[18] = _zz_execute_BranchPlugin_branch_src2_2;
    _zz_execute_BranchPlugin_branch_src2_3[17] = _zz_execute_BranchPlugin_branch_src2_2;
    _zz_execute_BranchPlugin_branch_src2_3[16] = _zz_execute_BranchPlugin_branch_src2_2;
    _zz_execute_BranchPlugin_branch_src2_3[15] = _zz_execute_BranchPlugin_branch_src2_2;
    _zz_execute_BranchPlugin_branch_src2_3[14] = _zz_execute_BranchPlugin_branch_src2_2;
    _zz_execute_BranchPlugin_branch_src2_3[13] = _zz_execute_BranchPlugin_branch_src2_2;
    _zz_execute_BranchPlugin_branch_src2_3[12] = _zz_execute_BranchPlugin_branch_src2_2;
    _zz_execute_BranchPlugin_branch_src2_3[11] = _zz_execute_BranchPlugin_branch_src2_2;
    _zz_execute_BranchPlugin_branch_src2_3[10] = _zz_execute_BranchPlugin_branch_src2_2;
    _zz_execute_BranchPlugin_branch_src2_3[9] = _zz_execute_BranchPlugin_branch_src2_2;
    _zz_execute_BranchPlugin_branch_src2_3[8] = _zz_execute_BranchPlugin_branch_src2_2;
    _zz_execute_BranchPlugin_branch_src2_3[7] = _zz_execute_BranchPlugin_branch_src2_2;
    _zz_execute_BranchPlugin_branch_src2_3[6] = _zz_execute_BranchPlugin_branch_src2_2;
    _zz_execute_BranchPlugin_branch_src2_3[5] = _zz_execute_BranchPlugin_branch_src2_2;
    _zz_execute_BranchPlugin_branch_src2_3[4] = _zz_execute_BranchPlugin_branch_src2_2;
    _zz_execute_BranchPlugin_branch_src2_3[3] = _zz_execute_BranchPlugin_branch_src2_2;
    _zz_execute_BranchPlugin_branch_src2_3[2] = _zz_execute_BranchPlugin_branch_src2_2;
    _zz_execute_BranchPlugin_branch_src2_3[1] = _zz_execute_BranchPlugin_branch_src2_2;
    _zz_execute_BranchPlugin_branch_src2_3[0] = _zz_execute_BranchPlugin_branch_src2_2;
  end

  assign _zz_execute_BranchPlugin_branch_src2_4 = _zz__zz_execute_BranchPlugin_branch_src2_4[11];
  always @(*) begin
    _zz_execute_BranchPlugin_branch_src2_5[18] = _zz_execute_BranchPlugin_branch_src2_4;
    _zz_execute_BranchPlugin_branch_src2_5[17] = _zz_execute_BranchPlugin_branch_src2_4;
    _zz_execute_BranchPlugin_branch_src2_5[16] = _zz_execute_BranchPlugin_branch_src2_4;
    _zz_execute_BranchPlugin_branch_src2_5[15] = _zz_execute_BranchPlugin_branch_src2_4;
    _zz_execute_BranchPlugin_branch_src2_5[14] = _zz_execute_BranchPlugin_branch_src2_4;
    _zz_execute_BranchPlugin_branch_src2_5[13] = _zz_execute_BranchPlugin_branch_src2_4;
    _zz_execute_BranchPlugin_branch_src2_5[12] = _zz_execute_BranchPlugin_branch_src2_4;
    _zz_execute_BranchPlugin_branch_src2_5[11] = _zz_execute_BranchPlugin_branch_src2_4;
    _zz_execute_BranchPlugin_branch_src2_5[10] = _zz_execute_BranchPlugin_branch_src2_4;
    _zz_execute_BranchPlugin_branch_src2_5[9] = _zz_execute_BranchPlugin_branch_src2_4;
    _zz_execute_BranchPlugin_branch_src2_5[8] = _zz_execute_BranchPlugin_branch_src2_4;
    _zz_execute_BranchPlugin_branch_src2_5[7] = _zz_execute_BranchPlugin_branch_src2_4;
    _zz_execute_BranchPlugin_branch_src2_5[6] = _zz_execute_BranchPlugin_branch_src2_4;
    _zz_execute_BranchPlugin_branch_src2_5[5] = _zz_execute_BranchPlugin_branch_src2_4;
    _zz_execute_BranchPlugin_branch_src2_5[4] = _zz_execute_BranchPlugin_branch_src2_4;
    _zz_execute_BranchPlugin_branch_src2_5[3] = _zz_execute_BranchPlugin_branch_src2_4;
    _zz_execute_BranchPlugin_branch_src2_5[2] = _zz_execute_BranchPlugin_branch_src2_4;
    _zz_execute_BranchPlugin_branch_src2_5[1] = _zz_execute_BranchPlugin_branch_src2_4;
    _zz_execute_BranchPlugin_branch_src2_5[0] = _zz_execute_BranchPlugin_branch_src2_4;
  end

  always @(*) begin
    case(execute_BRANCH_CTRL)
      BranchCtrlEnum_JAL : begin
        _zz_execute_BranchPlugin_branch_src2_6 = {{_zz_execute_BranchPlugin_branch_src2_1,{{{execute_INSTRUCTION[31],execute_INSTRUCTION[19 : 12]},execute_INSTRUCTION[20]},execute_INSTRUCTION[30 : 21]}},1'b0};
      end
      BranchCtrlEnum_JALR : begin
        _zz_execute_BranchPlugin_branch_src2_6 = {_zz_execute_BranchPlugin_branch_src2_3,execute_INSTRUCTION[31 : 20]};
      end
      default : begin
        _zz_execute_BranchPlugin_branch_src2_6 = {{_zz_execute_BranchPlugin_branch_src2_5,{{{execute_INSTRUCTION[31],execute_INSTRUCTION[7]},execute_INSTRUCTION[30 : 25]},execute_INSTRUCTION[11 : 8]}},1'b0};
      end
    endcase
  end

  assign execute_BranchPlugin_branch_src2 = _zz_execute_BranchPlugin_branch_src2_6;
  assign execute_BranchPlugin_branchAdder = (execute_BranchPlugin_branch_src1 + execute_BranchPlugin_branch_src2);
  assign BranchPlugin_jumpInterface_valid = ((execute_arbitration_isValid && execute_BRANCH_DO) && (! execute_arbitration_isStuckByOthers));
  assign BranchPlugin_jumpInterface_payload = execute_BRANCH_CALC;
  assign when_BranchPlugin_l171 = ((execute_arbitration_isValid && execute_IS_FENCEI) && 1'b0);
  always @(*) begin
    HazardSimplePlugin_src0Hazard = 1'b0;
    if(HazardSimplePlugin_writeBackBuffer_valid) begin
      if(HazardSimplePlugin_addr0Match) begin
        HazardSimplePlugin_src0Hazard = 1'b1;
      end
    end
    if(when_HazardSimplePlugin_l105) begin
      HazardSimplePlugin_src0Hazard = 1'b0;
    end
  end

  always @(*) begin
    HazardSimplePlugin_src1Hazard = 1'b0;
    if(HazardSimplePlugin_writeBackBuffer_valid) begin
      if(HazardSimplePlugin_addr1Match) begin
        HazardSimplePlugin_src1Hazard = 1'b1;
      end
    end
    if(when_HazardSimplePlugin_l108) begin
      HazardSimplePlugin_src1Hazard = 1'b0;
    end
  end

  assign HazardSimplePlugin_writeBackWrites_valid = (_zz_lastStageRegFileWrite_valid && execute_arbitration_isFiring);
  assign HazardSimplePlugin_writeBackWrites_payload_address = _zz_lastStageRegFileWrite_payload_address[11 : 7];
  assign HazardSimplePlugin_writeBackWrites_payload_data = _zz_lastStageRegFileWrite_payload_data;
  assign HazardSimplePlugin_addr0Match = (HazardSimplePlugin_writeBackBuffer_payload_address == execute_INSTRUCTION[19 : 15]);
  assign HazardSimplePlugin_addr1Match = (HazardSimplePlugin_writeBackBuffer_payload_address == execute_INSTRUCTION[24 : 20]);
  assign when_HazardSimplePlugin_l105 = (! execute_RS1_USE);
  assign when_HazardSimplePlugin_l108 = (! execute_RS2_USE);
  assign when_HazardSimplePlugin_l113 = (execute_arbitration_isValid && (HazardSimplePlugin_src0Hazard || HazardSimplePlugin_src1Hazard));
  assign when_DebugPlugin_l225 = (DebugPlugin_haltIt && (! DebugPlugin_isPipBusy));
  assign DebugPlugin_allowEBreak = (DebugPlugin_debugUsed && (! DebugPlugin_disableEbreak));
  always @(*) begin
    debug_bus_cmd_ready = 1'b1;
    if(debug_bus_cmd_valid) begin
      case(switch_DebugPlugin_l267)
        6'h01 : begin
          if(debug_bus_cmd_payload_wr) begin
            debug_bus_cmd_ready = IBusSimplePlugin_injectionPort_ready;
          end
        end
        default : begin
        end
      endcase
    end
  end

  always @(*) begin
    debug_bus_rsp_data = DebugPlugin_busReadDataReg;
    if(when_DebugPlugin_l244) begin
      debug_bus_rsp_data[0] = DebugPlugin_resetIt;
      debug_bus_rsp_data[1] = DebugPlugin_haltIt;
      debug_bus_rsp_data[2] = DebugPlugin_isPipBusy;
      debug_bus_rsp_data[3] = DebugPlugin_haltedByBreak;
      debug_bus_rsp_data[4] = DebugPlugin_stepIt;
    end
  end

  assign when_DebugPlugin_l244 = (! _zz_when_DebugPlugin_l244);
  always @(*) begin
    IBusSimplePlugin_injectionPort_valid = 1'b0;
    if(debug_bus_cmd_valid) begin
      case(switch_DebugPlugin_l267)
        6'h01 : begin
          if(debug_bus_cmd_payload_wr) begin
            IBusSimplePlugin_injectionPort_valid = 1'b1;
          end
        end
        default : begin
        end
      endcase
    end
  end

  assign IBusSimplePlugin_injectionPort_payload = debug_bus_cmd_payload_data;
  assign switch_DebugPlugin_l267 = debug_bus_cmd_payload_address[7 : 2];
  assign when_DebugPlugin_l271 = debug_bus_cmd_payload_data[16];
  assign when_DebugPlugin_l271_1 = debug_bus_cmd_payload_data[24];
  assign when_DebugPlugin_l272 = debug_bus_cmd_payload_data[17];
  assign when_DebugPlugin_l272_1 = debug_bus_cmd_payload_data[25];
  assign when_DebugPlugin_l273 = debug_bus_cmd_payload_data[25];
  assign when_DebugPlugin_l274 = debug_bus_cmd_payload_data[25];
  assign when_DebugPlugin_l275 = debug_bus_cmd_payload_data[18];
  assign when_DebugPlugin_l275_1 = debug_bus_cmd_payload_data[26];
  assign when_DebugPlugin_l295 = (execute_arbitration_isValid && execute_DO_EBREAK);
  assign when_DebugPlugin_l298 = (1'b0 == 1'b0);
  assign when_DebugPlugin_l311 = (DebugPlugin_stepIt && IBusSimplePlugin_incomingInstruction);
  assign debug_resetOut = DebugPlugin_resetIt_regNext;
  assign when_Pipeline_l124 = (! execute_arbitration_isStuck);
  assign when_Pipeline_l124_1 = (! execute_arbitration_isStuck);
  assign when_Pipeline_l124_2 = (! execute_arbitration_isStuck);
  assign _zz_decode_to_execute_SRC1_CTRL_1 = decode_SRC1_CTRL;
  assign _zz_decode_SRC1_CTRL = _zz_decode_SRC1_CTRL_1;
  assign when_Pipeline_l124_3 = (! execute_arbitration_isStuck);
  assign _zz_execute_SRC1_CTRL = decode_to_execute_SRC1_CTRL;
  assign when_Pipeline_l124_4 = (! execute_arbitration_isStuck);
  assign when_Pipeline_l124_5 = (! execute_arbitration_isStuck);
  assign when_Pipeline_l124_6 = (! execute_arbitration_isStuck);
  assign _zz_decode_to_execute_SRC2_CTRL_1 = decode_SRC2_CTRL;
  assign _zz_decode_SRC2_CTRL = _zz_decode_SRC2_CTRL_1;
  assign when_Pipeline_l124_7 = (! execute_arbitration_isStuck);
  assign _zz_execute_SRC2_CTRL = decode_to_execute_SRC2_CTRL;
  assign when_Pipeline_l124_8 = (! execute_arbitration_isStuck);
  assign when_Pipeline_l124_9 = (! execute_arbitration_isStuck);
  assign when_Pipeline_l124_10 = (! execute_arbitration_isStuck);
  assign _zz_decode_to_execute_ALU_CTRL_1 = decode_ALU_CTRL;
  assign _zz_decode_ALU_CTRL = _zz_decode_ALU_CTRL_1;
  assign when_Pipeline_l124_11 = (! execute_arbitration_isStuck);
  assign _zz_execute_ALU_CTRL = decode_to_execute_ALU_CTRL;
  assign when_Pipeline_l124_12 = (! execute_arbitration_isStuck);
  assign _zz_decode_to_execute_ALU_BITWISE_CTRL_1 = decode_ALU_BITWISE_CTRL;
  assign _zz_decode_ALU_BITWISE_CTRL = _zz_decode_ALU_BITWISE_CTRL_1;
  assign when_Pipeline_l124_13 = (! execute_arbitration_isStuck);
  assign _zz_execute_ALU_BITWISE_CTRL = decode_to_execute_ALU_BITWISE_CTRL;
  assign _zz_decode_to_execute_SHIFT_CTRL_1 = decode_SHIFT_CTRL;
  assign _zz_decode_SHIFT_CTRL = _zz_decode_SHIFT_CTRL_1;
  assign when_Pipeline_l124_14 = (! execute_arbitration_isStuck);
  assign _zz_execute_SHIFT_CTRL = decode_to_execute_SHIFT_CTRL;
  assign _zz_decode_to_execute_BRANCH_CTRL_1 = decode_BRANCH_CTRL;
  assign _zz_decode_BRANCH_CTRL = _zz_decode_BRANCH_CTRL_1;
  assign when_Pipeline_l124_15 = (! execute_arbitration_isStuck);
  assign _zz_execute_BRANCH_CTRL = decode_to_execute_BRANCH_CTRL;
  assign when_Pipeline_l124_16 = (! execute_arbitration_isStuck);
  assign when_Pipeline_l124_17 = (! execute_arbitration_isStuck);
  assign when_Pipeline_l124_18 = (! execute_arbitration_isStuck);
  assign decode_arbitration_isFlushed = ((execute_arbitration_flushNext != 1'b0) || ({execute_arbitration_flushIt,decode_arbitration_flushIt} != 2'b00));
  assign execute_arbitration_isFlushed = (1'b0 || (execute_arbitration_flushIt != 1'b0));
  assign decode_arbitration_isStuckByOthers = (decode_arbitration_haltByOther || (1'b0 || execute_arbitration_isStuck));
  assign decode_arbitration_isStuck = (decode_arbitration_haltItself || decode_arbitration_isStuckByOthers);
  assign decode_arbitration_isMoving = ((! decode_arbitration_isStuck) && (! decode_arbitration_removeIt));
  assign decode_arbitration_isFiring = ((decode_arbitration_isValid && (! decode_arbitration_isStuck)) && (! decode_arbitration_removeIt));
  assign execute_arbitration_isStuckByOthers = (execute_arbitration_haltByOther || 1'b0);
  assign execute_arbitration_isStuck = (execute_arbitration_haltItself || execute_arbitration_isStuckByOthers);
  assign execute_arbitration_isMoving = ((! execute_arbitration_isStuck) && (! execute_arbitration_removeIt));
  assign execute_arbitration_isFiring = ((execute_arbitration_isValid && (! execute_arbitration_isStuck)) && (! execute_arbitration_removeIt));
  assign when_Pipeline_l151 = ((! execute_arbitration_isStuck) || execute_arbitration_removeIt);
  assign when_Pipeline_l154 = ((! decode_arbitration_isStuck) && (! decode_arbitration_removeIt));
  always @(*) begin
    IBusSimplePlugin_injectionPort_ready = 1'b0;
    case(switch_Fetcher_l365)
      3'b100 : begin
        IBusSimplePlugin_injectionPort_ready = 1'b1;
      end
      default : begin
      end
    endcase
  end

  assign when_Fetcher_l381 = (! decode_arbitration_isStuck);
  assign when_Fetcher_l401 = (switch_Fetcher_l365 != 3'b000);
  always @(posedge debugCdCtrl_external_clk) begin
    if(systemCdCtrl_logic_outputReset) begin
      IBusSimplePlugin_fetchPc_pcReg <= 32'h80000000;
      IBusSimplePlugin_fetchPc_correctionReg <= 1'b0;
      IBusSimplePlugin_fetchPc_booted <= 1'b0;
      IBusSimplePlugin_fetchPc_inc <= 1'b0;
      _zz_IBusSimplePlugin_iBusRsp_stages_0_output_ready_2 <= 1'b0;
      _zz_IBusSimplePlugin_injector_decodeInput_valid <= 1'b0;
      IBusSimplePlugin_injector_nextPcCalc_valids_0 <= 1'b0;
      IBusSimplePlugin_injector_nextPcCalc_valids_1 <= 1'b0;
      IBusSimplePlugin_injector_nextPcCalc_valids_2 <= 1'b0;
      IBusSimplePlugin_cmd_rValid <= 1'b0;
      IBusSimplePlugin_pending_value <= 3'b000;
      IBusSimplePlugin_rspJoin_rspBuffer_discardCounter <= 3'b000;
      _zz_dBus_cmd_valid <= 1'b0;
      execute_LightShifterPlugin_isActive <= 1'b0;
      HazardSimplePlugin_writeBackBuffer_valid <= 1'b0;
      execute_arbitration_isValid <= 1'b0;
      switch_Fetcher_l365 <= 3'b000;
    end else begin
      if(IBusSimplePlugin_fetchPc_correction) begin
        IBusSimplePlugin_fetchPc_correctionReg <= 1'b1;
      end
      if(IBusSimplePlugin_fetchPc_output_fire) begin
        IBusSimplePlugin_fetchPc_correctionReg <= 1'b0;
      end
      IBusSimplePlugin_fetchPc_booted <= 1'b1;
      if(when_Fetcher_l134) begin
        IBusSimplePlugin_fetchPc_inc <= 1'b0;
      end
      if(IBusSimplePlugin_fetchPc_output_fire_1) begin
        IBusSimplePlugin_fetchPc_inc <= 1'b1;
      end
      if(when_Fetcher_l134_1) begin
        IBusSimplePlugin_fetchPc_inc <= 1'b0;
      end
      if(when_Fetcher_l161) begin
        IBusSimplePlugin_fetchPc_pcReg <= IBusSimplePlugin_fetchPc_pc;
      end
      if(IBusSimplePlugin_iBusRsp_flush) begin
        _zz_IBusSimplePlugin_iBusRsp_stages_0_output_ready_2 <= 1'b0;
      end
      if(_zz_IBusSimplePlugin_iBusRsp_stages_0_output_ready) begin
        _zz_IBusSimplePlugin_iBusRsp_stages_0_output_ready_2 <= (IBusSimplePlugin_iBusRsp_stages_0_output_valid && (! 1'b0));
      end
      if(decode_arbitration_removeIt) begin
        _zz_IBusSimplePlugin_injector_decodeInput_valid <= 1'b0;
      end
      if(IBusSimplePlugin_iBusRsp_output_ready) begin
        _zz_IBusSimplePlugin_injector_decodeInput_valid <= (IBusSimplePlugin_iBusRsp_output_valid && (! IBusSimplePlugin_externalFlush));
      end
      if(IBusSimplePlugin_fetchPc_flushed) begin
        IBusSimplePlugin_injector_nextPcCalc_valids_0 <= 1'b0;
      end
      if(when_Fetcher_l332) begin
        IBusSimplePlugin_injector_nextPcCalc_valids_0 <= 1'b1;
      end
      if(IBusSimplePlugin_fetchPc_flushed) begin
        IBusSimplePlugin_injector_nextPcCalc_valids_1 <= 1'b0;
      end
      if(when_Fetcher_l332_1) begin
        IBusSimplePlugin_injector_nextPcCalc_valids_1 <= IBusSimplePlugin_injector_nextPcCalc_valids_0;
      end
      if(IBusSimplePlugin_fetchPc_flushed) begin
        IBusSimplePlugin_injector_nextPcCalc_valids_1 <= 1'b0;
      end
      if(IBusSimplePlugin_fetchPc_flushed) begin
        IBusSimplePlugin_injector_nextPcCalc_valids_2 <= 1'b0;
      end
      if(when_Fetcher_l332_2) begin
        IBusSimplePlugin_injector_nextPcCalc_valids_2 <= IBusSimplePlugin_injector_nextPcCalc_valids_1;
      end
      if(IBusSimplePlugin_fetchPc_flushed) begin
        IBusSimplePlugin_injector_nextPcCalc_valids_2 <= 1'b0;
      end
      if(IBusSimplePlugin_cmd_valid) begin
        IBusSimplePlugin_cmd_rValid <= 1'b1;
      end
      if(IBusSimplePlugin_cmd_s2mPipe_ready) begin
        IBusSimplePlugin_cmd_rValid <= 1'b0;
      end
      IBusSimplePlugin_pending_value <= IBusSimplePlugin_pending_next;
      IBusSimplePlugin_rspJoin_rspBuffer_discardCounter <= (IBusSimplePlugin_rspJoin_rspBuffer_discardCounter - _zz_IBusSimplePlugin_rspJoin_rspBuffer_discardCounter);
      if(IBusSimplePlugin_iBusRsp_flush) begin
        IBusSimplePlugin_rspJoin_rspBuffer_discardCounter <= (IBusSimplePlugin_pending_value - _zz_IBusSimplePlugin_rspJoin_rspBuffer_discardCounter_2);
      end
      if(dBus_cmd_fire) begin
        _zz_dBus_cmd_valid <= 1'b1;
      end
      if(when_DBusSimplePlugin_l404) begin
        _zz_dBus_cmd_valid <= 1'b0;
      end
      if(when_ShiftPlugins_l169) begin
        if(when_ShiftPlugins_l175) begin
          execute_LightShifterPlugin_isActive <= 1'b1;
          if(execute_LightShifterPlugin_done) begin
            execute_LightShifterPlugin_isActive <= 1'b0;
          end
        end
      end
      if(execute_arbitration_removeIt) begin
        execute_LightShifterPlugin_isActive <= 1'b0;
      end
      HazardSimplePlugin_writeBackBuffer_valid <= HazardSimplePlugin_writeBackWrites_valid;
      if(when_Pipeline_l151) begin
        execute_arbitration_isValid <= 1'b0;
      end
      if(when_Pipeline_l154) begin
        execute_arbitration_isValid <= decode_arbitration_isValid;
      end
      case(switch_Fetcher_l365)
        3'b000 : begin
          if(IBusSimplePlugin_injectionPort_valid) begin
            switch_Fetcher_l365 <= 3'b001;
          end
        end
        3'b001 : begin
          switch_Fetcher_l365 <= 3'b010;
        end
        3'b010 : begin
          switch_Fetcher_l365 <= 3'b011;
        end
        3'b011 : begin
          if(when_Fetcher_l381) begin
            switch_Fetcher_l365 <= 3'b100;
          end
        end
        3'b100 : begin
          switch_Fetcher_l365 <= 3'b000;
        end
        default : begin
        end
      endcase
    end
  end

  always @(posedge debugCdCtrl_external_clk) begin
    if(IBusSimplePlugin_iBusRsp_output_ready) begin
      _zz_IBusSimplePlugin_injector_decodeInput_payload_pc <= IBusSimplePlugin_iBusRsp_output_payload_pc;
      _zz_IBusSimplePlugin_injector_decodeInput_payload_rsp_error <= IBusSimplePlugin_iBusRsp_output_payload_rsp_error;
      _zz_IBusSimplePlugin_injector_decodeInput_payload_rsp_inst <= IBusSimplePlugin_iBusRsp_output_payload_rsp_inst;
      _zz_IBusSimplePlugin_injector_decodeInput_payload_isRvc <= IBusSimplePlugin_iBusRsp_output_payload_isRvc;
    end
    if(IBusSimplePlugin_injector_decodeInput_ready) begin
      IBusSimplePlugin_injector_formal_rawInDecode <= IBusSimplePlugin_iBusRsp_output_payload_rsp_inst;
    end
    if(IBusSimplePlugin_cmd_ready) begin
      IBusSimplePlugin_cmd_rData_pc <= IBusSimplePlugin_cmd_payload_pc;
    end
    if(when_ShiftPlugins_l163) begin
      execute_LightShifterPlugin_shiftReg <= _zz_lastStageRegFileWrite_payload_data;
    end
    if(when_ShiftPlugins_l169) begin
      if(when_ShiftPlugins_l175) begin
        execute_LightShifterPlugin_amplitudeReg <= (execute_LightShifterPlugin_amplitude - 5'h01);
      end
    end
    HazardSimplePlugin_writeBackBuffer_payload_address <= HazardSimplePlugin_writeBackWrites_payload_address;
    HazardSimplePlugin_writeBackBuffer_payload_data <= HazardSimplePlugin_writeBackWrites_payload_data;
    if(when_Pipeline_l124) begin
      decode_to_execute_PC <= decode_PC;
    end
    if(when_Pipeline_l124_1) begin
      decode_to_execute_INSTRUCTION <= _zz_decode_to_execute_INSTRUCTION;
    end
    if(when_Pipeline_l124_2) begin
      decode_to_execute_FORMAL_PC_NEXT <= decode_FORMAL_PC_NEXT;
    end
    if(when_Pipeline_l124_3) begin
      decode_to_execute_SRC1_CTRL <= _zz_decode_to_execute_SRC1_CTRL;
    end
    if(when_Pipeline_l124_4) begin
      decode_to_execute_SRC_USE_SUB_LESS <= decode_SRC_USE_SUB_LESS;
    end
    if(when_Pipeline_l124_5) begin
      decode_to_execute_MEMORY_ENABLE <= decode_MEMORY_ENABLE;
    end
    if(when_Pipeline_l124_6) begin
      decode_to_execute_RS1_USE <= decode_RS1_USE;
    end
    if(when_Pipeline_l124_7) begin
      decode_to_execute_SRC2_CTRL <= _zz_decode_to_execute_SRC2_CTRL;
    end
    if(when_Pipeline_l124_8) begin
      decode_to_execute_REGFILE_WRITE_VALID <= decode_REGFILE_WRITE_VALID;
    end
    if(when_Pipeline_l124_9) begin
      decode_to_execute_MEMORY_STORE <= decode_MEMORY_STORE;
    end
    if(when_Pipeline_l124_10) begin
      decode_to_execute_RS2_USE <= decode_RS2_USE;
    end
    if(when_Pipeline_l124_11) begin
      decode_to_execute_ALU_CTRL <= _zz_decode_to_execute_ALU_CTRL;
    end
    if(when_Pipeline_l124_12) begin
      decode_to_execute_SRC_LESS_UNSIGNED <= decode_SRC_LESS_UNSIGNED;
    end
    if(when_Pipeline_l124_13) begin
      decode_to_execute_ALU_BITWISE_CTRL <= _zz_decode_to_execute_ALU_BITWISE_CTRL;
    end
    if(when_Pipeline_l124_14) begin
      decode_to_execute_SHIFT_CTRL <= _zz_decode_to_execute_SHIFT_CTRL;
    end
    if(when_Pipeline_l124_15) begin
      decode_to_execute_BRANCH_CTRL <= _zz_decode_to_execute_BRANCH_CTRL;
    end
    if(when_Pipeline_l124_16) begin
      decode_to_execute_IS_FENCEI <= decode_IS_FENCEI;
    end
    if(when_Pipeline_l124_17) begin
      decode_to_execute_SRC2_FORCE_ZERO <= decode_SRC2_FORCE_ZERO;
    end
    if(when_Pipeline_l124_18) begin
      decode_to_execute_DO_EBREAK <= decode_DO_EBREAK;
    end
    if(when_Fetcher_l401) begin
      _zz_IBusSimplePlugin_injector_decodeInput_payload_rsp_inst <= IBusSimplePlugin_injectionPort_payload;
    end
  end

  always @(posedge debugCdCtrl_external_clk) begin
    DebugPlugin_firstCycle <= 1'b0;
    if(debug_bus_cmd_ready) begin
      DebugPlugin_firstCycle <= 1'b1;
    end
    DebugPlugin_secondCycle <= DebugPlugin_firstCycle;
    DebugPlugin_isPipBusy <= (({execute_arbitration_isValid,decode_arbitration_isValid} != 2'b00) || IBusSimplePlugin_incomingInstruction);
    if(execute_arbitration_isValid) begin
      DebugPlugin_busReadDataReg <= _zz_lastStageRegFileWrite_payload_data;
    end
    _zz_when_DebugPlugin_l244 <= debug_bus_cmd_payload_address[2];
    if(when_DebugPlugin_l295) begin
      DebugPlugin_busReadDataReg <= execute_PC;
    end
    DebugPlugin_resetIt_regNext <= DebugPlugin_resetIt;
  end

  always @(posedge debugCdCtrl_external_clk) begin
    if(debugCdCtrl_logic_outputReset) begin
      DebugPlugin_resetIt <= 1'b0;
      DebugPlugin_haltIt <= 1'b0;
      DebugPlugin_stepIt <= 1'b0;
      DebugPlugin_godmode <= 1'b0;
      DebugPlugin_haltedByBreak <= 1'b0;
      DebugPlugin_debugUsed <= 1'b0;
      DebugPlugin_disableEbreak <= 1'b0;
    end else begin
      if(when_DebugPlugin_l225) begin
        DebugPlugin_godmode <= 1'b1;
      end
      if(debug_bus_cmd_valid) begin
        DebugPlugin_debugUsed <= 1'b1;
      end
      if(debug_bus_cmd_valid) begin
        case(switch_DebugPlugin_l267)
          6'h0 : begin
            if(debug_bus_cmd_payload_wr) begin
              DebugPlugin_stepIt <= debug_bus_cmd_payload_data[4];
              if(when_DebugPlugin_l271) begin
                DebugPlugin_resetIt <= 1'b1;
              end
              if(when_DebugPlugin_l271_1) begin
                DebugPlugin_resetIt <= 1'b0;
              end
              if(when_DebugPlugin_l272) begin
                DebugPlugin_haltIt <= 1'b1;
              end
              if(when_DebugPlugin_l272_1) begin
                DebugPlugin_haltIt <= 1'b0;
              end
              if(when_DebugPlugin_l273) begin
                DebugPlugin_haltedByBreak <= 1'b0;
              end
              if(when_DebugPlugin_l274) begin
                DebugPlugin_godmode <= 1'b0;
              end
              if(when_DebugPlugin_l275) begin
                DebugPlugin_disableEbreak <= 1'b1;
              end
              if(when_DebugPlugin_l275_1) begin
                DebugPlugin_disableEbreak <= 1'b0;
              end
            end
          end
          default : begin
          end
        endcase
      end
      if(when_DebugPlugin_l295) begin
        if(when_DebugPlugin_l298) begin
          DebugPlugin_haltIt <= 1'b1;
          DebugPlugin_haltedByBreak <= 1'b1;
        end
      end
      if(when_DebugPlugin_l311) begin
        if(decode_arbitration_isValid) begin
          DebugPlugin_haltIt <= 1'b1;
        end
      end
    end
  end


endmodule

module BufferCC_3 (
  input               io_dataIn,
  output              io_dataOut,
  input               debugCdCtrl_external_clk,
  input               debugCdCtrl_logic_outputReset
);

  (* async_reg = "true" *) reg                 buffers_0;
  (* async_reg = "true" *) reg                 buffers_1;

  assign io_dataOut = buffers_1;
  always @(posedge debugCdCtrl_external_clk or posedge debugCdCtrl_logic_outputReset) begin
    if(debugCdCtrl_logic_outputReset) begin
      buffers_0 <= 1'b1;
      buffers_1 <= 1'b1;
    end else begin
      buffers_0 <= io_dataIn;
      buffers_1 <= buffers_0;
    end
  end


endmodule

module BufferCC_2 (
  input               io_dataIn,
  output              io_dataOut,
  input               debugCdCtrl_external_clk,
  input               debugCdCtrl_external_reset
);

  (* async_reg = "true" *) reg                 buffers_0;
  (* async_reg = "true" *) reg                 buffers_1;

  assign io_dataOut = buffers_1;
  always @(posedge debugCdCtrl_external_clk or negedge debugCdCtrl_external_reset) begin
    if(!debugCdCtrl_external_reset) begin
      buffers_0 <= 1'b1;
      buffers_1 <= 1'b1;
    end else begin
      buffers_0 <= io_dataIn;
      buffers_1 <= buffers_0;
    end
  end


endmodule

//StreamFifo replaced by StreamFifo

module StreamFifo (
  input               io_push_valid,
  output reg          io_push_ready,
  input      [7:0]    io_push_payload,
  output              io_pop_valid,
  input               io_pop_ready,
  output     [7:0]    io_pop_payload,
  input               io_flush,
  output     [0:0]    io_occupancy,
  output     [0:0]    io_availability,
  input               debugCdCtrl_external_clk,
  input               systemCdCtrl_logic_outputReset
);

  wire                io_push_m2sPipe_valid;
  wire                io_push_m2sPipe_ready;
  wire       [7:0]    io_push_m2sPipe_payload;
  reg                 io_push_rValid;
  reg        [7:0]    io_push_rData;
  wire                when_Stream_l368;

  always @(*) begin
    io_push_ready = io_push_m2sPipe_ready;
    if(when_Stream_l368) begin
      io_push_ready = 1'b1;
    end
  end

  assign when_Stream_l368 = (! io_push_m2sPipe_valid);
  assign io_push_m2sPipe_valid = io_push_rValid;
  assign io_push_m2sPipe_payload = io_push_rData;
  assign io_pop_valid = io_push_m2sPipe_valid;
  assign io_push_m2sPipe_ready = io_pop_ready;
  assign io_pop_payload = io_push_m2sPipe_payload;
  assign io_occupancy = io_pop_valid;
  assign io_availability = (! io_pop_valid);
  always @(posedge debugCdCtrl_external_clk) begin
    if(systemCdCtrl_logic_outputReset) begin
      io_push_rValid <= 1'b0;
    end else begin
      if(io_push_ready) begin
        io_push_rValid <= io_push_valid;
      end
      if(io_flush) begin
        io_push_rValid <= 1'b0;
      end
    end
  end

  always @(posedge debugCdCtrl_external_clk) begin
    if(io_push_ready) begin
      io_push_rData <= io_push_payload;
    end
  end


endmodule

module UartCtrl (
  input      [2:0]    io_config_frame_dataLength,
  input      [0:0]    io_config_frame_stop,
  input      [1:0]    io_config_frame_parity,
  input      [11:0]   io_config_clockDivider,
  input               io_write_valid,
  output reg          io_write_ready,
  input      [7:0]    io_write_payload,
  output              io_read_valid,
  input               io_read_ready,
  output     [7:0]    io_read_payload,
  output              io_uart_txd,
  input               io_uart_rxd,
  output              io_readError,
  input               io_writeBreak,
  output              io_readBreak,
  input               debugCdCtrl_external_clk,
  input               systemCdCtrl_logic_outputReset
);
  localparam UartStopType_ONE = 1'd0;
  localparam UartStopType_TWO = 1'd1;
  localparam UartParityType_NONE = 2'd0;
  localparam UartParityType_EVEN = 2'd1;
  localparam UartParityType_ODD = 2'd2;

  wire                tx_io_write_ready;
  wire                tx_io_txd;
  wire                rx_io_read_valid;
  wire       [7:0]    rx_io_read_payload;
  wire                rx_io_rts;
  wire                rx_io_error;
  wire                rx_io_break;
  reg        [11:0]   clockDivider_counter;
  wire                clockDivider_tick;
  reg                 clockDivider_tickReg;
  reg                 io_write_thrown_valid;
  wire                io_write_thrown_ready;
  wire       [7:0]    io_write_thrown_payload;
  `ifndef SYNTHESIS
  reg [23:0] io_config_frame_stop_string;
  reg [31:0] io_config_frame_parity_string;
  `endif


  UartCtrlTx tx (
    .io_configFrame_dataLength      (io_config_frame_dataLength[2:0]), //i
    .io_configFrame_stop            (io_config_frame_stop           ), //i
    .io_configFrame_parity          (io_config_frame_parity[1:0]    ), //i
    .io_samplingTick                (clockDivider_tickReg           ), //i
    .io_write_valid                 (io_write_thrown_valid          ), //i
    .io_write_ready                 (tx_io_write_ready              ), //o
    .io_write_payload               (io_write_thrown_payload[7:0]   ), //i
    .io_cts                         (1'b0                           ), //i
    .io_txd                         (tx_io_txd                      ), //o
    .io_break                       (io_writeBreak                  ), //i
    .debugCdCtrl_external_clk       (debugCdCtrl_external_clk       ), //i
    .systemCdCtrl_logic_outputReset (systemCdCtrl_logic_outputReset )  //i
  );
  UartCtrlRx rx (
    .io_configFrame_dataLength      (io_config_frame_dataLength[2:0]), //i
    .io_configFrame_stop            (io_config_frame_stop           ), //i
    .io_configFrame_parity          (io_config_frame_parity[1:0]    ), //i
    .io_samplingTick                (clockDivider_tickReg           ), //i
    .io_read_valid                  (rx_io_read_valid               ), //o
    .io_read_ready                  (io_read_ready                  ), //i
    .io_read_payload                (rx_io_read_payload[7:0]        ), //o
    .io_rxd                         (io_uart_rxd                    ), //i
    .io_rts                         (rx_io_rts                      ), //o
    .io_error                       (rx_io_error                    ), //o
    .io_break                       (rx_io_break                    ), //o
    .debugCdCtrl_external_clk       (debugCdCtrl_external_clk       ), //i
    .systemCdCtrl_logic_outputReset (systemCdCtrl_logic_outputReset )  //i
  );
  `ifndef SYNTHESIS
  always @(*) begin
    case(io_config_frame_stop)
      UartStopType_ONE : io_config_frame_stop_string = "ONE";
      UartStopType_TWO : io_config_frame_stop_string = "TWO";
      default : io_config_frame_stop_string = "???";
    endcase
  end
  always @(*) begin
    case(io_config_frame_parity)
      UartParityType_NONE : io_config_frame_parity_string = "NONE";
      UartParityType_EVEN : io_config_frame_parity_string = "EVEN";
      UartParityType_ODD : io_config_frame_parity_string = "ODD ";
      default : io_config_frame_parity_string = "????";
    endcase
  end
  `endif

  assign clockDivider_tick = (clockDivider_counter == 12'h0);
  always @(*) begin
    io_write_thrown_valid = io_write_valid;
    if(rx_io_break) begin
      io_write_thrown_valid = 1'b0;
    end
  end

  always @(*) begin
    io_write_ready = io_write_thrown_ready;
    if(rx_io_break) begin
      io_write_ready = 1'b1;
    end
  end

  assign io_write_thrown_payload = io_write_payload;
  assign io_write_thrown_ready = tx_io_write_ready;
  assign io_read_valid = rx_io_read_valid;
  assign io_read_payload = rx_io_read_payload;
  assign io_uart_txd = tx_io_txd;
  assign io_readError = rx_io_error;
  assign io_readBreak = rx_io_break;
  always @(posedge debugCdCtrl_external_clk) begin
    if(systemCdCtrl_logic_outputReset) begin
      clockDivider_counter <= 12'h0;
      clockDivider_tickReg <= 1'b0;
    end else begin
      clockDivider_tickReg <= clockDivider_tick;
      clockDivider_counter <= (clockDivider_counter - 12'h001);
      if(clockDivider_tick) begin
        clockDivider_counter <= io_config_clockDivider;
      end
    end
  end


endmodule

module StreamArbiter (
  input               io_inputs_0_valid,
  output              io_inputs_0_ready,
  input               io_inputs_0_payload_last,
  input      [0:0]    io_inputs_0_payload_fragment_source,
  input      [0:0]    io_inputs_0_payload_fragment_opcode,
  input      [14:0]   io_inputs_0_payload_fragment_address,
  input      [1:0]    io_inputs_0_payload_fragment_length,
  input      [31:0]   io_inputs_0_payload_fragment_data,
  input      [3:0]    io_inputs_0_payload_fragment_mask,
  input      [0:0]    io_inputs_0_payload_fragment_context,
  input               io_inputs_1_valid,
  output              io_inputs_1_ready,
  input               io_inputs_1_payload_last,
  input      [0:0]    io_inputs_1_payload_fragment_source,
  input      [0:0]    io_inputs_1_payload_fragment_opcode,
  input      [14:0]   io_inputs_1_payload_fragment_address,
  input      [1:0]    io_inputs_1_payload_fragment_length,
  input      [31:0]   io_inputs_1_payload_fragment_data,
  input      [3:0]    io_inputs_1_payload_fragment_mask,
  input      [0:0]    io_inputs_1_payload_fragment_context,
  output              io_output_valid,
  input               io_output_ready,
  output              io_output_payload_last,
  output     [0:0]    io_output_payload_fragment_source,
  output     [0:0]    io_output_payload_fragment_opcode,
  output     [14:0]   io_output_payload_fragment_address,
  output     [1:0]    io_output_payload_fragment_length,
  output     [31:0]   io_output_payload_fragment_data,
  output     [3:0]    io_output_payload_fragment_mask,
  output     [0:0]    io_output_payload_fragment_context,
  output     [0:0]    io_chosen,
  output     [1:0]    io_chosenOH,
  input               debugCdCtrl_external_clk,
  input               systemCdCtrl_logic_outputReset
);

  wire       [1:0]    _zz_maskProposal_1_1;
  wire       [1:0]    _zz_maskProposal_1_2;
  reg                 locked;
  wire                maskProposal_0;
  wire                maskProposal_1;
  reg                 maskLocked_0;
  reg                 maskLocked_1;
  wire                maskRouted_0;
  wire                maskRouted_1;
  wire       [1:0]    _zz_maskProposal_1;
  wire                io_output_fire;
  wire                when_Stream_l621;
  wire                _zz_io_chosen;

  assign _zz_maskProposal_1_1 = (_zz_maskProposal_1 & (~ _zz_maskProposal_1_2));
  assign _zz_maskProposal_1_2 = (_zz_maskProposal_1 - 2'b01);
  assign maskRouted_0 = (locked ? maskLocked_0 : maskProposal_0);
  assign maskRouted_1 = (locked ? maskLocked_1 : maskProposal_1);
  assign _zz_maskProposal_1 = {io_inputs_1_valid,io_inputs_0_valid};
  assign maskProposal_0 = io_inputs_0_valid;
  assign maskProposal_1 = _zz_maskProposal_1_1[1];
  assign io_output_fire = (io_output_valid && io_output_ready);
  assign when_Stream_l621 = (io_output_fire && io_output_payload_last);
  assign io_output_valid = ((io_inputs_0_valid && maskRouted_0) || (io_inputs_1_valid && maskRouted_1));
  assign io_output_payload_last = (maskRouted_0 ? io_inputs_0_payload_last : io_inputs_1_payload_last);
  assign io_output_payload_fragment_source = (maskRouted_0 ? io_inputs_0_payload_fragment_source : io_inputs_1_payload_fragment_source);
  assign io_output_payload_fragment_opcode = (maskRouted_0 ? io_inputs_0_payload_fragment_opcode : io_inputs_1_payload_fragment_opcode);
  assign io_output_payload_fragment_address = (maskRouted_0 ? io_inputs_0_payload_fragment_address : io_inputs_1_payload_fragment_address);
  assign io_output_payload_fragment_length = (maskRouted_0 ? io_inputs_0_payload_fragment_length : io_inputs_1_payload_fragment_length);
  assign io_output_payload_fragment_data = (maskRouted_0 ? io_inputs_0_payload_fragment_data : io_inputs_1_payload_fragment_data);
  assign io_output_payload_fragment_mask = (maskRouted_0 ? io_inputs_0_payload_fragment_mask : io_inputs_1_payload_fragment_mask);
  assign io_output_payload_fragment_context = (maskRouted_0 ? io_inputs_0_payload_fragment_context : io_inputs_1_payload_fragment_context);
  assign io_inputs_0_ready = (maskRouted_0 && io_output_ready);
  assign io_inputs_1_ready = (maskRouted_1 && io_output_ready);
  assign io_chosenOH = {maskRouted_1,maskRouted_0};
  assign _zz_io_chosen = io_chosenOH[1];
  assign io_chosen = _zz_io_chosen;
  always @(posedge debugCdCtrl_external_clk) begin
    if(systemCdCtrl_logic_outputReset) begin
      locked <= 1'b0;
    end else begin
      if(io_output_valid) begin
        locked <= 1'b1;
      end
      if(when_Stream_l621) begin
        locked <= 1'b0;
      end
    end
  end

  always @(posedge debugCdCtrl_external_clk) begin
    if(io_output_valid) begin
      maskLocked_0 <= maskRouted_0;
      maskLocked_1 <= maskRouted_1;
    end
  end


endmodule

module FlowCCByToggle (
  input               io_input_valid,
  input               io_input_payload_last,
  input      [0:0]    io_input_payload_fragment,
  output              io_output_valid,
  output              io_output_payload_last,
  output     [0:0]    io_output_payload_fragment,
  input               io_jtag_tck,
  input               debugCdCtrl_external_clk,
  input               debugCdCtrl_logic_outputReset
);

  wire                inputArea_target_buffercc_io_dataOut;
  reg                 inputArea_target;
  reg                 inputArea_data_last;
  reg        [0:0]    inputArea_data_fragment;
  wire                outputArea_target;
  reg                 outputArea_hit;
  wire                outputArea_flow_valid;
  wire                outputArea_flow_payload_last;
  wire       [0:0]    outputArea_flow_payload_fragment;
  reg                 outputArea_flow_m2sPipe_valid;
  reg                 outputArea_flow_m2sPipe_payload_last;
  reg        [0:0]    outputArea_flow_m2sPipe_payload_fragment;

  BufferCC_1 inputArea_target_buffercc (
    .io_dataIn                     (inputArea_target                    ), //i
    .io_dataOut                    (inputArea_target_buffercc_io_dataOut), //o
    .debugCdCtrl_external_clk      (debugCdCtrl_external_clk            ), //i
    .debugCdCtrl_logic_outputReset (debugCdCtrl_logic_outputReset       )  //i
  );
  initial begin
  `ifndef SYNTHESIS
    inputArea_target = $urandom;
    outputArea_hit = $urandom;
  `endif
  end

  assign outputArea_target = inputArea_target_buffercc_io_dataOut;
  assign outputArea_flow_valid = (outputArea_target != outputArea_hit);
  assign outputArea_flow_payload_last = inputArea_data_last;
  assign outputArea_flow_payload_fragment = inputArea_data_fragment;
  assign io_output_valid = outputArea_flow_m2sPipe_valid;
  assign io_output_payload_last = outputArea_flow_m2sPipe_payload_last;
  assign io_output_payload_fragment = outputArea_flow_m2sPipe_payload_fragment;
  always @(posedge io_jtag_tck) begin
    if(io_input_valid) begin
      inputArea_target <= (! inputArea_target);
      inputArea_data_last <= io_input_payload_last;
      inputArea_data_fragment <= io_input_payload_fragment;
    end
  end

  always @(posedge debugCdCtrl_external_clk) begin
    outputArea_hit <= outputArea_target;
    if(outputArea_flow_valid) begin
      outputArea_flow_m2sPipe_payload_last <= outputArea_flow_payload_last;
      outputArea_flow_m2sPipe_payload_fragment <= outputArea_flow_payload_fragment;
    end
  end

  always @(posedge debugCdCtrl_external_clk) begin
    if(debugCdCtrl_logic_outputReset) begin
      outputArea_flow_m2sPipe_valid <= 1'b0;
    end else begin
      outputArea_flow_m2sPipe_valid <= outputArea_flow_valid;
    end
  end


endmodule

module StreamFifoLowLatency (
  input               io_push_valid,
  output              io_push_ready,
  input               io_push_payload_error,
  input      [31:0]   io_push_payload_inst,
  output reg          io_pop_valid,
  input               io_pop_ready,
  output reg          io_pop_payload_error,
  output reg [31:0]   io_pop_payload_inst,
  input               io_flush,
  output     [0:0]    io_occupancy,
  input               debugCdCtrl_external_clk,
  input               systemCdCtrl_logic_outputReset
);

  reg                 when_Phase_l623;
  reg                 pushPtr_willIncrement;
  reg                 pushPtr_willClear;
  wire                pushPtr_willOverflowIfInc;
  wire                pushPtr_willOverflow;
  reg                 popPtr_willIncrement;
  reg                 popPtr_willClear;
  wire                popPtr_willOverflowIfInc;
  wire                popPtr_willOverflow;
  wire                ptrMatch;
  reg                 risingOccupancy;
  wire                empty;
  wire                full;
  wire                pushing;
  wire                popping;
  wire                readed_error;
  wire       [31:0]   readed_inst;
  wire       [32:0]   _zz_readed_error;
  wire                when_Stream_l1137;
  wire                when_Stream_l1150;
  wire       [32:0]   _zz_readed_error_1;
  reg        [32:0]   _zz_readed_error_2;

  always @(*) begin
    when_Phase_l623 = 1'b0;
    if(pushing) begin
      when_Phase_l623 = 1'b1;
    end
  end

  always @(*) begin
    pushPtr_willIncrement = 1'b0;
    if(pushing) begin
      pushPtr_willIncrement = 1'b1;
    end
  end

  always @(*) begin
    pushPtr_willClear = 1'b0;
    if(io_flush) begin
      pushPtr_willClear = 1'b1;
    end
  end

  assign pushPtr_willOverflowIfInc = 1'b1;
  assign pushPtr_willOverflow = (pushPtr_willOverflowIfInc && pushPtr_willIncrement);
  always @(*) begin
    popPtr_willIncrement = 1'b0;
    if(popping) begin
      popPtr_willIncrement = 1'b1;
    end
  end

  always @(*) begin
    popPtr_willClear = 1'b0;
    if(io_flush) begin
      popPtr_willClear = 1'b1;
    end
  end

  assign popPtr_willOverflowIfInc = 1'b1;
  assign popPtr_willOverflow = (popPtr_willOverflowIfInc && popPtr_willIncrement);
  assign ptrMatch = 1'b1;
  assign empty = (ptrMatch && (! risingOccupancy));
  assign full = (ptrMatch && risingOccupancy);
  assign pushing = (io_push_valid && io_push_ready);
  assign popping = (io_pop_valid && io_pop_ready);
  assign io_push_ready = (! full);
  assign _zz_readed_error = _zz_readed_error_1;
  assign readed_error = _zz_readed_error[0];
  assign readed_inst = _zz_readed_error[32 : 1];
  assign when_Stream_l1137 = (! empty);
  always @(*) begin
    if(when_Stream_l1137) begin
      io_pop_valid = 1'b1;
    end else begin
      io_pop_valid = io_push_valid;
    end
  end

  always @(*) begin
    if(when_Stream_l1137) begin
      io_pop_payload_error = readed_error;
    end else begin
      io_pop_payload_error = io_push_payload_error;
    end
  end

  always @(*) begin
    if(when_Stream_l1137) begin
      io_pop_payload_inst = readed_inst;
    end else begin
      io_pop_payload_inst = io_push_payload_inst;
    end
  end

  assign when_Stream_l1150 = (pushing != popping);
  assign io_occupancy = (risingOccupancy && ptrMatch);
  assign _zz_readed_error_1 = _zz_readed_error_2;
  always @(posedge debugCdCtrl_external_clk) begin
    if(systemCdCtrl_logic_outputReset) begin
      risingOccupancy <= 1'b0;
    end else begin
      if(when_Stream_l1150) begin
        risingOccupancy <= pushing;
      end
      if(io_flush) begin
        risingOccupancy <= 1'b0;
      end
    end
  end

  always @(posedge debugCdCtrl_external_clk) begin
    if(when_Phase_l623) begin
      _zz_readed_error_2 <= {io_push_payload_inst,io_push_payload_error};
    end
  end


endmodule

module UartCtrlRx (
  input      [2:0]    io_configFrame_dataLength,
  input      [0:0]    io_configFrame_stop,
  input      [1:0]    io_configFrame_parity,
  input               io_samplingTick,
  output              io_read_valid,
  input               io_read_ready,
  output     [7:0]    io_read_payload,
  input               io_rxd,
  output              io_rts,
  output reg          io_error,
  output              io_break,
  input               debugCdCtrl_external_clk,
  input               systemCdCtrl_logic_outputReset
);
  localparam UartStopType_ONE = 1'd0;
  localparam UartStopType_TWO = 1'd1;
  localparam UartParityType_NONE = 2'd0;
  localparam UartParityType_EVEN = 2'd1;
  localparam UartParityType_ODD = 2'd2;
  localparam UartCtrlRxState_IDLE = 3'd0;
  localparam UartCtrlRxState_START = 3'd1;
  localparam UartCtrlRxState_DATA = 3'd2;
  localparam UartCtrlRxState_PARITY = 3'd3;
  localparam UartCtrlRxState_STOP = 3'd4;

  wire                io_rxd_buffercc_io_dataOut;
  wire       [2:0]    _zz_when_UartCtrlRx_l139;
  wire       [0:0]    _zz_when_UartCtrlRx_l139_1;
  reg                 _zz_io_rts;
  wire                sampler_synchroniser;
  wire                sampler_samples_0;
  reg                 sampler_samples_1;
  reg                 sampler_samples_2;
  reg                 sampler_value;
  reg                 sampler_tick;
  reg        [2:0]    bitTimer_counter;
  reg                 bitTimer_tick;
  wire                when_UartCtrlRx_l43;
  reg        [2:0]    bitCounter_value;
  reg        [6:0]    break_counter;
  wire                break_valid;
  wire                when_UartCtrlRx_l69;
  reg        [2:0]    stateMachine_state;
  reg                 stateMachine_parity;
  reg        [7:0]    stateMachine_shifter;
  reg                 stateMachine_validReg;
  wire                when_UartCtrlRx_l93;
  wire                when_UartCtrlRx_l103;
  wire                when_UartCtrlRx_l111;
  wire                when_UartCtrlRx_l113;
  wire                when_UartCtrlRx_l125;
  wire                when_UartCtrlRx_l136;
  wire                when_UartCtrlRx_l139;
  `ifndef SYNTHESIS
  reg [23:0] io_configFrame_stop_string;
  reg [31:0] io_configFrame_parity_string;
  reg [47:0] stateMachine_state_string;
  `endif


  assign _zz_when_UartCtrlRx_l139_1 = ((io_configFrame_stop == UartStopType_ONE) ? 1'b0 : 1'b1);
  assign _zz_when_UartCtrlRx_l139 = {2'd0, _zz_when_UartCtrlRx_l139_1};
  BufferCC io_rxd_buffercc (
    .io_dataIn                      (io_rxd                        ), //i
    .io_dataOut                     (io_rxd_buffercc_io_dataOut    ), //o
    .debugCdCtrl_external_clk       (debugCdCtrl_external_clk      ), //i
    .systemCdCtrl_logic_outputReset (systemCdCtrl_logic_outputReset)  //i
  );
  `ifndef SYNTHESIS
  always @(*) begin
    case(io_configFrame_stop)
      UartStopType_ONE : io_configFrame_stop_string = "ONE";
      UartStopType_TWO : io_configFrame_stop_string = "TWO";
      default : io_configFrame_stop_string = "???";
    endcase
  end
  always @(*) begin
    case(io_configFrame_parity)
      UartParityType_NONE : io_configFrame_parity_string = "NONE";
      UartParityType_EVEN : io_configFrame_parity_string = "EVEN";
      UartParityType_ODD : io_configFrame_parity_string = "ODD ";
      default : io_configFrame_parity_string = "????";
    endcase
  end
  always @(*) begin
    case(stateMachine_state)
      UartCtrlRxState_IDLE : stateMachine_state_string = "IDLE  ";
      UartCtrlRxState_START : stateMachine_state_string = "START ";
      UartCtrlRxState_DATA : stateMachine_state_string = "DATA  ";
      UartCtrlRxState_PARITY : stateMachine_state_string = "PARITY";
      UartCtrlRxState_STOP : stateMachine_state_string = "STOP  ";
      default : stateMachine_state_string = "??????";
    endcase
  end
  `endif

  always @(*) begin
    io_error = 1'b0;
    case(stateMachine_state)
      UartCtrlRxState_IDLE : begin
      end
      UartCtrlRxState_START : begin
      end
      UartCtrlRxState_DATA : begin
      end
      UartCtrlRxState_PARITY : begin
        if(bitTimer_tick) begin
          if(!when_UartCtrlRx_l125) begin
            io_error = 1'b1;
          end
        end
      end
      default : begin
        if(bitTimer_tick) begin
          if(when_UartCtrlRx_l136) begin
            io_error = 1'b1;
          end
        end
      end
    endcase
  end

  assign io_rts = _zz_io_rts;
  assign sampler_synchroniser = io_rxd_buffercc_io_dataOut;
  assign sampler_samples_0 = sampler_synchroniser;
  always @(*) begin
    bitTimer_tick = 1'b0;
    if(sampler_tick) begin
      if(when_UartCtrlRx_l43) begin
        bitTimer_tick = 1'b1;
      end
    end
  end

  assign when_UartCtrlRx_l43 = (bitTimer_counter == 3'b000);
  assign break_valid = (break_counter == 7'h41);
  assign when_UartCtrlRx_l69 = (io_samplingTick && (! break_valid));
  assign io_break = break_valid;
  assign io_read_valid = stateMachine_validReg;
  assign when_UartCtrlRx_l93 = ((sampler_tick && (! sampler_value)) && (! break_valid));
  assign when_UartCtrlRx_l103 = (sampler_value == 1'b1);
  assign when_UartCtrlRx_l111 = (bitCounter_value == io_configFrame_dataLength);
  assign when_UartCtrlRx_l113 = (io_configFrame_parity == UartParityType_NONE);
  assign when_UartCtrlRx_l125 = (stateMachine_parity == sampler_value);
  assign when_UartCtrlRx_l136 = (! sampler_value);
  assign when_UartCtrlRx_l139 = (bitCounter_value == _zz_when_UartCtrlRx_l139);
  assign io_read_payload = stateMachine_shifter;
  always @(posedge debugCdCtrl_external_clk) begin
    if(systemCdCtrl_logic_outputReset) begin
      _zz_io_rts <= 1'b0;
      sampler_samples_1 <= 1'b1;
      sampler_samples_2 <= 1'b1;
      sampler_value <= 1'b1;
      sampler_tick <= 1'b0;
      break_counter <= 7'h0;
      stateMachine_state <= UartCtrlRxState_IDLE;
      stateMachine_validReg <= 1'b0;
    end else begin
      _zz_io_rts <= (! io_read_ready);
      if(io_samplingTick) begin
        sampler_samples_1 <= sampler_samples_0;
      end
      if(io_samplingTick) begin
        sampler_samples_2 <= sampler_samples_1;
      end
      sampler_value <= (((1'b0 || ((1'b1 && sampler_samples_0) && sampler_samples_1)) || ((1'b1 && sampler_samples_0) && sampler_samples_2)) || ((1'b1 && sampler_samples_1) && sampler_samples_2));
      sampler_tick <= io_samplingTick;
      if(sampler_value) begin
        break_counter <= 7'h0;
      end else begin
        if(when_UartCtrlRx_l69) begin
          break_counter <= (break_counter + 7'h01);
        end
      end
      stateMachine_validReg <= 1'b0;
      case(stateMachine_state)
        UartCtrlRxState_IDLE : begin
          if(when_UartCtrlRx_l93) begin
            stateMachine_state <= UartCtrlRxState_START;
          end
        end
        UartCtrlRxState_START : begin
          if(bitTimer_tick) begin
            stateMachine_state <= UartCtrlRxState_DATA;
            if(when_UartCtrlRx_l103) begin
              stateMachine_state <= UartCtrlRxState_IDLE;
            end
          end
        end
        UartCtrlRxState_DATA : begin
          if(bitTimer_tick) begin
            if(when_UartCtrlRx_l111) begin
              if(when_UartCtrlRx_l113) begin
                stateMachine_state <= UartCtrlRxState_STOP;
                stateMachine_validReg <= 1'b1;
              end else begin
                stateMachine_state <= UartCtrlRxState_PARITY;
              end
            end
          end
        end
        UartCtrlRxState_PARITY : begin
          if(bitTimer_tick) begin
            if(when_UartCtrlRx_l125) begin
              stateMachine_state <= UartCtrlRxState_STOP;
              stateMachine_validReg <= 1'b1;
            end else begin
              stateMachine_state <= UartCtrlRxState_IDLE;
            end
          end
        end
        default : begin
          if(bitTimer_tick) begin
            if(when_UartCtrlRx_l136) begin
              stateMachine_state <= UartCtrlRxState_IDLE;
            end else begin
              if(when_UartCtrlRx_l139) begin
                stateMachine_state <= UartCtrlRxState_IDLE;
              end
            end
          end
        end
      endcase
    end
  end

  always @(posedge debugCdCtrl_external_clk) begin
    if(sampler_tick) begin
      bitTimer_counter <= (bitTimer_counter - 3'b001);
      if(when_UartCtrlRx_l43) begin
        bitTimer_counter <= 3'b100;
      end
    end
    if(bitTimer_tick) begin
      bitCounter_value <= (bitCounter_value + 3'b001);
    end
    if(bitTimer_tick) begin
      stateMachine_parity <= (stateMachine_parity ^ sampler_value);
    end
    case(stateMachine_state)
      UartCtrlRxState_IDLE : begin
        if(when_UartCtrlRx_l93) begin
          bitTimer_counter <= 3'b001;
        end
      end
      UartCtrlRxState_START : begin
        if(bitTimer_tick) begin
          bitCounter_value <= 3'b000;
          stateMachine_parity <= (io_configFrame_parity == UartParityType_ODD);
        end
      end
      UartCtrlRxState_DATA : begin
        if(bitTimer_tick) begin
          stateMachine_shifter[bitCounter_value] <= sampler_value;
          if(when_UartCtrlRx_l111) begin
            bitCounter_value <= 3'b000;
          end
        end
      end
      UartCtrlRxState_PARITY : begin
        if(bitTimer_tick) begin
          bitCounter_value <= 3'b000;
        end
      end
      default : begin
      end
    endcase
  end


endmodule

module UartCtrlTx (
  input      [2:0]    io_configFrame_dataLength,
  input      [0:0]    io_configFrame_stop,
  input      [1:0]    io_configFrame_parity,
  input               io_samplingTick,
  input               io_write_valid,
  output reg          io_write_ready,
  input      [7:0]    io_write_payload,
  input               io_cts,
  output              io_txd,
  input               io_break,
  input               debugCdCtrl_external_clk,
  input               systemCdCtrl_logic_outputReset
);
  localparam UartStopType_ONE = 1'd0;
  localparam UartStopType_TWO = 1'd1;
  localparam UartParityType_NONE = 2'd0;
  localparam UartParityType_EVEN = 2'd1;
  localparam UartParityType_ODD = 2'd2;
  localparam UartCtrlTxState_IDLE = 3'd0;
  localparam UartCtrlTxState_START = 3'd1;
  localparam UartCtrlTxState_DATA = 3'd2;
  localparam UartCtrlTxState_PARITY = 3'd3;
  localparam UartCtrlTxState_STOP = 3'd4;

  wire       [2:0]    _zz_clockDivider_counter_valueNext;
  wire       [0:0]    _zz_clockDivider_counter_valueNext_1;
  wire       [2:0]    _zz_when_UartCtrlTx_l93;
  wire       [0:0]    _zz_when_UartCtrlTx_l93_1;
  reg                 clockDivider_counter_willIncrement;
  wire                clockDivider_counter_willClear;
  reg        [2:0]    clockDivider_counter_valueNext;
  reg        [2:0]    clockDivider_counter_value;
  wire                clockDivider_counter_willOverflowIfInc;
  wire                clockDivider_counter_willOverflow;
  reg        [2:0]    tickCounter_value;
  reg        [2:0]    stateMachine_state;
  reg                 stateMachine_parity;
  reg                 stateMachine_txd;
  wire                when_UartCtrlTx_l58;
  wire                when_UartCtrlTx_l73;
  wire                when_UartCtrlTx_l76;
  wire                when_UartCtrlTx_l93;
  reg                 _zz_io_txd;
  `ifndef SYNTHESIS
  reg [23:0] io_configFrame_stop_string;
  reg [31:0] io_configFrame_parity_string;
  reg [47:0] stateMachine_state_string;
  `endif


  assign _zz_clockDivider_counter_valueNext_1 = clockDivider_counter_willIncrement;
  assign _zz_clockDivider_counter_valueNext = {2'd0, _zz_clockDivider_counter_valueNext_1};
  assign _zz_when_UartCtrlTx_l93_1 = ((io_configFrame_stop == UartStopType_ONE) ? 1'b0 : 1'b1);
  assign _zz_when_UartCtrlTx_l93 = {2'd0, _zz_when_UartCtrlTx_l93_1};
  `ifndef SYNTHESIS
  always @(*) begin
    case(io_configFrame_stop)
      UartStopType_ONE : io_configFrame_stop_string = "ONE";
      UartStopType_TWO : io_configFrame_stop_string = "TWO";
      default : io_configFrame_stop_string = "???";
    endcase
  end
  always @(*) begin
    case(io_configFrame_parity)
      UartParityType_NONE : io_configFrame_parity_string = "NONE";
      UartParityType_EVEN : io_configFrame_parity_string = "EVEN";
      UartParityType_ODD : io_configFrame_parity_string = "ODD ";
      default : io_configFrame_parity_string = "????";
    endcase
  end
  always @(*) begin
    case(stateMachine_state)
      UartCtrlTxState_IDLE : stateMachine_state_string = "IDLE  ";
      UartCtrlTxState_START : stateMachine_state_string = "START ";
      UartCtrlTxState_DATA : stateMachine_state_string = "DATA  ";
      UartCtrlTxState_PARITY : stateMachine_state_string = "PARITY";
      UartCtrlTxState_STOP : stateMachine_state_string = "STOP  ";
      default : stateMachine_state_string = "??????";
    endcase
  end
  `endif

  always @(*) begin
    clockDivider_counter_willIncrement = 1'b0;
    if(io_samplingTick) begin
      clockDivider_counter_willIncrement = 1'b1;
    end
  end

  assign clockDivider_counter_willClear = 1'b0;
  assign clockDivider_counter_willOverflowIfInc = (clockDivider_counter_value == 3'b100);
  assign clockDivider_counter_willOverflow = (clockDivider_counter_willOverflowIfInc && clockDivider_counter_willIncrement);
  always @(*) begin
    if(clockDivider_counter_willOverflow) begin
      clockDivider_counter_valueNext = 3'b000;
    end else begin
      clockDivider_counter_valueNext = (clockDivider_counter_value + _zz_clockDivider_counter_valueNext);
    end
    if(clockDivider_counter_willClear) begin
      clockDivider_counter_valueNext = 3'b000;
    end
  end

  always @(*) begin
    stateMachine_txd = 1'b1;
    case(stateMachine_state)
      UartCtrlTxState_IDLE : begin
      end
      UartCtrlTxState_START : begin
        stateMachine_txd = 1'b0;
      end
      UartCtrlTxState_DATA : begin
        stateMachine_txd = io_write_payload[tickCounter_value];
      end
      UartCtrlTxState_PARITY : begin
        stateMachine_txd = stateMachine_parity;
      end
      default : begin
      end
    endcase
  end

  always @(*) begin
    io_write_ready = io_break;
    case(stateMachine_state)
      UartCtrlTxState_IDLE : begin
      end
      UartCtrlTxState_START : begin
      end
      UartCtrlTxState_DATA : begin
        if(clockDivider_counter_willOverflow) begin
          if(when_UartCtrlTx_l73) begin
            io_write_ready = 1'b1;
          end
        end
      end
      UartCtrlTxState_PARITY : begin
      end
      default : begin
      end
    endcase
  end

  assign when_UartCtrlTx_l58 = ((io_write_valid && (! io_cts)) && clockDivider_counter_willOverflow);
  assign when_UartCtrlTx_l73 = (tickCounter_value == io_configFrame_dataLength);
  assign when_UartCtrlTx_l76 = (io_configFrame_parity == UartParityType_NONE);
  assign when_UartCtrlTx_l93 = (tickCounter_value == _zz_when_UartCtrlTx_l93);
  assign io_txd = _zz_io_txd;
  always @(posedge debugCdCtrl_external_clk) begin
    if(systemCdCtrl_logic_outputReset) begin
      clockDivider_counter_value <= 3'b000;
      stateMachine_state <= UartCtrlTxState_IDLE;
      _zz_io_txd <= 1'b1;
    end else begin
      clockDivider_counter_value <= clockDivider_counter_valueNext;
      case(stateMachine_state)
        UartCtrlTxState_IDLE : begin
          if(when_UartCtrlTx_l58) begin
            stateMachine_state <= UartCtrlTxState_START;
          end
        end
        UartCtrlTxState_START : begin
          if(clockDivider_counter_willOverflow) begin
            stateMachine_state <= UartCtrlTxState_DATA;
          end
        end
        UartCtrlTxState_DATA : begin
          if(clockDivider_counter_willOverflow) begin
            if(when_UartCtrlTx_l73) begin
              if(when_UartCtrlTx_l76) begin
                stateMachine_state <= UartCtrlTxState_STOP;
              end else begin
                stateMachine_state <= UartCtrlTxState_PARITY;
              end
            end
          end
        end
        UartCtrlTxState_PARITY : begin
          if(clockDivider_counter_willOverflow) begin
            stateMachine_state <= UartCtrlTxState_STOP;
          end
        end
        default : begin
          if(clockDivider_counter_willOverflow) begin
            if(when_UartCtrlTx_l93) begin
              stateMachine_state <= (io_write_valid ? UartCtrlTxState_START : UartCtrlTxState_IDLE);
            end
          end
        end
      endcase
      _zz_io_txd <= (stateMachine_txd && (! io_break));
    end
  end

  always @(posedge debugCdCtrl_external_clk) begin
    if(clockDivider_counter_willOverflow) begin
      tickCounter_value <= (tickCounter_value + 3'b001);
    end
    if(clockDivider_counter_willOverflow) begin
      stateMachine_parity <= (stateMachine_parity ^ stateMachine_txd);
    end
    case(stateMachine_state)
      UartCtrlTxState_IDLE : begin
      end
      UartCtrlTxState_START : begin
        if(clockDivider_counter_willOverflow) begin
          stateMachine_parity <= (io_configFrame_parity == UartParityType_ODD);
          tickCounter_value <= 3'b000;
        end
      end
      UartCtrlTxState_DATA : begin
        if(clockDivider_counter_willOverflow) begin
          if(when_UartCtrlTx_l73) begin
            tickCounter_value <= 3'b000;
          end
        end
      end
      UartCtrlTxState_PARITY : begin
        if(clockDivider_counter_willOverflow) begin
          tickCounter_value <= 3'b000;
        end
      end
      default : begin
      end
    endcase
  end


endmodule

module BufferCC_1 (
  input               io_dataIn,
  output              io_dataOut,
  input               debugCdCtrl_external_clk,
  input               debugCdCtrl_logic_outputReset
);

  (* async_reg = "true" *) reg                 buffers_0;
  (* async_reg = "true" *) reg                 buffers_1;

  initial begin
  `ifndef SYNTHESIS
    buffers_0 = $urandom;
    buffers_1 = $urandom;
  `endif
  end

  assign io_dataOut = buffers_1;
  always @(posedge debugCdCtrl_external_clk) begin
    buffers_0 <= io_dataIn;
    buffers_1 <= buffers_0;
  end


endmodule

module BufferCC (
  input               io_dataIn,
  output              io_dataOut,
  input               debugCdCtrl_external_clk,
  input               systemCdCtrl_logic_outputReset
);

  (* async_reg = "true" *) reg                 buffers_0;
  (* async_reg = "true" *) reg                 buffers_1;

  assign io_dataOut = buffers_1;
  always @(posedge debugCdCtrl_external_clk) begin
    if(systemCdCtrl_logic_outputReset) begin
      buffers_0 <= 1'b0;
      buffers_1 <= 1'b0;
    end else begin
      buffers_0 <= io_dataIn;
      buffers_1 <= buffers_0;
    end
  end


endmodule
