#include <zephyr.h>
#include <misc/printk.h>
#include <device.h>
#include <irq.h>
#include <gpio.h>


/* size of stack area used by each thread */
#define STACKSIZE 1024

/* scheduling priority used by each thread */
#define PRIORITY 7


#define GPIOA_PORT "gpioA"

#define SWITCH_PORT GPIOA_PORT
#define SWITCH_PIN 0
#define SWITCH_IRQ  11+2
#define SWITCH_PRIO  2

#define LED_PORT GPIOA_PORT
#define LED_PIN	1

void switch_isr(void *arg)
{
   static u32_t count = 0;
   printk("INTERRUPT %d\n", count++);
}


void printer_thread(void)
{
    struct device *dev;
	dev = device_get_binding(LED_PORT);

    u32_t count = 0;
	while (1) {
		printk("Miaou %d\n", count++);
		gpio_pin_write(dev, LED_PIN, count % 2);
		k_sleep(50);
	}
}

void init(void)
{
    struct device *dev;

    printk("Init start %s\n", CONFIG_BOARD);

    //Init switch interrupt
    dev = device_get_binding(SWITCH_PORT);
    gpio_pin_configure(dev, SWITCH_PIN, GPIO_DIR_IN | GPIO_INT | GPIO_INT_EDGE | GPIO_INT_ACTIVE_HIGH);
    IRQ_CONNECT(SWITCH_IRQ, SWITCH_PRIO, switch_isr, NULL, 0);
    irq_enable(SWITCH_IRQ);

    //Init led output
    dev = device_get_binding(LED_PORT);
    gpio_pin_configure(dev, LED_PIN, GPIO_DIR_OUT);

    printk("Init done %s\n", CONFIG_BOARD);
}


void main(void)
{
    init();
}


K_THREAD_DEFINE(printerThread_id, STACKSIZE, printer_thread, NULL, NULL, NULL, PRIORITY, 0, K_NO_WAIT);



