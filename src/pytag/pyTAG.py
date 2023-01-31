import os.path
import random
import time
import json
import jpype
from jpype import java
import jpype.imports
from jpype.types import *
from utils.common import get_agent_class
import numpy as np


class PyTAG():
    def __init__(self, agents, seed=42, game="Diamant", jar_path="jars/ModernBoardGame.jar", isNormalized = True):
        # JPYPE setup
        self.root_path = os.getcwd()
        # jpype.addClassPath(os.path.join(self.root_path, jar_path))
        jpype.addClassPath(jpype.getDefaultJVMPath())
        jpype.addClassPath(jar_path)
        if not jpype.isJVMStarted():
            # jpype.startJVM(jpype.getDefaultJVMPath(), '-Djava.class.path=' + jar_path)
            jpype.startJVM(convertStrings=False) #classpath=[jpype.getDefaultJVMPath(), jar_path]) #classpath=os.path.join(self.root_path, jar_path))

        print(jpype.java.lang.System.getProperty("java.class.path"))
        # todo it sees the packages, but not the classes
        import core
        import games.catan
        # jpype.JClass("core.Game").main([]) # execute the main method in Game - runs a pandmic game by default - just an example

        # from utilities import Utils
        # jpype.JException

        GYMEnv = jpype.JClass("core.GYMEnv")
        Utils = jpype.JClass("utilities.Utils")
        GameType = jpype.JClass("games.GameType")

        try:
            # it does see the package structure as PyCharm makes recommendations in the debugger
            GYMEnv = jpype.JClass("<core.GYMEnv>")
            Utils = jpype.JClass("utilities.Utils")
            GameType = jpype.JClass("games.GameType")
        except jpype.JException as ex:
            print("Caught base exception : ", str(ex))
            print(ex.stacktrace())
        except Exception as e:
            print(e)

        null = jpype.java.lang.String @ None
        null_list = jpype.java.util.List @ None
        gameType = Utils.getArg([""], "game", game)
        players = jpype.java.util.ArrayList()
        # todo throw exception if player is incorrect
        for agent in agents:
            agent_class = get_agent_class(agent)
            players.add(agent_class())
        self.env = GYMEnv(GameType.valueOf(gameType), null, players, java.lang.Long(seed), isNormalized)
        # todo get obs and action spaces
        self.observation_space = 9 #self.env.getObservationSpace()
        self.action_space = 3 #self.env.getActionSpace()
        self.gs = None
        self.prev_reward = 0

    def getObs(self):
        return self.env.getFeatures()

    def getPlayerID(self):
        return self.env.getPlayerID()

    def reset(self):
        self.prev_reward = 0
        self.gs = self.env.reset()
        obs = np.asarray(self.env.getObservationVector())
        return obs

    def getActions(self):
        return self.env.getActions()

    def get_observation_as_json(self):
        java_json = self.env.getObservationJson()
        return json.loads(str(java_json))

    def has_won(self):
        if str(self.env.getPlayerResults()[0]) == "WIN":
            # print(f"Player won with reward {reward}")
            return True
        return False

    def step(self, action):
        playerID = self.env.getPlayerID()
        self.env.step(action)
        obs = np.asarray(self.env.getObservationVector())
        # reward = self.env.getReward()
        # reward = self.prev_reward - reward
        # self.prev_reward = reward
        # reward = self.env.getReward()/17
        # todo win/loss reward
        reward = 0.0
        if str(self.env.getPlayerResults()[0]) == "WIN":
            reward = 1.0
        done = self.env.isDone()
        return obs, reward, done, {}

    def close(self):
        jpype.shutdownJVM()

if __name__ == "__main__":
    EPISODES = 100
    players = ["python", "random"]
    env = PyTAG(players)
    done = False

    start_time = time.time()
    steps = 0
    wins = 0
    for e in range(EPISODES):
        obs = env.reset()
        done = False
        while not done:
            steps += 1

            rnd_action = random.randint(0, len(env.getActions())-1)
            # print(f"player {env.env.getPlayerID()} choose action {rnd_action}")
            obs, reward, done, info = env.step(rnd_action)
            if done:
                print(f"Game over rewards {reward} in {steps} steps results =  {env.env.getPlayerResults()[0]}")
                if str(env.env.getPlayerResults()[0]) == "WIN":
                    wins += 1
                break

    print(f"win rate = {wins/EPISODES} {EPISODES} episodes done in {time.time() - start_time} with total steps = {steps}")
    env.close()

