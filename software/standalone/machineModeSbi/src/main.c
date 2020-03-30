#include <stdint.h>
#include "riscv.h"
#include "machineModeSbiConfig.h"
#include "hal.h"

extern const uint32_t _sp;
extern void trapEntry();
extern void emulationTrap();


void putHex(int value){
    for(int i = 7; i >=     0;i--){
        int hex = (value >> i*4) & 0xF;
        putC(hex > 9 ? 'A' + hex - 10 : '0' + hex);
    }
}

void putString(char* s){
    while(*s){
        putC(*s);
        s++;
    }
}

#define GETC_BUFFER_SIZE 1024
char getC_buffer[GETC_BUFFER_SIZE];
int32_t getC_buffer_wptr = 0;
int32_t getC_buffer_rptr = 0;

void getC_buffer_update(){
    while(1){
        int32_t ch = getC();
        if(ch == -1) break;
        int32_t wptr_next = (getC_buffer_wptr+1) % GETC_BUFFER_SIZE;
        if(wptr_next == getC_buffer_rptr) break;
        getC_buffer[getC_buffer_wptr] = ch;
        getC_buffer_wptr = wptr_next;
    }
}

int getC_buffer_read(){
    if(getC_buffer_wptr == getC_buffer_rptr) return -1;
    int ch = getC_buffer[getC_buffer_rptr];
    getC_buffer_rptr = (getC_buffer_rptr+1) % GETC_BUFFER_SIZE;
    return ch;
}

//Affect mtvec
void setup_pmp(void)
{
  // Set up a PMP to permit access to all of memory.
  // Ignore the illegal-instruction trap if PMPs aren't supported.
  uintptr_t pmpc = PMP_NAPOT | PMP_R | PMP_W | PMP_X;
  asm volatile ("la t0, 1f\n\t"
                "csrw mtvec, t0\n\t"
                "csrw pmpaddr0, %1\n\t"
                "csrw pmpcfg0, %0\n\t"
                ".align 2\n\t"
                "1:"
                : : "r" (pmpc), "r" (-1UL) : "t0");
}

void init() {
    setup_pmp();
    halInit();
    putString("*** VexRiscv BIOS ***\n");
    uint32_t sp = (uint32_t) (&_sp);
    csr_write(mtvec, trapEntry);
    csr_write(mscratch, sp -32*4);
    csr_write(mstatus, 0x0800 | MSTATUS_MPIE);
    csr_write(mie, 0);
    csr_write(mepc, OS_CALL);
    //In future it would probably need to manage missaligned stuff, now it will stop the simulation
    csr_write(medeleg, MEDELEG_INSTRUCTION_PAGE_FAULT | MEDELEG_LOAD_PAGE_FAULT | MEDELEG_STORE_PAGE_FAULT | MEDELEG_USER_ENVIRONNEMENT_CALL);
    csr_write(mideleg, MIDELEG_SUPERVISOR_TIMER | MIDELEG_SUPERVISOR_EXTERNAL | MIDELEG_SUPERVISOR_SOFTWARE);
    csr_write(sbadaddr, 0); //Used to avoid simulation missmatch

    putString("*** Supervisor ***\n");
}

int readRegister(uint32_t id){
    unsigned int sp = (unsigned int) (&_sp);
    return ((int*) sp)[id-32];
}
void writeRegister(uint32_t id, int value){
    uint32_t sp = (uint32_t) (&_sp);
    ((uint32_t*) sp)[id-32] = value;
}


//Currently, this should not happen, unless kernel things are going wrong
void redirectTrap(){
    stopSim();
    csr_write(sbadaddr, csr_read(mbadaddr));
    csr_write(sepc,     csr_read(mepc));
    csr_write(scause,   csr_read(mcause));
    csr_write(mepc,        csr_read(stvec));
}

void emulationTrapToSupervisorTrap(uint32_t sepc, uint32_t mstatus){
    csr_write(mtvec, trapEntry);
    csr_write(sbadaddr, csr_read(mbadaddr));
    csr_write(scause, csr_read(mcause));
    csr_write(sepc, sepc);
    csr_write(mepc,    csr_read(stvec));
    csr_write(mstatus,
              (mstatus & ~(MSTATUS_SPP | MSTATUS_MPP | MSTATUS_SIE | MSTATUS_SPIE))
            | ((mstatus >> 3) & MSTATUS_SPP)
            | (0x0800 | MSTATUS_MPIE)
            | ((mstatus & MSTATUS_SIE) << 4)
    );
}

