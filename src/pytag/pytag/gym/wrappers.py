import time
from collections import deque
from typing import Optional

import numpy as np
import torch


import gymnasium as gym
from gymnasium.vector import VectorEnvWrapper


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

class SushiGoWrapper(gym.ObservationWrapper):
    def __init__(self, env):
        super().__init__(env)
        # self.card_list = ["Tempura", "Maki-1"]
        self.observation_space = gym.spaces.Box(low=0, high=1, shape=(27, 10, 10), dtype=np.float32)
    def reset(self, **kwargs):
        obs, info = self.env.reset(**kwargs)
        return self.observation(obs), info
    def observation(self, observation):
        # self._java_env.getObservationJson()
        observation_ = torch.from_numpy(observation.reshape(10, 10)).to(torch.int64)
        observation_ = torch.nn.functional.one_hot(observation_+13, num_classes=27)
        observation_ = observation_.permute(2, 0, 1).float()
        return observation_

class RecordEpisodeStatistics(gym.Wrapper):
    # Based on RecordEpisodeStatistics from gymnasium, but it checks whether the player has won the game
    """This wrapper will keep track of cumulative rewards and episode lengths.

    At the end of an episode, the statistics of the episode will be added to ``info``
    using the key ``episode``. If using a vectorized environment also the key
    ``_episode`` is used which indicates whether the env at the respective index has
    the episode statistics.
    """

    def __init__(self, env: gym.Env, deque_size: int = 100):
        """This wrapper will keep track of cumulative rewards and episode lengths.

        Args:
            env (Env): The environment to apply the wrapper
            deque_size: The size of the buffers :attr:`return_queue` and :attr:`length_queue`
        """
        super().__init__(env)
        self.num_envs = getattr(env, "num_envs", 1)
        self.episode_count = 0
        self.episode_start_times: np.ndarray = None
        self.episode_returns: Optional[np.ndarray] = None
        self.episode_lengths: Optional[np.ndarray] = None
        self.episode_wins: Optional[np.ndarray] = None
        self.return_queue = deque(maxlen=deque_size)
        self.length_queue = deque(maxlen=deque_size)
        self.win_queue = deque(maxlen=deque_size)
        self.is_vector_env = getattr(env, "is_vector_env", False)

    def reset(self, **kwargs):
        """Resets the environment using kwargs and resets the episode returns and lengths."""
        obs, info = super().reset(**kwargs)
        self.episode_start_times = np.full(
            self.num_envs, time.perf_counter(), dtype=np.float32
        )
        self.episode_returns = np.zeros(self.num_envs, dtype=np.float32)
        self.episode_lengths = np.zeros(self.num_envs, dtype=np.int32)
        self.episode_wins = np.zeros(self.num_envs, dtype=np.int32)
        return obs, info

    def step(self, action):
        """Steps through the environment, recording the episode statistics."""
        (
            observations,
            rewards,
            terminations,
            truncations,
            infos,
        ) = self.env.step(action)
        assert isinstance(
            infos, dict
        ), f"`info` dtype is {type(infos)} while supported dtype is `dict`. This may be due to usage of other wrappers in the wrong order."
        self.episode_returns += rewards
        self.episode_lengths += 1
        self.episode_wins += infos["has_won"]
        dones = np.logical_or(terminations, truncations)
        num_dones = np.sum(dones)
        if num_dones:
            if "episode" in infos or "_episode" in infos:
                raise ValueError(
                    "Attempted to add episode stats when they already exist"
                )
            else:
                infos["episode"] = {
                    "r": np.where(dones, self.episode_returns, 0.0),
                    "l": np.where(dones, self.episode_lengths, 0),
                    "w": [0 if final_inf is None else final_inf["has_won"] for final_inf in infos["final_info"]],
                    "t": np.where(
                        dones,
                        np.round(time.perf_counter() - self.episode_start_times, 6),
                        0.0,
                    ),
                }
                if self.is_vector_env:
                    infos["_episode"] = np.where(dones, True, False)
            self.return_queue.extend(self.episode_returns[dones])
            self.length_queue.extend(self.episode_lengths[dones])
            self.episode_count += num_dones
            self.episode_lengths[dones] = 0
            self.episode_returns[dones] = 0
            self.episode_start_times[dones] = time.perf_counter()
        return (
            observations,
            rewards,
            terminations,
            truncations,
            infos,
        )