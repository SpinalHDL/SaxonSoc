module core_pll
(
	input clkin,
	output clkout0,
	output locked
);
(* FREQUENCY_PIN_CLKI="12" *)
(* FREQUENCY_PIN_CLKOP="50" *)

(* ICP_CURRENT = "6", LPF_RESISTOR = "16", MFG_ENABLE_FILTEROPAMP = "1", MFG_GMCREF_SEL = "2" *)
EHXPLLL #(
    .CLKFB_DIV(6'd42),
    .CLKI_DIV(1'd1),
    .CLKOP_CPHASE(1'd0),
    .CLKOP_DIV(4'd10),
    .CLKOP_ENABLE("ENABLED"),
    .CLKOP_FPHASE(1'd0),
    .CLKOS3_DIV(1'd1),
    .CLKOS3_ENABLE("ENABLED"),
    .FEEDBK_PATH("INT_OS3")
) EHXPLLL (
    .CLKI(clkin),
    .RST(1'b0),
    .CLKOP(clkout0),
    .LOCK(locked)
);

endmodule
