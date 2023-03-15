# docs and experiment results can be found at https://docs.cleanrl.dev/rl-algorithms/ppo/#ppopy
import argparse
import os
import random
import time
from distutils.util import strtobool
import gym_
import gymnasium as gym
import numpy as np
import torch

from src.pytag.gym_.wrappers import MergeActionMaskWrapper, StrategoWrapper
from src.pytag.ppo import make_env
from src.pytag.utils.networks import PPONet

def parse_args():
    # fmt: off
    parser = argparse.ArgumentParser()
    parser.add_argument("--exp-name", type=str, default=os.path.basename(__file__).rstrip(".py"),
        help="the name of this experiment")
    parser.add_argument('--model', type=str, default="", help="Path to pre-trained model")
    parser.add_argument("--seed", type=int, default=1,
        help="seed of the experiment")
    parser.add_argument("--torch-deterministic", type=lambda x: bool(strtobool(x)), default=True, nargs="?", const=True,
        help="if toggled, `torch.backends.cudnn.deterministic=False`")
    parser.add_argument("--cuda", type=lambda x: bool(strtobool(x)), default=True, nargs="?", const=True,
        help="if toggled, cuda will be enabled by default")
    parser.add_argument("--track", type=lambda x: bool(strtobool(x)), default=False, nargs="?", const=True,
        help="if toggled, this experiment will be tracked with Weights and Biases")
    parser.add_argument("--wandb-project-name", type=str, default="cleanRL",
        help="the wandb's project name")
    parser.add_argument("--wandb-entity", type=str, default=None,
        help="the entity (team) of wandb's project")
    parser.add_argument("--capture-video", type=lambda x: bool(strtobool(x)), default=False, nargs="?", const=True,
        help="whether to capture videos of the agent performances (check out `videos` folder)")

    # Algorithm specific arguments
    parser.add_argument("--env-id", type=str, default="TAG/Diamant-v0",
        help="the id of the environment")
    parser.add_argument("--episodes", type=int, default=50,
        help="total timesteps of the experiments")


    args = parser.parse_args()
    return args


if __name__ == "__main__":
    args = parse_args()
    run_name = f"{args.env_id}__{args.exp_name}__{args.seed}__{int(time.time())}"

    # TRY NOT TO MODIFY: seeding
    random.seed(args.seed)
    np.random.seed(args.seed)
    torch.manual_seed(args.seed)
    torch.backends.cudnn.deterministic = args.torch_deterministic

    device = torch.device("cuda" if torch.cuda.is_available() and args.cuda else "cpu")

    # env = gym.make(args.env_id)
    # if "Stratego" in args.env_id:
    #     env = StrategoWrapper(env)
    env = gym.vector.SyncVectorEnv(
        [make_env(args.env_id, args.seed, 0, args.capture_video, run_name)]
    )
    env = MergeActionMaskWrapper(env)
    env = gym.wrappers.RecordEpisodeStatistics(env)

    agent = PPONet(args, env).to(device)
    if args.model:  # Load pretrained model if provided
        if os.path.isfile(args.model):
                checkpoint = torch.load(args.model,
                                        map_location='cpu')  # Always load tensors onto CPU by default, will shift to GPU if necessary
                agent.load_state_dict(checkpoint)
                agent.to(device)
                print("Loading pretrained model: " + args.model)
        else:  # Raise error if incorrect model path provided
            raise FileNotFoundError(args.model)
    rewards = []
    wins = []
    steps = []
    done = False

    EVAL_EPISODES = 10
    for episode in range(EVAL_EPISODES):
        ep_rewards = 0
        ep_steps = 0
        done = False
        obs, info = env.reset()
        mask = torch.from_numpy(info["action_mask"]).to(device)
        obs = torch.Tensor(obs).to(device)

        while not done:
            action, logprob, _, value = agent.get_action_and_value(obs, mask=mask)


            # TRY NOT TO MODIFY: execute the game and log data.
            next_obs, reward, done, truncated, info = env.step(action.cpu().numpy())
            next_masks = torch.from_numpy(info["action_mask"]).to(device)
            next_obs, next_done = torch.Tensor(next_obs).to(device), torch.Tensor(done).to(device)

            ep_rewards += reward
            ep_steps += 1

            obs = next_obs
            mask = next_masks

            if done:
                rewards.append(ep_rewards)
                wins.append(info["final_info"][0]["has_won"])
                steps.append(ep_steps)
    print(f"Evaluation done Rewards={np.mean(ep_rewards)} wins={np.mean(wins)} steps={np.mean(steps)}")

    env.close()