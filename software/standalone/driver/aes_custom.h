#pragma once

#include "riscv.h"

#define aes_enc_round(rs1, rs2, sel) opcode_R(CUSTOM0, 0x00, (sel << 3), rs1, rs2)
#define aes_enc_round_last(rs1, rs2, sel) opcode_R(CUSTOM0, 0x00, (sel << 3) | 2, rs1, rs2)

#define aes_dec_round(rs1, rs2, sel) opcode_R(CUSTOM0, 0x00, (sel << 3) | 1, rs1, rs2)
#define aes_dec_round_last(rs1, rs2, sel) opcode_R(CUSTOM0, 0x00, (sel << 3) | 2 | 1, rs1, rs2)

static inline __attribute__((always_inline)) unsigned int  vexriscv_aes_load_unaligned(const unsigned char *ptr)   {
    return ((unsigned int)ptr[0] << 0) | ((unsigned int)ptr[1] << 8) | ((unsigned int)ptr[2] << 16) | ((unsigned int)ptr[3] << 24);
}

static inline __attribute__((always_inline)) void vexriscv_aes_store_unaligned(unsigned char *ptr, unsigned int value) {
    ptr[0] = value >> 0;
    ptr[1] = value >> 8;
    ptr[2] = value >> 16;
    ptr[3] = value >> 24;
}

static inline __attribute__((always_inline)) void vexriscv_aes_encrypt(unsigned char *in,
                          unsigned char *out,
                          unsigned int *rk,
                          int round_count) {
    unsigned int s0, s1, s2, s3, t0, t1, t2, t3;
    round_count >>= 1;

    if(((int)in & 3) == 0){
        s0 = ((volatile unsigned int*)in)[0];
        s1 = ((volatile unsigned int*)in)[1];
        s2 = ((volatile unsigned int*)in)[2];
        s3 = ((volatile unsigned int*)in)[3];
    } else {
        s0 = vexriscv_aes_load_unaligned(in + 0);
        s1 = vexriscv_aes_load_unaligned(in + 4);
        s2 = vexriscv_aes_load_unaligned(in + 8);
        s3 = vexriscv_aes_load_unaligned(in + 12);
    }

    t0 = ((volatile unsigned int*)rk)[0];
    t1 = ((volatile unsigned int*)rk)[1];
    t2 = ((volatile unsigned int*)rk)[2];
    t3 = ((volatile unsigned int*)rk)[3];

    s0 ^= t0;
    s1 ^= t1;
    s2 ^= t2;
    s3 ^= t3;

    for (;;) {
        t0 = ((volatile unsigned int*)rk)[4];
        t1 = ((volatile unsigned int*)rk)[5];
        t2 = ((volatile unsigned int*)rk)[6];
        t3 = ((volatile unsigned int*)rk)[7];


        t0 = aes_enc_round(t0, s0, 0);
        t1 = aes_enc_round(t1, s1, 0);
        t2 = aes_enc_round(t2, s2, 0);
        t3 = aes_enc_round(t3, s3, 0);

        t0 = aes_enc_round(t0, s1, 1);
        t1 = aes_enc_round(t1, s2, 1);
        t2 = aes_enc_round(t2, s3, 1);
        t3 = aes_enc_round(t3, s0, 1);

        t0 = aes_enc_round(t0, s2, 2);
        t1 = aes_enc_round(t1, s3, 2);
        t2 = aes_enc_round(t2, s0, 2);
        t3 = aes_enc_round(t3, s1, 2);

        t0 = aes_enc_round(t0, s3, 3);
        t1 = aes_enc_round(t1, s0, 3);
        t2 = aes_enc_round(t2, s1, 3);
        t3 = aes_enc_round(t3, s2, 3);

        rk += 8;
        if (--round_count == 0) {
            break;
        }

        s0 = ((volatile unsigned int*)rk)[0];
        s1 = ((volatile unsigned int*)rk)[1];
        s2 = ((volatile unsigned int*)rk)[2];
        s3 = ((volatile unsigned int*)rk)[3];

        s0 = aes_enc_round(s0, t0, 0);
        s1 = aes_enc_round(s1, t1, 0);
        s2 = aes_enc_round(s2, t2, 0);
        s3 = aes_enc_round(s3, t3, 0);

        s0 = aes_enc_round(s0, t1, 1);
        s1 = aes_enc_round(s1, t2, 1);
        s2 = aes_enc_round(s2, t3, 1);
        s3 = aes_enc_round(s3, t0, 1);

        s0 = aes_enc_round(s0, t2, 2);
        s1 = aes_enc_round(s1, t3, 2);
        s2 = aes_enc_round(s2, t0, 2);
        s3 = aes_enc_round(s3, t1, 2);

        s0 = aes_enc_round(s0, t3, 3);
        s1 = aes_enc_round(s1, t0, 3);
        s2 = aes_enc_round(s2, t1, 3);
        s3 = aes_enc_round(s3, t2, 3);
    }

    s0 = ((volatile unsigned int*)rk)[0];
    s1 = ((volatile unsigned int*)rk)[1];
    s2 = ((volatile unsigned int*)rk)[2];
    s3 = ((volatile unsigned int*)rk)[3];

    s0 = aes_enc_round_last(s0, t0, 0);
    s1 = aes_enc_round_last(s1, t1, 0);
    s2 = aes_enc_round_last(s2, t2, 0);
    s3 = aes_enc_round_last(s3, t3, 0);

    s0 = aes_enc_round_last(s0, t1, 1);
    s1 = aes_enc_round_last(s1, t2, 1);
    s2 = aes_enc_round_last(s2, t3, 1);
    s3 = aes_enc_round_last(s3, t0, 1);

    s0 = aes_enc_round_last(s0, t2, 2);
    s1 = aes_enc_round_last(s1, t3, 2);
    s2 = aes_enc_round_last(s2, t0, 2);
    s3 = aes_enc_round_last(s3, t1, 2);

    s0 = aes_enc_round_last(s0, t3, 3);
    s1 = aes_enc_round_last(s1, t0, 3);
    s2 = aes_enc_round_last(s2, t1, 3);
    s3 = aes_enc_round_last(s3, t2, 3);

    if(((int)out & 3) == 0){
        ((volatile unsigned int*)out)[0] = s0;
        ((volatile unsigned int*)out)[1] = s1;
        ((volatile unsigned int*)out)[2] = s2;
        ((volatile unsigned int*)out)[3] = s3;
    } else {
        vexriscv_aes_store_unaligned(out + 0, s0);
        vexriscv_aes_store_unaligned(out + 4, s1);
        vexriscv_aes_store_unaligned(out + 8, s2);
        vexriscv_aes_store_unaligned(out + 12, s3);
    }
}


