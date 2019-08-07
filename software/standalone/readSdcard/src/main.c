#include <stdint.h>
#include "stdbool.h"

#include "saxon.h"
#include "soc.h"
#include "io.h"
#include "sdcard.h"

void print_hex(uint32_t val, uint32_t digits)
{
	for (int i = (4*digits)-4; i >= 0; i -= 4)
		uart_write(UART_A, "0123456789ABCDEF"[(val >> i) % 16]);
}

void print(uint8_t * data) {
  uart_writeStr(UART_A, data);
}

void print_data(uint8_t * data, int len) {
    for (int i = 0; i < len; i++) {
        print_hex(data[i],2);
        if ((i & 0x1f) == 0x1f) print("\n");
        else if ((i & 3) == 3) print(" ");
    }
}

void printf1(char* msg, uint32_t r) {
  print(msg);
  print(" : ");
  print_hex(r, 8);
  print("\n");
}

void printf2(char* msg, uint32_t r1, uint32_t r2) {
  print(msg);
  print(" : ");
  print_hex(r1, 8);
  print(" , ");
  print_hex(r2, 8);
  print("\n");
}

void sdcard_config () {
  write_u32(500, SPI_SDCARD_SETUP);
  write_u32(500, SPI_SDCARD_HOLD);
  write_u32(500, SPI_SDCARD_DISABLE);
}

void sdcard_divider(uint32_t divider) {
  write_u32(divider, SPI_SDCARD_DIVIDER);
}

void sdcard_mode(uint32_t mode) {
  write_u32(mode, SPI_SDCARD_DATAMODE);
}

void sdcard_begin() {
  write_u32(0x11000000, SPI_SDCARD_XFER);
}

void sdcard_end() {
 write_u32(0x10000000, SPI_SDCARD_XFER);
}

uint8_t sdcard_xfer(uint8_t data) {
  write_u32(data | 0x01000000, SPI_SDCARD_XFER);

  for(int i=0;i<1000;i++) {
    uint32_t r = read_u32(SPI_SDCARD_XFER);
    if ((r & SPI_SDCARD_RX_VALID)) return r & 0xff;
  }
  return 0xFF;
}

bool sdcard_ccs_mode;

static const uint8_t sdcard_crc7_table[256] = {
	0x00, 0x12, 0x24, 0x36, 0x48, 0x5a, 0x6c, 0x7e,
	0x90, 0x82, 0xb4, 0xa6, 0xd8, 0xca, 0xfc, 0xee,
	0x32, 0x20, 0x16, 0x04, 0x7a, 0x68, 0x5e, 0x4c,
	0xa2, 0xb0, 0x86, 0x94, 0xea, 0xf8, 0xce, 0xdc,
	0x64, 0x76, 0x40, 0x52, 0x2c, 0x3e, 0x08, 0x1a,
	0xf4, 0xe6, 0xd0, 0xc2, 0xbc, 0xae, 0x98, 0x8a,
	0x56, 0x44, 0x72, 0x60, 0x1e, 0x0c, 0x3a, 0x28,
	0xc6, 0xd4, 0xe2, 0xf0, 0x8e, 0x9c, 0xaa, 0xb8,
	0xc8, 0xda, 0xec, 0xfe, 0x80, 0x92, 0xa4, 0xb6,
	0x58, 0x4a, 0x7c, 0x6e, 0x10, 0x02, 0x34, 0x26,
	0xfa, 0xe8, 0xde, 0xcc, 0xb2, 0xa0, 0x96, 0x84,
	0x6a, 0x78, 0x4e, 0x5c, 0x22, 0x30, 0x06, 0x14,
	0xac, 0xbe, 0x88, 0x9a, 0xe4, 0xf6, 0xc0, 0xd2,
	0x3c, 0x2e, 0x18, 0x0a, 0x74, 0x66, 0x50, 0x42,
	0x9e, 0x8c, 0xba, 0xa8, 0xd6, 0xc4, 0xf2, 0xe0,
	0x0e, 0x1c, 0x2a, 0x38, 0x46, 0x54, 0x62, 0x70,
	0x82, 0x90, 0xa6, 0xb4, 0xca, 0xd8, 0xee, 0xfc,
	0x12, 0x00, 0x36, 0x24, 0x5a, 0x48, 0x7e, 0x6c,
	0xb0, 0xa2, 0x94, 0x86, 0xf8, 0xea, 0xdc, 0xce,
	0x20, 0x32, 0x04, 0x16, 0x68, 0x7a, 0x4c, 0x5e,
	0xe6, 0xf4, 0xc2, 0xd0, 0xae, 0xbc, 0x8a, 0x98,
	0x76, 0x64, 0x52, 0x40, 0x3e, 0x2c, 0x1a, 0x08,
	0xd4, 0xc6, 0xf0, 0xe2, 0x9c, 0x8e, 0xb8, 0xaa,
	0x44, 0x56, 0x60, 0x72, 0x0c, 0x1e, 0x28, 0x3a,
	0x4a, 0x58, 0x6e, 0x7c, 0x02, 0x10, 0x26, 0x34,
	0xda, 0xc8, 0xfe, 0xec, 0x92, 0x80, 0xb6, 0xa4,
	0x78, 0x6a, 0x5c, 0x4e, 0x30, 0x22, 0x14, 0x06,
	0xe8, 0xfa, 0xcc, 0xde, 0xa0, 0xb2, 0x84, 0x96,
	0x2e, 0x3c, 0x0a, 0x18, 0x66, 0x74, 0x42, 0x50,
	0xbe, 0xac, 0x9a, 0x88, 0xf6, 0xe4, 0xd2, 0xc0,
	0x1c, 0x0e, 0x38, 0x2a, 0x54, 0x46, 0x70, 0x62,
	0x8c, 0x9e, 0xa8, 0xba, 0xc4, 0xd6, 0xe0, 0xf2
};

