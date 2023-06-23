package evaluation;

import core.AbstractParameters;
import core.AbstractPlayer;
import core.Game;
import core.interfaces.IGameHeuristic;
import core.interfaces.IStateHeuristic;
import core.interfaces.IStatisticLogger;
import evodef.SearchSpace;
import evodef.SolutionEvaluator;
import games.GameType;

import java.util.*;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

/**
 * Game Evaluator is used for NTBEA optimisation of parameters. It implements the SolutionEvaluator interface.
 * On each NTBEA trial the evaluate(int[] settings) function is called with the set of parameters to try next.
 * The meaning of these settings is encapsulated in the AgentSearchSpace, as this will vary with whatever is being
 * optimised.
 */
public class GameEvaluator implements SolutionEvaluator {

    public IStatisticLogger statsLogger;
    public boolean debug = false;
    GameType game;
    AbstractParameters gameParams;
    ITPSearchSpace searchSpace;
    int nPlayers;
    List<AbstractPlayer> opponents;
    int nEvals = 0;
    Random rnd;
    boolean avoidOppDupes;
    boolean fullyCoop;
    IStateHeuristic stateHeuristic;
    IGameHeuristic gameHeuristic;

    /**
     * GameEvaluator
     *
     * @param game                    The game that will be run for each trial. After each trial it is reset().
     * @param parametersToTune        The ITunableParameters object that defines the parameter space we are optimising over.
     *                                This will vary with whatever is being optimised.
     * @param opponents               A List of opponents to be played against. In each trial a random set of these opponents will be
     *                                used in addition to the main agent being tested.
     *                                To use the same set of opponents in each game, this should contain N-1 AbstractPlayers, where
     *                                N is the number of players in the game.
     * @param seed                    Random seed to use
     * @param avoidOpponentDuplicates If this is true, then each individual in opponents will only be used once per game.
     *                                If this is false, then it is important not to use AbstractPlayers that maintain
     *                                any state, or that make any use of their playerId. (So RandomPlayer is fine.)
     */
    public GameEvaluator(GameType game, ITPSearchSpace parametersToTune,
                         AbstractParameters gameParams,
                         int nPlayers,
                         List<AbstractPlayer> opponents, long seed,
                         IStateHeuristic stateHeuristic, IGameHeuristic gameHeuristic,
                         boolean avoidOpponentDuplicates) {
        this.game = game;
        this.gameParams = gameParams;
        this.searchSpace = parametersToTune;
        this.nPlayers = nPlayers;
        this.stateHeuristic = stateHeuristic;
        this.gameHeuristic = gameHeuristic;
        this.opponents = opponents;
        this.rnd = new Random(seed);
        this.avoidOppDupes = avoidOpponentDuplicates && opponents.size() > 1;
        if (avoidOppDupes && opponents.size() < nPlayers - 1)
            throw new AssertionError("Insufficient Opponents to avoid duplicates");
        if (opponents.isEmpty()) fullyCoop = true;
    }

    @Override
    public void reset() {
        nEvals = 0;
    }

    @Override
    public double evaluate(double[] doubles) {
        throw new AssertionError("No need for implementation according to NTBEA library javadoc");
    }

    /**
     * There should never be a need to call this method directly. It is called by the NTBEA framework as needed.
     *
     * @param settings is an integer array corresponding to the searchSpace.
     *                 The length of settings corresponds to searchSpace.nDims()
     *                 the value of settings[i] is a number in [0, searchSpace.nValues(i)]
     *                 the actual underlying parameter value can be found with searchSpace.value(i, settings[i])
     * @return Returns the game score for the agent being optimised
     */
    @Override
    public double evaluate(int[] settings) {
        if (debug)
            System.out.printf("Starting evaluation %d of %s at %tT%n", nEvals,
                    Arrays.toString(settings), System.currentTimeMillis());
        Object configuredThing = searchSpace.getAgent(settings);
        boolean tuningPlayer = configuredThing instanceof AbstractPlayer;
        boolean tuningGame = configuredThing instanceof Game;

        List<AbstractPlayer> allPlayers = new ArrayList<>(nPlayers);

        // We can reduce variance here by cycling the playerIndex on each iteration
        // If we're not tuning the player, then setting index to -99 means we just use the provided opponents list
        int playerIndex = tuningPlayer ? nEvals % nPlayers : -99;

        // create a random permutation of opponents - this is used if we want to avoid opponent duplicates
        // if we allow duplicates, then we randomise them all independently
        List<Integer> opponentOrdering = IntStream.range(0, opponents.size()).boxed().collect(toList());
        Collections.shuffle(opponentOrdering);
        int count = 0;
        for (int i = 0; i < nPlayers; i++) {
            if (!fullyCoop && i != playerIndex) {
                int oppIndex = (avoidOppDupes) ? count : rnd.nextInt(opponents.size());
                count = (count + 1) % nPlayers;
                allPlayers.add(opponents.get(oppIndex).copy());
            } else {
                AbstractPlayer tunedPlayer = (AbstractPlayer) searchSpace.getAgent(settings); // we create for each, in case this is coop
                allPlayers.add(tunedPlayer);
            }
        }

        Game newGame = tuningGame ? (Game) configuredThing : game.createGameInstance(nPlayers, gameParams);
        // always reset the random seed for each new game
        newGame.reset(allPlayers, rnd.nextLong());

        newGame.run();

        nEvals++;
        return tuningGame ? gameHeuristic.evaluateGame(newGame) : stateHeuristic.evaluateState(newGame.getGameState(), playerIndex);
    }

    /**
     * @return The searchSpace
     */
    @Override
    public SearchSpace searchSpace() {
        return searchSpace;
    }

    /**
     * @return The number of NTBEA iterations/trials that have been run so far
     */
    @Override
    public int nEvals() {
        return nEvals;
    }


}
