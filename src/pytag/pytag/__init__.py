import gymnasium as gym
import jpype
import os

# Setup jpype
tag_jar = os.path.join(os.path.dirname(__file__), 'jars', 'ModernBoardGame.jar')
jpype.addClassPath(tag_jar)
if not jpype.isJVMStarted():
    jpype.startJVM(convertStrings=False)

gym.envs.register(
     id='TAG/Diamant-v0',
     entry_point='pytag.gym.envs:TagSingleplayerGym',
     kwargs={"game_id": "Diamant", "agent_ids": ["python", "random"]}
)

gym.envs.register(
     id='TAG/ExplodingKittens-v0',
     entry_point='pytag.gym.envs:TagSingleplayerGym',
     kwargs={"game_id": "ExplodingKittens", "agent_ids": ["python", "random"]}
)

gym.envs.register(
     id='TAG/TicTacToe-v0',
     entry_point='gym_.envs:TagSingleplayerGym',
     kwargs={"game_id": "TicTacToe", "agent_ids": ["python", "random"]}
)

gym.envs.register(
     id='TAG/Stratego-v0',
     entry_point='gym_.envs:TagSingleplayerGym',
     kwargs={"game_id": "Stratego", "agent_ids": ["python", "random"]}
)

gym.envs.register(
     id='TAG/LoveLetter-v0',
     entry_point='gym_.envs:TagSingleplayerGym',
     kwargs={"game_id": "LoveLetter", "agent_ids": ["python", "random"]}
)