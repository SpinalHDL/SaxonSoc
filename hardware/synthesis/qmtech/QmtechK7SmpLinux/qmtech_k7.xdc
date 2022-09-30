set_property CFGBVS VCCO [current_design]
set_property CONFIG_VOLTAGE 3.3 [current_design]
set_property BITSTREAM.CONFIG.SPI_BUSWIDTH 4 [current_design]

create_clock -period 20.000 -name clocking_GCLK50 [get_nets clocking_GCLK50]

set_clock_groups -asynchronous -group clocking_pll_CLKOUT0 -group clocking_pll_CLKOUT1
set_clock_groups -asynchronous -group clocking_pll_CLKOUT0 -group clocking_pll_CLKOUT2
set_clock_groups -asynchronous -group clocking_pll_CLKOUT0 -group clocking_pll_CLKOUT3
set_clock_groups -asynchronous -group clocking_pll_CLKOUT0 -group clocking_pll_CLKOUT4
set_clock_groups -asynchronous -group clocking_pll_CLKOUT0 -group clocking_pll_CLKOUT5
set_clock_groups -asynchronous -group clocking_pll_CLKOUT0 -group clocking_pll_CLKOUT6
set_clock_groups -asynchronous -group clocking_pll_CLKOUT0 -group clocking_pll2_CLKOUT0

set_property -dict {PACKAGE_PIN F22 IOSTANDARD LVCMOS33} [get_ports clocking_GCLK50]

# JP3 Header

## UART
set_property -dict {PACKAGE_PIN AF22 IOSTANDARD LVCMOS18} [get_ports system_uartA_uart_txd]
set_property -dict {PACKAGE_PIN AE22 IOSTANDARD LVCMOS18} [get_ports system_uartA_uart_rxd]

## User SPI
set_property -dict {PACKAGE_PIN AE23 IOSTANDARD LVCMOS18 IOB TRUE} [get_ports {system_spiA_user_data[1]}]
set_property -dict {PACKAGE_PIN AF23 IOSTANDARD LVCMOS18 IOB TRUE} [get_ports {system_spiA_user_data[0]}]
set_property -dict {PACKAGE_PIN AF24 IOSTANDARD LVCMOS18 IOB TRUE} [get_ports system_spiA_user_sclk]

## GPIO
set_property -dict {PACKAGE_PIN AE26 IOSTANDARD LVCMOS18} [get_ports {system_gpioA_gpio[18]}]
set_property -dict {PACKAGE_PIN AD26 IOSTANDARD LVCMOS18} [get_ports {system_gpioA_gpio[19]}]
set_property -dict {PACKAGE_PIN AF25 IOSTANDARD LVCMOS18} [get_ports {system_gpioA_gpio[20]}]

# LEDs, Buttons and Switches

## LEDs
set_property -dict {PACKAGE_PIN J26 IOSTANDARD LVCMOS33} [get_ports {system_gpioA_gpio[0]}]
set_property -dict {PACKAGE_PIN H26 IOSTANDARD LVCMOS33} [get_ports {system_gpioA_gpio[1]}]
set_property -dict {PACKAGE_PIN E25 IOSTANDARD LVCMOS33} [get_ports {system_gpioA_gpio[2]}]
set_property -dict {PACKAGE_PIN C14 IOSTANDARD LVCMOS33} [get_ports {system_gpioA_gpio[3]}]
set_property -dict {PACKAGE_PIN B14 IOSTANDARD LVCMOS33} [get_ports {system_gpioA_gpio[4]}]

## Buttons
set_property -dict {PACKAGE_PIN AF9 IOSTANDARD LVCMOS18} [get_ports {system_gpioA_gpio[5]}]
set_property -dict {PACKAGE_PIN AF10 IOSTANDARD LVCMOS18} [get_ports {system_gpioA_gpio[6]}]
set_property -dict {PACKAGE_PIN AD18 IOSTANDARD LVCMOS18} [get_ports {system_gpioA_gpio[7]}]
set_property -dict {PACKAGE_PIN AF19 IOSTANDARD LVCMOS18} [get_ports {system_gpioA_gpio[8]}]
set_property -dict {PACKAGE_PIN AF20 IOSTANDARD LVCMOS18} [get_ports {system_gpioA_gpio[9]}]

