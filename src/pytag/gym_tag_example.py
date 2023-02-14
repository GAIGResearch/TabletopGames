import gym_
from gym_.wrappers import MergeActionMaskWrapper
import gymnasium as gym
from gymnasium.vector import AsyncVectorEnv, SyncVectorEnv

if __name__ == "__main__":
    env = AsyncVectorEnv([
        lambda: gym.make("TAG/Diamant")
        for i in range(2)
    ])
    # For environments in which the action-masks align (aka same amount of actions)
    # This wrapper will merge them all into one numpy array, instead of having an array of arrays
    env = MergeActionMaskWrapper(env)
    
    # Note env.action_space is the merged action_space
    # use env.single_action_space to access the original action space of Diamant
    # same for env.observation_space and env.single_observation_space
    
    obs, info = env.reset()
    dones = [False]
    for i in range(20):
        obs, rewards, dones, truncated, infos = env.step(env.action_space.sample())
        print(rewards) # -1 means you executed a wrong action