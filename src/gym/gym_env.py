import jpype
from jpype import *
import jpype.imports
from jpype.types import *

# jpype.startJVM()
jpype.addClassPath("ModernBoardGame.jar")
jpype.startJVM()
# jpype.startJVM(classpath=[""])
java.lang.System.out.println("Hello World!!")
# from games.pandemic import PandemicGame
# PandemicGame.main([""])
# from games.tictactoe import TicTacToeGame
# TicTacToeGame.main([""])
import java
from core import Game
from games import GameType
from players.human import ActionController
from players.simple import RandomPlayer
from utilities import Utils
# gameType = "pandemic"
null = jpype.java.lang.String@None
null_list = jpype.java.util.List@None
gameType = Utils.getArg([""], "game", "Pandemic");
ac = ActionController()
turnPause = 0
players = jpype.java.util.ArrayList()
players.add(RandomPlayer())
players.add(RandomPlayer())
game = Game.runOne(GameType.valueOf(gameType), null, players, java.lang.Long(42), java.lang.Boolean(False), null_list, ac, turnPause)
print(game.getTick())
jpype.shutdownJVM()

# Game.main([""])
print('done')