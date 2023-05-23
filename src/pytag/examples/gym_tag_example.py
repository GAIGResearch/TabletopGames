import pytag
from pytag.gym.wrappers import MergeActionMaskWrapper

import gymnasium as gym
from gymnasium.vector import AsyncVectorEnv, SyncVectorEnv
import numpy as np

if __name__ == "__main__":
    env = AsyncVectorEnv([
        lambda: gym.make("TAG/ExplodingKittens")
        for i in range(1)
    ])
    # For environments in which the action-masks align (aka same amount of actions)
    # This wrapper will merge them all into one numpy array, instead of having an array of arrays
    env = MergeActionMaskWrapper(env)
    
    # Note env.action_space is the merged action_space
    # use env.single_action_space to access the original action space of Diamant
    # same for env.observation_space and env.single_observation_space
    
    obs, infos = env.reset()
    dones = [False]
    for i in range(200):
        # pick random, but valid action todo: only works with n-env == 1
        _, available_a_idx = np.where(np.array(infos["action_mask"]) == 1.0)
        action = np.random.choice(available_a_idx)
        obs, rewards, dones, truncated, infos = env.step([action])
        print(rewards) # -1 means you executed a wrong action or lost the game
        if dones[0]:
            print(f"game over player got reward {rewards}")