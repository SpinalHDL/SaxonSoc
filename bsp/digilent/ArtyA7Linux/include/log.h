#include "uart.h"

static void putChar(char c){
	uart_write(UART_A, c);
}

static void putString(char *s){
	char c;
	while(c = *s++){
		putChar(c);
	}
}
