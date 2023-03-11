import numpy as np
import torch
from torch import nn
from torch.distributions import Categorical
from src.pytag.utils.common import layer_init
class PPONet(nn.Module):
    def __init__(self, args, envs):
        super().__init__()
        self.args = args
        if "Stratego" in args.env_id:
            self.hidden_units = 256
            self.conv_out_dims = 3200
            self.actor = nn.Sequential(
                nn.Conv2d(envs.single_observation_space.shape[0], 32, kernel_size=3, stride=1, padding=1),
                nn.ReLU(),
                nn.Flatten(),
                nn.Linear(self.conv_out_dims, self.hidden_units),
                nn.ReLU(),
                nn.Linear(self.hidden_units, self.hidden_units),
                nn.ReLU(),
                layer_init(nn.Linear(self.hidden_units, envs.single_action_space.n), std=0.01)
            )
            self.critic = nn.Sequential(
                nn.Conv2d(envs.single_observation_space.shape[0], 32, kernel_size=3, stride=1, padding=1),
                nn.ReLU(),
                nn.Flatten(),
                nn.Linear(self.conv_out_dims, self.hidden_units),
                nn.ReLU(),
                layer_init(nn.Linear(self.hidden_units, self.hidden_units)),
                nn.ReLU(),
                layer_init(nn.Linear(self.hidden_units, 1), std=1.0)
            )
        else:
            self.hidden_units = 64
            self.actor = nn.Sequential(
                nn.Linear(np.array(envs.single_observation_space.shape).prod(), self.hidden_units),
                nn.ReLU(),
                nn.Linear(self.hidden_units, self.hidden_units),
                nn.ReLU(),
                layer_init(nn.Linear(self.hidden_units, envs.single_action_space.n), std=0.01)
            )

            self.critic = nn.Sequential(
                nn.Linear(np.array(envs.single_observation_space.shape).prod(), self.hidden_units),
                nn.ReLU(),
                layer_init(nn.Linear(self.hidden_units, self.hidden_units)),
                nn.ReLU(),
                layer_init(nn.Linear(self.hidden_units, 1), std=1.0)
            )

    def get_value(self, x):
        return self.critic(x)

    def get_action_and_value(self, x, action=None, mask=None):
        logits = self.actor(x)
        if mask is not None:
            logits = torch.where(mask, logits, torch.tensor(-1e+8, device=logits.device))
        probs = Categorical(logits=logits)
        if action is None:
            action = probs.sample()
        return action, probs.log_prob(action), probs.entropy(), self.critic(x)

class PPOLSTM(nn.Module):
    def __init__(self, args, envs):
        super().__init__()
        self.args = args
        if "Stratego" in args.env_id:
            self.hidden_units = 256
            self.conv_out_dims = 3200
            self.network = nn.Sequential(
                nn.Conv2d(envs.single_observation_space.shape[0], 32, kernel_size=3, stride=1, padding=1),
                nn.ReLU(),
                nn.Flatten(),
                nn.Linear(self.conv_out_dims, self.hidden_units),
                nn.ReLU(),
                nn.Linear(self.hidden_units, self.hidden_units),
                nn.ReLU(),
            )
        else:
            self.hidden_units = 128
            self.network = nn.Sequential(
                nn.Linear(np.array(envs.single_observation_space.shape).prod(), self.hidden_units),
                nn.ReLU(),
                nn.Linear(self.hidden_units, self.hidden_units),
                nn.ReLU(),
            )

        self.lstm = nn.LSTM(self.hidden_units, self.hidden_units)
        for name, param in self.lstm.named_parameters():
            if "bias" in name:
                nn.init.constant_(param, 0)
            elif "weight" in name:
                nn.init.orthogonal_(param, 1.0)
        self.actor = layer_init(nn.Linear(self.hidden_units, envs.single_action_space.n), std=0.01)
        self.critic = layer_init(nn.Linear(self.hidden_units, 1), std=1)
    def get_states(self, x, lstm_state, done):
        hidden = self.network(x)

        # LSTM logic
        batch_size = lstm_state[0].shape[1]
        hidden = hidden.reshape((-1, batch_size, self.lstm.input_size))
        done = done.reshape((-1, batch_size))
        new_hidden = []
        for h, d in zip(hidden, done):
            h, lstm_state = self.lstm(
                h.unsqueeze(0),
                (
                    (1.0 - d).view(1, -1, 1) * lstm_state[0],
                    (1.0 - d).view(1, -1, 1) * lstm_state[1],
                ),
            )
            new_hidden += [h]
        new_hidden = torch.flatten(torch.cat(new_hidden), 0, 1)
        return new_hidden, lstm_state

    def get_value(self, x, lstm_state, done):
        hidden, _ = self.get_states(x, lstm_state, done)
        return self.critic(hidden)

    def get_action_and_value(self, x, lstm_state, done, action=None, mask=None):
        hidden, lstm_state = self.get_states(x, lstm_state, done)
        logits = self.actor(hidden)
        if mask is not None:
            logits = torch.where(mask, logits, torch.tensor(-1e+8, device=logits.device))
        probs = Categorical(logits=logits)
        if action is None:
            action = probs.sample()
        return action, probs.log_prob(action), probs.entropy(), self.critic(hidden), lstm_state