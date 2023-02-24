import time
import random
from collections import deque

import torch
import wandb
import numpy as np

from pyTAG import PyTAG
from dqn.agent import DQNAgent

def get_epsilon(steps, max_steps, final_epsilon=0.0):
  # linearly decay towards 0
  lr = ((max_steps - steps) / max_steps)  # * 1 - final_epsilon
  if (lr < final_epsilon):
    lr = final_epsilon
  return lr

def mask_logits(logits, mask):
    logits = logits * torch.tensor(mask, dtype=torch.float32)
    return logits

def process_obs(obs, device="cpu"):
    x = torch.from_numpy(obs)
    x = x.unsqueeze(0).float().to(device)

    return x

if __name__ == "__main__":
    max_reward = 1.0 # reward clipping
    disable_wandb = False
    project_name = "TAG-DQN"
    epsilon = 1.0
    if disable_wandb:
        wandb.init(project=project_name, mode="disabled")
    else:
        wandb.init(project=project_name)
    MAX_STEPS = 1000000
    replay_freq = 4 # number of steps
    agents = ["python", "random"]
    env = PyTAG(agents=agents, game="Diamant")
    agent = DQNAgent(env)

    start_time = time.time()
    wins = 0
    episodes = 0
    done = True
    invalid_action = False
    ep_steps = 0
    running_wins = deque(maxlen=20)
    for step in range(MAX_STEPS):
        if done:
            # logging
            episodes += 1
            epsilon = get_epsilon(step, MAX_STEPS, 0.05)

            if step > 0:
                running_wins.append(env.has_won())
                if env.has_won():
                    wins += 1

                wandb.log({
                    "train/steps": ep_steps,
                    "train/win_rate": np.mean(running_wins),
                    "train/rewards": rewards,

                })
            # reset
            rewards = 0
            ep_steps = 0
            obs = process_obs(env.reset())
            done = False

        if step % replay_freq == 0 and step > 1000:
            agent.learn(step)

        ep_steps += 1
        # masking happens on the agent side
        action, q = agent.act(obs, env.getActionMask(), epsilon=epsilon)

        playerID = env.getPlayerID()
        next_obs, reward, done, info = env.step(action)
        # reward = np.clip(reward, -1, max_reward) # value, min, max

        next_obs = process_obs(next_obs)
        agent.mem.append(obs, action, reward, done)
        obs = next_obs
        rewards+=reward

    env.close()

