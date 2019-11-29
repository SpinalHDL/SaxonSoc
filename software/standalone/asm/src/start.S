    .section .init
    .globl _start
    .type _start,@function

_start:

    nop
    nop
    nop
    nop
    nop
    nop
    nop
    nop
    nop
    la a3, memA
    li a2, 1
    amoadd.w a3, a2, (a3)
    nop
    nop
    nop
    la a3, memA
    li a2, 1
    amoadd.w a3, a2, (a3)
    nop
    nop
    nop
    nop

memA:
.word 0x10
memB:
.word 0x20
