#ifndef CONFIG_H
#define CONFIG_H

//#define QEMU
//#define SIM
#define HARD
#ifndef OS_CALL
#define OS_CALL 0x80004000
#endif
#ifndef DTB
#define DTB     0x80074000
#endif

#endif