## Switches
set_property -dict {PACKAGE_PIN Y22 IOSTANDARD LVCMOS18} [get_ports {system_gpioA_gpio[10]}]
set_property -dict {PACKAGE_PIN AA22 IOSTANDARD LVCMOS18} [get_ports {system_gpioA_gpio[11]}]
set_property -dict {PACKAGE_PIN Y23 IOSTANDARD LVCMOS18} [get_ports {system_gpioA_gpio[12]}]
set_property -dict {PACKAGE_PIN AA24 IOSTANDARD LVCMOS18} [get_ports {system_gpioA_gpio[13]}]
set_property -dict {PACKAGE_PIN AC23 IOSTANDARD LVCMOS18} [get_ports {system_gpioA_gpio[14]}]
set_property -dict {PACKAGE_PIN AC24 IOSTANDARD LVCMOS18} [get_ports {system_gpioA_gpio[15]}]
set_property -dict {PACKAGE_PIN AA25 IOSTANDARD LVCMOS18} [get_ports {system_gpioA_gpio[16]}]
set_property -dict {PACKAGE_PIN AB25 IOSTANDARD LVCMOS18} [get_ports {system_gpioA_gpio[17]}]

# HDMI, VGA, AUDIO OUT

## HDMI INT
set_property -dict {PACKAGE_PIN AE7 IOSTANDARD LVCMOS18} [get_ports {system_gpioA_gpio[21]}]

## HDMI I2C
set_property -dict {PACKAGE_PIN AF7 IOSTANDARD LVCMOS18} [get_ports system_i2c_i2c_scl]
set_property -dict {PACKAGE_PIN AC8 IOSTANDARD LVCMOS18} [get_ports system_i2c_i2c_sda]

## VGA
set_property -dict {PACKAGE_PIN B20 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports {system_vgaPhy_color_r[0]}]
set_property -dict {PACKAGE_PIN C19 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports {system_vgaPhy_color_r[1]}]
set_property -dict {PACKAGE_PIN A20 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports {system_vgaPhy_color_r[2]}]
set_property -dict {PACKAGE_PIN A18 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports {system_vgaPhy_color_r[3]}]
set_property -dict {PACKAGE_PIN B19 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports {system_vgaPhy_color_r[4]}]
set_property -dict {PACKAGE_PIN A19 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports {system_vgaPhy_color_g[0]}]
set_property -dict {PACKAGE_PIN C17 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports {system_vgaPhy_color_g[1]}]
set_property -dict {PACKAGE_PIN E18 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports {system_vgaPhy_color_g[2]}]
set_property -dict {PACKAGE_PIN C18 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports {system_vgaPhy_color_g[3]}]
set_property -dict {PACKAGE_PIN C13 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports {system_vgaPhy_color_g[4]}]
set_property -dict {PACKAGE_PIN D18 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports {system_vgaPhy_color_g[5]}]
set_property -dict {PACKAGE_PIN A14 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports {system_vgaPhy_color_b[0]}]
set_property -dict {PACKAGE_PIN D13 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports {system_vgaPhy_color_b[1]}]
set_property -dict {PACKAGE_PIN D14 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports {system_vgaPhy_color_b[2]}]
set_property -dict {PACKAGE_PIN A12 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports {system_vgaPhy_color_b[3]}]
set_property -dict {PACKAGE_PIN A13 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports {system_vgaPhy_color_b[4]}]
set_property -dict {PACKAGE_PIN C12 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports system_vgaPhy_hSync]
set_property -dict {PACKAGE_PIN C11 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports system_vgaPhy_vSync]

# Audio
set_property -dict {PACKAGE_PIN A8 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports {system_audioOut_outputs[0]}]
set_property -dict {PACKAGE_PIN A9 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports {system_audioOut_outputs[1]}]

# USB
set_property -dict {PACKAGE_PIN D20 IOSTANDARD LVCMOS33 IOB TRUE SLEW SLOW DRIVE 8 PULLDOWN YES} [get_ports {system_usbAPort_0_dp}]
set_property -dict {PACKAGE_PIN D19 IOSTANDARD LVCMOS33 IOB TRUE SLEW SLOW DRIVE 8 PULLDOWN YES} [get_ports {system_usbAPort_0_dm}]
set_property -dict {PACKAGE_PIN A24 IOSTANDARD LVCMOS33 IOB TRUE SLEW SLOW DRIVE 8 PULLDOWN YES} [get_ports {system_usbAPort_1_dp}]
set_property -dict {PACKAGE_PIN A23 IOSTANDARD LVCMOS33 IOB TRUE SLEW SLOW DRIVE 8 PULLDOWN YES} [get_ports {system_usbAPort_1_dm}]
set_property -dict {PACKAGE_PIN E22 IOSTANDARD LVCMOS33 IOB TRUE SLEW SLOW DRIVE 8 PULLDOWN YES} [get_ports {system_usbAPort_2_dp}]
set_property -dict {PACKAGE_PIN E21 IOSTANDARD LVCMOS33 IOB TRUE SLEW SLOW DRIVE 8 PULLDOWN YES} [get_ports {system_usbAPort_2_dm}]
set_property -dict {PACKAGE_PIN D24 IOSTANDARD LVCMOS33 IOB TRUE SLEW SLOW DRIVE 8 PULLDOWN YES} [get_ports {system_usbAPort_3_dp}]
set_property -dict {PACKAGE_PIN D23 IOSTANDARD LVCMOS33 IOB TRUE SLEW SLOW DRIVE 8 PULLDOWN YES} [get_ports {system_usbAPort_3_dm}]

