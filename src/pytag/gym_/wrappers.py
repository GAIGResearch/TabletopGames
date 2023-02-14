import numpy as np

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