#define max(a,b) \
  ({ __typeof__ (a) _a = (a); \
      __typeof__ (b) _b = (b); \
    _a > _b ? _a : _b; })


#define min(a,b) \
  ({ __typeof__ (a) _a = (a); \
      __typeof__ (b) _b = (b); \
    _a < _b ? _a : _b; })

#define trapReadyStart \
        "      li       %[tmp],  0x00020000\n" \
        "    csrs     mstatus,  %[tmp]\n" \
        "      la       %[tmp],  1f\n" \
        "    csrw     mtvec,  %[tmp]\n" \
        "    li       %[fail], 1\n" \

#define trapReadyEnd \
        "    li       %[fail], 0\n" \
        "1:\n" \
        "      li       %[tmp],  0x00020000\n" \
        "    csrc     mstatus,  %[tmp]\n" \

//Will modify MTVEC
int32_t readWord(uint32_t address, int32_t *data){
    int32_t result, tmp;
    int32_t fail;
    __asm__ __volatile__ (
        trapReadyStart
        "    lw       %[result], 0(%[address])\n"
        trapReadyEnd
        : [result]"=&r" (result), [fail]"=&r" (fail), [tmp]"=&r" (tmp)
        : [address]"r" (address)
        : "memory"
    );

    *data = result;
    return fail;
}

//Will modify MTVEC
int32_t readWordUnaligned(uint32_t address, int32_t *data){
    int32_t result, tmp;
    int32_t fail;
    __asm__ __volatile__ (
            trapReadyStart
        "    lbu      %[result], 0(%[address])\n"
        "    lbu      %[tmp],    1(%[address])\n"
        "    slli     %[tmp],  %[tmp], 8\n"
        "    or       %[result], %[result], %[tmp]\n"
        "    lbu      %[tmp],    2(%[address])\n"
        "    slli     %[tmp],  %[tmp], 16\n"
        "    or       %[result], %[result], %[tmp]\n"
        "    lbu      %[tmp],    3(%[address])\n"
        "    slli     %[tmp],  %[tmp], 24\n"
        "    or       %[result], %[result], %[tmp]\n"
        trapReadyEnd
        : [result]"=&r" (result), [fail]"=&r" (fail), [tmp]"=&r" (tmp)
        : [address]"r" (address)
        : "memory"
    );

    *data = result;
    return fail;
}

//Will modify MTVEC
int32_t readHalfUnaligned(uint32_t address, int32_t *data){
    int32_t result, tmp;
    int32_t fail;
    __asm__ __volatile__ (
        trapReadyStart
        "    lb       %[result], 1(%[address])\n"
        "    slli     %[result],  %[result], 8\n"
        "    lbu      %[tmp],    0(%[address])\n"
        "    or       %[result], %[result], %[tmp]\n"
        trapReadyEnd
        : [result]"=&r" (result), [fail]"=&r" (fail), [tmp]"=&r" (tmp)
        : [address]"r" (address)
        : "memory"
    );

    *data = result;
    return fail;
}





//Will modify MTVEC
int32_t writeWord(uint32_t address, int32_t data){
    int32_t tmp;
    int32_t fail;
    __asm__ __volatile__ (
        trapReadyStart
        "    sw       %[data], 0(%[address])\n"
        trapReadyEnd
        : [fail]"=&r" (fail), [tmp]"=&r" (tmp)
        : [address]"r" (address), [data]"r" (data)
        : "memory"
    );

    return fail;
}


//Will modify MTVEC
int32_t writeWordUnaligned(uint32_t address, int32_t data){
    int32_t tmp;
    int32_t fail;
    __asm__ __volatile__ (
        trapReadyStart
        "    sb       %[data], 0(%[address])\n"
        "    srl      %[data], %[data], 8\n"
        "    sb       %[data], 1(%[address])\n"
        "    srl      %[data], %[data], 8\n"
        "    sb       %[data], 2(%[address])\n"
        "    srl      %[data], %[data], 8\n"
        "    sb       %[data], 3(%[address])\n"
        trapReadyEnd
        : [fail]"=&r" (fail), [tmp]"=&r" (tmp)
        : [address]"r" (address), [data]"r" (data)
        : "memory"
    );

    return fail;
}


