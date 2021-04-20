package evaluation;

import core.AbstractPlayer;
import games.GameType;

import java.util.*;
import java.util.function.IntSupplier;
import java.util.stream.IntStream;

public class RandomRRTournament extends RoundRobinTournament {

    private int totalMatchups;
    private IntSupplier idStream;

    /**
     * Create a round robin tournament, which plays all agents against all others.
     *
     * @param agents          - players for the tournament.
     * @param gameToPlay      - game to play in this tournament.
     * @param playersPerGame  - number of players per game.
     * @param gamesPerMatchUp - number of games for each combination of players.
     * @param selfPlay        - true if agents are allowed to play copies of themselves.
     */
    public RandomRRTournament(LinkedList<AbstractPlayer> agents, GameType gameToPlay, int playersPerGame,
                              int gamesPerMatchUp, boolean selfPlay, int totalMatchUps, long seed) {
        super(agents, gameToPlay, playersPerGame, gamesPerMatchUp, selfPlay);
        this.totalMatchups = totalMatchUps;
        idStream = new PermutationCycler(agents.size(), seed, playersPerGame);
    }

    /**
     * Instead of recursively constructing all possible combinations of players for the game (as in the super-class)
     * here we create random matchups. This is designed for large numbers of players, for which an exhaustive
     * search of all permutations would be prohibitive.
     *
     * @param ignored - this input is ignored
     * @param gameIdx - index of game to play with this match-up.
     */
    @Override
    public void createAndRunMatchUp(LinkedList<Integer> ignored, int gameIdx) {
        int nPlayers = playersPerGame.get(gameIdx);
        for (int i = 0; i < totalMatchups; i++) {
            List<Integer> matchup = new ArrayList<>(nPlayers);
            for (int j = 0; j < nPlayers; j++)
                matchup.add(idStream.getAsInt());
            evaluateMatchUp(matchup, gameIdx);
        }
    }

    /**
     * This is a measure to reduce variance by ensuring that each agent plays an equal number of games.
     * It shuffles the list of agent indices, and runs through this.
     * Once it reaches the end it reshuffles the list and starts again.
     */
    static class PermutationCycler implements IntSupplier {

        int[] currentPermutation;
        int currentPosition;
        int nPlayers;
        Random rnd;

        public PermutationCycler(int maxNumberExclusive, long seed, int nPlayers) {
            currentPermutation = IntStream.range(0, maxNumberExclusive).toArray();
            currentPosition = -1;
            rnd = new Random(seed);
            this.nPlayers = nPlayers;
            shuffle();
        }

        /**
         * Uses the $famousName algorithm for shuffling an array in situ
         * <p>
         * the only tweak is to ensure that on the reshuffle we don;t have an overlap of ids within the nPlayer range
         */
        private void shuffle() {
            int[] leastEntries = new int[nPlayers];
            for (int i = 0; i < nPlayers; i++)
                leastEntries[i] = currentPermutation[currentPermutation.length - nPlayers + i];
            for (int i = 0; i < currentPermutation.length - 1; i++) {
                int swapPosition = rnd.nextInt(currentPermutation.length - i) + i;
                if (swapPosition != i && !overlapRisk(i, swapPosition, leastEntries)) {
                    int a = currentPermutation[i];
                    currentPermutation[i] = currentPermutation[swapPosition];
                    currentPermutation[swapPosition] = a;
                }
            }
        }

        private boolean overlapRisk(int i1, int i2, int[] lastValues) {
            // the problem only occurs if we are populating one of the first nPlayer indices (i1), with a value that
            // is in the last (nPlayer - i1) values of the previous permutation
            if (i1 >= nPlayers) return false;
            for (int i = nPlayers - 1; i >= i1; i--) {
                if (lastValues[i] == currentPermutation[i2]) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int getAsInt() {
            currentPosition++;
            if (currentPosition == currentPermutation.length) {
                currentPosition = 0;
                shuffle();
            }
            return currentPermutation[currentPosition];
        }
    }
}
