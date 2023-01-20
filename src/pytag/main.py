import time
import random
from pyTAG import PyTAG

if __name__ == "__main__":
    EPISODES = 100
    agents = ["python", "random"]
    env = PyTAG(agents=agents, game="Diamant")
    done = False

    start_time = time.time()
    steps = 0
    wins = 0
    for e in range(EPISODES):
        obs = env.reset()
        done = False
        while not done:
            steps += 1

            rnd_action = random.randint(0, len(env.getActions())-1)
            # print(f"player {env.env.getPlayerID()} choose action {rnd_action}")
            obs, reward, done, info = env.step(rnd_action)
            if done:
                print(f"Game over rewards {reward} in {steps} steps results =  {env.env.getPlayerResults()[0]}")
                if str(env.env.getPlayerResults()[0]) == "WIN":
                    wins += 1
                break

    print(f"win rate = {wins/EPISODES} {EPISODES} episodes done in {time.time() - start_time} with total steps = {steps}")
    env.close()

