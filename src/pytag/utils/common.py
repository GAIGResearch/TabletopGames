# various helper functions
import gymnasium as gym
import numpy as np
import torch

import jpype
from jpype import *
import jpype.imports
from gym_.wrappers import StrategoWrapper

def make_env(env_id, seed, opponent, n_players):
    def thunk():
        # always have a python agent first (at least in our experiments)
        agent_ids = ["python"]
        for i in range(n_players - 1):
            agent_ids.append(opponent)
        env = gym.make(env_id, seed=seed, agent_ids=agent_ids)
        if "Stratego" in env_id:
            env = StrategoWrapper(env)
        return env
    return thunk
def get_agent_list():
    return ["random", "mcts", "osla", "python"]

def get_agent_class(agent_name):
    if agent_name == "random":
        return jpype.JClass("players.simple.RandomPlayer")
    if agent_name == "mcts":
        return jpype.JClass("players.mcts.MCTSPlayer")
    if agent_name == "osla":
        return jpype.JClass("players.simple.OSLAPlayer")
    if agent_name == "python":
        return jpype.JClass("players.python.PythonAgent")
    return None

def layer_init(layer, std=np.sqrt(2), bias_const=0.0):
    torch.nn.init.orthogonal_(layer.weight, std)
    torch.nn.init.constant_(layer.bias, bias_const)
    return layer