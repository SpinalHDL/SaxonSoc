#include <stdint.h>

#include "saxon.h"

extern const uint32_t _sp;
extern void trapEntry();

#define MACHINE_TIMER_CMP (*(volatile uint64_t*)0x10008008)
#define MACHINE_TIMER (*(volatile uint64_t*)0x10008000)

#define MSTATUS_MPP 0x00001800
#define MSTATUS_MIE 0x00000008



#define csr_write(csr, val)					\
({								\
	unsigned long __v = (unsigned long)(val);		\
	__asm__ __volatile__ ("csrw " #csr ", %0"		\
			      : : "rK" (__v));			\
})

#define csr_set(csr, val)					\
({								\
	unsigned long __v = (unsigned long)(val);		\
	__asm__ __volatile__ ("csrs " #csr ", %0"		\
			      : : "rK" (__v));			\
})

#define csr_clear(csr, val)					\
({								\
	unsigned long __v = (unsigned long)(val);		\
	__asm__ __volatile__ ("csrc " #csr ", %0"		\
			      : : "rK" (__v));			\
})

#define NULL 0

#define PROMPT 0
#define NEXT 1
#define LOOP 2

static int c, cnt, pos, val, len;
static char *cp;
static void *base_addr = NULL;
static int state;
static int echo = 0;

void putch(char c) {
  UART_A->DATA = c;
}

void print(const char *p) {
  while (*p) putch(*(p++));
}

void prompt() {
  print("\r\nrv32> ");
}

void start_prog() {
  //GPIO_A->OUTPUT = 0x00000000; // Switch off all Leds
  // Drain the uart
  while (UART_A->STATUS >> 24) c = UART_A->DATA;
  __asm __volatile__(
    "lui sp, 0x80003;"  /* 12K RAM top = stack address */
    "mv ra, zero;"
    "jr %0;"
    : 
    : "r" (base_addr)
  );
}

void trap() {
  GPIO_A->OUTPUT |= 4;
  GPIO_A->OUTPUT ^= 8;
  MACHINE_TIMER_CMP = 0x7FFFFFFFFFFFFFFF;
}

void main() {
  // set 4 output bits for LEDs
  GPIO_A->OUTPUT_ENABLE = 0x0000000F;
  GPIO_A->OUTPUT = 0x00000000;

  MACHINE_TIMER_CMP = 0x7FFFFFFFFFFFFFFF;

  csr_write(mtvec, trapEntry); //Set the machine trap vector (trap.S)
  csr_write(mie, MIE_MTIE); // Enable machine timer interrupts
  csr_write(mstatus, MSTATUS_MPP | MSTATUS_MIE); //Enable interrupts

  //GPIO_A->OUTPUT |= 0x01; // Set Red LED

  state = PROMPT;
  int cycles = 0;

  // Loop reading characters from UART
  while (1) {
    //GPIO_A->OUTPUT |= 0x04; // Set Green LED
    //if (cycles > 1000000) GPIO_A->OUTPUT |= 0x08;
    if (cycles++ >= 1000000 && base_addr != NULL &&
        ((*(volatile uint32_t*) base_addr) & 0xfff) == 0x197)  { // If no input, start user program
      start_prog();
    }

    if (UART_A->STATUS >> 24) { // UART RX interrupt?
      cycles = 0; 
      //GPIO_A->OUTPUT |= 0x02;  // Set Yellow Led

      // Output prompt
      if (state == PROMPT) { 
        prompt();
        state = NEXT;
      }

      // Prepare for next SREC
      if (state == NEXT) {
        pos = -1;
        len = 255;
        state = LOOP;
      }

      c = (UART_A->DATA) & 0xFF; // Get character
      if (echo) putch(c);        // Echo character

      // Look for SREC
      if (pos < 0) {
        if (c == 'S') {
          pos = 0;
        } else if (c == '\r') {
          state = PROMPT;
          continue;
        }
        val = 0;
        continue;
      } 

      // End of SREC
      if (c >= 10 && c <= 13) {
        state = NEXT;
        continue;
      }

      // Convert to hex
      val <<= 4;

      if (c >= 'a') c -= 32;

      if (c >= 'A') val |= c - 'A' + 10;
      else val |= c - '0';

      // Move along SREC
      pos++;

      // Check SREC type
      if (pos == 1) {
        if (val >= 7 && val <= 9) { // Start address record
	  start_prog();
        }
        if (val <= 3) len = (val << 1) + 5; // Get length of record
        val = 0;
        state = LOOP;
        continue;
      }

      // Byte count
      if (pos == 3) {
        cnt = 2 + (val << 1);
        val = 0;
        continue;
      }
       
      // Valid length? Skips S0 record
      if (len < 6) continue;
      
      // End of address
      if (pos == len) {
        cp = (char *) val;
        if (base_addr == NULL) base_addr = (void *) val;
        continue;
      }

      // Write byte to RAM
      if (pos > len && (pos & 1) && pos < cnt) {
        *cp++ = val;
      }                  
    }
  }
}

