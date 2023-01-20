import random
import time
import jpype
from jpype import *
import jpype.imports
from utils.common import get_agent_class


class PyTAG():
    def __init__(self, agents, seed=42, game="Diamant", jar_path="ModernBoardGame.jar"):
        # JPYPE setup
        jpype.addClassPath(jar_path)
        if not jpype.isJVMStarted():
            jpype.startJVM()

        Utils = jpype.JClass("utilities.Utils")
        GYMEnv = jpype.JClass("core.GYMEnv")
        GameType = jpype.JClass("games.GameType")

        null = jpype.java.lang.String @ None
        null_list = jpype.java.util.List @ None
        gameType = Utils.getArg([""], "game", game)
        players = jpype.java.util.ArrayList()
        # todo throw exception if player is incorrect
        for agent in agents:
            agent_class = get_agent_class(agent)
            players.add(agent_class())
        self.env = GYMEnv(GameType.valueOf(gameType), null, players, java.lang.Long(seed))
        # todo get obs and action spaces
        # self.observation_space = self.env.ObservationSpace()
        # self.action_space = gym.spaces.Discrete(self.env.ActionSpace())
        self.gs = None


    def getObs(self):
        return self.env.getFeatures()

    def reset(self):
        self.gs = self.env.reset()
        obs = self.gs.getFeatureVector()
        return obs

    def getActions(self):
        return self.env.getActions()

    def convertJSONtoObs(self, json):
        pass

    def step(self, action):
        self.gs = self.env.step(action)
        obs = self.gs.getFeatureVector()
        reward = self.env.getReward()
        done = self.env.isDone()
        return obs, reward, done, ""

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