# Memory: SPI Flash, SD Card, DDR3

## SPI Flash
set_property -dict {PACKAGE_PIN C23 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports {system_spiA_flash_ss[0]}]
set_property -dict {PACKAGE_PIN B24 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports {system_spiA_flash_data[0]}]
set_property -dict {PACKAGE_PIN A25 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports {system_spiA_flash_data[1]}]

## SD Card
set_property -dict {PACKAGE_PIN E26 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports {system_spiA_sdcard_ss[0]}]
set_property -dict {PACKAGE_PIN F25 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports {system_spiA_sdcard_data[0]}]
set_property -dict {PACKAGE_PIN B25 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports {system_spiA_sdcard_data[1]}]
set_property -dict {PACKAGE_PIN B26 IOSTANDARD LVCMOS33 IOB TRUE} [get_ports system_spiA_sdcard_sclk]

## SDRAM
create_clock -period 3.333 -name {sdramDomain_phyA_sdram_DQS[0]} -waveform {0.000 1.667} [get_ports {sdramDomain_phyA_sdram_DQS[0]}]
create_clock -period 3.333 -name {sdramDomain_phyA_sdram_DQS[1]} -waveform {0.000 1.667} [get_ports {sdramDomain_phyA_sdram_DQS[1]}]

set_property INTERNAL_VREF 0.75 [get_iobanks 34]

set_property -dict {PACKAGE_PIN AF5 IOSTANDARD SSTL15 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[0]}]
set_property -dict {PACKAGE_PIN AF2 IOSTANDARD SSTL15 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[1]}]
set_property -dict {PACKAGE_PIN AD6 IOSTANDARD SSTL15 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[2]}]
set_property -dict {PACKAGE_PIN AC6 IOSTANDARD SSTL15 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[3]}]
set_property -dict {PACKAGE_PIN AD4 IOSTANDARD SSTL15 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[4]}]
set_property -dict {PACKAGE_PIN AB6 IOSTANDARD SSTL15 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[5]}]
set_property -dict {PACKAGE_PIN AE2 IOSTANDARD SSTL15 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[6]}]
set_property -dict {PACKAGE_PIN Y5 IOSTANDARD SSTL15 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[7]}]
set_property -dict {PACKAGE_PIN AA4 IOSTANDARD SSTL15 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[8]}]
set_property -dict {PACKAGE_PIN AE6 IOSTANDARD SSTL15 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[9]}]
set_property -dict {PACKAGE_PIN AE3 IOSTANDARD SSTL15 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[10]}]
set_property -dict {PACKAGE_PIN AD5 IOSTANDARD SSTL15 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[11]}]
set_property -dict {PACKAGE_PIN AB4 IOSTANDARD SSTL15 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[12]}]
set_property -dict {PACKAGE_PIN Y6 IOSTANDARD SSTL15 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[13]}]

set_property -dict {PACKAGE_PIN AD3 IOSTANDARD SSTL15 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_BA[0]}]
set_property -dict {PACKAGE_PIN AE1 IOSTANDARD SSTL15 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_BA[1]}]
set_property -dict {PACKAGE_PIN AE5 IOSTANDARD SSTL15 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_BA[2]}]
set_property -dict {PACKAGE_PIN AC3 IOSTANDARD SSTL15 SLEW FAST} [get_ports sdramDomain_phyA_sdram_RASn]
set_property -dict {PACKAGE_PIN AC4 IOSTANDARD SSTL15 SLEW FAST} [get_ports sdramDomain_phyA_sdram_CASn]
set_property -dict {PACKAGE_PIN AF4 IOSTANDARD SSTL15 SLEW FAST} [get_ports sdramDomain_phyA_sdram_WEn]
set_property -dict {PACKAGE_PIN V1 IOSTANDARD SSTL15 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_DM[0]}]
set_property -dict {PACKAGE_PIN V3 IOSTANDARD SSTL15 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_DM[1]}]

