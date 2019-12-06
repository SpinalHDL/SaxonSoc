#ifndef CONFIG_H
#define CONFIG_H

//#define QEMU
//#define SIM
#define HARD
#ifndef OS_CALL
#define OS_CALL 0x80000000
#endif
#ifndef DTB
#define DTB     0x80FFC000
#endif

#define USE_XIP

#ifndef FLASH_IMAGE_START 
#define FLASH_IMAGE_START 0x20200000
#endif

#ifndef FLASH_IMAGE_LENGTH 
#define FLASH_IMAGE_LENGTH (8 * 1024 * 1024)
#endif

#ifndef FLASH_DTB_START
#define FLASH_DTB_START 0x20FFC000
#endif

#ifndef FLASH_DTB_LENGTH
#define FLASH_DTB_LENGTH (16 * 1024)
#endif

#endif
