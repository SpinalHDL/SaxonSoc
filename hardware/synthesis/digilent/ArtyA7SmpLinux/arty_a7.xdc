# Mainly from https://github.com/Digilent/Arty/blob/master/Projects/GPIO/src/constraints/Arty_Master.xdc

set_property CFGBVS VCCO [current_design]
set_property CONFIG_VOLTAGE 3.3 [current_design]
set_property BITSTREAM.CONFIG.SPI_BUSWIDTH 4 [current_design]

create_clock -period 10.000 -name clocking_GCLK100 [get_nets clocking_GCLK100]


set_clock_groups -asynchronous -group clocking_pll_CLKOUT0 -group clocking_pll_CLKOUT1

# JTAG
set_property -dict {PACKAGE_PIN T18 IOSTANDARD LVCMOS33} [get_ports debug_master_jtag_tms]
set_property -dict {PACKAGE_PIN R18 IOSTANDARD LVCMOS33} [get_ports debug_master_jtag_tdo]
set_property -dict {PACKAGE_PIN P18 IOSTANDARD LVCMOS33} [get_ports debug_master_jtag_tdi]
set_property -dict {PACKAGE_PIN N17 IOSTANDARD LVCMOS33} [get_ports debug_master_jtag_tck]
set_property CLOCK_DEDICATED_ROUTE FALSE [get_nets debug_master_jtag_tck_IBUF]

# UART
set_property -dict {PACKAGE_PIN D10 IOSTANDARD LVCMOS33} [get_ports system_uartA_uart_txd]
set_property -dict {PACKAGE_PIN A9 IOSTANDARD LVCMOS33} [get_ports system_uartA_uart_rxd]

## Buttons
set_property -dict { PACKAGE_PIN D9    IOSTANDARD LVCMOS33 } [get_ports { system_gpioA_gpio[0] }]; #IO_L6N_T0_VREF_16 Sch=btn[0]
set_property -dict { PACKAGE_PIN C9    IOSTANDARD LVCMOS33 } [get_ports { system_gpioA_gpio[1] }]; #IO_L11P_T1_SRCC_16 Sch=btn[1]
set_property -dict { PACKAGE_PIN B9    IOSTANDARD LVCMOS33 } [get_ports { system_gpioA_gpio[2] }]; #IO_L11N_T1_SRCC_16 Sch=btn[2]
set_property -dict { PACKAGE_PIN B8    IOSTANDARD LVCMOS33 } [get_ports { system_gpioA_gpio[3] }]; #IO_L12P_T1_MRCC_16 Sch=btn[3]

## Switches
set_property -dict { PACKAGE_PIN A8    IOSTANDARD LVCMOS33 } [get_ports { system_gpioA_gpio[4] }]; #IO_L12N_T1_MRCC_16 Sch=sw[0]
set_property -dict { PACKAGE_PIN C11   IOSTANDARD LVCMOS33 } [get_ports { system_gpioA_gpio[5] }]; #IO_L13P_T2_MRCC_16 Sch=sw[1]
set_property -dict { PACKAGE_PIN C10   IOSTANDARD LVCMOS33 } [get_ports { system_gpioA_gpio[6] }]; #IO_L13N_T2_MRCC_16 Sch=sw[2]
set_property -dict { PACKAGE_PIN A10   IOSTANDARD LVCMOS33 } [get_ports { system_gpioA_gpio[7] }]; #IO_L14P_T2_SRCC_16 Sch=sw[3]

## LEDs
set_property -dict { PACKAGE_PIN H5    IOSTANDARD LVCMOS33 } [get_ports { system_gpioA_gpio[8] }]; #IO_L24N_T3_35 Sch=led[4]
set_property -dict { PACKAGE_PIN J5    IOSTANDARD LVCMOS33 } [get_ports { system_gpioA_gpio[9] }]; #IO_25_35 Sch=led[5]
set_property -dict { PACKAGE_PIN T9    IOSTANDARD LVCMOS33 } [get_ports { system_gpioA_gpio[10] }]; #IO_L24P_T3_A01_D17_14 Sch=led[6]
set_property -dict { PACKAGE_PIN T10   IOSTANDARD LVCMOS33 } [get_ports { system_gpioA_gpio[11] }]; #IO_L24N_T3_A00_D16_14 Sch=led[7]

