import numpy as np
import torch


import gymnasium as gym
from gymnasium.vector import VectorEnvWrapper
import gymnasium.wrappers


class MergeActionMaskWrapper(VectorEnvWrapper):
    def reset_wait(self, **kwargs):
        obs, infos = self.env.reset_wait(**kwargs)
        return obs, self._merge_action_masks(infos)
    
    def step_wait(self):
        obs, rewards, dones, truncated, infos = self.env.step_wait()
        return obs, rewards, dones, truncated, self._merge_action_masks(infos)
    
    def _merge_action_masks(self, infos):
        infos["action_mask"] = np.stack(infos["action_mask"])
        del infos["_action_mask"] # Not needed
        return infos

class StrategoWrapper(gym.ObservationWrapper):
    def __init__(self, env):
        super().__init__(env)
        self.observation_space = gym.spaces.Box(low=0, high=1, shape=(27, 10, 10), dtype=np.float32)
    def reset(self, **kwargs):
        obs, info = self.env.reset(**kwargs)
        return self.observation(obs), info
    def observation(self, observation):
        observation_ = torch.from_numpy(observation.reshape(10, 10)).to(torch.int64)
        observation_ = torch.nn.functional.one_hot(observation_+13, num_classes=27)
        observation_ = observation_.permute(2, 0, 1).float()
        return observation_