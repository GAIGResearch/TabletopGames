import gym_
from gym_.wrappers import MergeActionMaskWrapper
import gymnasium as gym
from gymnasium.vector import AsyncVectorEnv, SyncVectorEnv
import numpy as np

class ActionTree():
    def __init__(self, java_tree):
        self.shape = java_tree.shape
        # recursively extract the tree
        self.tree = self.convert_to_numpy(java_tree)
        self.subtree_idx = self._get_idx(self.tree)

        # should work with batches
        # for i in range(len(java_tree.shape)):
        #     available_actions = mask[0][:java_tree.shape[i]]
        #     action[i] = np.random.choice(available_actions)
        # print(action[i])

    def apply_mask(self, logits, mask):
        # todo should recursively apply the mask to the logits
        pass

    def _get_idx(self, subtree):
        # breadth first traversal of the subtree, idx is the index of the starting position of the child node
        # todo we are not summing up the number of nodes in a level
        counter = len(subtree)
        idx = []
        for i in range(len(subtree)):
            if isinstance(subtree[i], np.integer) or isinstance(subtree[i], int):
                # idx.append(counter)
                counter += 1
            else:
                # todo somewhere we should add an actual value for idx
                # isinstance(subtree[i], np.ndarray)
                counter += len(subtree[i])
                idx.append(counter)
                idx.append(self._get_idx(subtree[i]))
            # counter += 1
        return idx

    def convert_to_numpy(self, subtree):
        # recursively convert the tree into a numpy array
        if not isinstance(subtree, np.ndarray) and subtree == 1:
            return int(subtree)
        subtree = np.array(subtree)
        for i in range(len(subtree)):
            if not isinstance(subtree[i], np.ndarray):
                # we get int, but subtree on left converts it back to int64
                subtree[i] = self.convert_to_numpy(subtree[i])
        return subtree


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
    action_tree = ActionTree(action_tree)
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