## RGB LEDs
set_property -dict { PACKAGE_PIN E1    IOSTANDARD LVCMOS33 } [get_ports { system_gpioA_gpio[12] }]; #IO_L18N_T2_35 Sch=led0_b
set_property -dict { PACKAGE_PIN F6    IOSTANDARD LVCMOS33 } [get_ports { system_gpioA_gpio[13] }]; #IO_L19N_T3_VREF_35 Sch=led0_g
set_property -dict { PACKAGE_PIN G6    IOSTANDARD LVCMOS33 } [get_ports { system_gpioA_gpio[14] }]; #IO_L19P_T3_35 Sch=led0_r
set_property -dict { PACKAGE_PIN G4    IOSTANDARD LVCMOS33 } [get_ports { system_gpioA_gpio[15] }]; #IO_L20P_T3_35 Sch=led1_b
set_property -dict { PACKAGE_PIN J4    IOSTANDARD LVCMOS33 } [get_ports { system_gpioA_gpio[16] }]; #IO_L21P_T3_DQS_35 Sch=led1_g
set_property -dict { PACKAGE_PIN G3    IOSTANDARD LVCMOS33 } [get_ports { system_gpioA_gpio[17] }]; #IO_L20N_T3_35 Sch=led1_r
set_property -dict { PACKAGE_PIN H4    IOSTANDARD LVCMOS33 } [get_ports { system_gpioA_gpio[18] }]; #IO_L21N_T3_DQS_35 Sch=led2_b
set_property -dict { PACKAGE_PIN J2    IOSTANDARD LVCMOS33 } [get_ports { system_gpioA_gpio[19] }]; #IO_L22N_T3_35 Sch=led2_g
set_property -dict { PACKAGE_PIN J3    IOSTANDARD LVCMOS33 } [get_ports { system_gpioA_gpio[20] }]; #IO_L22P_T3_35 Sch=led2_r
set_property -dict { PACKAGE_PIN K2    IOSTANDARD LVCMOS33 } [get_ports { system_gpioA_gpio[21] }]; #IO_L23P_T3_35 Sch=led3_b
set_property -dict { PACKAGE_PIN H6    IOSTANDARD LVCMOS33 } [get_ports { system_gpioA_gpio[22] }]; #IO_L24P_T3_35 Sch=led3_g
set_property -dict { PACKAGE_PIN K1    IOSTANDARD LVCMOS33 } [get_ports { system_gpioA_gpio[23] }]; #IO_L23N_T3_35 Sch=led3_r

# GPIO
set_property -dict {PACKAGE_PIN V15 IOSTANDARD LVCMOS33} [get_ports {system_gpioA_gpio[24]}]
set_property -dict {PACKAGE_PIN U16 IOSTANDARD LVCMOS33} [get_ports {system_gpioA_gpio[25]}]
set_property -dict {PACKAGE_PIN P14 IOSTANDARD LVCMOS33} [get_ports {system_gpioA_gpio[26]}]
set_property -dict {PACKAGE_PIN T11 IOSTANDARD LVCMOS33} [get_ports {system_gpioA_gpio[27]}]
set_property -dict {PACKAGE_PIN R12 IOSTANDARD LVCMOS33} [get_ports {system_gpioA_gpio[28]}]
set_property -dict {PACKAGE_PIN T14 IOSTANDARD LVCMOS33} [get_ports {system_gpioA_gpio[29]}]
set_property -dict {PACKAGE_PIN T15 IOSTANDARD LVCMOS33} [get_ports {system_gpioA_gpio[30]}]
set_property -dict {PACKAGE_PIN T16 IOSTANDARD LVCMOS33} [get_ports {system_gpioA_gpio[31]}]

