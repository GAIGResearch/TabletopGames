package evaluation.tournaments;

import core.AbstractParameters;
import core.AbstractPlayer;
import games.GameType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.IntSupplier;
import java.util.stream.IntStream;

public class RandomRRTournament extends RoundRobinTournament {

    private int totalMatchups;
    private IntSupplier idStream;
    private int reportPeriod;

    /**
     * Create a round robin tournament, which plays all agents against all others.
     *
     * @param agents          - players for the tournament.
     * @param gameToPlay      - game to play in this tournament.
     * @param playersPerGame  - number of players per game.
     */
    public RandomRRTournament(List<? extends AbstractPlayer> agents, GameType gameToPlay, int playersPerGame,
                              TournamentMode tournamentMode, int totalMatchUps, int reportPeriod, long seed, AbstractParameters gameParams) {
        super(agents, gameToPlay, playersPerGame, 1, tournamentMode, gameParams);
        this.totalMatchups = totalMatchUps;
        this.reportPeriod = reportPeriod;
        idStream = new PermutationCycler(agents.size(), seed, playersPerGame);
    }

    /**
     * Instead of recursively constructing all possible combinations of players for the game (as in the super-class)
     * here we create random matchups. This is designed for large numbers of players, for which an exhaustive
     * search of all permutations would be prohibitive.
     *
     * @param ignored - this input is ignored
     */
    @Override
    public void createAndRunMatchUp(LinkedList<Integer> ignored) {
        for (int i = 0; i < totalMatchups; i++) {
            List<Integer> matchup = new ArrayList<>(playersPerGame);
            for (int j = 0; j < playersPerGame; j++)
                matchup.add(idStream.getAsInt());
            evaluateMatchUp(matchup);
            if(reportPeriod > 0 && (i+1) % reportPeriod == 0 && i != totalMatchups - 1)
                reportResults();
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
            if (maxNumberExclusive >= nPlayers)
                currentPermutation = IntStream.range(0, maxNumberExclusive).toArray();
            else {
                // in this case we ensure the agents we do have are all equally represented - self-play will occur
                currentPermutation = IntStream.range(0, maxNumberExclusive * nPlayers).toArray();
                for (int i = maxNumberExclusive; i < currentPermutation.length; i++) {
                    currentPermutation[i] = i % maxNumberExclusive;
                }
            }
            currentPosition = -1;
            rnd = new Random(seed);
            this.nPlayers = nPlayers;
            shuffle();
        }

        /**
         * Uses the $famousName algorithm for shuffling an array in situ
         * <p>
         * the only tweak is to ensure that on the reshuffle we don't have an overlap of ids within the nPlayer range
         */
        private void shuffle() {
            int[] leastEntries = new int[nPlayers - 1];
            for (int i = 0; i < leastEntries.length; i++)
                leastEntries[i] = currentPermutation[currentPermutation.length - (nPlayers - 1) + i];
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
            // the problem only occurs if we are populating one of the first (i1) nPlayer indices, with a value that
            // is in the last (nPlayer - i1 - 1) values of the previous permutation
            if (i1 >= nPlayers) return false;
            if (currentPermutation.length == nPlayers) return false;
            for (int i = nPlayers - 2; i >= i1; i--) {
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
