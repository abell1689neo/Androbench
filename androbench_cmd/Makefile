CC			= gcc
ACC			= arm-linux-gnueabi-gcc
CFLAGS	= -g
RM			= rm -f
EXECFILE= andro_cui
LBITS := $(shell getconf LONG_BIT)
ifeq ($(LBITS),64)
   # do 64 bit stuff here, like set some CFLAGS
	 LIBPATH	= -liniparser_64
else
	 # do 32 bit stuff here
	 LIBPATH	= -liniparser_32
endif

all:
	$(CC) $(CFLAGS) -o postermark main.c interface_JNI.c  jni.c -L. $(LIBPATH)

arm:
		$(ACC) $(CFLAGS) -static -o postermark main.c interface_JNI.c  jni.c -L. -liniparser_arm 

clean:
	$(RM) postermark