//Will modify MTVEC
int32_t writeShortUnaligned(uint32_t address, int32_t data){
    int32_t tmp;
    int32_t fail;
    __asm__ __volatile__ (
        trapReadyStart
        "    sb       %[data], 0(%[address])\n"
        "    srl      %[data], %[data], 8\n"
        "    sb       %[data], 1(%[address])\n"
        trapReadyEnd
        : [fail]"=&r" (fail), [tmp]"=&r" (tmp)
        : [address]"r" (address), [data]"r" (data)
        : "memory"
    );

    return fail;
}



uint32_t readInstruction(uint32_t pc){
    uint32_t i;
    if (pc & 2) {
        readWord(pc - 2, &i);
        i >>= 16;
        if ((i & 3) == 3) {
            uint32_t u32Buf;
            readWord(pc+2, &u32Buf);
            i |= u32Buf << 16;
        }
    } else {
        readWord(pc, &i);
    }
    return i;
}


void trap(){
    int32_t cause = csr_read(mcause);
    if(cause < 0){ //interrupt
        switch(cause & 0xFF){
        case CAUSE_MACHINE_TIMER:{
            csr_set(sip, MIP_STIP);
            csr_clear(mie, MIE_MTIE);
            getC_buffer_update();
        }break;
        default: redirectTrap(); break;
        }
    } else { //exception
        switch(cause){
        case CAUSE_UNALIGNED_LOAD:{
            uint32_t mepc = csr_read(mepc);
            uint32_t mstatus = csr_read(mstatus);
            uint32_t instruction = readInstruction(mepc);
            uint32_t address = csr_read(mbadaddr);
            uint32_t func3 =(instruction >> 12) & 0x7;
            uint32_t rd = (instruction >> 7) & 0x1F;
            int32_t readValue;
            int32_t fail;

            switch(func3){
            case 1: fail = readHalfUnaligned(address, &readValue); break;  //LH
            case 2: fail = readWordUnaligned(address, &readValue); break; //LW
            case 5: fail = readHalfUnaligned(address, &readValue) & 0xFFFF; break; //LHU
            }

            if(fail){
                emulationTrapToSupervisorTrap(mepc, mstatus);
                return;
            }

            writeRegister(rd, readValue);
            csr_write(mepc, mepc + 4);
            csr_write(mtvec, trapEntry); //Restore mtvec
        }break;
        case CAUSE_UNALIGNED_STORE:{
            uint32_t mepc = csr_read(mepc);
            uint32_t mstatus = csr_read(mstatus);
            uint32_t instruction = readInstruction(mepc);
            uint32_t address = csr_read(mbadaddr);
            uint32_t func3 =(instruction >> 12) & 0x7;
            int32_t writeValue = readRegister((instruction >> 20) & 0x1F);
            int32_t fail;

            switch(func3){
            case 1: fail = writeShortUnaligned(address, writeValue); break; //SH
            case 2: fail = writeWordUnaligned(address, writeValue); break; //SW
            }

            if(fail){
                emulationTrapToSupervisorTrap(mepc, mstatus);
                return;
            }

            csr_write(mepc, mepc + 4);
            csr_write(mtvec, trapEntry); //Restore mtvec
        }break;
        case CAUSE_ILLEGAL_INSTRUCTION:{
            uint32_t mepc = csr_read(mepc);
            uint32_t mstatus = csr_read(mstatus);
#ifdef SIM
            uint32_t instruction = csr_read(mbadaddr);
#endif
#ifdef HARD
            uint32_t instruction = csr_read(mbadaddr);
#endif
#ifdef QEMU
            uint32_t instruction = readInstruction(mepc);
            csr_write(mtvec, trapEntry); //Restore mtvec
#endif

            uint32_t opcode = instruction & 0x7F;
            uint32_t funct3 = (instruction >> 12) & 0x7;
            switch(opcode){
            case 0x2F: //Atomic
                switch(funct3){
                case 0x2:{
                    uint32_t sel = instruction >> 27;
                    uint32_t addr = readRegister((instruction >> 15) & 0x1F);
                    int32_t  src = readRegister((instruction >> 20) & 0x1F);
                    uint32_t rd = (instruction >> 7) & 0x1F;
                    int32_t readValue;
                    if(readWord(addr, &readValue)){
                        emulationTrapToSupervisorTrap(mepc, mstatus);
                        return;
                    }
                    int writeValue;
                    switch(sel){
                    case 0x0:  writeValue = src + readValue; break;
                    case 0x1:  writeValue = src; break;
//LR SC done in hardware (cheap), and require to keep track of context switches
//                    case 0x2:{ //LR
//                    }break;
//                    case 0x3:{ //SC
//                    }break;
                    case 0x4:  writeValue = src ^ readValue; break;
                    case 0xC:  writeValue = src & readValue; break;
                    case 0x8:  writeValue = src | readValue; break;
                    case 0x10: writeValue = min(src, readValue); break;
                    case 0x14: writeValue = max(src, readValue); break;
                    case 0x18: writeValue = min((unsigned int)src, (unsigned int)readValue); break;
                    case 0x1C: writeValue = max((unsigned int)src, (unsigned int)readValue); break;
                    default: redirectTrap(); return; break;
                    }
                    if(writeWord(addr, writeValue)){
                        emulationTrapToSupervisorTrap(mepc, mstatus);
                        return;
                    }
                    writeRegister(rd, readValue);
                    csr_write(mepc, mepc + 4);
                    csr_write(mtvec, trapEntry); //Restore mtvec
                }break;
                default: redirectTrap(); break;
                } break;
                case 0x73:{
                    //CSR
                    uint32_t input = (instruction & 0x4000) ? ((instruction >> 15) & 0x1F) : readRegister((instruction >> 15) & 0x1F);
                    uint32_t clear, set;
                    uint32_t write;
                    switch (funct3 & 0x3) {
                    case 0: redirectTrap(); break;
                    case 1: clear = ~0; set = input; write = 1; break;
                    case 2: clear = 0; set = input; write = ((instruction >> 15) & 0x1F) != 0; break;
                    case 3: clear = input; set = 0; write = ((instruction >> 15) & 0x1F) != 0; break;
                    }
                    uint32_t csrAddress = instruction >> 20;
                    uint32_t old;
                    switch(csrAddress){
                    case RDCYCLE :
                    case RDINSTRET:
                    case RDTIME  : old = rdtime(); break;
                    case RDCYCLEH :
                    case RDINSTRETH:
                    case RDTIMEH : old = rdtimeh(); break;
                    default: redirectTrap(); break;
                    }
                    if(write) {
                        uint32_t newValue = (old & ~clear) | set;
                        switch(csrAddress){
                        default: redirectTrap(); break;
                        }
                    }

                    writeRegister((instruction >> 7) & 0x1F, old);
                    csr_write(mepc, mepc + 4);

                }break;
                default: redirectTrap();  break;
            }
        }break;
        case CAUSE_SCALL:{
            uint32_t which = readRegister(17);
            uint32_t a0 = readRegister(10);
            uint32_t a1 = readRegister(11);
            uint32_t a2 = readRegister(12);
            switch(which){
            case SBI_CONSOLE_PUTCHAR:{
                putC(a0);
                csr_write(mepc, csr_read(mepc) + 4);
            }break;
            case SBI_CONSOLE_GETCHAR:{
                getC_buffer_update();
                writeRegister(10, getC_buffer_read());
                csr_write(mepc, csr_read(mepc) + 4);
            }break;
            case SBI_SET_TIMER:{
                setMachineTimerCmp(a0, a1);
                csr_set(mie, MIE_MTIE);
                csr_clear(sip, MIP_STIP);
                csr_write(mepc, csr_read(mepc) + 4);
            }break;
            default: {
                putString("Unknown SABI\n");
                stopSim();
            } break;
            }
        }break;
        default: {
            putString("Unknown exception ");
            putHex(cause);
            putString("\nPC=");
            putHex(csr_read(mepc));
            putString("\nMTVAL=");
            putHex(csr_read(mbadaddr));
            putString("\n");
            redirectTrap();
        } break;
        }
    }

}
