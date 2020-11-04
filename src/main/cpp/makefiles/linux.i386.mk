DIR_BLD		:=	build/linux/i386
DIR_OUT		:=	out/linux/i386

include makefiles/i386.mk
include makefiles/linux.mk
include makefiles/common.mk

all: $(DIR_OUT)/$(TARGET)
