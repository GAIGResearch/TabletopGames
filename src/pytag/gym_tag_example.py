import gym_
from gym_.wrappers import MergeActionMaskWrapper
import gymnasium as gym
from gymnasium.vector import AsyncVectorEnv, SyncVectorEnv
import numpy as np
import torch
from torch.distributions.categorical import Categorical
from torch import einsum
from typing import Optional

class CategoricalMasked(Categorical):
    def __init__(self, logits: torch.Tensor, mask: Optional[torch.Tensor] = None):
        self.mask = mask
        self.batch, self.nb_action = logits.size()
        if mask is None:
            super(CategoricalMasked, self).__init__(logits=logits)
        else:
            self.mask_value = torch.tensor(
                torch.finfo(logits.dtype).min, dtype=logits.dtype
            )
            logits = torch.where(self.mask, logits, self.mask_value)
            super(CategoricalMasked, self).__init__(logits=logits)

    def entropy(self):
        if self.mask is None:
            return super().entropy()
        # Elementwise multiplication
        p_log_p = einsum("ij,ij->ij", self.logits, self.probs)
        # Compute the entropy with possible action only
        p_log_p = torch.where(
            self.mask,
            p_log_p,
            torch.tensor(0, dtype=p_log_p.dtype, device=p_log_p.device),
        )
        ent = einsum("b a -> b", p_log_p)
        return ent
        # return -reduce(p_log_p, "b a -> b", "sum", b=self.batch, a=self.nb_action)

class ActionTree():
    def __init__(self, java_tree, device="cpu"):
        self.shape = java_tree.shape
        # recursively extract the tree
        self.device = device
        self.tree = torch.tensor(java_tree, dtype=torch.int32).to(device)
        #
        # self.tree = self.convert_to_torch(torch.tensor(java_tree))

    def entropy(self):
        if len(self.masks) == 0:
            return super(CategoricalMasked, self).entropy()
        p_log_p = self.logits * self.probs
        p_log_p = torch.where(self.masks, p_log_p, torch.tensor(0.).to(device))
        return -p_log_p.sum(-1)

    def sample_from_logits(self, logits, mask):
        # todo should recursively sample from the logits
        start_id = 0
        for shp in self.tree.shape:
            log = logits[start_id:len(shp)]
            m = mask[start_id:len(shp)]
            # logits = torch.where(m, log, torch.tensor(-1e+8))
            logits = np.where(m, log, -1e+8)
            probs = Categorical(logits=logits)
        action = probs.sample()
        pass

    def convert_to_numpy(self, subtree):
        # recursively convert the tree into a numpy array - works in cases where the tree does not have a regular shape
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


if __name__ == "__main__":
    # env = gym.make("TAG/ExplodingKittens")
    env = gym.make("TAG/TicTacToe")
    obs, infos = env.reset()
    action_tree = infos["action_tree"]
    action_tree = ActionTree(action_tree)

    # env = AsyncVectorEnv([
    #     lambda: gym.make("TAG/ExplodingKittens")
    #     # lambda: gym.make("TAG/TicTacToe")
    #     for i in range(1)
    # ])
    # For environments in which the action-masks align (aka same amount of actions)
    # This wrapper will merge them all into one numpy array, instead of having an array of arrays
    # env = MergeActionMaskWrapper(env)
    
    # Note env.action_space is the merged action_space
    # use env.single_action_space to access the original action space of Diamant
    # same for env.observation_space and env.single_observation_space
    
    obs, infos = env.reset()
    # action_tree = infos["action_tree"] #[0]
    dones = [False]
    for i in range(200):
        # pick random, but valid action todo: only works with n-env == 1
        mask = torch.tensor(infos["action_mask"])
        mask = mask.view(-1, 3)
        masked = CategoricalMasked(logits=torch.ones((1, 3, 3)), mask=mask[3:].reshape(3,3))
        multi_cat = [CategoricalMasked(logits=torch.ones((1, 3)), mask=iam) for iam in mask.view(-1, 3)]
        action = torch.stack([cat.sample() for cat in multi_cat])

        action = action_tree.sample_from_logits(np.ones(env.action_space.n), mask)
        # action = action_tree.apply_mask(np.ones(env.action_space.n), mask)
        action = get_random_action(mask , action_tree)
        # _, available_as = np.where(np.array(infos["action_mask"]) == 1.0)
        # available_a_idx = available_as[np.where(available_as >= 3)] # todo this is tictactoe specific
        # action = np.random.choice(available_a_idx)
        obs, rewards, dones, truncated, infos = env.step([action])
        # print(rewards) # -1 means you executed a wrong action or lost the game
        if dones[0]:
            print(f"game over player got reward {rewards}")