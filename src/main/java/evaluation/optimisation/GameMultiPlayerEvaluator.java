package evaluation.optimisation;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.interfaces.IStateHeuristic;
import evodef.MultiSolutionEvaluator;
import evodef.SearchSpace;
import games.GameType;

import java.util.*;

/**
 * Game Evaluator is used for NTBEA optimisation of parameters. It implements the SolutionEvaluator interface.
 * On each NTBEA trial the evaluate(int[] settings) function is called with the set of parameters to try next.
 * The meaning of these settings is encapsulated in the AgentSearchSpace, as this will vary with whatever is being
 * optimised.
 */
public class GameMultiPlayerEvaluator implements MultiSolutionEvaluator {

    GameType game;
    ITPSearchSpace searchSpace;
    int nPlayers;
    int nEvals = 0;
    Random rnd;
    IStateHeuristic stateHeuristic;


    /**
     * GameEvaluator
     *
     * @param game             The game that will be run for each trial. After each trial it is reset().
     * @param parametersToTune The ITunableParameters object that defines the parameter space we are optimising over.
     *                         This will vary with whatever is being optimised.
     * @param seed             Random seed to use
     */
    public <T> GameMultiPlayerEvaluator(GameType game, ITPSearchSpace parametersToTune,
                                        int nPlayers, IStateHeuristic stateHeuristic,
                                        long seed) {
        this.game = game;
        this.searchSpace = parametersToTune;
        this.stateHeuristic = stateHeuristic;
        this.nPlayers = nPlayers;
        this.rnd = new Random(seed);
    }

    @Override
    public void reset() {
        nEvals = 0;
    }


    /**
     * There should never be a need to call this method directly. It is called by the NTBEA framework as needed.
     *
     * @param settings is an integer array per player corresponding to the searchSpace.
     *                 The length of settings corresponds to searchSpace.nDims()
     *                 the value of settings[i] is a number in [0, searchSpace.nValues(i)]
     *                 the actual underlying parameter value can be found with searchSpace.value(i, settings[i])
     * @return Returns the game score for the agent being optimised
     */
    @Override
    public double[] evaluate(List<int[]> settings) {
   //     System.out.printf("Starting evaluation %d of %n\t%s at %tT%n", nEvals,
     //           settings.stream().map(Arrays::toString).collect(joining(",\n\t")), System.currentTimeMillis());

        Game newGame = game.createGameInstance(nPlayers);
        int nTeams = newGame.getGameState().getNTeams();
        List<AbstractPlayer> allPlayers = new ArrayList<>(nTeams);

        for (int i = 0; i < nTeams; i++) {
            AbstractPlayer tunedPlayer = (AbstractPlayer) searchSpace.getAgent(settings.get(i));
            allPlayers.add(tunedPlayer);
        }

        newGame.reset(allPlayers, rnd.nextLong());

        newGame.run();
        AbstractGameState finalState = newGame.getGameState();

        nEvals++;
        double[] retValue = new double[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            int team = finalState.getTeam(i);
            retValue[team] = stateHeuristic.evaluateState(finalState, i);
        }

     //   System.out.printf("Result : %s%n", Arrays.toString(retValue));
        return retValue;
    }

    /**
     * @return The searchSpace
     */
    @Override
    public SearchSpace searchSpace() {
        return searchSpace;
    }

}
