import os
import wandb

import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F
from torch.distributions.categorical import Categorical

class GPUReplayMemory():
    # keeps everything on GPU instead of manually shifting them back and forth
    def __init__(self, args, obs_space, action_space, n_envs=1):
        self.obs_space = obs_space
        self.action_space = action_space
        # self.batch_size = args.batch_size
        self.device = args.device
        self.capacity = args.replay_frequency
        self.gamma = args.gamma
        self.n_envs = n_envs
        self.obs = torch.zeros([self.capacity, self.n_envs, obs_space], dtype=torch.float32).to(self.device)
        self.actions = torch.zeros([self.capacity, self.n_envs], dtype=torch.int64).to(self.device)
        self.masks = torch.zeros([self.capacity, self.n_envs, self.action_space], dtype=torch.int64).to(self.device)
        self.logprobs = torch.zeros([self.capacity, self.n_envs], dtype=torch.int64).to(self.device)
        self.rewards = torch.zeros([self.capacity, self.n_envs], dtype=torch.float32).to(self.device)
        self.dones = torch.zeros([self.capacity, self.n_envs], dtype=torch.uint8).to(self.device)
        self.pos = 0

    def append(self, obs, action, mask, logprobs, reward, done):
        pos = self.pos % self.capacity
        self.obs[pos] = obs
        self.actions[pos] = action
        self.masks[pos] = mask
        self.logprobs[pos] = logprobs
        self.rewards[pos] = torch.from_numpy(reward).float()
        self.dones[pos] = torch.from_numpy(done)
        self.pos += 1
    def get_buffer(self):
        # PPO related processing and getting trajectories
        # todo at the merging point done should not be carried over
        obs = self.obs.view(self.capacity * self.n_envs, -1)
        actions = self.actions.view(self.capacity * self.n_envs)
        masks = self.masks.view(self.capacity * self.n_envs, -1)
        log_probs = self.logprobs.view(self.capacity * self.n_envs)
        rewards = self.rewards.view(self.capacity * self.n_envs)
        dones = self.dones.view(self.capacity * self.n_envs)

        # Monte Carlo estimate of returns
        discounted_rewards = []
        discounted_reward = 0
        for reward, is_terminal in zip(reversed(rewards), reversed(dones)):
            if is_terminal:
                discounted_reward = 0
            discounted_reward = reward + (self.gamma * discounted_reward)
            discounted_rewards.insert(0, discounted_reward)

        # Normalizing the rewards
        discounted_rewards = torch.tensor(discounted_rewards, dtype=torch.float32).to(self.device)
        discounted_rewards = (discounted_rewards - discounted_rewards.mean()) / (discounted_rewards.std() + 1e-7)
        rewards = discounted_rewards

        return (obs, actions, log_probs, rewards, dones, masks)

    def reset(self):
        self.obs = torch.zeros([self.capacity, self.n_envs, self.obs_space], dtype=torch.float32).to(self.device)
        self.actions = torch.zeros([self.capacity, self.n_envs], dtype=torch.int64).to(self.device)
        self.masks = torch.zeros([self.capacity, self.n_envs, self.action_space], dtype=torch.int64).to(self.device)
        self.logprobs = torch.zeros([self.capacity, self.n_envs], dtype=torch.int64).to(self.device)
        self.rewards = torch.zeros([self.capacity, self.n_envs], dtype=torch.float32).to(self.device)
        self.dones = torch.zeros([self.capacity, self.n_envs], dtype=torch.uint8).to(self.device)
        self.pos = 0

class ActorCritic(nn.Module):
    def __init__(self, args, input_dims, n_actions):
        super(ActorCritic, self).__init__()
        self.args = args
        self.input_dims = input_dims
        self.n_actions = n_actions
        self.hidden_units = args.hidden_size
        self.device = args.device

        self.network = nn.Sequential(nn.Linear(input_dims, self.hidden_units), nn.ReLU(), nn.Linear(self.hidden_units, self.hidden_units), nn.ReLU()).to(self.device)

        self.actor = nn.Linear(self.hidden_units, self.n_actions).to(self.device)
        self.critic = nn.Linear(self.hidden_units, 1).to(self.device)

    def forward(self, x):
        x_ = self.network(x)
        return x_

    def act(self, obs, mask=None):
        x_ = self(obs)

        action_probs = self.actor(x_)
        if mask is not None:
            mask_ = torch.tensor(mask, device=self.device)
            action_probs = torch.where(mask_, action_probs, torch.tensor(-1e+8, device=self.device))
        action_probs = F.softmax(action_probs, dim=-1)
        dist = Categorical(action_probs)

        action = dist.sample()
        action_logprob = dist.log_prob(action)

        return action.detach(), action_logprob.detach()

    def get_logprobs(self, obs):
        x_ = self(obs)

        action_probs = self.actor(x_)
        action_probs = F.softmax(action_probs, dim=-1)

        return action_probs.detach()

    def evaluate(self, obs, action):
        x_ = self(obs)
        action_probs = self.actor(x_)
        action_probs = F.softmax(action_probs, dim=-1)
        dist = Categorical(action_probs)

        action_logprobs = dist.log_prob(action)
        dist_entropy = dist.entropy()
        state_values = self.critic(x_)

        return action_logprobs, state_values, dist_entropy