# serial flash
set_property -dict {PACKAGE_PIN L13 IOSTANDARD LVCMOS33} [get_ports {system_spiA_flash_ss[0]}]
set_property -dict {PACKAGE_PIN K17 IOSTANDARD LVCMOS33} [get_ports {system_spiA_flash_data[0]}]
set_property -dict {PACKAGE_PIN K18 IOSTANDARD LVCMOS33} [get_ports {system_spiA_flash_data[1]}]

# SDCARD
set_property -dict {PACKAGE_PIN D4 IOSTANDARD LVCMOS33} [get_ports {system_spiA_sdcard_ss[0]}]
set_property -dict {PACKAGE_PIN D3 IOSTANDARD LVCMOS33} [get_ports {system_spiA_sdcard_data[0]}]
set_property -dict {PACKAGE_PIN F4 IOSTANDARD LVCMOS33} [get_ports {system_spiA_sdcard_data[1]}]
set_property -dict {PACKAGE_PIN F3 IOSTANDARD LVCMOS33} [get_ports system_spiA_sdcard_sclk]

# User SPI
set_property -dict {PACKAGE_PIN G1 IOSTANDARD LVCMOS33} [get_ports {system_spiA_user_data[1]}]
set_property -dict {PACKAGE_PIN H1 IOSTANDARD LVCMOS33} [get_ports {system_spiA_user_data[0]}]
set_property -dict {PACKAGE_PIN F1 IOSTANDARD LVCMOS33} [get_ports system_spiA_user_sclk]
#set_property -dict { PACKAGE_PIN C1    IOSTANDARD LVCMOS33 } [get_ports { ck_ss }]; #IO_L16N_T2_35 Sch=ck_ss

set_property -dict { PACKAGE_PIN U11   IOSTANDARD LVCMOS33 } [get_ports { inWfi }]; #IO_L19N_T3_A09_D25_VREF_14 Sch=ck_io[26]


set_property -dict {PACKAGE_PIN E3 IOSTANDARD LVCMOS33} [get_ports clocking_GCLK100]

create_clock -period 3.333 -name {sdramDomain_phyA_sdram_DQS[0]} -waveform {0.000 1.667} [get_ports {sdramDomain_phyA_sdram_DQS[0]}]
create_clock -period 3.333 -name {sdramDomain_phyA_sdram_DQS[1]} -waveform {0.000 1.667} [get_ports {sdramDomain_phyA_sdram_DQS[1]}]

set_property INTERNAL_VREF 0.675 [get_iobanks 34]

set_property -dict {PACKAGE_PIN R2 IOSTANDARD SSTL135 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[0]}]
set_property -dict {PACKAGE_PIN M6 IOSTANDARD SSTL135 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[1]}]
set_property -dict {PACKAGE_PIN N4 IOSTANDARD SSTL135 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[2]}]
set_property -dict {PACKAGE_PIN T1 IOSTANDARD SSTL135 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[3]}]
set_property -dict {PACKAGE_PIN N6 IOSTANDARD SSTL135 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[4]}]
set_property -dict {PACKAGE_PIN R7 IOSTANDARD SSTL135 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[5]}]
set_property -dict {PACKAGE_PIN V6 IOSTANDARD SSTL135 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[6]}]
set_property -dict {PACKAGE_PIN U7 IOSTANDARD SSTL135 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[7]}]
set_property -dict {PACKAGE_PIN R8 IOSTANDARD SSTL135 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[8]}]
set_property -dict {PACKAGE_PIN V7 IOSTANDARD SSTL135 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[9]}]
set_property -dict {PACKAGE_PIN R6 IOSTANDARD SSTL135 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[10]}]
set_property -dict {PACKAGE_PIN U6 IOSTANDARD SSTL135 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[11]}]
set_property -dict {PACKAGE_PIN T6 IOSTANDARD SSTL135 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[12]}]
set_property -dict {PACKAGE_PIN T8 IOSTANDARD SSTL135 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_ADDR[13]}]


