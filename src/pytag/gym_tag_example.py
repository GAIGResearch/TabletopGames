import gym_
from gym_.wrappers import MergeActionMaskWrapper
import gymnasium as gym
from gymnasium.vector import AsyncVectorEnv, SyncVectorEnv
import numpy as np

def get_random_action(mask, action_tree):
    # todo should map the logits to the action tree
    action = np.zeros(len(action_tree.shape))
    # todo need to work out how to count the indices
    for i in range(len(action_tree.shape)):
        available_actions = mask[0][:action_tree.shape[i]]
        action[i] = np.random.choice(available_actions)
    # select top level action first

    # select second level action
    return action

def process_action_tree(action_tree):
    # todo should recursively convert it into a numpy array
    action_tree = np.array(action_tree)
    [np.array(x) for x in action_tree]

if __name__ == "__main__":
    env = gym.make("TAG/ExplodingKittens")
    obs, infos = env.reset()
    action_tree = infos["action_tree"]
    process_action_tree(action_tree)

    env = AsyncVectorEnv([
        lambda: gym.make("TAG/ExplodingKittens")
        # lambda: gym.make("TAG/TicTacToe")
        for i in range(1)
    ])
    # For environments in which the action-masks align (aka same amount of actions)
    # This wrapper will merge them all into one numpy array, instead of having an array of arrays
    env = MergeActionMaskWrapper(env)
    
    # Note env.action_space is the merged action_space
    # use env.single_action_space to access the original action space of Diamant
    # same for env.observation_space and env.single_observation_space
    
    obs, infos = env.reset()
    action_tree = infos["action_tree"][0]
    dones = [False]
    for i in range(200):
        # pick random, but valid action todo: only works with n-env == 1
        mask = infos["action_mask"]
        action = get_random_action(mask , action_tree)
        # _, available_as = np.where(np.array(infos["action_mask"]) == 1.0)
        # available_a_idx = available_as[np.where(available_as >= 3)] # todo this is tictactoe specific
        # action = np.random.choice(available_a_idx)
        obs, rewards, dones, truncated, infos = env.step([action])
        # print(rewards) # -1 means you executed a wrong action or lost the game
        if dones[0]:
            print(f"game over player got reward {rewards}")