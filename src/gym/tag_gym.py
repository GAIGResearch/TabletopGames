import gymnasium as gym

from abc import abstractmethod
from typing import Dict, List, Union

class TAGSingleplayerGym(gym.Env):
    def __init__(self, game_id: str):
        super().__init__()
        self.action_space = gym.spaces.Discrete(2)
        self.observation_space = gym.spaces.Box(shape=(9,))
    
    def reset(self):
        pass
        # return obs
    
    def step(self, action):
        # 0, 1
        num_actions = self.game_runner.num_actions()
        self.game_runner.step(0-9)
        
        action = self._create_tag_action(action)
        self.game_runner.step(action)
        pass
    
    def close(self):
        pass
    
    def verify(self, action) -> bool:
        pass
    
class TAGMultiplayerGym(gym.Env):
    def __init__(self, game_id: str):
        pass
    
    def reset(self):
        pass
        # return {"player1": obs1, "player2": obs2}
        
    def step(self, actions: Dict[str, int]):
        pass
        # return {"player1": obs1, "player2": obs2}
        
    def close(self):
        pass