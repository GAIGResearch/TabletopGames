package games.cribbage;

import core.components.FrenchCard;

import java.util.ArrayList;
import java.util.List;

/**
 * Card values and scoring for Cribbage. These depend only on the cards (and the parameters), not on the game state.
 */
public final class CribbageUtils {

    private CribbageUtils() {
    }

    /**
     * The value of a card for scoring: Ace 1, court cards 10.
     */
    public static int pipValue(FrenchCard card) {
        return switch (card.type) {
            case Ace -> 1;
            case Number -> card.number;
            case Jack, Queen, King -> 10;
        };
    }

    /**
     * The rank of a card for pairs and runs, from Ace 1 to King 13.
     */
    public static int rank(FrenchCard card) {
        // FrenchCard numbers Aces as 14, but they are low in Cribbage
        return card.type == FrenchCard.FrenchCardType.Ace ? 1 : card.number;
    }

    /**
     * Points for pairs made by the last card of the sequence: the number of cards of the same rank immediately
     * before it decides pair, pair royal or double pair royal (CribbageParameters.pairPoints etc.).
     */
    public static int playPairPoints(List<FrenchCard> sequence, CribbageParameters params) {
        int last = sequence.size() - 1;
        int matching = 0;
        while (matching < last && rank(sequence.get(last - matching - 1)) == rank(sequence.get(last)))
            matching++;
        return switch (matching) {
            case 0 -> 0;
            case 1 -> params.pairPoints;
            case 2 -> params.pairRoyalPoints;
            default -> params.doublePairRoyalPoints;
        };
    }

    /**
     * Points for a run made by the last card of the sequence: the length of the longest run of 3 or more formed by
     * the last N cards (consecutive ranks in any order, no rank repeated), or 0.
     */
    public static int playRunPoints(List<FrenchCard> sequence) {
        for (int n = sequence.size(); n >= 3; n--) {
            if (isRun(sequence.subList(sequence.size() - n, sequence.size())))
                return n;
        }
        return 0;
    }

    /**
     * True if the cards have distinct ranks that are consecutive, in any order.
     */
    static boolean isRun(List<FrenchCard> cards) {
        int[] ranks = cards.stream().mapToInt(CribbageUtils::rank).sorted().toArray();
        for (int i = 1; i < ranks.length; i++)
            if (ranks[i] != ranks[i - 1] + 1) return false;
        return true;
    }

    /**
     * Points for a hand or crib in the show, with the starter: fifteens, pairs, runs, flush and his nobs.
     * Runs include the starter only if CribbageParameters.runsIncludeStarter; a crib flush needs the starter to
     * match only if CribbageParameters.cribFlushNeedsStarter.
     *
     * @param cards  the four cards of the hand or crib (not including the starter)
     * @param isCrib true when scoring the crib
     */
    public static int showScore(List<FrenchCard> cards, FrenchCard starter, boolean isCrib, CribbageParameters params) {
        List<FrenchCard> withStarter = new ArrayList<>(cards);
        withStarter.add(starter);
        int score = 0;

        // fifteens: every subset of the cards and the starter
        for (int subset = 1; subset < 1 << withStarter.size(); subset++) {
            int total = 0;
            for (int i = 0; i < withStarter.size(); i++)
                if ((subset >> i & 1) == 1) total += pipValue(withStarter.get(i));
            if (total == 15) score += params.fifteenPoints;
        }

        // pairs, by the number of cards of each rank
        int[] rankCounts = rankCounts(withStarter);
        for (int count : rankCounts) {
            if (count == 2) score += params.pairPoints;
            else if (count == 3) score += params.pairRoyalPoints;
            else if (count == 4) score += params.doublePairRoyalPoints;
        }

        score += showRunPoints(rankCounts(params.runsIncludeStarter ? withStarter : cards));

        // flush: all the hand (or crib) cards of one suit
        boolean flush = cards.size() == params.nCardsDealt - params.nCardsToCrib
                && cards.stream().allMatch(c -> c.suite == cards.get(0).suite);
        if (flush) {
            boolean starterMatches = starter.suite == cards.get(0).suite;
            if (starterMatches)
                score += params.flushPoints + 1;
            else if (!isCrib || !params.cribFlushNeedsStarter)
                score += params.flushPoints;
        }

        // his nobs
        if (cards.stream().anyMatch(c -> c.type == FrenchCard.FrenchCardType.Jack && c.suite == starter.suite))
            score += params.hisNobsPoints;
        return score;
    }

    /**
     * The number of cards of each rank, indexed by rank (1 to 13).
     */
    private static int[] rankCounts(List<FrenchCard> cards) {
        int[] counts = new int[14];
        for (FrenchCard c : cards)
            counts[rank(c)]++;
        return counts;
    }

    /**
     * Points for runs in the show: only the longest runs score. Each distinct combination of cards making a run of
     * that length scores its length, so a run with one rank doubled (6-7-7-8) scores twice.
     */
    private static int showRunPoints(int[] rankCounts) {
        for (int length = 13; length >= 3; length--) {
            int points = 0;
            for (int start = 1; start + length - 1 <= 13; start++) {
                int combinations = 1;
                for (int r = start; r < start + length; r++)
                    combinations *= rankCounts[r];
                points += combinations * length;
            }
            if (points > 0) return points;
        }
        return 0;
    }
}
