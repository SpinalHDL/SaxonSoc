#include "main.h"

#define LED_PATTERN_1		// for testing different LED blinking patterns

void main() {
    bsp_init();

    Bus16Wr(DAC_CLK_PRD_CS, 100);							// Clk80MHz/100 = 800KHz

    gpio_setOutputEnable(BSP_LED_GPIO, BSP_LED_MASK);
    gpio_setOutput(BSP_LED_GPIO, 0);

    //uart_write(BSP_UART_TERMINAL, '!');
    uart_writeStr(BSP_UART_TERMINAL, "input d4,f5,g,w,e,r");


    while(1)
    {
#ifdef LED_PATTERN_1
    	gpio_setOutput(BSP_LED_GPIO, (gpio_getOutput(BSP_LED_GPIO) +1) & BSP_LED_MASK);
#else
    	gpio_setOutput(BSP_LED_GPIO, (gpio_getOutput(BSP_LED_GPIO) ^ BSP_LED_MASK));
#endif

        while(uart_readOccupancy(BSP_UART_TERMINAL))
        {
        	char c = uart_read(BSP_UART_TERMINAL);
            uart_write(BSP_UART_TERMINAL, c );

            if ((c == 'd') || (c == 'f'))									// test Dpr / Fifo
            {
                uart_write(BSP_UART_TERMINAL, '?' );

                int j = 20;
                while (j--)
                {
                	if (uart_readOccupancy(BSP_UART_TERMINAL))
                	{
                		if (c == 'd') {
                			 testDpr(uart_read(BSP_UART_TERMINAL)-48);
                			 break;
                		}
                		else {
                			char c1 = uart_read(BSP_UART_TERMINAL);
                			uart_write(BSP_UART_TERMINAL, c1);
                			writeFifo(c1 - 48);
                		break;
                		}
                	}
                	bsp_uDelay(LOOP_UDELAY);
                }
            }
            else if (c == 'g')								// read one word from FiFo
            {
            	s32 a = Bus32Rd(FIFO_RD_CS);
        	    i2a(a);
            }
            else if (c == 'w')								// start Dac
            {
            	Bus32Wr(GPIO_SET_BITS, 0x2);				// see a_demoEx_main.scala: (DacDraw, AnBcurve)   := Gpio(1 downto 0)
            }
            else if (c == 'e') 								// stop Dac
            {
            	Bus32Wr(GPIO_CLR_BITS, 0x2);				// see a_demoEx_main.scala: (DacDraw, AnBcurve)   := Gpio(1 downto 0)
            }
            else if (c == 'r')								// Toggle and Pulse Demo
            {
            	Pulse;
            	Toggle;
            	Toggle;
            	Pulse;
            	Pulse;
            }
        }


        bsp_uDelay(LOOP_UDELAY);
    }
}


void testDpr (char a)
{
	int n;
	volatile u16* p;
	p = (u16*)DPR_BASE;
	u16  b[32];

	for (n=0; n<30; n++)			// write
		*p++ = (u16) (n+a);

	p = (u16*)DPR_BASE;
	for (n=0; n<30; n++)			// read back
		b[n] = *p++;

	for (n=0; n<30; n++)			// to serial
	{
	    i2a((s32)b[n]);
	}
}

void writeFifo (char a)					// hardware writes burst of a, a+1, a+2 .... Use readFifo to read word by word
{
	Bus16Wr(MONO_SHOT_REG_CS,0x01);		// optionally clear Fifo before every write bursts. FifoA_clr := RegNext (Cs.MonoShotRegCs & Bus16_Dout(0))
	Bus16Wr(FIFO_A_WDATA_CS, a);		// 4 words written by hardware

	uart_writeStr(BSP_UART_TERMINAL, " g>");
}

void i2a(s32 val)
{
	char str[6]={0,0,0,0,0,0xa};
	int i=4,j=0;
	val = val > 9999? 9999 : val;
	val = val < -9999? -9999 : val;

	while(val)
	{
		str[i]=val%10;
		val=val/10;
		i--;
	}

	while(str[j]==0)
		j++;

	if(val<0) uart_write(BSP_UART_TERMINAL,'-');
	for(i=j;i<6;i++)
		uart_write(BSP_UART_TERMINAL, 48+str[i]);

}

