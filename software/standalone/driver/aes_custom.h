#pragma once

#include "riscv.h"

#define aes_enc_round(rs1, rs2, sel) opcode_R(CUSTOM1, 0x00, (sel << 3), rs1, rs2)
#define aes_enc_round_last(rs1, rs2, sel) opcode_R(CUSTOM1, 0x00, (sel << 3) | 2, rs1, rs2)

#define aes_dec_round(rs1, rs2, sel) opcode_R(CUSTOM1, 0x00, (sel << 3) | 1, rs1, rs2)
#define aes_dec_round_last(rs1, rs2, sel) opcode_R(CUSTOM1, 0x00, (sel << 3) | 2 | 1, rs1, rs2)