static uint8_t sdcard_crc7(uint8_t crc, uint8_t data)
{
	return sdcard_crc7_table[crc ^ data];
}

static uint16_t sdcard_crc16(uint16_t crc, uint8_t data)
{
	uint16_t x = (crc >> 8) ^ data;
	x ^= x >> 4;
	return (crc << 8) ^ (x << 12) ^ (x << 5) ^ x;
}

static uint8_t sdcard_cmd_r1(uint8_t cmd, uint32_t arg)
{
	uint8_t r1;

	sdcard_begin();

	sdcard_xfer(0x40 | cmd);
	sdcard_xfer(arg >> 24);
	sdcard_xfer(arg >> 16);
	sdcard_xfer(arg >> 8);
	sdcard_xfer(arg);

	uint8_t crc = 0;
	crc = sdcard_crc7(crc, 0x40 | cmd);
	crc = sdcard_crc7(crc, arg >> 24);
	crc = sdcard_crc7(crc, arg >> 16);
	crc = sdcard_crc7(crc, arg >> 8);
	crc = sdcard_crc7(crc, arg);
	sdcard_xfer(crc | 1);

	do {
		r1 = sdcard_xfer(0xff);
	} while (r1 == 0xff);

	sdcard_end();
	return r1;
}

static uint8_t sdcard_cmd_rw(uint8_t cmd, uint32_t arg)
{
	uint8_t r1;

	sdcard_begin();

	sdcard_xfer(0x40 | cmd);
	sdcard_xfer(arg >> 24);
	sdcard_xfer(arg >> 16);
	sdcard_xfer(arg >> 8);
	sdcard_xfer(arg);

	uint8_t crc = 0;
	crc = sdcard_crc7(crc, 0x40 | cmd);
	crc = sdcard_crc7(crc, arg >> 24);
	crc = sdcard_crc7(crc, arg >> 16);
	crc = sdcard_crc7(crc, arg >> 8);
	crc = sdcard_crc7(crc, arg);
	sdcard_xfer(crc | 1);

	do {
		r1 = sdcard_xfer(0xff);
	} while (r1 == 0xff);
	
	return r1;
}

static uint8_t sdcard_cmd_r37(uint8_t cmd, uint32_t arg, uint32_t *r37)
{
	uint8_t r1;

	sdcard_begin();

	sdcard_xfer(0x40 | cmd);
	sdcard_xfer(arg >> 24);
	sdcard_xfer(arg >> 16);
	sdcard_xfer(arg >> 8);
	sdcard_xfer(arg);

	uint8_t crc = 0;
	crc = sdcard_crc7(crc, 0x40 | cmd);
	crc = sdcard_crc7(crc, arg >> 24);
	crc = sdcard_crc7(crc, arg >> 16);
	crc = sdcard_crc7(crc, arg >> 8);
	crc = sdcard_crc7(crc, arg);
	sdcard_xfer(crc | 1);

	do {
		r1 = sdcard_xfer(0xff);
	} while (r1 == 0xff);

	for (int i = 0; i < 4; i++)
		*r37 = (*r37 << 8) | sdcard_xfer(0xff);

	sdcard_end();
	return r1;
}