set_property -dict {PACKAGE_PIN R1 IOSTANDARD SSTL135 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_BA[0]}]
set_property -dict {PACKAGE_PIN P4 IOSTANDARD SSTL135 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_BA[1]}]
set_property -dict {PACKAGE_PIN P2 IOSTANDARD SSTL135 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_BA[2]}]
set_property -dict {PACKAGE_PIN P3 IOSTANDARD SSTL135 SLEW FAST} [get_ports sdramDomain_phyA_sdram_RASn]
set_property -dict {PACKAGE_PIN M4 IOSTANDARD SSTL135 SLEW FAST} [get_ports sdramDomain_phyA_sdram_CASn]
set_property -dict {PACKAGE_PIN P5 IOSTANDARD SSTL135 SLEW FAST} [get_ports sdramDomain_phyA_sdram_WEn]
set_property -dict {PACKAGE_PIN U8 IOSTANDARD SSTL135 SLEW FAST} [get_ports sdramDomain_phyA_sdram_CSn]
set_property -dict {PACKAGE_PIN L1 IOSTANDARD SSTL135 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_DM[0]}]
set_property -dict {PACKAGE_PIN U1 IOSTANDARD SSTL135 SLEW FAST} [get_ports {sdramDomain_phyA_sdram_DM[1]}]

set_property -dict {PACKAGE_PIN K5 IOSTANDARD SSTL135 SLEW FAST IN_TERM UNTUNED_SPLIT_50} [get_ports {sdramDomain_phyA_sdram_DQ[0]}]
set_property -dict {PACKAGE_PIN L3 IOSTANDARD SSTL135 SLEW FAST IN_TERM UNTUNED_SPLIT_50} [get_ports {sdramDomain_phyA_sdram_DQ[1]}]
set_property -dict {PACKAGE_PIN K3 IOSTANDARD SSTL135 SLEW FAST IN_TERM UNTUNED_SPLIT_50} [get_ports {sdramDomain_phyA_sdram_DQ[2]}]
set_property -dict {PACKAGE_PIN L6 IOSTANDARD SSTL135 SLEW FAST IN_TERM UNTUNED_SPLIT_50} [get_ports {sdramDomain_phyA_sdram_DQ[3]}]
set_property -dict {PACKAGE_PIN M3 IOSTANDARD SSTL135 SLEW FAST IN_TERM UNTUNED_SPLIT_50} [get_ports {sdramDomain_phyA_sdram_DQ[4]}]
set_property -dict {PACKAGE_PIN M1 IOSTANDARD SSTL135 SLEW FAST IN_TERM UNTUNED_SPLIT_50} [get_ports {sdramDomain_phyA_sdram_DQ[5]}]
set_property -dict {PACKAGE_PIN L4 IOSTANDARD SSTL135 SLEW FAST IN_TERM UNTUNED_SPLIT_50} [get_ports {sdramDomain_phyA_sdram_DQ[6]}]
set_property -dict {PACKAGE_PIN M2 IOSTANDARD SSTL135 SLEW FAST IN_TERM UNTUNED_SPLIT_50} [get_ports {sdramDomain_phyA_sdram_DQ[7]}]
set_property -dict {PACKAGE_PIN V4 IOSTANDARD SSTL135 SLEW FAST IN_TERM UNTUNED_SPLIT_50} [get_ports {sdramDomain_phyA_sdram_DQ[8]}]
set_property -dict {PACKAGE_PIN T5 IOSTANDARD SSTL135 SLEW FAST IN_TERM UNTUNED_SPLIT_50} [get_ports {sdramDomain_phyA_sdram_DQ[9]}]
set_property -dict {PACKAGE_PIN U4 IOSTANDARD SSTL135 SLEW FAST IN_TERM UNTUNED_SPLIT_50} [get_ports {sdramDomain_phyA_sdram_DQ[10]}]
set_property -dict {PACKAGE_PIN V5 IOSTANDARD SSTL135 SLEW FAST IN_TERM UNTUNED_SPLIT_50} [get_ports {sdramDomain_phyA_sdram_DQ[11]}]
set_property -dict {PACKAGE_PIN V1 IOSTANDARD SSTL135 SLEW FAST IN_TERM UNTUNED_SPLIT_50} [get_ports {sdramDomain_phyA_sdram_DQ[12]}]
set_property -dict {PACKAGE_PIN T3 IOSTANDARD SSTL135 SLEW FAST IN_TERM UNTUNED_SPLIT_50} [get_ports {sdramDomain_phyA_sdram_DQ[13]}]
set_property -dict {PACKAGE_PIN U3 IOSTANDARD SSTL135 SLEW FAST IN_TERM UNTUNED_SPLIT_50} [get_ports {sdramDomain_phyA_sdram_DQ[14]}]
set_property -dict {PACKAGE_PIN R3 IOSTANDARD SSTL135 SLEW FAST IN_TERM UNTUNED_SPLIT_50} [get_ports {sdramDomain_phyA_sdram_DQ[15]}]


