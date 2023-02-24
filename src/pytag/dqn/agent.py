import os

import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F
import wandb

class ReplayMemory():
    # this version stores all the data in torch tesnors and indexes the last position by a counter
    # The required space is blocked at the start by filling up the replay memory with torch.zeros, not dynamically allocated
    def __init__(self, obs_space, batch_size=32, capacity=int(5e4), device="cpu"):
        self.obs_space = obs_space
        self.batch_size = batch_size
        self.device = device
        self.capacity = capacity
        self.obs = torch.zeros([self.capacity, obs_space], dtype=torch.float32).to(device)
        self.actions = torch.zeros([self.capacity], dtype=torch.int64).to(device)
        self.rewards = torch.zeros([self.capacity], dtype=torch.float32).to(device)
        self.dones = torch.zeros([self.capacity], dtype=torch.uint8).to(device)

        self.pos = 0

    def append(self, obs, action, reward, done):
        pos = self.pos % self.capacity
        self.obs[pos] = obs
        self.actions[pos] = action
        self.rewards[pos] = reward
        self.dones[pos] = done
        self.pos += 1

    def sample(self):
        # transitions are processed here
        max_id = min(self.pos, self.capacity-1)
        idx = np.random.randint(0, max_id, self.batch_size)
        obs = self.obs[idx].float()
        action = self.actions[idx].unsqueeze(1)
        next_obs = self.obs[(idx + 1) % self.capacity].float()
        rewards = self.rewards[idx]
        dones = 1 - self.dones[idx]
        return obs, action, next_obs, rewards, dones

    def reset(self):
        obs_space = self.obs_space
        self.obs = torch.zeros([self.capacity, obs_space], dtype=torch.uint8).to(self.device)
        self.actions = torch.zeros([self.capacity], dtype=torch.int64).to(self.device)
        self.rewards = torch.zeros([self.capacity], dtype=torch.float32).to(self.device)
        self.dones = torch.zeros([self.capacity], dtype=torch.uint8).to(self.device)
        self.pos = 0
class DQN(nn.Module):
    def __init__(self, input_dims, n_actions, hidden_units=64):
        super(DQN, self).__init__()
        self.n_actions = n_actions
        self.input_dims = input_dims
        self.hidden_units = hidden_units

        self.network = nn.Sequential(nn.Linear(self.input_dims, self.hidden_units), nn.ReLU(), nn.Linear(self.hidden_units, self.hidden_units), nn.ReLU())

        self.out = nn.Linear(self.hidden_units, self.n_actions)

    def forward(self, x):
        x = self.network(x)
        q = self.out(x)
        return q
class DQNAgent():
    def __init__(self, env, device="cpu", seed=42, model=None):
        self.random = np.random.RandomState(seed)
        self.device = device
        self.env = env
        self.n_actions = env.action_space
        self.input_dims = env.observation_space

        self.batch_size = 32
        self.gamma = 0.95
        self.norm_clip = 3
        self.update_counter = 0
        self.lr = 1e-3

        self.mem = ReplayMemory(self.input_dims, batch_size=self.batch_size)

        self.q_net = DQN(self.input_dims, self.n_actions).to(device)

        if model:  # Load pretrained model if provided
            if os.path.isfile(model):
                checkpoint = torch.load(model,
                                        map_location='cpu')  # Always load tensors onto CPU by default, will shift to GPU if necessary
                self.q_net.load_state_dict(checkpoint['state_dict'])
                print("Loading pretrained model: " + model)
            else:  # Raise error if incorrect model path provided
                raise FileNotFoundError(model)
        self.target_net = DQN(self.input_dims, self.n_actions).to(self.device)
        self.optim = torch.optim.Adam(self.q_net.parameters(), lr=self.lr)
        wandb.watch(self.q_net)



    def act(self, state, mask, epsilon=0.0):
        mask = torch.tensor(mask, dtype=torch.float32).to(self.device)
        if self.random.uniform(0, 1) < epsilon:
            available_a_idx, = torch.where(mask == 1.0)
            action = np.random.choice(available_a_idx)
            # action = self.random.randint(0, self.n_actions)
            q = torch.zeros(self.n_actions, dtype=torch.float32).to(self.device)
            q[action] = 1.0
        else:
            with torch.no_grad():
                q = self.q_net(state)
                q = q * mask
                action = q.argmax(1).item()
                q.detach().cpu().numpy()
        return action, q

    def get_q(self, state):
        with torch.no_grad():
            return self.q_net(state)

    def td_loss(self, transitions):
        q_values = self.q_net(transitions[0])
        # pick the actions that agent took
        selected_action_values = q_values.gather(1, transitions[1])
        with torch.no_grad():
            next_q_values = self.target_net(transitions[2]).max(1)
        td = transitions[3] + (self.gamma * next_q_values[0] * (transitions[4]))
        loss = F.smooth_l1_loss(selected_action_values.squeeze().float(), td.float())
        return loss

    def learn(self, steps):
        experience = self.mem.sample() # obs, actions, next_obs, rewards
        loss = self.td_loss(experience)

        self.optim.zero_grad()
        loss.backward()
        nn.utils.clip_grad_norm_(self.q_net.parameters(), self.norm_clip)
        self.optim.step()

        wandb.log({"train/total_steps": steps,
                   "train/td_loss": loss}, commit=False)
                   # "train/mean_q": q.mean(),
                   # "train/max_q": q.max(-1).mean(),
                   # "train/min_q": q.min(-1).mean()


        self.update_counter += 1

        if self.update_counter % 250 == 0:
            self.update_target_net()

        return loss.cpu().detach().numpy()

    def update_target_net(self):
        self.target_net.load_state_dict(self.q_net.state_dict())

    def soft_update_target_net(self, tau):
        # moves the target net toward q_net
        for target_param, q_param in zip(self.target_net.parameters(), self.q_net.parameters()):
            target_param.data.copy_(tau*q_param.data + (1-tau)*target_param.data)

    def save_model(self, filename="checkpoint.pth"):
        # todo this is not used
        state = {
            "state_dict": self.q_net.state_dict(),
            "optimizer": self.optim.state_dict(),
            "gamma": self.gamma,
            "norm_clip": self.norm_clip,
        }
        art = wandb.Artifact("final", type="model")
        torch.save(state, filename)
        art.add_dir(os.path.dirname(filename))
        wandb.log_artifact(art, aliases=["final"])

    def load_model(self, path, eval_only=False):
        # todo this is not used
        state = torch.load(path)
        agent_type = DQN(self.input_dims, self.n_actions)
        self.q_net = agent_type(self.input_dims, self.n_actions)
        self.q_net.load_state_dict(state["state_dict"])

        if eval_only:
            self.q_net.eval()
        else:
            # load and copy q_net params to target_net
            self.target_net = agent_type(self.input_dims, self.n_actions)
            self.update_target_net()
            self.optim.load_state_dict(state["optimizer"])
            self.gamma = state["gamma"]
            self.norm_clip = state["norm_clip"]

    def train(self):
        self.q_net.train()

    def eval(self):
        self.q_net.eval()
