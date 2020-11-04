DIR_BLD		:=	build/linux/amd64
DIR_OUT		:=	out/linux/amd64

include makefiles/amd64.mk
include makefiles/linux.mk
include makefiles/common.mk

all: $(DIR_OUT)/$(TARGET)
