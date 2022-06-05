SUBDIRS := $(wildcard */.)

all:
	for dir in $(SUBDIRS); do \
		(cd $$dir; ${MAKE} all); \
	done

clean:
	for dir in $(SUBDIRS); do \
		(cd $$dir; ${MAKE} clean); \
	done

.PHONY: all $(SUBDIRS)