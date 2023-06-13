package gui;

import core.AbstractPlayer;
import core.Game;
import games.GameType;
import players.PlayerFactory;
import players.human.ActionController;
import players.human.HumanGUIPlayer;
import utilities.Utils;

import java.io.File;
import java.util.*;

public class FrontendSimple {

    public static void main(String[] args) {
        String gameType = Utils.getArg(args, "game", "Sirius");
        int turnPause = Utils.getArg(args, "turnPause", 500);
        int nPlayers = Utils.getArg(args, "nPlayers", 3);
        long seed = Utils.getArg(args, "seed", System.currentTimeMillis());
        Random rnd = new Random(seed);
        String agentDir = Utils.getArg(args, "agentDir", "data/" + gameType + "/agents/");
        ActionController ac = new ActionController();

        File dir = new File(agentDir);
        if (!dir.exists()) {
            throw new AssertionError("Agent directory " + agentDir + " does not exist");
        }
        List<AbstractPlayer> allAgents = PlayerFactory.createPlayers(agentDir);
        Collections.shuffle(allAgents, rnd);

        /* Set up players for the game */
        List<AbstractPlayer> players = new ArrayList<>();
        int totalAgents = allAgents.size();

        int humanPosition = rnd.nextInt(nPlayers);
        for (int i = 0; i < nPlayers; i++) {
            if (i == humanPosition) {
                players.add(new HumanGUIPlayer(ac));
            } else {
                players.add(allAgents.get(i % totalAgents));
            }
        }

        /* Game parameter configuration. Set to null to ignore and use default parameters */
        String gameParams = null;

        /* Run! */
        Game.runOne(GameType.valueOf(gameType), gameParams, players, seed, false, null, ac, turnPause);
    }

    // stage 2 is then to feed in a directory of agents

}
