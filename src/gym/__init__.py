import gymnasium as gym
import jpype

# Setup jpype
jpype.addClassPath("ModernBoardGame.jar")
if not jpype.isJVMStarted():
    jpype.startJVM()

# Register gym environments
gym.envs.register(
     id='TAG/TicTacToe-v0',
     entry_point='tag_gym:TAGSinglePlayerGym',
     kwargs={"game_id": "TicTacToe"}
)