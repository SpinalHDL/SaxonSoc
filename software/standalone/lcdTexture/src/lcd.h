#ifndef lcd_h
#define lcd_h
#include <soc.h>

#define LCD_TEXTURE SYSTEM_LCD_APB
#define LCD_OFFSET (SYSTEM_LCD_APB + 0x4)
#define LCD_VALUE (SYSTEM_LCD_APB + 0x8)
#define LCD_DIAG (SYSTEM_LCD_APB + 0xc)

#endif