static inline __attribute__((always_inline)) void vexriscv_aes_decrypt(unsigned char *in,
                          unsigned char *out,
                          unsigned int *rk,
                          int round_count) {
    unsigned int s0, s1, s2, s3, t0, t1, t2, t3;
    round_count >>= 1;

    if(((int)in & 3) == 0){
        s0 = ((volatile unsigned int*)in)[0];
        s1 = ((volatile unsigned int*)in)[1];
        s2 = ((volatile unsigned int*)in)[2];
        s3 = ((volatile unsigned int*)in)[3];
    } else {
        s0 = vexriscv_aes_load_unaligned(in + 0);
        s1 = vexriscv_aes_load_unaligned(in + 4);
        s2 = vexriscv_aes_load_unaligned(in + 8);
        s3 = vexriscv_aes_load_unaligned(in + 12);
    }

    t0 = ((volatile unsigned int*)rk)[0];
    t1 = ((volatile unsigned int*)rk)[1];
    t2 = ((volatile unsigned int*)rk)[2];
    t3 = ((volatile unsigned int*)rk)[3];

    s0 ^= t0;
    s1 ^= t1;
    s2 ^= t2;
    s3 ^= t3;

    for (;;) {
        t0 = ((volatile unsigned int*)rk)[4];
        t1 = ((volatile unsigned int*)rk)[5];
        t2 = ((volatile unsigned int*)rk)[6];
        t3 = ((volatile unsigned int*)rk)[7];

        t0 = aes_dec_round(t0, s0, 0);
        t1 = aes_dec_round(t1, s1, 0);
        t2 = aes_dec_round(t2, s2, 0);
        t3 = aes_dec_round(t3, s3, 0);

        t0 = aes_dec_round(t0, s3, 1);
        t1 = aes_dec_round(t1, s0, 1);
        t2 = aes_dec_round(t2, s1, 1);
        t3 = aes_dec_round(t3, s2, 1);

        t0 = aes_dec_round(t0, s2, 2);
        t1 = aes_dec_round(t1, s3, 2);
        t2 = aes_dec_round(t2, s0, 2);
        t3 = aes_dec_round(t3, s1, 2);

        t0 = aes_dec_round(t0, s1, 3);
        t1 = aes_dec_round(t1, s2, 3);
        t2 = aes_dec_round(t2, s3, 3);
        t3 = aes_dec_round(t3, s0, 3);

        rk += 8;
        if (--round_count == 0) {
            break;
        }

        s0 = ((volatile unsigned int*)rk)[0];
        s1 = ((volatile unsigned int*)rk)[1];
        s2 = ((volatile unsigned int*)rk)[2];
        s3 = ((volatile unsigned int*)rk)[3];

        s0 = aes_dec_round(s0, t0, 0);
        s1 = aes_dec_round(s1, t1, 0);
        s2 = aes_dec_round(s2, t2, 0);
        s3 = aes_dec_round(s3, t3, 0);

        s0 = aes_dec_round(s0, t3, 1);
        s1 = aes_dec_round(s1, t0, 1);
        s2 = aes_dec_round(s2, t1, 1);
        s3 = aes_dec_round(s3, t2, 1);

        s0 = aes_dec_round(s0, t2, 2);
        s1 = aes_dec_round(s1, t3, 2);
        s2 = aes_dec_round(s2, t0, 2);
        s3 = aes_dec_round(s3, t1, 2);

        s0 = aes_dec_round(s0, t1, 3);
        s1 = aes_dec_round(s1, t2, 3);
        s2 = aes_dec_round(s2, t3, 3);
        s3 = aes_dec_round(s3, t0, 3);
    }

    s0 = ((volatile unsigned int*)rk)[0];
    s1 = ((volatile unsigned int*)rk)[1];
    s2 = ((volatile unsigned int*)rk)[2];
    s3 = ((volatile unsigned int*)rk)[3];

    s0 = aes_dec_round_last(s0, t0, 0);
    s1 = aes_dec_round_last(s1, t1, 0);
    s2 = aes_dec_round_last(s2, t2, 0);
    s3 = aes_dec_round_last(s3, t3, 0);

    s0 = aes_dec_round_last(s0, t3, 1);
    s1 = aes_dec_round_last(s1, t0, 1);
    s2 = aes_dec_round_last(s2, t1, 1);
    s3 = aes_dec_round_last(s3, t2, 1);

    s0 = aes_dec_round_last(s0, t2, 2);
    s1 = aes_dec_round_last(s1, t3, 2);
    s2 = aes_dec_round_last(s2, t0, 2);
    s3 = aes_dec_round_last(s3, t1, 2);

    s0 = aes_dec_round_last(s0, t1, 3);
    s1 = aes_dec_round_last(s1, t2, 3);
    s2 = aes_dec_round_last(s2, t3, 3);
    s3 = aes_dec_round_last(s3, t0, 3);

    if(((int)out & 3) == 0){
        ((volatile unsigned int*)out)[0] = s0;
        ((volatile unsigned int*)out)[1] = s1;
        ((volatile unsigned int*)out)[2] = s2;
        ((volatile unsigned int*)out)[3] = s3;
    } else {
        vexriscv_aes_store_unaligned(out + 0, s0);
        vexriscv_aes_store_unaligned(out + 4, s1);
        vexriscv_aes_store_unaligned(out + 8, s2);
        vexriscv_aes_store_unaligned(out + 12, s3);
    }
}

