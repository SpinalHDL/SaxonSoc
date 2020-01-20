#ifndef CONFIG_H
#define CONFIG_H

//#define QEMU
//#define SIM
#define HARD
#ifndef OS_CALL
#define OS_CALL 0x80400000
#endif
#ifndef DTB
#define DTB     0x80FF0000
#endif

#endif
