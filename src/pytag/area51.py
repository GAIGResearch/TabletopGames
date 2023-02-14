import gym_
from gym_.wrappers import MergeActionMaskWrapper
import gymnasium as gym
from gymnasium.vector import AsyncVectorEnv, SyncVectorEnv

if __name__ == "__main__":
    env = AsyncVectorEnv([
        lambda: gym.make("TAG/Diamant")
        for i in range(2)
    ])
    env = MergeActionMaskWrapper(env)
    
    
    obs, info = env.reset()
    dones = [False]
    for i in range(20):
        obs, rewards, dones, truncated, infos = env.step(env.action_space.sample())
        print(rewards)