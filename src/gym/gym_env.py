import random
import time
import jpype
from jpype import *
import jpype.imports
from jpype.types import *

# jpype.startJVM()
jpype.addClassPath("ModernBoardGame.jar")
# Game = jpype.JClass("core.Game")
jpype.startJVM()
# jpype.startJVM(classpath=[""])
java.lang.System.out.println("Hello World!!")
# from games.pandemic import PandemicGame
# PandemicGame.main([""])
# from games.tictactoe import TicTacToeGame
# TicTacToeGame.main([""])
import java

# Import Calls
GYMEnv = jpype.JClass("core.GYMEnv")
Game = jpype.JClass("core.Game")
AbstractGameState = jpype.JClass("core.AbstractGameState")
GameType = jpype.JClass("games.GameType")
ActionController = jpype.JClass("players.human.ActionController")
RandomPlayer = jpype.JClass("players.simple.RandomPlayer")
MCTSPlayer = jpype.JClass("players.mcts.MCTSPlayer")
PythonAgent = jpype.JClass("players.python.PythonAgent")
TicTacToeStateVector = jpype.JClass("games.tictactoe.TicTacToeStateVector")
Utils = jpype.JClass("utilities.Utils")

# from core import GYMEnv
# from core import Game, AbstractGameState
# from games import GameType
# from players.human import ActionController
# from players.simple import RandomPlayer
# from players.mcts import MCTSPlayer
# from players.python import PythonAgent
# from games.tictactoe import TicTacToeStateVector
# from utilities import Utils
# gameType = "pandemic"

class TAG():
    def __init__(self, seed=42, game="Pandemic"):
        null = jpype.java.lang.String @ None
        null_list = jpype.java.util.List @ None
        gameType = Utils.getArg([""], "game", game)
        players = jpype.java.util.ArrayList()
        players.add(PythonAgent())
        players.add(PythonAgent())
        # players.add(PythonAgent())
        self.env = GYMEnv(GameType.valueOf(gameType), null, players, java.lang.Long(seed))
        self.gs = None

    def getObs(self):
        return self.env.getFeatures()

    def reset(self):
        gs = self.env.reset()
        obs = gs.getFeatureVector()
        return obs

    def getActions(self):
        return self.env.getActions()

    def step(self, action):
        gs = self.env.step(action)
        obs = gs.getFeatureVector()
        reward = self.env.getReward()
        done = self.env.isDone()
        return obs, reward, done, ""

    def close(self):
        jpype.shutdownJVM()

if __name__ == "__main__":
    EPISODES = 100
    env = TAG()
    done = False

    start_time = time.time()
    steps = 0
    for e in range(EPISODES):
        obs = env.reset()
        done = False
        while not done:
            steps +=1
            rnd_action = random.randint(0, len(env.getActions())-1)
            # print(f"player {env.env.getPlayerID()} choose action {rnd_action}")
            obs, reward, done, info = env.step(rnd_action)
            if done:
                print(f"Game over rewards {reward} in {steps} steps results =  {env.env.getPlayerResults()[0]}")
                break

    print(f"{EPISODES} episodes done in {time.time() - start_time} with total steps = {steps}")
    env.close()

