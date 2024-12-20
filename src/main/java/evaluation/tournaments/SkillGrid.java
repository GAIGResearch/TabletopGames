package evaluation.tournaments;

import core.AbstractParameters;
import core.AbstractPlayer;
import evaluation.RunArg;
import evaluation.listeners.IGameListener;
import games.GameType;

import java.util.*;

import static evaluation.RunArg.*;

/**
 * This is a wrapper to run one RoundRobin tournament on a ONE_VS_ALL basis between each pair
 * of agents in the given list of agents.
 */
public class SkillGrid {

    List<AbstractPlayer> agents;
    Map<RunArg, Object> config;

    public SkillGrid(List<AbstractPlayer> agents, Map<RunArg, Object> config) {
        this.agents = agents;
        this.config = config;
    }

    public void run() {
        // sort in alphabetical order
        agents.sort(Comparator.comparing(AbstractPlayer::toString));
        config.put(RunArg.mode, "onevsall");

        // We iterate through each pair of agents
        // agentTwo is the player that will have a single copy against multiple copies of agentOne
        // For a SkillGrid we'd like to get some results early, so we start with playing everyone against the weakest agent
        for (int agentOneIndex = 0; agentOneIndex < agents.size(); agentOneIndex++) {
            for (int agentTwoIndex = agentOneIndex + 1; agentTwoIndex < agents.size(); agentTwoIndex++) {
                GameType gameType = GameType.valueOf((String) config.get(game));
                AbstractParameters params = config.get(gameParams).equals("") ? null : AbstractParameters.createFromFile(gameType, (String) config.get(gameParams));

                RoundRobinTournament tournament = new RoundRobinTournament(
                        Arrays.asList(agents.get(agentTwoIndex), agents.get(agentOneIndex)),
                        gameType,
                        (Integer) config.get(RunArg.nPlayers),
                        params,
                        config);

                // Add listeners
                //noinspection unchecked
                for (String listenerClass : ((List<String>) config.get(listener))) {
                    IGameListener gameTracker = IGameListener.createListener(listenerClass);
                    tournament.addListener(gameTracker);
                    List<String> directories = new ArrayList<>();
                    directories.add((String) config.get(destDir));
                    String subDir = agents.get(agentTwoIndex).toString() + " vs " + agents.get(agentOneIndex).toString();
                    directories.add(subDir);
                    gameTracker.setOutputDirectory(directories.toArray(new String[0]));
                }

                // run tournament
                tournament.run();
            }
        }

    }
}
