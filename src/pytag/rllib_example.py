import time
import random

import numpy as np
import gym
import ray
from ray import tune, air
from ray.rllib.models import ModelCatalog
from ray.rllib.models.torch.torch_modelv2 import TorchModelV2
from ray.rllib.models.torch.fcnet import FullyConnectedNetwork as TorchFC
from ray.tune.registry import get_trainable_cls
from ray.rllib.utils.framework import try_import_torch
from ray.rllib.algorithms import ppo

torch, nn = try_import_torch()


from pyTAG import PyTAG


class Diamant(gym.Env):
    def __init__(self, env_config):
        agents = ["python", "random"]
        self.env = PyTAG(agents=agents, game="Diamant")
        obs_space = np.ones(self.env.observation_space, dtype=np.float32) * 20
        self.action_space = gym.spaces.Discrete(self.env.action_space)
        self.observation_space = gym.spaces.Box(low=-obs_space, high=obs_space, dtype=np.float32)
    def reset(self):
        return self.env.reset()
    def step(self, action):
        return self.env.step(action)

class TorchCustomModel(TorchModelV2, nn.Module):
    """Example of a PyTorch custom model that just delegates to a fc-net."""

    def __init__(self, obs_space, action_space, num_outputs, model_config, name):
        TorchModelV2.__init__(
            self, obs_space, action_space, num_outputs, model_config, name
        )
        nn.Module.__init__(self)

        self.torch_sub_model = TorchFC(
            obs_space, action_space, num_outputs, model_config, name
        )

    def forward(self, input_dict, state, seq_lens):
        input_dict["obs"] = input_dict["obs"].float()
        fc_out, _ = self.torch_sub_model(input_dict, state, seq_lens)
        return fc_out, []

    def value_function(self):
        return torch.reshape(self.torch_sub_model.value_function(), [-1])

if __name__ == "__main__":
    ray.init()
    T_MAX = 1000
    agent = "PPO"

    ModelCatalog.register_custom_model(
        "simpleFC", TorchCustomModel
    )

    # algo = ppo.PPO(env=Diamant, config={
    #     "framework": "torch",
    #     "model": {
    #         "custom_model": "simpleFC",
    #         # Extra kwargs to be passed to your model's c'tor.
    #         "custom_model_config": {},
    #     },
    # })

    config = {
        "framework": "torch",
        "model": {"custom_model": "simpleFC",
                  "vf_share_layers": True},
        "environment": Diamant,


    }

    # config = (
    #     get_trainable_cls(agent)
    #     .get_default_config()
    #     # or "corridor" if registered above
    #     .environment(Diamant)
    #     .framework("torch")
    #     .rollouts(num_rollout_workers=1)
    #     .training(
    #         model={
    #             "custom_model": "simpleFC",
    #             "vf_share_layers": True,
    #         }
    #     )
    #     # Use GPUs iff `RLLIB_NUM_GPUS` env var set to > 0.
    #     # .resources(num_gpus=int(os.environ.get("RLLIB_NUM_GPUS", "0")))
    # )

    stop = {
        "timesteps_total": T_MAX,
    }

    tuner = tune.Tuner(
        "PPO",
        tune_config=tune.TuneConfig(
            metric="episode_reward_mean",
            mode="max",
            num_samples=1,
        ),
        param_space= {
            "env": Diamant,
            "num_workers": 1,
            "framework": "torch",
            "model": {"custom_model": "simpleFC",
                      "vf_share_layers": True},
            "environment": Diamant,},
    )
    results = tuner.fit()
    ray.shutdown()