static void sdcard_init()
{
	uint8_t r1;
	uint32_t r37;

	sdcard_config();
	sdcard_divider(5);
	sdcard_mode(0);

	for (int i = 0; i < 10; i++)
		sdcard_xfer(0xff);

	r1 = sdcard_cmd_r1(0, 0);

	if (r1 != 0x01) {
		printf1("Unexpected SD Card CMD0 R1: %02x\n", r1);
		while (1) { }
	}

	r1 = sdcard_cmd_r1(59, 1);

	if (r1 != 0x01) {
		printf1("Unexpected SD Card CMD59 R1: %02x\n", r1);
		while (1) { }
	}

	r1 = sdcard_cmd_r37(8, 0x1ab, &r37);
	if (r1 != 0x01 || (r37 & 0xfff) != 0x1ab) {
		printf2("Unexpected SD Card CMD8 R1 / R7: %02x %08x\n", r1, (int)r37);
		while (1) { }
	}

	r1 = sdcard_cmd_r37(58, 0, &r37);

	if (r1 != 0x01) {
		printf1("Unexpected SD Card CMD58 R1: %02x\n", r1);
		while (1) { }
	}

	if ((r37 & 0x00300000) == 0) {
		printf1("SD Card doesn't support 3.3V! OCR reg: %08x\n", (int)r37);
		while (1) { }
	}

	for (int i = 0;; i++)
	{
		// ACMD41, set HCS
		sdcard_cmd_r1(55, 0);
		r1 = sdcard_cmd_r1(41, 1 << 30);

		if (r1 == 0x00)
			break;

		if (r1 != 0x01) {
			printf1("Unexpected SD Card ACMD41 R1: %02x\n", r1);
			while (1) { }
		}

		if (i == 10000) {
			print("Timeout on SD Card ACMD41.\n");
			while (1) { }
		}
	}

	r1 = sdcard_cmd_r37(58, 0, &r37);

	if (r1 != 0x00) {
		printf1("Unexpected SD Card CMD58 R1: %02x\n", r1);
		while (1) { }
	}

	sdcard_ccs_mode = !!(r37 & (1 << 30));

	r1 = sdcard_cmd_r1(16, 512);

	if (r1 != 0x00) {
		printf1("Unexpected SD Card CMD16 R1: %02x\n", r1);
		while (1) { }
	}
}

static void sdcard_read(uint8_t *data, uint32_t blockaddr)
{
	if (!sdcard_ccs_mode)
		blockaddr <<= 9;

	uint8_t r1 = sdcard_cmd_rw(17, blockaddr);

	if (r1 != 0x00) {
		printf1("Unexpected SD Card CMD17 R1: %02x\n", r1);
		while (1) { }
	}

	while (1) {
		r1 = sdcard_xfer(0xff);
		if (r1 == 0xfe) break;
		if (r1 == 0xff) continue;
		printf1("Unexpected SD Card CMD17 data token: %02x\n", r1);
		while (1) { }
	}

	uint16_t crc = 0x0;
	for (int i = 0; i < 512; i++) {
		data[i] = sdcard_xfer(0xff);
		crc = sdcard_crc16(crc, data[i]);
	}

	crc = sdcard_crc16(crc, sdcard_xfer(0xff));
	crc = sdcard_crc16(crc, sdcard_xfer(0xff));

	if (crc != 0) {
		print("CRC Error while reading from SD Card!\n");
		while (1) { }
	}

	sdcard_end();
}

static void sdcard_write(const uint8_t *data, uint32_t blockaddr)
{
	if (!sdcard_ccs_mode)
		blockaddr <<= 9;

	uint8_t r1 = sdcard_cmd_rw(24, blockaddr);

	if (r1 != 0x00) {
		//printf("Unexpected SD Card CMD24 R1: %02x\n", r1);
		while (1) { }
	}

	sdcard_xfer(0xff);
	sdcard_xfer(0xfe);

	uint16_t crc = 0x0;
	for (int i = 0; i < 512; i++) {
		crc = sdcard_crc16(crc, data[i]);
		sdcard_xfer(data[i]);
	}

	sdcard_xfer(crc >> 8);
	sdcard_xfer(crc);

	r1 = sdcard_xfer(0xff);
	if ((r1 & 0x0f) != 0x05) {
		//printf("Unexpected SD Card CMD24 data response: %02x\n", r1);
		while (1) { }
	}

	while (sdcard_xfer(0xff) != 0xff) {
		/* waiting for sd card */
	}

	sdcard_end();
}

