DIR_SRC		:=	../java
DIR_BLD		:=	build/javaGarbage
DIR_INC		:=	build/headers
DIR_JDK		:=	/usr/lib/jvm/jdk-15

FILES_J		:=	$(DIR_SRC)/net/gudenau/minecraft/fps/cpu/CpuNatives.java \
				$(DIR_SRC)/net/gudenau/minecraft/fps/cpu/SimdNatives.java

JAVAC		:=	$(DIR_JDK)/bin/javac
MKDIR		:=	mkdir -p

all:
	$(MKDIR) $(DIR_BLD) $(DIR_INC)
	$(JAVAC) -d $(DIR_BLD) -h $(DIR_INC) $(FILES_J)

clean:
