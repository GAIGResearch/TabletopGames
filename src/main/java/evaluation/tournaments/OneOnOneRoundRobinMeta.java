package evaluation.tournaments;

import core.AbstractParameters;
import core.AbstractPlayer;
import evaluation.RunArg;
import evaluation.listeners.IGameListener;
import games.GameType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static evaluation.RunArg.*;

/**
 * This is a wrapper to run one RoundRobin tournament on a ONE_VS_ALL basis between each pair
 * of agents in the given list of agents.
 */
public class OneOnOneRoundRobinMeta {

    List<AbstractPlayer> agents;
    Map<RunArg, Object> config;

    public OneOnOneRoundRobinMeta(List<AbstractPlayer> agents, Map<RunArg, Object> config) {
        this.agents = agents;
        this.config = config;
    }

    public void run() {

        // We iterate through each pair of agents
        for (int agentOneIndex = 0; agentOneIndex < agents.size(); agentOneIndex++) {
            for (int agentTwoIndex = agentOneIndex + 1; agentTwoIndex < agents.size(); agentTwoIndex++) {

                GameType gameType = GameType.valueOf((String) config.get(game));
                AbstractParameters params = config.get(gameParams).equals("") ? null : AbstractParameters.createFromFile(gameType, (String) config.get(gameParams));

                RoundRobinTournament tournament = new RoundRobinTournament(
                        Arrays.asList(agents.get(agentOneIndex), agents.get(agentTwoIndex)),
                        gameType,
                        (Integer) config.get(RunArg.nPlayers),
                        (Integer) config.get(RunArg.matchups),
                        AbstractTournament.TournamentMode.ONE_VS_ALL,
                        params);

                // Add listeners
                //noinspection unchecked
                for (String listenerClass : ((List<String>) config.get(listener))) {
                    IGameListener gameTracker = IGameListener.createListener(listenerClass, (String) config.get(metrics));
                    tournament.addListener(gameTracker);
                    List<String> directories = new ArrayList<>();
                    directories.add((String) config.get(destDir));
                    String subDir = agents.get(agentOneIndex).toString() + " vs " + agents.get(agentTwoIndex).toString();
                    directories.add(subDir);
                    gameTracker.setOutputDirectory(directories.toArray(new String[0]));
                }

                // run tournament
                tournament.setRandomSeed( (long) config.get(RunArg.seed));
                tournament.setVerbose((boolean) config.get(verbose));
                tournament.setResultsFile((String) config.get(output));
                tournament.setRandomGameParams((boolean) config.get(randomGameParams));
                tournament.run();
            }
        }

    }
}
