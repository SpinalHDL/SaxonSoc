#define binaryOp1(op) \
asm(op " f1, f0, f0");  \

#define binaryOp20(op) \
asm(op " f1, f0, f0");  \
asm(op " f2, f0, f0");  \
asm(op " f3, f0, f0");  \
asm(op " f4, f0, f0");  \
asm(op " f5, f0, f0");  \
asm(op " f6, f0, f0");  \
asm(op " f7, f0, f0");  \
asm(op " f8, f0, f0");  \
asm(op " f9, f0, f0");  \
asm(op " f10, f0, f0");  \
asm(op " f11, f0, f0");  \
asm(op " f12, f0, f0");  \
asm(op " f13, f0, f0");  \
asm(op " f14, f0, f0");  \
asm(op " f15, f0, f0");  \
asm(op " f16, f0, f0");  \
asm(op " f17, f0, f0");  \
asm(op " f18, f0, f0");  \
asm(op " f19, f0, f0");  \
asm(op " f20, f0, f0");

#define binaryOp100(op) \
binaryOp20(op) \
binaryOp20(op) \
binaryOp20(op) \
binaryOp20(op) \
binaryOp20(op)

#define fmaOp1(op) \
asm(op " f1, f0, f0, f0");  \

#define fmaOp20(op) \
asm(op " f1, f0, f0, f0");  \
asm(op " f2, f0, f0, f0");  \
asm(op " f3, f0, f0, f0");  \
asm(op " f4, f0, f0, f0");  \
asm(op " f5, f0, f0, f0");  \
asm(op " f6, f0, f0, f0");  \
asm(op " f7, f0, f0, f0");  \
asm(op " f8, f0, f0, f0");  \
asm(op " f9, f0, f0, f0");  \
asm(op " f10, f0, f0, f0");  \
asm(op " f11, f0, f0, f0");  \
asm(op " f12, f0, f0, f0");  \
asm(op " f13, f0, f0, f0");  \
asm(op " f14, f0, f0, f0");  \
asm(op " f15, f0, f0, f0");  \
asm(op " f16, f0, f0, f0");  \
asm(op " f17, f0, f0, f0");  \
asm(op " f18, f0, f0, f0");  \
asm(op " f19, f0, f0, f0");  \
asm(op " f20, f0, f0, f0");

#define fmaOp100(op) \
fmaOp20(op) \
fmaOp20(op) \
fmaOp20(op) \
fmaOp20(op) \
fmaOp20(op)


#define fldOp1(op) \
asm(op " f1, 0(sp)");  \

#define fldOp20(op) \
asm(op " f1,  -8(sp)");  \
asm(op " f2,  -8(sp)");  \
asm(op " f3,  -8(sp)");  \
asm(op " f4,  -8(sp)");  \
asm(op " f5,  -8(sp)");  \
asm(op " f6,  -8(sp)");  \
asm(op " f7,  -8(sp)");  \
asm(op " f8,  -8(sp)");  \
asm(op " f9,  -8(sp)");  \
asm(op " f10, -8(sp)");  \
asm(op " f11, -8(sp)");  \
asm(op " f12, -8(sp)");  \
asm(op " f13, -8(sp)");  \
asm(op " f14, -8(sp)");  \
asm(op " f15, -8(sp)");  \
asm(op " f16, -8(sp)");  \
asm(op " f17, -8(sp)");  \
asm(op " f18, -8(sp)");  \
asm(op " f19, -8(sp)");  \
asm(op " f20, -8(sp)");

#define fldOp100(op) \
fldOp20(op) \
fldOp20(op) \
fldOp20(op) \
fldOp20(op) \
fldOp20(op)



#define fsOp1(op) \
asm(op " f1, 0(sp)");  \

#define fsOp20(op) \
asm(op " f1,  -8(sp)");  \
asm(op " f2,  -8(sp)");  \
asm(op " f3,  -8(sp)");  \
asm(op " f4,  -8(sp)");  \
asm(op " f5,  -8(sp)");  \
asm(op " f6,  -8(sp)");  \
asm(op " f7,  -8(sp)");  \
asm(op " f8,  -8(sp)");  \
asm(op " f9,  -8(sp)");  \
asm(op " f10, -8(sp)");  \
asm(op " f11, -8(sp)");  \
asm(op " f12, -8(sp)");  \
asm(op " f13, -8(sp)");  \
asm(op " f14, -8(sp)");  \
asm(op " f15, -8(sp)");  \
asm(op " f16, -8(sp)");  \
asm(op " f17, -8(sp)");  \
asm(op " f18, -8(sp)");  \
asm(op " f19, -8(sp)");  \
asm(op " f20, -8(sp)");

#define fsOp100(op) \
fldOp20(op) \
fldOp20(op) \
fldOp20(op) \
fldOp20(op) \
fldOp20(op)

#define test(body)                    \
{                                              \
                                               \
    volatile u32 time;                         \
    for(u32 i = 0;i < 2;i++){                  \
        uint32_t startAt = csr_read(0xC01);    \
        {body}                                   \
        uint32_t stopAt = csr_read(0xC01);     \
        time = stopAt - startAt-3;               \
    }                                          \
    printf("%d", time);      \
    fflush(0);      \
}

