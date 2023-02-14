import gymnasium as gym
import jpype

# Setup jpype
jpype.addClassPath("./jars/ModernBoardGame.jar")
if not jpype.isJVMStarted():
    jpype.startJVM(convertStrings=False)

gym.envs.register(
     id='TAG/Diamant-v0',
     entry_point='gym_.envs:TagSingleplayerGym',
     kwargs={"game_id": "Diamant", "agent_ids": ["random", "python"]}
)