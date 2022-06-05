#ifndef CONFIG_H
#define CONFIG_H

//#define QEMU
//#define SIM
#define HARD
#ifndef OS_CALL
#define OS_CALL 0x81F00000
#endif
#ifndef DTB
#define DTB     0x81EF0000
#endif

#endif
