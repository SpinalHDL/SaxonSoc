module mixed_width_ram    // 256x16 write and 512x8 read
(
		input [7:0] waddr,  
		input [15:0] wdata, 
		input we, clk,
		input [8:0] raddr,
		output logic [7:0] q
);
	logic [1:0][7:0] ram[0:255];
	always_ff@(posedge clk)
		begin
			if(we) ram[waddr] <= wdata;
			q <= ram[raddr / 2][raddr % 2];
		end
endmodule : mixed_width_ram