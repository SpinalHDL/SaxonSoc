#include "type.h"

#include "bsp.h"
#include "riscv.h"
#include "stdio.h"
#include "macro.h"

//void _sbrk(){ bsp_putString("_sbrk\n"); }
//void _exit(int a){ bsp_putString("_exit\n"); while(1); }
//void _kill(){ bsp_putString("_kill\n"); }
//void _isatty(){ bsp_putString("_isatty\n"); }
//void _getpid(){ bsp_putString("_getpid\n"); }
////void _write(){ bsp_putString("_write\n"); }
//void _close(){ bsp_putString("_close\n"); }
//void _fstat(){ bsp_putString("_fstat\n"); }
//void _lseek(){ bsp_putString("_lseek\n"); }
//void _read(){ bsp_putString("_read\n"); }

void _sbrk(){ bsp_putString("_sbrk\n"); }
void _exit(int a){ bsp_putString("_exit\n"); while(1); }
void _kill(){ bsp_putString("_kill\n"); }
void _isatty(){ bsp_putString("_isatty\n"); }
void _getpid(){ bsp_putString("_getpid\n"); }
//void _write(){ bsp_putString("_write\n"); }
void _close(){ bsp_putString("_close\n"); }
void _fstat(){ bsp_putString("_fstat\n"); }
void _lseek(){ bsp_putString("_lseek\n"); }
void _read(){ bsp_putString("_read\n"); }

ssize_t _write(int fd, const void *buf, size_t count){
    for(int i = 0;i < count;i++) bsp_putChar(((char*)buf)[i]);
    return count;
}


#define testBinaryOp(op) \
    bsp_putString(op); \
    bsp_putString("\n  1 => "); \
    test(binaryOp1(op)) \
    bsp_putString("\n  20 => "); \
    test(binaryOp20(op)) \
    bsp_putString("\n 100 => "); \
    test(binaryOp100(op)) \
    bsp_putString("\n");

#define testFmaOp(op) \
    bsp_putString(op); \
    bsp_putString("\n  1 => "); \
    test(fmaOp1(op)) \
    bsp_putString("\n  20 => "); \
    test(fmaOp20(op)) \
    bsp_putString("\n 100 => "); \
    test(fmaOp100(op)) \
    bsp_putString("\n");

#define testFldOp(op) \
    bsp_putString(op); \
    bsp_putString("\n  1 => "); \
    test(fldOp1(op)) \
    bsp_putString("\n  20 => "); \
    test(fldOp20(op)) \
    bsp_putString("\n 100 => "); \
    test(fldOp100(op)) \
    bsp_putString("\n");

#define testFsOp(op) \
    bsp_putString(op); \
    bsp_putString("\n  1 => "); \
    test(fsOp1(op)) \
    bsp_putString("\n  20 => "); \
    test(fsOp20(op)) \
    bsp_putString("\n 100 => "); \
    test(fsOp100(op)) \
    bsp_putString("\n");


void main() {
    test(
    )
    bsp_putString("\n");

    testBinaryOp("fadd.s")
    testBinaryOp("fmul.s")
    testBinaryOp("fdiv.s")
    testFmaOp("fmadd.s")
    testFldOp("flw")
    testFsOp("fsw")

}