set_property -dict {PACKAGE_PIN N2 IOSTANDARD DIFF_SSTL135 SLEW FAST IN_TERM UNTUNED_SPLIT_50} [get_ports {sdramDomain_phyA_sdram_DQS[0]}]
set_property -dict {PACKAGE_PIN U2 IOSTANDARD DIFF_SSTL135 SLEW FAST IN_TERM UNTUNED_SPLIT_50} [get_ports {sdramDomain_phyA_sdram_DQS[1]}]
set_property -dict {PACKAGE_PIN N1 IOSTANDARD DIFF_SSTL135 SLEW FAST IN_TERM UNTUNED_SPLIT_50} [get_ports {sdramDomain_phyA_sdram_DQSn[0]}]
set_property -dict {PACKAGE_PIN V2 IOSTANDARD DIFF_SSTL135 SLEW FAST IN_TERM UNTUNED_SPLIT_50} [get_ports {sdramDomain_phyA_sdram_DQSn[1]}]

set_property -dict {PACKAGE_PIN U9 IOSTANDARD DIFF_SSTL135 SLEW FAST} [get_ports sdramDomain_phyA_sdram_CK]
set_property -dict {PACKAGE_PIN V9 IOSTANDARD DIFF_SSTL135 SLEW FAST} [get_ports sdramDomain_phyA_sdram_CKn]
set_property -dict {PACKAGE_PIN N5 IOSTANDARD SSTL135 SLEW FAST} [get_ports sdramDomain_phyA_sdram_CKE]
set_property -dict {PACKAGE_PIN R5 IOSTANDARD SSTL135 SLEW FAST} [get_ports sdramDomain_phyA_sdram_ODT]
set_property -dict {PACKAGE_PIN K6 IOSTANDARD SSTL135 SLEW FAST} [get_ports sdramDomain_phyA_sdram_RESETn]



set_property -dict { PACKAGE_PIN G13   IOSTANDARD LVCMOS33 } [get_ports { ja[0] }]; #IO_0_15 Sch=ja[1]
set_property -dict { PACKAGE_PIN B11   IOSTANDARD LVCMOS33 } [get_ports { ja[1] }]; #IO_L4P_T0_15 Sch=ja[2]
set_property -dict { PACKAGE_PIN A11   IOSTANDARD LVCMOS33 } [get_ports { ja[2] }]; #IO_L4N_T0_15 Sch=ja[3]
set_property -dict { PACKAGE_PIN D12   IOSTANDARD LVCMOS33 } [get_ports { ja[3] }]; #IO_L6P_T0_15 Sch=ja[4]
set_property -dict { PACKAGE_PIN D13   IOSTANDARD LVCMOS33 } [get_ports { ja[4] }]; #IO_L6N_T0_VREF_15 Sch=ja[7]
set_property -dict { PACKAGE_PIN B18   IOSTANDARD LVCMOS33 } [get_ports { ja[5] }]; #IO_L10P_T1_AD11P_15 Sch=ja[8]
set_property -dict { PACKAGE_PIN A18   IOSTANDARD LVCMOS33 } [get_ports { ja[6] }]; #IO_L10N_T1_AD11N_15 Sch=ja[9]
set_property -dict { PACKAGE_PIN E15   IOSTANDARD LVCMOS33 } [get_ports { ja[7] }]; #IO_L11P_T1_SRCC_15 Sch=ja[10]


