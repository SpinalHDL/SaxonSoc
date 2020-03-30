#include <stdint.h>

#include "bsp.h"
#include "plic.h"
#include "i2c.h"
#include "i2cDemo.h" //From BSP
#include "riscv.h"


/*
 * This demo use the I2C peripheral to communicate with the I2C simulation model *
 */

void init();
void main();
void trap();
void crash();
void trap_entry();
void externalInterrupt();
void externalInterrupt_i2c();


void init(){
    //I2C init
    I2c_Config i2c;
    i2c.samplingClockDivider = 3;
    i2c.timeout = BSP_CORE_HZ/1000;    //1 ms;
    i2c.tsuDat  = BSP_CORE_HZ/2000000; //500 ns

    i2c.tLow  = BSP_CORE_HZ/800000;  //1.25 us
    i2c.tHigh = BSP_CORE_HZ/800000; //1.25 us
    i2c.tBuf  = BSP_CORE_HZ/400000;  //2.5 us

    i2c_applyConfig(I2C_DEMO, &i2c);
    i2c_setFilterConfig(I2C_DEMO, 0, 0x30 | I2C_FILTER_7_BITS | I2C_FILTER_ENABLE); //0x30 => Address byte = 0x60 | 0x61
    i2c_enableInterrupt(I2C_DEMO, I2C_INTERRUPT_FILTER | I2C_INTERRUPT_DROP);

    //configure PLIC
    plic_set_threshold(BSP_PLIC, BSP_PLIC_CPU_0, 0); //cpu 0 accept all interrupts with priority above 0

    //enable PLIC I2C interrupts
    plic_set_enable(BSP_PLIC, BSP_PLIC_CPU_0, PLIC_I2C_INTERRUPT, 1);
    plic_set_priority(BSP_PLIC, PLIC_I2C_INTERRUPT, 1);

    //configure RISC-V interrupt CSR
    csr_write(mtvec, trap_entry); //Set the machine trap vector (trap.S)
    csr_write(mie, MIE_MEIE); //Enable machine external interrupts
    csr_write(mstatus, MSTATUS_MPP | MSTATUS_MIE); //Enable interrupts
}

void print_hex_digit(uint8_t digit){
    uart_write(BSP_UART_TERMINAL, digit < 10 ? '0' + digit : 'A' + digit - 10);
}


void print_hex_byte(uint8_t byte){
    print_hex_digit(byte >> 4);
    print_hex_digit(byte & 0x0F);
}

void assert(int cond){
    if(!cond) {
        uart_writeStr(BSP_UART_TERMINAL, "Assert failure\n");
        while(1);
    }
}

uint32_t phase = 0;
void main() {
    init();

    // I2C write blocking 0x42 -> [0x95 0x64]
    i2c_masterStartBlocking(I2C_DEMO);
    i2c_txByte(I2C_DEMO, 0x42);
    i2c_txNackBlocking(I2C_DEMO);
    assert(i2c_rxAck(I2C_DEMO)); // Optional check
    i2c_txByte(I2C_DEMO, 0x95);
    i2c_txNackBlocking(I2C_DEMO);
    assert(i2c_rxAck(I2C_DEMO)); // Optional check
    i2c_txByte(I2C_DEMO, 0x64);
    i2c_txNackBlocking(I2C_DEMO);
    assert(i2c_rxNack(I2C_DEMO)); // Optional check
    i2c_masterStopBlocking(I2C_DEMO);

    // I2C read blocking 0x86 -> [0xA8 0xE4]
    i2c_masterStartBlocking(I2C_DEMO);
    i2c_txByte(I2C_DEMO, 0x86);
    i2c_txNackBlocking(I2C_DEMO);
    assert(i2c_rxAck(I2C_DEMO)); // Optional check
    i2c_txByte(I2C_DEMO, 0xFF);
    i2c_txAckBlocking(I2C_DEMO);
    assert(i2c_rxData(I2C_DEMO) == 0xA8); // Expected value
    assert(i2c_rxAck(I2C_DEMO)); // Optional check
    i2c_txByte(I2C_DEMO, 0xFF);
    i2c_txNackBlocking(I2C_DEMO);
    assert(i2c_rxData(I2C_DEMO) == 0xE4); // Expected value
    assert(i2c_rxNack(I2C_DEMO)); // Optional check
    i2c_masterStopBlocking(I2C_DEMO);



    // I2C write blocking on 0xEE to ask the simulation model to send us two frame which will be handled by the interrupts (0x61, 0x60)
    i2c_masterStartBlocking(I2C_DEMO);
    i2c_txByte(I2C_DEMO, 0xEE);
    i2c_txNackBlocking(I2C_DEMO);
    assert(i2c_rxAck(I2C_DEMO)); // Optional check
    i2c_masterStopBlocking(I2C_DEMO);


    while(phase == 0); //Blocking wait before continueing the demo

    // I2C master frame managed in interrupts (0x42)
    i2c_enableInterrupt(I2C_DEMO, I2C_INTERRUPT_CLOCK_GEN_BUSY);
    i2c_masterStart(I2C_DEMO);

    while(phase == 1); //Blocking wait before continueing the demo

    // I2C master frame managed in interrupts (0x42)
    i2c_enableInterrupt(I2C_DEMO, I2C_INTERRUPT_CLOCK_GEN_BUSY);
    i2c_masterStart(I2C_DEMO);

    while(phase == 2); //Blocking wait before continueing the demo

    uart_writeStr(BSP_UART_TERMINAL, "Done\n");

}