class PPO:
    def __init__(self, args, env, model=None):
        self.name = "PPO"
        self.random = np.random.RandomState(args.seed)
        self.args = args
        self.device = args.device
        self.env = env

        self.args = args
        self.gamma = args.gamma

        # self.lr = args.learning_rate
        self.lr_actor = args.lr_actor
        self.lr_critic = args.lr_critic

        self.eps_clip = args.eps_clip
        self.K_epochs = args.k_epochs

        self.n_actions = env.action_space[0].n
        self.input_dims = env.observation_space.shape[1]

        self.n_envs = env.observation_space.shape[0]
        self.mem = GPUReplayMemory(args, env.observation_space.shape[1], env.action_space[0].n, n_envs=self.n_envs)

        self.policy = ActorCritic(args, self.input_dims, self.n_actions).to(args.device)
        if model is None:
            model = args.model
        if model:  # Load pretrained model if provided
            if os.path.isfile(model):
                checkpoint = torch.load(model,
                                        map_location='cpu')  # Always load tensors onto CPU by default, will shift to GPU if necessary
                self.policy.load_state_dict(checkpoint['state_dict'])
                print("Loading pretrained model: " + model)
            else:  # Raise error if incorrect model path provided
                raise FileNotFoundError(model)

        self.optim = torch.optim.Adam([
            {'params': self.policy.actor.parameters(), 'lr': self.lr_actor},
            {'params': self.policy.critic.parameters(), 'lr': self.lr_critic}
        ])
        # self.optim = torch.optim.Adam(self.policy.parameters(), lr=args.learning_rate)

        self.policy_old = ActorCritic(args, self.input_dims, self.n_actions).to(self.args.device)
        self.policy_old.load_state_dict(self.policy.state_dict())

        self.MseLoss = nn.MSELoss()

    def act(self, state, mask=None):
        with torch.no_grad():
            action, action_logprob = self.policy_old.act(state, mask)

        return action, action_logprob.detach()

    def get_logprobs(self, state):
        with torch.no_grad():
            logprobs = self.policy.get_logprobs(state)
        return logprobs

    def learn(self, steps):
        # transitions -1 contains the masks
        transitions = self.mem.get_buffer()

        actor_loss = 0
        critic_loss = 0
        entropy_loss = 0
        total_loss = 0

        # Optimize policy for K epochs
        for _ in range(self.K_epochs):
            # Evaluating old actions and values
            logprobs, state_values, dist_entropy = self.policy.evaluate(transitions[0], transitions[1].squeeze())

            # match state_values tensor dimensions with rewards tensor
            state_values = torch.squeeze(state_values)

            # Finding the ratio (pi_theta / pi_theta__old)
            ratios = torch.exp(logprobs - transitions[2].detach())

            # Finding Surrogate Loss
            advantages = transitions[3] - state_values.detach()
            surr1 = ratios * advantages
            surr2 = torch.clamp(ratios, 1 - self.eps_clip, 1 + self.eps_clip) * advantages

            # final loss of clipped objective PPO
            a_loss = -torch.min(surr1, surr2)
            c_loss = self.MseLoss(state_values, transitions[3])
            loss = a_loss + 0.5 * c_loss - 0.01 * dist_entropy

            # take gradient step
            self.optim.zero_grad()
            loss.mean().backward()
            self.optim.step()

            actor_loss += a_loss.mean().detach().cpu().numpy()
            critic_loss += c_loss.detach().cpu().numpy()
            entropy_loss += dist_entropy.mean().detach().cpu().numpy()
            total_loss += loss.detach().mean().cpu().numpy()

        # Copy new weights into old policy
        self.policy_old.load_state_dict(self.policy.state_dict())

        # reset buffer
        self.mem.reset()
        wandb.log({
            # "train/total-steps": steps,
            "train/total-loss": total_loss/self.K_epochs,
            "train/actor-loss": actor_loss/self.K_epochs,
            "train/critic-loss": critic_loss/self.K_epochs,
            "train/entropy-loss": entropy_loss/self.K_epochs,
        })

        return total_loss

    def save_model(self, path, name="checkpoint.pth"):
        state = {
            "state_dict": self.policy.state_dict(),
            "optimizer": self.optim.state_dict(),
            "args": self.args,
            "gamma": self.gamma,
        }
        torch.save(state, os.path.join(path, name))
        pass

    def load_model(self, path, eval_only=False):

        state = torch.load(path)
        self.args = state["args"]
        self.policy = ActorCritic(self.args, self.input_dims, self.n_actions, self.scalar_dims)
        self.policy.load_state_dict(state["state_dict"])

        if eval_only:
            self.policy.eval()
        else:
            # load and copy q_net params to target_net
            self.old_policy = ActorCritic(self.args, self.input_dims, self.n_actions, self.scalar_dims)
            self.old_policy.load_state_dict(state["state_dict"])
            self.optim.load_state_dict(state["optimizer"])
            self.gamma = state["gamma"]




