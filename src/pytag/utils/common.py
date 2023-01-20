# various helper functions
import jpype
from jpype import *
import jpype.imports

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

# # Import Calls
# GYMEnv = jpype.JClass("core.GYMEnv")
# Game = jpype.JClass("core.Game")
# AbstractGameState = jpype.JClass("core.AbstractGameState")
# GameType = jpype.JClass("games.GameType")
# ActionController = jpype.JClass("players.human.ActionController")
# Utils = jpype.JClass("utilities.Utils")