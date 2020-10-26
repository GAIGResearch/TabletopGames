package evaluation;

import core.*;
import evodef.*;

import java.util.*;

/**
 *  Game Evaluator is used for NTBEA optimisation of parameters. It implements the SolutionEvaluator interface.
 *  On each NTBEA trial the evaluate(int[] settings) function is called with the set of parameters to try next.
 *  The meaning of these settings is encapsulated in the AgentSearchSpace, as this will vary with whatever is being
 *  optimised.
 */
public class GameEvaluator implements SolutionEvaluator {

    Game game;
    AgentSearchSpace<AbstractPlayer> searchSpace;
    int nPlayers;
    List<AbstractPlayer> opponents;
    int nEvals = 0;
    Random rnd;
    boolean avoidOppDupes;

    /**
     * GameEvaluator
     *
     * @param game The game that will be run for each trial. After each trial it is reset().
     * @param searchSpace The AgentSearchSpace that defines the parameter space we are optimising over.
     *                    This will vary with whatever is being optimised.
     * @param opponents A List of opponents to be played against. In each trial a random set of these opponents will be
     *                  used in addition to the main agent being tested.
     *                  To use the same set of opponents in each game, this should contain N-1 AbstractPlayers, where
     *                  N is the number of players in the game.
     * @param rnd       Random thingy.
     * @param avoidOpponentDuplicates If this is true, then each individual in opponents will only be used once per game.
     *                                If this is false, then it is important not to use AbstractPlayers that maintain
     *                                any state, or that make any use of their playerId. (So RandomPlayer is fine.)
     *
     */
    public GameEvaluator(Game game, AgentSearchSpace<AbstractPlayer> searchSpace,
                         List<AbstractPlayer> opponents, Random rnd,
                         boolean avoidOpponentDuplicates) {
        this.game = game;
        this.searchSpace = searchSpace;
        this.nPlayers = game.getGameState().getNPlayers();
        this.opponents = opponents;
        this.rnd = rnd;
        this.avoidOppDupes = avoidOpponentDuplicates;
        if (avoidOppDupes && opponents.size() < nPlayers - 1)
            throw new AssertionError("Insufficient Opponents to avoid duplicates");
    }

    @Override
    public void reset() {
        nEvals = 0;
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
        return evaluate(searchSpace.convertSettings(settings));
    }

    /**
     * This is distinguished from the version with an int[] parameter in that the value of the input array
     * is the actual value of the underlying parameter where this is a Integer or Double. (For other types it remains
     * the index of the actual value.)
     * This should not need ot be called directly.
     *
     * @param settings
     * @return Returns the game score for the agent being optimised.
     */
    @Override
    public double evaluate(double[] settings) {

        double finalScore = 0.0;

        AbstractPlayer player = searchSpace.getAgent(settings);
        List<AbstractPlayer> allPlayers = new ArrayList<>(nPlayers);

        // We can reduce variance here by cycling the playerIndex on each iteration
        int playerIndex = nEvals % nPlayers;
        for (int i = 0; i < nPlayers; i++) {
            if (i != playerIndex) {
                int oppIndex;
                do {
                    oppIndex = rnd.nextInt(opponents.size());
                } while (avoidOppDupes && allPlayers.contains(opponents.get(oppIndex)));
                allPlayers.add(opponents.get(oppIndex));
            } else {
                allPlayers.add(player);
            }
        }

        game.reset(allPlayers, rnd.nextLong());

        game.run();

        AbstractGameState finalState = game.getGameState();
        finalScore += finalState.getScore(playerIndex);

        nEvals++;

        return finalScore;
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

    /**
     * @return Not used. Retained purely for obedience to interface.
     */
    @Override
    public EvolutionLogger logger() {
        return null;
    }


}
