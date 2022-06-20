/*
 * main.h
 *
 *  Created on: Jun 12, 2022
 *      Author: ravi
 */

#ifndef SRC_MAIN_H_
#define SRC_MAIN_H_

//#include <stdio.h>


#include "bsp.h"
#include "gpio.h"
#include "uart.h"

// see soc.h
#define DPR_BASE				SYSTEM_DPR_CTRL
#define BUS16_BASE				SYSTEM__BUS16_CTRL
#define BUS32_BASE				SYSTEM__BUS32_CTRL

// see cs.scala
//		on Bu16
#define FIFO_A_WDATA_CS		0x00
#define DAC_CLK_PRD_CS		0x10
#define DAC_OFFSET_A_CS		0x12
#define TOGGLE_REG_CS		0x60
#define MONO_SHOT_REG_CS	0x62

#define DAC_DPR_CS			0x200

//		on Bus32
#define FIFO_RD_CS			64
#define GPIO_CS 			76
#define GPIO_SET_BITS		80
#define GPIO_CLR_BITS		84




#define	Toggle					write_u16(0x01,BUS16_BASE+TOGGLE_REG_CS)
#define	Pulse					write_u16(0x02,BUS16_BASE+MONO_SHOT_REG_CS)


#define	Bus16Wr(offset,val) 	write_u16(val,BUS16_BASE+offset)

#define	Bus32Wr(off,val) 		write_u32(val,BUS32_BASE+off)
#define	Bus32Rd(off) 			read_u32(BUS32_BASE+off)


#define DprWr(off,val)			write_u16(val,DPR_BASE+off)
#define DprRd(off)				read_u16(DPR_BASE+off)




#ifdef SPINAL_SIM
#define LOOP_UDELAY 100
#else
#define LOOP_UDELAY 100000
#endif

void testDpr (char a);
void writeFifo (char a);
void i2a (s32 val);



#endif /* SRC_MAIN_H_ */