uint8_t buffer[512];
uint32_t cluster_begin_lba;
uint8_t sectors_per_cluster;
uint32_t fat[128];
uint32_t fat_begin_lba;

void read_files() {
    uint8_t filename[13];
    filename[8] = '.';
    filename[12] = 0;
    
    for(int i=0; buffer[i];i+=32) {
        if (buffer[i] != 0xe5) {
            if (buffer[i+11] != 0x0f) {
                for(int j=0;j<8;j++) filename[j] = buffer[i+j];
                for(int j=0;j<3;j++) filename[9+j] = buffer[i+8+j];
                uint8_t attrib = buffer[i+0x0b];
                uint16_t first_cluster_hi = *((uint16_t *) &buffer[i+0x14]);
                uint16_t first_cluster_lo = *((uint16_t *) &buffer[i+0x1a]);
                uint32_t first_cluster = (first_cluster_hi << 16) + first_cluster_lo;
                uint32_t file_size = *((uint32_t *) &buffer[i+0x1c]);
                if ((attrib & 0x1f) == 0) {
                  print(filename);
                  print(" ");
                  print_hex(first_cluster, 8);
                  print(" ");
                  print_hex(file_size, 8);
                  print("\n");
                }
            }
        }
    }

}

void main() {
    print("\nSD card contents\n\n");

    sdcard_init();

    // Read the master boot record
    sdcard_read(buffer, 0);

    print("Boot sector\n\n");

    print_data(buffer, 512);
    print("\n");

#ifdef debug
    if (buffer[510] == 0x55 && buffer[511] == 0xAA) print("MBR is valid.\n\n");
#endif

    uint8_t *part = &buffer[446];

#ifdef debug
    printf1("Boot flag: %d\n", part[0]);
    printf1("Type code: 0x%x\n", part[4]);
#endif

    uint16_t *lba_begin = (uint16_t *) &part[8];

    uint32_t part_lba_begin = lba_begin[0];

#ifdef debug
    printf1("LBA begin: %ld\n", part_lba_begin);
#endif

    // Read the volume id
    sdcard_read(buffer, part_lba_begin);

#ifdef debug
    if (buffer[510] == 0x55 && buffer[511] == 0xAA) print("Volume ID is valid\n\n");
#endif

    uint16_t num_rsvd_sectors = *((uint16_t *) &buffer[0x0e]);

#ifdef debug
    printf1("Number of reserved sectors: %d\n", num_rsvd_sectors);
#endif

    uint8_t num_fats = buffer[0x10];
    uint32_t sectors_per_fat = *((uint32_t *) &buffer[0x24]);
    sectors_per_cluster = buffer[0x0d];
    uint32_t root_dir_first_cluster = *((uint32_t *) &buffer[0x2c]);

#ifdef debug
    printf1("Number of FATs: %d\n", num_fats);
    printf1("Sectors per FAT: %ld\n", sectors_per_fat);
    printf1("Sectors per cluster: %d\n", sectors_per_cluster);
    printf1("Root dir first cluster: %ld\n", root_dir_first_cluster);
#endif

    fat_begin_lba = part_lba_begin + num_rsvd_sectors;

#ifdef debug
    printf1("fat begin lba: %ld\n", fat_begin_lba);
#endif

    // Assumes 2 FATs
    cluster_begin_lba = part_lba_begin + num_rsvd_sectors + (sectors_per_fat << 1);

#ifdef debug
    printf1("cluster begin lba: %ld\n", cluster_begin_lba);
#endif

    // Assumes 8 sectors per cluster
    uint32_t root_dir_lba = cluster_begin_lba + ((root_dir_first_cluster - 2) << 3);

#ifdef debug
    printf1("root dir lba: %ld\n", root_dir_lba);
#endif

    // Read the root directory
    sdcard_read(buffer, root_dir_lba);

    read_files();

    GPIO_A->OUTPUT_ENABLE = 0x000000FF;
    GPIO_A->OUTPUT = 0x00000000;

    uint32_t counter = 0;
    while(1){
        if(counter++ == 10000){
            GPIO_A->OUTPUT = GPIO_A->OUTPUT + 1;
            counter = 0;
        }
        while(UART_A->STATUS >> 24){ //UART RX interrupt
            UART_A->DATA = (UART_A->DATA) & 0xFF;
        }
    }
}

