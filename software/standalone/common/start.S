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

#ifdef SMP
.global smp_lottery_target
.global smp_lottery_lock
.global smp_slave


  sw x0, smp_lottery_lock, a1

smp_tyranny:
  csrr a0, mhartid
  beqz a0, init

smp_slave:
	lw a0, smp_lottery_lock
	beqz a0, smp_slave

	fence r, r
	//li a1, -1
	//amoadd.w x0, a1,(a0)

	.word(0x100F) //i$ flush
	lw a5, smp_lottery_target
	li a0, 0
	li a1, 0
	li a2, 0
	jr a5

.global   smp_unlock
.type    smp_unlock,%function
smp_unlock:
	sw a0, smp_lottery_target, a1
	fence w, w
	li a0, 1
	sw a0, smp_lottery_lock, a1
    ret
#endif

init:
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

#ifndef NO_LIBC_INIT_ARRAY
	call __libc_init_array
#endif

	call main
mainDone:
    j mainDone


	.globl _init
_init:
    ret

#ifdef SMP
.bss
.align 2
  smp_lottery_target: .word 0
  smp_lottery_lock:   .word 0
#endif