# SMSC Ethernet PHY
set_property -dict { PACKAGE_PIN D17   IOSTANDARD LVCMOS33 } [get_ports { system_eth_mii_RX_COL }]; #IO_L16N_T2_A27_15 Sch=eth_col
set_property -dict { PACKAGE_PIN G14   IOSTANDARD LVCMOS33 } [get_ports { system_eth_mii_RX_CRS }]; #IO_L15N_T2_DQS_ADV_B_15 Sch=eth_crs
set_property -dict { PACKAGE_PIN F16   IOSTANDARD LVCMOS33 } [get_ports { system_spiA_md_C }]; #IO_L14N_T2_SRCC_15 Sch=eth_mdc
set_property -dict { PACKAGE_PIN K13   IOSTANDARD LVCMOS33 } [get_ports { system_spiA_md_IO }]; #IO_L17P_T2_A26_15 Sch=eth_mdio
set_property -dict { PACKAGE_PIN G18   IOSTANDARD LVCMOS33 } [get_ports { clocking_clk25 }]; #IO_L22P_T3_A17_15 Sch=eth_ref_clk
#set_property -dict { PACKAGE_PIN C16   IOSTANDARD LVCMOS33 } [get_ports { system_gpioA_gpio[14] }]; #IO_L20P_T3_A20_15 Sch=eth_rstn
set_property -dict { PACKAGE_PIN F15   IOSTANDARD LVCMOS33 } [get_ports { system_eth_mii_RX_CLK }]; #IO_L14P_T2_SRCC_15 Sch=eth_rx_clk
set_property -dict { PACKAGE_PIN G16   IOSTANDARD LVCMOS33 } [get_ports { system_eth_mii_RX_DV }]; #IO_L13N_T2_MRCC_15 Sch=eth_rx_dv
set_property -dict { PACKAGE_PIN D18   IOSTANDARD LVCMOS33 } [get_ports { system_eth_mii_RX_D[0] }]; #IO_L21N_T3_DQS_A18_15 Sch=eth_rxd[0]
set_property -dict { PACKAGE_PIN E17   IOSTANDARD LVCMOS33 } [get_ports { system_eth_mii_RX_D[1] }]; #IO_L16P_T2_A28_15 Sch=eth_rxd[1]
set_property -dict { PACKAGE_PIN E18   IOSTANDARD LVCMOS33 } [get_ports { system_eth_mii_RX_D[2] }]; #IO_L21P_T3_DQS_15 Sch=eth_rxd[2]
set_property -dict { PACKAGE_PIN G17   IOSTANDARD LVCMOS33 } [get_ports { system_eth_mii_RX_D[3] }]; #IO_L18N_T2_A23_15 Sch=eth_rxd[3]
set_property -dict { PACKAGE_PIN C17   IOSTANDARD LVCMOS33 } [get_ports { system_eth_mii_RX_ER }]; #IO_L20N_T3_A19_15 Sch=eth_rxerr
set_property -dict { PACKAGE_PIN H16   IOSTANDARD LVCMOS33 } [get_ports { system_eth_mii_TX_CLK }]; #IO_L13P_T2_MRCC_15 Sch=eth_tx_clk
set_property -dict { PACKAGE_PIN H15   IOSTANDARD LVCMOS33 } [get_ports { system_eth_mii_TX_EN }]; #IO_L19N_T3_A21_VREF_15 Sch=eth_tx_en
set_property -dict { PACKAGE_PIN H14   IOSTANDARD LVCMOS33 } [get_ports { system_eth_mii_TX_D[0] }]; #IO_L15P_T2_DQS_15 Sch=eth_txd[0]
set_property -dict { PACKAGE_PIN J14   IOSTANDARD LVCMOS33 } [get_ports { system_eth_mii_TX_D[1] }]; #IO_L19P_T3_A22_15 Sch=eth_txd[1]
set_property -dict { PACKAGE_PIN J13   IOSTANDARD LVCMOS33 } [get_ports { system_eth_mii_TX_D[2] }]; #IO_L17N_T2_A25_15 Sch=eth_txd[2]
set_property -dict { PACKAGE_PIN H17   IOSTANDARD LVCMOS33 } [get_ports { system_eth_mii_TX_D[3] }]; #IO_L18P_T2_A24_15 Sch=eth_txd[3]


##Pmod Header JB

set_property -dict { PACKAGE_PIN E15   IOSTANDARD LVCMOS33 } [get_ports { system_vgaPhy_color_r[0] }]; #IO_L11P_T1_SRCC_15 Sch=jb_p[1]
set_property -dict { PACKAGE_PIN E16   IOSTANDARD LVCMOS33 } [get_ports { system_vgaPhy_color_r[1] }]; #IO_L11N_T1_SRCC_15 Sch=jb_n[1]
set_property -dict { PACKAGE_PIN D15   IOSTANDARD LVCMOS33 } [get_ports { system_vgaPhy_color_r[2] }]; #IO_L12P_T1_MRCC_15 Sch=jb_p[2]
set_property -dict { PACKAGE_PIN C15   IOSTANDARD LVCMOS33 } [get_ports { system_vgaPhy_color_r[3] }]; #IO_L12N_T1_MRCC_15 Sch=jb_n[2]
set_property -dict { PACKAGE_PIN J17   IOSTANDARD LVCMOS33 } [get_ports { system_vgaPhy_color_b[0] }]; #IO_L23P_T3_FOE_B_15 Sch=jb_p[3]
set_property -dict { PACKAGE_PIN J18   IOSTANDARD LVCMOS33 } [get_ports { system_vgaPhy_color_b[1] }]; #IO_L23N_T3_FWE_B_15 Sch=jb_n[3]
set_property -dict { PACKAGE_PIN K15   IOSTANDARD LVCMOS33 } [get_ports { system_vgaPhy_color_b[2] }]; #IO_L24P_T3_RS1_15 Sch=jb_p[4]
set_property -dict { PACKAGE_PIN J15   IOSTANDARD LVCMOS33 } [get_ports { system_vgaPhy_color_b[3] }]; #IO_L24N_T3_RS0_15 Sch=jb_n[4]

##Pmod Header JC

set_property -dict { PACKAGE_PIN U12   IOSTANDARD LVCMOS33 } [get_ports { system_vgaPhy_color_g[0] }]; #IO_L20P_T3_A08_D24_14 Sch=jc_p[1]
set_property -dict { PACKAGE_PIN V12   IOSTANDARD LVCMOS33 } [get_ports { system_vgaPhy_color_g[1] }]; #IO_L20N_T3_A07_D23_14 Sch=jc_n[1]
set_property -dict { PACKAGE_PIN V10   IOSTANDARD LVCMOS33 } [get_ports { system_vgaPhy_color_g[2] }]; #IO_L21P_T3_DQS_14 Sch=jc_p[2]
set_property -dict { PACKAGE_PIN V11   IOSTANDARD LVCMOS33 } [get_ports { system_vgaPhy_color_g[3] }]; #IO_L21N_T3_DQS_A06_D22_14 Sch=jc_n[2]
set_property -dict { PACKAGE_PIN U14   IOSTANDARD LVCMOS33 } [get_ports { system_vgaPhy_hSync }]; #IO_L22P_T3_A05_D21_14 Sch=jc_p[3]
set_property -dict { PACKAGE_PIN V14   IOSTANDARD LVCMOS33 } [get_ports { system_vgaPhy_vSync }]; #IO_L22N_T3_A04_D20_14 Sch=jc_n[3]
#set_property -dict { PACKAGE_PIN T13   IOSTANDARD LVCMOS33 } [get_ports { jc[6] }]; #IO_L23P_T3_A03_D19_14 Sch=jc_p[4]
#set_property -dict { PACKAGE_PIN U13   IOSTANDARD LVCMOS33 } [get_ports { jc[7] }]; #IO_L23N_T3_A02_D18_14 Sch=jc_n[4]
