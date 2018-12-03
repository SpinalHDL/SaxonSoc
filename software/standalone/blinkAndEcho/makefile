PROJ_NAME=blinkAndEcho
DEBUG=yes
MULDIV=no
COMPRESSED=no
STANDALONE = ..


SRCS = 	$(wildcard src/*.c) \
		$(wildcard src/*.cpp)  \
		$(wildcard src/*.S)


LDSCRIPT = ${STANDALONE}/common/linker_xip.ld

include ${STANDALONE}/common/zephyrSdkGcc.mk
include ${STANDALONE}/common/standalone.mk

