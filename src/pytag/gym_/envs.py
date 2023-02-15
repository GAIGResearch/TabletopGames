import gymnasium as gym
import numpy as np
import jpype

from .core import GymEnv, GameType, Utils, get_agent_class

from abc import abstractmethod
from typing import Dict, List, Union

class TagSingleplayerGym(gym.Env):
    def __init__(self, game_id: str, agent_ids: List[str], seed: int=0):
        super().__init__()
        self._last_obs_vector = None
        self._last_action_mask = None
        
        # Initialize the java environment
        gameType = GameType.valueOf(Utils.getArg([""], "game", game_id))
        # ToDo throw exception if player is incorrect
        agents = [get_agent_class(agent_id)() for agent_id in agent_ids]
        self._playerID = agent_ids.index("python")
        # ToDo accept the List interface in GymEnv, this allows us to pass agents directly instead of converting it first
        self._java_env = GymEnv(gameType, None, jpype.java.util.ArrayList(agents), seed, True)
        
        # Construct action/observation space
        self._java_env.reset()
        action_mask = self._java_env.getActionMask()
        num_actions = len(action_mask)
        self.action_space = gym.spaces.Discrete(num_actions)
        # ToDo better low and high values
        obs_size = int(self._java_env.getObservationSpace())
        self.observation_space = gym.spaces.Box(shape=(obs_size,), low=float("-inf"), high=float("inf"))
    
    def reset(self):
        self._java_env.reset()
        self._update_data()
        
        return self._last_obs_vector, {"action_mask": self._last_action_mask, "has_won": int(str(self._java_env.getPlayerResults()[self._playerID]) == "WIN")}
    
    def step(self, action):
        # Verify
        if not self.is_valid_action(action):
            # Execute a random action
            valid_actions = np.where(self._last_action_mask)[0]
            action = self.np_random.choice(valid_actions)
            self._java_env.step(action)
            reward = -1
        else:
            self._java_env.step(action)
            reward = int(str(self._java_env.getPlayerResults()[self._playerID]) == "WIN")
            if str(self._java_env.getPlayerResults()[self._playerID]) == "LOSE": reward = -1

        self._update_data()
        done = self._java_env.isDone()
        truncated = False
        info = {"action_mask": self._last_action_mask,
                "has_won": int(str(self._java_env.getPlayerResults()[self._playerID]) == "WIN")}
        return self._last_obs_vector, reward, done, truncated, info
    
    def close(self):
        pass
    
    def is_valid_action(self, action: int) -> bool:
        return self._last_action_mask[action] 
    
    def _update_data(self):
        obs = self._java_env.getObservationVector()
        self._last_obs_vector = np.array(obs, dtype=np.float32)
        
        action_mask = self._java_env.getActionMask()
        self._last_action_mask = np.array(action_mask, dtype=bool)
    
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