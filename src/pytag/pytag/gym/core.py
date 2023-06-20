import jpype
import json
import os

# Setup jpype
tag_jar = os.path.join(os.path.dirname(__file__), 'jars', 'ModernBoardGame.jar')
jpype.addClassPath(tag_jar)
if not jpype.isJVMStarted():
    jpype.startJVM(convertStrings=False)

PyTAG = jpype.JClass("core.PyTAG")
Utils = jpype.JClass("utilities.Utils")
GameType = jpype.JClass("games.GameType")
PlayerFactory = jpype.JClass("players.PlayerFactory")

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

def get_mcts_with_params(json_path):
    with open(os.path.expanduser(json_path)) as json_file:
        json_string = json.load(json_file)
    json_string = str(json_string).replace('\'', '\"') # JAVA only uses " for string
    return jpype.JClass("players.mcts.MCTSPlayer")(PlayerFactory.fromJSONString(json_string))