import jpype
from jpype import *
import jpype.imports
from jpype.types import *

# jpype.startJVM()
jpype.addClassPath("ModernBoardGame.jar")
jpype.startJVM()
# jpype.startJVM(classpath=[""])
java.lang.System.out.println("Hello World!!")
from core import Game
Game.main([""])
print('done')