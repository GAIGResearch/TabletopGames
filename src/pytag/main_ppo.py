import time
import argparse
import random
from collections import deque

import torch
import wandb
import numpy as np

# from pyTAG import PyTAG
from gym_.envs import TagSingleplayerGym
from gym_.wrappers import MergeActionMaskWrapper
import gymnasium as gym
from gymnasium.vector import AsyncVectorEnv, SyncVectorEnv
from ppo.agent import PPO


def process_obs(obs, device="cpu"):
    x = torch.from_numpy(obs)
    x = x.float().to(device)

    # mask = torch.from_numpy(obs[1]["action_mask"])
    return x

def mask_logits(logits, mask):
    logits * torch.tensor(mask)
    return logits

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='PyTAG')
    parser.add_argument('--seed', type=int, default=-1, help='Random seed')
    parser.add_argument('--max-steps', type=int, default=int(5e5), metavar='STEPS', help='Number of agent-env interactions')
    parser.add_argument('--n-envs', type=int, default=1, metavar='STEPS', help='Number of agent-env interactions')

    # RL args
    parser.add_argument('--gamma', type=float, default=0.95, help='Discount rate')
    parser.add_argument('--replay-frequency', type=int, default=512, metavar='k',
                        help='Frequency of sampling from memory')
    parser.add_argument('--hidden-size', type=int, default=64, metavar='SIZE', help='Network hidden size')
    parser.add_argument('--model', type=str, metavar='PARAMS', help='Pretrained model (state dict)')

    # PPO args
    parser.add_argument('--k-epochs', type=int, default=10,
                        help='Number of epochs to train on collected data per update')
    parser.add_argument('--lr-actor', type=float, default=0.003, help='learning rate for actor network')
    parser.add_argument('--lr-critic', type=float, default=0.05, help='learning rate for critic network')
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

    disable_wandb = False
    project_name = "TAG-PPO"
    if disable_wandb:
        wandb.init(project=project_name, mode="disabled")
    else:
        wandb.init(project=project_name)

    agents = ["python", "random"]
    # env = PyTAG(agents=agents, game="Diamant")
    env = SyncVectorEnv([
        lambda: gym.make("TAG/Diamant")
        for i in range(args.n_envs)
    ])
    env = MergeActionMaskWrapper(env)

    agent = PPO(args, env)

    start_time = time.time()
    wins = 0
    episodes = 0
    done = [False] * env.num_envs
    ep_steps = 0
    running_wins = deque(maxlen=20)
    obs, info = env.reset()
    obs = process_obs(obs, device=args.device)
    mask = torch.from_numpy(info["action_mask"]).to(args.device)
    for step in range(args.max_steps//args.n_envs):

        if done[0]:
            # logging
            episodes += 1

            if step > 0:
                running_wins.append(info["final_info"][0]["has_won"])
                # running_wins.append(info["has_won"][0])
                # if env.has_won():
                #     wins += 1

                wandb.log({
                    "train/total_steps": step * args.n_envs,
                    "train/steps": ep_steps,
                    "train/win_rate": np.mean(running_wins),
                    "train/rewards": rewards,
                })

            # reset
            rewards = 0
            ep_steps = 0
            # obs, info = env.reset()
            # obs = process_obs(obs, device=args.device)
            # mask = torch.from_numpy(info["action_mask"])
            # done = False

        if step % args.replay_frequency == 0 and step > 0:
            agent.learn(step)

        ep_steps += 1
        action, log_probs = agent.act(obs, mask)

        next_obs, reward, done, truncation, next_info = env.step(action.detach().cpu().numpy())

        next_obs = process_obs(next_obs, device=args.device)
        next_mask = torch.from_numpy(next_info["action_mask"]).to(args.device)
        agent.mem.append(obs, action, mask, log_probs, reward, done)
        obs = next_obs
        mask = next_mask
        info = next_info
        rewards+=reward[0]

    env.close()

