#pragma once

#include "type.h"
#include "soc.h"
#include "io.h"

static void console_write(u32 ch) {
    static u8 escape_mode = 0;
    static int n;
    static u8 arg[3];

    if (ch == 0x1b) {
        escape_mode = 1;
    } else if (escape_mode == 1) {
        if (ch == '[') {
            escape_mode = 2;
            arg[0] = 0;        
            n = 0;
        } else {
            escape_mode = 0;
        }
    } else if (escape_mode == 2) {
        if ((ch >= 'a' && ch <= 'z') ||
            (ch >= 'A' && ch <= 'Z')) {
            escape_mode = 0;
            if (ch == 'm') {
                u32 fore = 0;
                u32 back = 0;

                for (int i=0; i<=n; i++) {
                    u8 v = arg[i];

                    if (v >= 30 && v <= 37) {
                        fore = v - 30;
                    } else if (v >= 40 && v <= 47) {
                        back = v - 40;
                    }
                }
                write_u32((back << 3) + fore, SYSTEM_HDMI_CONSOLE_A_APB + 4);
            }
        } else if (ch == ';') {
            if (n < 2) arg[++n] = 0;
        } else if (ch >= '0' && ch<= '9') {
               arg[n] = (arg[n] * 10) + (ch - '0');
        }
    } else {
        write_u32(ch, SYSTEM_HDMI_CONSOLE_A_APB);
    }
}