//Called by trap_entry on both exceptions and interrupts events
void trap(){
    int32_t mcause = csr_read(mcause);
    int32_t interrupt = mcause < 0;    //Interrupt if set, exception if cleared
    int32_t cause     = mcause & 0xF;
    if(interrupt){
        switch(cause){
        case CAUSE_MACHINE_EXTERNAL: externalInterrupt(); break;
        default: crash(); break;
        }
    } else {
        crash();
    }
}


void externalInterrupt(){
    uint32_t claim;
    //While there is pending interrupts
    while(claim = plic_claim(BSP_PLIC, BSP_PLIC_CPU_0)){
        switch(claim){
        case PLIC_I2C_INTERRUPT: externalInterrupt_i2c(); break;
        default: crash(); break;
        }
        plic_release(BSP_PLIC, BSP_PLIC_CPU_0, claim); //unmask the claimed interrupt
    }
}

void crash(){
    uart_writeStr(BSP_UART_TERMINAL, "\n*** CRASH ***\n");
    while(1);
}


//I2C interrupt state
//The name of state is xAA_BBB_C where
//- AA is the address byte value
//- BBB is after which event we are (DATA / ACK)
//- C the occurence count of BBB
enum {
    IDLE,
    x61_DATA_2, x61_DATA_3, //I2C frame read for us
    x60_DATA_2, x60_DATA_3, //I2C frame write for us
    x42_ACK_1, x42_ACK_2, x42_ACK_3 //I2C write frame from us
} state = IDLE;


