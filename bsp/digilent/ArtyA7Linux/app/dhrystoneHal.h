#pragma once

#include "io.h"
#include "bsp.h"
#include "machineTimer.h"

long time(){
  return machineTimer_getTime(SYSTEM_MACHINE_TIMER_APB);
}