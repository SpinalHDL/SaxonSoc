    .section .init
    .globl _start
    .type _start,@function

_start:
#ifdef USE_GP
.option push
.option norelax
	la gp, __global_pointer$
.option pop
#endif
	la sp, _sp


	/* Load data section */
	la a0, _data_lma
	la a1, _data
	la a2, _edata
	bgeu a1, a2, 2f
1:
	lw t0, (a0)
	sw t0, (a1)
	addi a0, a0, 4
	addi a1, a1, 4
	bltu a1, a2, 1b
2:

	/* Clear bss section */
	la a0, __bss_start
	la a1, _end
	bgeu a0, a1, 2f
1:
	sw zero, (a0)
	addi a0, a0, 4
	bltu a0, a1, 1b
2:

	call __libc_init_array
	li a0, 0xF0F0
	li a1, 0xFF00
	.word (0x0B + ( 10 << 7 ) + ( 10 << 15 ) + ( 11 << 20 ) + (0 << 25))
	nop
	nop
	nop
	nop
	li a0, 0x2
	li a1, 0x6
	.word (0x0B + ( 10 << 7 ) + ( 10 << 15 ) + ( 11 << 20 ) + (1 << 25))
	nop
	nop
	nop
	nop
	li a0, 0x3
	li a1, 0x10
	.word (0x0B + ( 10 << 7 ) + ( 10 << 15 ) + ( 11 << 20 ) + (2 << 25))
	nop
	nop
	nop
	nop
	li a0, 0x4
	li a1, 0x10
	.word (0x0B + ( 10 << 7 ) + ( 10 << 15 ) + ( 11 << 20 ) + (2 << 25))

mainDone:
    j mainDone


	.globl _init
_init:
    ret