//I2C_DEMO handler to manage master/slave frames
void externalInterrupt_i2c(){
    if(gpio_getInterruptFlag(I2C_DEMO) & I2C_INTERRUPT_DROP){ //Frame drop detected
        state = IDLE;
        i2c_disableInterrupt(I2C_DEMO, I2C_INTERRUPT_CLOCK_GEN_BUSY | I2C_INTERRUPT_TX_ACK | I2C_INTERRUPT_TX_DATA);
        if(gpio_getMasterStatus(I2C_DEMO) & I2C_MASTER_BUSY) i2c_masterDrop(I2C_DEMO);
        return;
    }

    switch(state){
    case IDLE:
        if(gpio_getFilteringHit(I2C_DEMO) == 1){ //I2C filter 0 hit => frame for us
            if(gpio_getFilteringStatus(I2C_DEMO) == 1){ //read (0x61)
                i2c_txAck(I2C_DEMO);
                i2c_txByte(I2C_DEMO, 0x9A);
                i2c_enableInterrupt(I2C_DEMO, I2C_INTERRUPT_TX_DATA); //Interrupt when the tx data buffer is empty again
                state = x61_DATA_2;
            } else {  //write (0x60)
                i2c_txAck(I2C_DEMO);
                i2c_txByte(I2C_DEMO, 0xFF);
                i2c_enableInterrupt(I2C_DEMO, I2C_INTERRUPT_TX_DATA); //Interrupt when the tx data buffer is empty again
                state = x60_DATA_2;
            }
            i2c_clearInterruptFlag(I2C_DEMO, I2C_INTERRUPT_FILTER);
        } else if(gpio_getInterruptFlag(I2C_DEMO) & I2C_INTERRUPT_CLOCK_GEN_BUSY){ //We sucessfuly emited a i2C START sequance
            //Write the address
            i2c_txByte(I2C_DEMO, 0x42);
            i2c_txNack(I2C_DEMO);

            i2c_disableInterrupt(I2C_DEMO, I2C_INTERRUPT_CLOCK_GEN_BUSY);
            i2c_enableInterrupt(I2C_DEMO, I2C_INTERRUPT_TX_ACK); //Interrupt when the tx ack buffer is empty again
            state = x42_ACK_1;
        }else {
            crash();
        }
        break;


    //Write frame from us
    case x42_ACK_1:
        if(i2c_rxData(I2C_DEMO) != 0x42){ //Another master used a higher priority address at the same time
            i2c_masterDrop(I2C_DEMO);
            i2c_disableInterrupt(I2C_DEMO, I2C_INTERRUPT_TX_ACK);
            state = IDLE;
        } else if(i2c_rxNack(I2C_DEMO)){ //No slave ACK the address byte
            i2c_masterStop(I2C_DEMO);
            i2c_disableInterrupt(I2C_DEMO, I2C_INTERRUPT_TX_ACK);
            state = IDLE;
        } else { //Everything is ok
            //Write the first data byte
            i2c_txByte(I2C_DEMO, 0x95); //Write 0x95 as the first data byte of the frame
            i2c_txNack(I2C_DEMO);       //Let's the slave ACK that byte

            //If that frame was a read, instead of the above, do :
            //i2c_txByte(I2C_DEMO, 0xFF); //Let's the slave  give its data.
            //i2c_txAck(I2C_DEMO);        //Ack the byte

            state = x42_ACK_2;
        }
        break;
    case x42_ACK_2:
        assert(i2c_rxAck(I2C_DEMO)); // Expected value

        //Write the second data byte
        i2c_txByte(I2C_DEMO, 0x64);
        i2c_txNack(I2C_DEMO);

        //If that frame was a read, instead of the above, do :
        //dataByte[0] = i2c_rxData(I2C_DEMO);
        //i2c_txByte(I2C_DEMO, 0xFF); //Let's the slave give its data.
        //i2c_txNack(I2C_DEMO);        //Nack the byte to stop the transfer

        state = x42_ACK_3;
        break;
    case x42_ACK_3:
        assert(i2c_rxNack(I2C_DEMO)); // Expected value

        //If that frame was a read, instead of the above, do :
        //dataByte[1] = i2c_rxData(I2C_DEMO);

        i2c_masterStop(I2C_DEMO);
        i2c_disableInterrupt(I2C_DEMO, I2C_INTERRUPT_TX_ACK);
        state = IDLE;

        phase++; //Used to let's the demo continue
        break;


    //Write frame to us
    case x60_DATA_2:
        i2c_txAck(I2C_DEMO);
        i2c_txByte(I2C_DEMO, 0xFF);
        assert(i2c_rxData(I2C_DEMO) == 0x33); // Expected value
        state = x60_DATA_3;
        break;
    case x60_DATA_3:
        //End of the frame, do not interfere with it anymore
        i2c_txNackRepeat(I2C_DEMO);
        i2c_txByteRepeat(I2C_DEMO, 0xFF);
        assert(i2c_rxData(I2C_DEMO) == 0x48); // Expected value
        i2c_disableInterrupt(I2C_DEMO, I2C_INTERRUPT_TX_DATA);
        state = IDLE;
        break;


    //Read frame to us
    case x61_DATA_2:
        i2c_txNack(I2C_DEMO);
        i2c_txByte(I2C_DEMO, 0x7E);
        assert(i2c_rxAck(I2C_DEMO)); // Expected value
        state = x61_DATA_3;
        break;
    case x61_DATA_3:
        //End of the frame, do not interfere with it anymore
        i2c_txNackRepeat(I2C_DEMO);
        i2c_txByteRepeat(I2C_DEMO, 0xFF);
        i2c_disableInterrupt(I2C_DEMO, I2C_INTERRUPT_TX_DATA);
        state = IDLE;

        phase++; //Used to let's the demo continue
        break;
    }
}

//8

