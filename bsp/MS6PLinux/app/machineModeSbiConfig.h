#ifndef CONFIG_H
#define CONFIG_H

//#define QEMU
//#define SIM
#define HARD
#ifndef OS_CALL
#define OS_CALL 0x80010000
#endif
#ifndef DTB
#define DTB     0x8007F000
#endif

#endif
