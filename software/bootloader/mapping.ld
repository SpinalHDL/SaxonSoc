/*
This is free and unencumbered software released into the public domain.

Anyone is free to copy, modify, publish, use, compile, sell, or
distribute this software, either in source code form or as a compiled
binary, for any purpose, commercial or non-commercial, and by any
means.
*/
OUTPUT_FORMAT("elf32-littleriscv", "elf32-littleriscv", "elf32-littleriscv")
OUTPUT_ARCH(riscv)
ENTRY(crtStart)

MEMORY {
  rom : ORIGIN = 0xF001E000, LENGTH = 0x00000100
  dummy : ORIGIN = 0xF001F000, LENGTH = 0x00000100
}

SECTIONS {
  .memory : {
    *(.text);
  } > rom


  .crap : {
    *(.note.gnu.build-id)
  } > dummy
}

