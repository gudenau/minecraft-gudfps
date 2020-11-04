DIR_SRC		:=	source

FILES_CXX	:=	$(wildcard $(DIR_SRC)/*.cpp)
FILES_O		:=	$(patsubst $(DIR_SRC)/%.cpp,$(DIR_BLD)/%.o,$(FILES_CXX))

FLAGS_CXX	:=	$(FLAGS_PCXX) \
				-Wall -Wextra -Werror \
				-c -fPIC \
				$(addprefix -I,$(INCLUDES))
FLAGS_LD	:=	$(FLAGS_PLD) \
				-Wall -Wextra -Werror \
				-shared

CXX			:=	$(PREFIX)gcc
LD			:=	$(PREFIX)gcc

$(DIR_OUT)/$(TARGET): $(FILES_O)
	@$(MKDIR) $(@D)
	$(LD) -o $@ $^ $(FLAGS_LD)

$(DIR_BLD)/%.o: $(DIR_SRC)/%.cpp
	@$(MKDIR) $(@D)
	$(CXX) $(FLAGS_CXX) -o $@ $^

clean:
	$(RM) $(FILES_O) $(DIR_OUT)/$(TARGET)
