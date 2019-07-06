/**
 * PLL configuration
 *
 * This Verilog module was generated automatically
 * using the icepll tool from the IceStorm project.
 * Use at your own risk.
 *
 * Given input frequency:        25.000 MHz
 * Requested output frequency:   16.000 MHz
 * Achieved output frequency:    16.016 MHz
 */

module blackice_mx_pll(
	input  clock_in,
	output clock_out,
	output sdram_clock_out,
	output locked
	);

SB_PLL40_2F_CORE #(
        .FEEDBACK_PATH("DELAY"),
        .DELAY_ADJUSTMENT_MODE_FEEDBACK("FIXED"),
        .DELAY_ADJUSTMENT_MODE_RELATIVE("FIXED"),
        .PLLOUT_SELECT_PORTA("GENCLK"),
        .PLLOUT_SELECT_PORTB("GENCLK"),
        .SHIFTREG_DIV_MODE(1'b0),
        .FDA_FEEDBACK(4'b1011),
        .FDA_RELATIVE(4'b1011),
	.DIVR(4'b0000),		// DIVR =  0
	.DIVF(7'b0101000),	// DIVF = 40
	.DIVQ(3'b110),		// DIVQ =  6
	.FILTER_RANGE(3'b010),	// FILTER_RANGE = 2
        .DIVF(7'b0000000),
        .DIVQ(3'b101),
        .FILTER_RANGE(3'b111)
    ) pll (
        .REFERENCECLK   (clock_in),
        .PLLOUTGLOBALA  (clock_out),
        .PLLOUTGLOBALB  (sdram_clock_out0),
        .LOCK           (pll_locked),
        .BYPASS         (1'b0),
        .RESETB         (1'b1)
    );

endmodule
