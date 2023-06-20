import os, random, time
import json

import jpype
import jpype.imports

import numpy as np
from typing import List

def get_agent_class(agent_name):
    if agent_name == "random":
        return jpype.JClass("players.simple.RandomPlayer")
    if agent_name == "mcts":
        return jpype.JClass("players.mcts.MCTSPlayer")
    if agent_name == "osla":
        return jpype.JClass("players.simple.OSLAPlayer")
    if agent_name == "python":
        return jpype.JClass("players.python.PythonAgent")
    return None

def get_mcts_with_params(json_path):
    PlayerFactory = jpype.JClass("players.PlayerFactory")
    with open(os.path.expanduser(json_path)) as json_file:
        json_string = json.load(json_file)
    json_string = str(json_string).replace('\'', '\"') # JAVA only uses " for string
    return jpype.JClass("players.mcts.MCTSPlayer")(PlayerFactory.fromJSONString(json_string))

class PyTAG():
    def __init__(self, agent_ids: List[str], game_id: str="Diamant", seed: int=0, obs_type:str="vector",  jar_path="jars/ModernBoardGame.jar", isNormalized = True):
        self._last_obs_vector = None
        self._last_action_mask = None
        self._rnd = random.Random(seed)
        self._obs_type = obs_type

        # start up the JVM
        tag_jar = os.path.join(os.path.dirname(__file__), 'jars', 'ModernBoardGame.jar')
        jpype.addClassPath(tag_jar)
        if not jpype.isJVMStarted():
            jpype.startJVM(convertStrings=False)

        # access to the java classes
        GYMEnv = jpype.JClass("core.GYMEnv")
        Utils = jpype.JClass("utilities.Utils")
        GameType = jpype.JClass("games.GameType")

        # Initialize the java environment
        gameType = GameType.valueOf(Utils.getArg([""], "game", game_id))

        if agent_ids[0] == "mcts":
            agents = [get_mcts_with_params(f"~/data/pyTAG/MCTS_for_{game_id}.json")() for agent_id in agent_ids]
        else:
            agents = [get_agent_class(agent_id)() for agent_id in agent_ids]
        self._playerID = agent_ids.index("python") # if multiple python agents this is the first one

        self._java_env = GYMEnv(gameType, None, jpype.java.util.ArrayList(agents), seed, True)

        # Construct action/observation space
        self._java_env.reset()
        action_mask = self._java_env.getActionMask()
        num_actions = len(action_mask)
        self.action_space = num_actions

        obs_size = int(self._java_env.getObservationSpace())
        self.observation_space = (obs_size,)
        self._action_tree_shape = 1

    def get_action_tree_shape(self):
        return self._action_tree_shape

    def reset(self):
        self._java_env.reset()
        self._update_data()

        return self._last_obs_vector, {"action_tree": self._action_tree_shape, "action_mask": self._last_action_mask,
                                       "has_won": int(
                                           str(self._java_env.getPlayerResults()[self._playerID]) == "WIN_GAME")}

    def step(self, action):
        # Verify
        if not self.is_valid_action(action):
            # Execute a random action
            valid_actions = np.where(self._last_action_mask)[0]
            action = self._rnd.choice(valid_actions)
            self._java_env.step(action)
            reward = -1
        else:
            self._java_env.step(action)
            reward = int(str(self._java_env.getPlayerResults()[self._playerID]) == "WIN_GAME")
            if str(self._java_env.getPlayerResults()[self._playerID]) == "LOSE_GAME": reward = -1

        self._update_data()
        done = self._java_env.isDone()
        info = {"action_mask": self._last_action_mask,
                "has_won": int(str(self._java_env.getPlayerResults()[self._playerID]) == "WIN_GAME")}
        return self._last_obs_vector, reward, done, info

    def close(self):
        jpype.shutdownJVM()

    def is_valid_action(self, action: int) -> bool:
        return self._last_action_mask[action]

    def _update_data(self):
        if self._obs_type == "vector":
            obs = self._java_env.getObservationVector()
            self._last_obs_vector = np.array(obs, dtype=np.float32)
        elif self._obs_type == "json":
            obs = self._java_env.getObservationJson()
            self._last_obs_vector = obs

        action_mask = self._java_env.getActionMask()
        self._last_action_mask = np.array(action_mask, dtype=bool)

    def getVectorObs(self):
        return self._java_env.getFeatures()

    def getJSONObs(self):
        return self._java_env.getObservationJson()

    def sample_rnd_action(self):
        valid_actions = np.where(self._last_action_mask)[0]
        action = self._rnd.choice(valid_actions)
        return action


    def getPlayerID(self):
        return self._java_env.getPlayerID()

    def has_won(self):
        return int(str(self._java_env.getPlayerResults()[self._playerID]) == "WIN_GAME")

def get_card_id(card):
    card_types = ["Maki-1", "Maki-2", "Maki-3", "Chopsticks", "Tempura", "Sashimi", "Dumpling", "SquidNigiri", "SalmonNigiri", "EggNigiri", "Wasabi", "Pudding"]
    card_emb = np.zeros(len(card_types))
    if card != "EmptyDeck":
        card_emb[card_types.index(card)] = 1
    return card_emb
def process_json_obs(json_obs, normalise=True):
    # actions represent cardIds from left to right
    # todo fix observation shape for N-players
    json_ = json.loads(str(json_obs))
    player_id = json_["PlayerID"]
    played_cards = json_["playedCards"].split(",")
    cards_in_hand = json_["cardsInHand"].split(",")
    score = json_["playerScore"] / 50 # keep it close to 0-1
    round = json_["rounds"] / 3 # max 3 rounds

    opp_scores = []
    opponent_played_cards_ = []
    for key in json_.keys():
        if f"opp" in key and "playedCards" in key:
            opp_played_cards = json_[key].split(",")
            opponent_played_cards_.append(([get_card_id(card) for card in opp_played_cards]))
        if f"opp" in key and "score" in key:
            opp_score = json_[key] / 50
            opp_scores.append(opp_score)

    played_cards_ = [get_card_id(card) for card in played_cards]
    cards_in_hand_ = [get_card_id(card)  for card in cards_in_hand]

    score = np.expand_dims(score, 0)
    round = np.expand_dims(round, 0)
    played_cards = np.stack(played_cards_).flatten()
    cards_in_hand = np.stack(cards_in_hand_, 0).flatten()
    opp_played_cards = np.stack(opponent_played_cards_, 1).flatten()
    obs = np.concatenate([score, round, played_cards, cards_in_hand, opp_played_cards, opp_scores])
    return obs


if __name__ == "__main__":
    EPISODES = 100
    players = ["python", "python"]
    env = PyTAG(players, game_id="SushiGo", obs_type="json")
    done = False

    start_time = time.time()
    steps = 0
    wins = 0
    for e in range(EPISODES):
        obs = env.reset()
        done = False
        while not done:
            steps += 1

            rnd_action = env.sample_rnd_action()

            obs, reward, done, info = env.step(rnd_action)
            json_obs  = env.getJSONObs()
            json_obs = process_json_obs(env.getJSONObs())
            if done:
                print(f"Game over rewards {reward} in {steps} steps results =  {env.has_won()}")
                if env.has_won():
                    wins += 1
                break

    print(f"win rate = {wins/EPISODES} {EPISODES} episodes done in {time.time() - start_time} with total steps = {steps}")
    env.close()

