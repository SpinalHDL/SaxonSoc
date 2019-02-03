/*
 * Copyright (c) 2012-2014 Wind River Systems, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include <zephyr.h>
#include <misc/printk.h>
#include <irq.h>
#include <device.h>
#include <gpio.h>


/* size of stack area used by each thread */
#define STACKSIZE 1024

/* scheduling priority used by each thread */
#define PRIORITY 7


#define MY_DEV_IRQ  11+2       /* device uses IRQ 24 */
#define MY_DEV_PRIO  2       /* device uses interrupt priority 2 */
/* argument passed to my_isr(), in this case a pointer to the device */

#define MY_IRQ_FLAGS 0       /* IRQ flags. Unused on non-x86 */

#define LED_PORT "gpioA"
#define LED	1
#define SWITCH 0

void my_isr(void *arg)
{
   static u32_t count = 0;
   printk("INTERRUPT %d\n", count++);
}

void my_isr_installer(void)
{
    struct device *dev;

    dev = device_get_binding(LED_PORT);

    gpio_pin_configure(dev, SWITCH, GPIO_DIR_IN | GPIO_INT | GPIO_INT_EDGE | GPIO_INT_ACTIVE_HIGH);
    IRQ_CONNECT(MY_DEV_IRQ, MY_DEV_PRIO, my_isr, NULL, 0);
    irq_enable(MY_DEV_IRQ);            /* enable IRQ */
}





void uart_out(void)
{
    struct device *dev;

	dev = device_get_binding(LED_PORT);
	/* Set LED pin as output */
	gpio_pin_configure(dev, LED, GPIO_DIR_OUT);

    u32_t count = 0;
	while (1) {
		printk("Miaou %d\n", count++);
		gpio_pin_write(dev, LED, count % 2);
		k_sleep(50);
	}
}








void main(void)
{
	printk("Hello World! %s\n", CONFIG_BOARD);
    my_isr_installer();
}





K_THREAD_DEFINE(uart_out_id, STACKSIZE, uart_out, NULL, NULL, NULL,
		PRIORITY, 0, K_NO_WAIT);