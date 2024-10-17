package evaluation.tournaments;

import utilities.Utils;

import java.util.Random;
import java.util.function.IntSupplier;
import java.util.stream.IntStream;

/**
 * This is a measure to reduce variance by ensuring that each agent plays an equal number of games.
 * It shuffles the list of agent indices, and runs through this.
 * Once it reaches the end it reshuffles the list and starts again.
 */
class PermutationCycler implements IntSupplier {

    int[] currentPermutation;
    int currentPosition;
    int nPlayers;
    Random rnd;

    public PermutationCycler(int maxNumberExclusive, Random rnd, int nPlayers) {
        this.rnd = rnd;
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
        System.arraycopy(currentPermutation, currentPermutation.length - (nPlayers - 1), leastEntries, 0, leastEntries.length);
        for (int i = 0; i < currentPermutation.length - 1; i++) {
            int swapPosition = rnd.nextInt(currentPermutation.length - i) + i;
            if (swapPosition != i && !overlapRisk(i, swapPosition, leastEntries)) {
                Utils.swap(currentPermutation, i, swapPosition);
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
