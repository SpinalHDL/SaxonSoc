////////////////////////////////////////////////////////////////////////////////
// Copyright (C) 2013-2021 Efinix Inc. All rights reserved.              
//
// This   document  contains  proprietary information  which   is        
// protected by  copyright. All rights  are reserved.  This notice       
// refers to original work by Efinix, Inc. which may be derivitive       
// of other work distributed under license of the authors.  In the       
// case of derivative work, nothing in this notice overrides the         
// original author's license agreement.  Where applicable, the           
// original license agreement is included in it's original               
// unmodified form immediately below this header.                        
//                                                                       
// WARRANTY DISCLAIMER.                                                  
//     THE  DESIGN, CODE, OR INFORMATION ARE PROVIDED “AS IS” AND        
//     EFINIX MAKES NO WARRANTIES, EXPRESS OR IMPLIED WITH               
//     RESPECT THERETO, AND EXPRESSLY DISCLAIMS ANY IMPLIED WARRANTIES,  
//     INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTIES OF          
//     MERCHANTABILITY, NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR    
//     PURPOSE.  SOME STATES DO NOT ALLOW EXCLUSIONS OF AN IMPLIED       
//     WARRANTY, SO THIS DISCLAIMER MAY NOT APPLY TO LICENSEE.           
//                                                                       
// LIMITATION OF LIABILITY.                                              
//     NOTWITHSTANDING ANYTHING TO THE CONTRARY, EXCEPT FOR BODILY       
//     INJURY, EFINIX SHALL NOT BE LIABLE WITH RESPECT TO ANY SUBJECT    
//     MATTER OF THIS AGREEMENT UNDER TORT, CONTRACT, STRICT LIABILITY   
//     OR ANY OTHER LEGAL OR EQUITABLE THEORY (I) FOR ANY INDIRECT,      
//     SPECIAL, INCIDENTAL, EXEMPLARY OR CONSEQUENTIAL DAMAGES OF ANY    
//     CHARACTER INCLUDING, WITHOUT LIMITATION, DAMAGES FOR LOSS OF      
//     GOODWILL, DATA OR PROFIT, WORK STOPPAGE, OR COMPUTER FAILURE OR   
//     MALFUNCTION, OR IN ANY EVENT (II) FOR ANY AMOUNT IN EXCESS, IN    
//     THE AGGREGATE, OF THE FEE PAID BY LICENSEE TO EFINIX HEREUNDER    
//     (OR, IF THE FEE HAS BEEN WAIVED, $100), EVEN IF EFINIX SHALL HAVE 
//     BEEN INFORMED OF THE POSSIBILITY OF SUCH DAMAGES.  SOME STATES DO 
//     NOT ALLOW THE EXCLUSION OR LIMITATION OF INCIDENTAL OR            
//     CONSEQUENTIAL DAMAGES, SO THIS LIMITATION AND EXCLUSION MAY NOT   
//     APPLY TO LICENSEE.                                                
//
////////////////////////////////////////////////////////////////////////////////
------------- Begin Cut here for COMPONENT Declaration ------
COMPONENT DcFifo is
PORT (
prog_full_o : out std_logic;
full_o : out std_logic;
empty_o : out std_logic;
prog_empty_o : out std_logic;
rdata : out std_logic_vector(15 downto 0);
wr_clk_i : in std_logic;
rd_clk_i : in std_logic;
wr_en_i : in std_logic;
rd_en_i : in std_logic;
a_rst_i : in std_logic;
wdata : in std_logic_vector(15 downto 0);
wr_datacount_o : out std_logic_vector(9 downto 0);
rd_datacount_o : out std_logic_vector(9 downto 0);
rst_busy : out std_logic);
END COMPONENT;
---------------------- End COMPONENT Declaration ------------

------------- Begin Cut here for INSTANTIATION Template -----
u_DcFifo : DcFifo
PORT MAP (
prog_full_o => prog_full_o,
full_o => full_o,
empty_o => empty_o,
prog_empty_o => prog_empty_o,
rdata => rdata,
wr_clk_i => wr_clk_i,
rd_clk_i => rd_clk_i,
wr_en_i => wr_en_i,
rd_en_i => rd_en_i,
a_rst_i => a_rst_i,
wdata => wdata,
wr_datacount_o => wr_datacount_o,
rd_datacount_o => rd_datacount_o,
rst_busy => rst_busy);
------------------------ End INSTANTIATION Template ---------
