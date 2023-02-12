import time
import argparse
import random
from collections import deque

import torch
import wandb
import numpy as np

from pyTAG import PyTAG
from ppo.agent import PPO


def process_obs(obs, device="cpu"):
    x = torch.from_numpy(obs)
    x = x.unsqueeze(0).float().to(device)

    return x

def mask_logits(logits, mask):
    logits * torch.tensor(mask)
    return logits

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='PyTAG')
    parser.add_argument('--seed', type=int, default=-1, help='Random seed')
    parser.add_argument('--max-steps', type=int, default=int(5e5), metavar='STEPS', help='Number of agent-env interactions')

    # RL args
    parser.add_argument('--gamma', type=float, default=0.9, help='Discount rate')
    parser.add_argument('--replay-frequency', type=int, default=1024, metavar='k',
                        help='Frequency of sampling from memory')
    parser.add_argument('--hidden-size', type=int, default=64, metavar='SIZE', help='Network hidden size')
    parser.add_argument('--model', type=str, metavar='PARAMS', help='Pretrained model (state dict)')

    # PPO args
    parser.add_argument('--k-epochs', type=int, default=5,
                        help='Number of epochs to train on collected data per update')
    parser.add_argument('--lr-actor', type=float, default=0.0003, help='learning rate for actor network')
    parser.add_argument('--lr-critic', type=float, default=0.001, help='learning rate for critic network')
    parser.add_argument('--eps-clip', type=float, default=0.2, help='clip parameter for PPO')

    parser.add_argument('--gpu-id', type=int, default=0,
                        help='Specify a GPU ID, -1 refers to CPU')
    args = parser.parse_args()
    if args.seed == -1:
        args.seed = np.random.randint(1, 10000)
        print(f"chosen seed = {args.seed}")

    np.random.seed(args.seed)
    torch.manual_seed(np.random.randint(1, 10000))
    if torch.cuda.is_available() and args.gpu_id != -1:
        args.device = torch.device(f'cuda:{args.gpu_id}')
        torch.cuda.manual_seed(np.random.randint(1, 10000))
        # torch.backends.cudnn.enabled = args.enable_cudnn
    else:
        args.device = torch.device('cpu')

    max_reward = 1.0 # reward clipping
    disable_wandb = False
    project_name = "TAG-PPO"
    if disable_wandb:
        wandb.init(project=project_name, mode="disabled")
    else:
        wandb.init(project=project_name)

    agents = ["python", "random"]
    env = PyTAG(agents=agents, game="Diamant")
    agent = PPO(args, env)

    start_time = time.time()
    wins = 0
    episodes = 0
    done = True
    ep_steps = 0
    running_wins = deque(maxlen=20)
    for step in range(args.max_steps):
        if done:
            # logging
            episodes += 1

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
            obs = process_obs(env.reset(), device=args.device)
            done = False

        if step % args.replay_frequency == 0 and step > 1000:
            agent.learn(step)

        ep_steps += 1
        action, log_probs = agent.act(obs, env.getActionMask())

        next_obs, reward, done, info = env.step(action)
        # reward = np.clip(reward, -1, max_reward) # value, min, max

        next_obs = process_obs(next_obs, device=args.device)
        agent.mem.append(obs, action, log_probs, reward, done)
        obs = next_obs
        rewards+=reward

    env.close()