set_property -dict {PACKAGE_PIN W1 IOSTANDARD SSTL15_T_DCI SLEW FAST} [get_ports {sdramDomain_phyA_sdram_DQ[0]}]
set_property -dict {PACKAGE_PIN V2 IOSTANDARD SSTL15_T_DCI SLEW FAST} [get_ports {sdramDomain_phyA_sdram_DQ[1]}]
set_property -dict {PACKAGE_PIN Y1 IOSTANDARD SSTL15_T_DCI SLEW FAST} [get_ports {sdramDomain_phyA_sdram_DQ[2]}]
set_property -dict {PACKAGE_PIN Y3 IOSTANDARD SSTL15_T_DCI SLEW FAST} [get_ports {sdramDomain_phyA_sdram_DQ[3]}]
set_property -dict {PACKAGE_PIN AC2 IOSTANDARD SSTL15_T_DCI SLEW FAST} [get_ports {sdramDomain_phyA_sdram_DQ[4]}]
set_property -dict {PACKAGE_PIN Y2 IOSTANDARD SSTL15_T_DCI SLEW FAST} [get_ports {sdramDomain_phyA_sdram_DQ[5]}]
set_property -dict {PACKAGE_PIN AB2 IOSTANDARD SSTL15_T_DCI SLEW FAST} [get_ports {sdramDomain_phyA_sdram_DQ[6]}]
set_property -dict {PACKAGE_PIN AA3 IOSTANDARD SSTL15_T_DCI SLEW FAST} [get_ports {sdramDomain_phyA_sdram_DQ[7]}]
set_property -dict {PACKAGE_PIN U1 IOSTANDARD SSTL15_T_DCI SLEW FAST} [get_ports {sdramDomain_phyA_sdram_DQ[8]}]
set_property -dict {PACKAGE_PIN V4 IOSTANDARD SSTL15_T_DCI SLEW FAST} [get_ports {sdramDomain_phyA_sdram_DQ[9]}]
set_property -dict {PACKAGE_PIN U6 IOSTANDARD SSTL15_T_DCI SLEW FAST} [get_ports {sdramDomain_phyA_sdram_DQ[10]}]
set_property -dict {PACKAGE_PIN W3 IOSTANDARD SSTL15_T_DCI SLEW FAST} [get_ports {sdramDomain_phyA_sdram_DQ[11]}]
set_property -dict {PACKAGE_PIN V6 IOSTANDARD SSTL15_T_DCI SLEW FAST} [get_ports {sdramDomain_phyA_sdram_DQ[12]}]
set_property -dict {PACKAGE_PIN U2 IOSTANDARD SSTL15_T_DCI SLEW FAST} [get_ports {sdramDomain_phyA_sdram_DQ[13]}]
set_property -dict {PACKAGE_PIN U7 IOSTANDARD SSTL15_T_DCI SLEW FAST} [get_ports {sdramDomain_phyA_sdram_DQ[14]}]
set_property -dict {PACKAGE_PIN U5 IOSTANDARD SSTL15_T_DCI SLEW FAST} [get_ports {sdramDomain_phyA_sdram_DQ[15]}]

set_property -dict {PACKAGE_PIN AB1 IOSTANDARD DIFF_SSTL15_T_DCI SLEW FAST} [get_ports {sdramDomain_phyA_sdram_DQS[0]}]
set_property -dict {PACKAGE_PIN W6 IOSTANDARD DIFF_SSTL15_T_DCI SLEW FAST} [get_ports {sdramDomain_phyA_sdram_DQS[1]}]
set_property -dict {PACKAGE_PIN AC1 IOSTANDARD DIFF_SSTL15_T_DCI SLEW FAST} [get_ports {sdramDomain_phyA_sdram_DQSn[0]}]
set_property -dict {PACKAGE_PIN W5 IOSTANDARD DIFF_SSTL15_T_DCI SLEW FAST} [get_ports {sdramDomain_phyA_sdram_DQSn[1]}]

set_property -dict {PACKAGE_PIN AA5 IOSTANDARD DIFF_SSTL15 SLEW FAST} [get_ports sdramDomain_phyA_sdram_CK]
set_property -dict {PACKAGE_PIN AB5 IOSTANDARD DIFF_SSTL15 SLEW FAST} [get_ports sdramDomain_phyA_sdram_CKn]
set_property -dict {PACKAGE_PIN AD1 IOSTANDARD SSTL15 SLEW FAST} [get_ports sdramDomain_phyA_sdram_CKE]
set_property -dict {PACKAGE_PIN AF3 IOSTANDARD SSTL15 SLEW FAST} [get_ports sdramDomain_phyA_sdram_ODT]
set_property -dict {PACKAGE_PIN W4 IOSTANDARD LVCMOS15 SLEW FAST} [get_ports sdramDomain_phyA_sdram_RESETn]
set_property -dict {PACKAGE_PIN AA2 IOSTANDARD SSTL15 SLEW FAST} [get_ports sdramDomain_phyA_sdram_CSn]



