package players.heuristics;

import core.AbstractGameState;
import core.components.Deck;
import core.interfaces.IStateHeuristic;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.cards.SGCard;

import static games.sushigo.cards.SGCard.SGCardType.*;

/**
 * Scores a SushiGo state from playerId's point of view in [-1, 1].
 * Mid-game it projects each player's final score (banked points plus the expected value of
 * unfinished sets, Maki and Pudding) and returns the margin over the best opponent.
 */
public class SushiGoHeuristic implements IStateHeuristic {

    // Tunable weights
    public double marginScale = 15.0;    // a margin of this many points maps to tanh(1) ~ 0.76
    public double midGameCap = 0.8;      // projected values stay inside (-0.8, 0.8), so a real win always beats a projected lead
    public double pickShare = 1.5;       // how much more than a random 1/nPlayers share of a card type a player gets when they want it
    public double makiBaseWeight = 0.3;  // trust in the current Maki ranking at the start of a round (rises to 1 at round end)
    public double puddingBaseWeight = 0.4; // trust in the current Pudding ranking at game start (puddings are kept, so leads stick)
    public double chopsticksValue = 1.5; // option value of unused chopsticks with plenty of turns left

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        SGGameState s = (SGGameState) gs;
        if (!gs.isNotTerminal()) {
            double result = switch (gs.getPlayerResults()[playerId]) {
                case WIN_GAME -> 1.0;
                case LOSE_GAME -> -1.0;
                default -> 0.0;
            };
            // final scores are exact, so the real margin breaks ties between wins (or between losses)
            double[] finalScores = new double[gs.getNPlayers()];
            for (int p = 0; p < finalScores.length; p++) finalScores[p] = gs.getGameScore(p);
            return 0.8 * result + 0.2 * Math.tanh(margin(finalScores, playerId) / marginScale);
        }
        return midGameCap * Math.tanh(margin(projectedScores(s), playerId) / marginScale);
    }

    private static double margin(double[] scores, int playerId) {
        double bestOpponent = Double.NEGATIVE_INFINITY;
        for (int p = 0; p < scores.length; p++)
            if (p != playerId) bestOpponent = Math.max(bestOpponent, scores[p]);
        return scores[playerId] - bestOpponent;
    }

    private double[] projectedScores(SGGameState s) {
        SGParameters params = (SGParameters) s.getGameParameters();
        int n = s.getNPlayers();
        int handStart = params.nCards - n + 2;
        int turnsLeft = s.getPlayerHands().get(0).getSize();  // all hands are the same size
        double roundProgress = 1.0 - (double) turnsLeft / handStart;
        double gameProgress = Math.min(1.0, (s.getRoundCounter() + roundProgress) / params.nRounds);

        // cards still in circulation this round (in a determinised copy every hand is concrete)
        int tempuraLeft = 0, sashimiLeft = 0, nigiriLeft = 0;
        double nigiriValueLeft = 0;
        for (Deck<SGCard> hand : s.getPlayerHands()) {
            for (SGCard c : hand.getComponents()) {
                switch (c.type) {
                    case Tempura -> tempuraLeft++;
                    case Sashimi -> sashimiLeft++;
                    case SquidNigiri -> { nigiriLeft++; nigiriValueLeft += params.valueSquidNigiri; }
                    case SalmonNigiri -> { nigiriLeft++; nigiriValueLeft += params.valueSalmonNigiri; }
                    case EggNigiri -> { nigiriLeft++; nigiriValueLeft += params.valueEggNigiri; }
                    default -> { }
                }
            }
        }
        double avgNigiri = nigiriLeft == 0 ? 0 : nigiriValueLeft / nigiriLeft;
        double tempuraExpected = expectedPicks(tempuraLeft, n, turnsLeft);
        double sashimiExpected = expectedPicks(sashimiLeft, n, turnsLeft);
        double nigiriExpected = expectedPicks(nigiriLeft, n, turnsLeft);

        double[] maki = makiPoints(s, params);
        double makiMean = (params.valueMakiMost + params.valueMakiSecond) / (double) n;
        double makiWeight = makiBaseWeight + (1 - makiBaseWeight) * roundProgress;

        double[] pudding = puddingPoints(s, params);
        double puddingMean = (params.valuePuddingMost + (n > 2 ? params.valuePuddingLeast : 0)) / (double) n;
        double puddingWeight = puddingBaseWeight + (1 - puddingBaseWeight) * gameProgress;

        double[] scores = new double[n];
        for (int p = 0; p < n; p++) {
            double score = s.getGameScore(p);

            int tempura = s.getPlayedCardTypes(Tempura, p).getValue();
            if (tempura % 2 == 1)
                score += params.valueTempuraPair * pAtLeast(1, tempuraExpected);

            int sashimiInSet = s.getPlayedCardTypes(Sashimi, p).getValue() % 3;
            if (sashimiInSet > 0)
                score += params.valueSashimiTriple * pAtLeast(3 - sashimiInSet, sashimiExpected);

            // the Wasabi counter only holds wasabi with no nigiri on it yet
            int wasabi = s.getPlayedCardTypes(Wasabi, p).getValue();
            if (wasabi > 0)
                score += (params.multiplierWasabi - 1) * avgNigiri * expectedMin(wasabi, nigiriExpected);

            if (s.getPlayedCardTypes(Chopsticks, p).getValue() > 0 && turnsLeft >= 2)
                score += chopsticksValue * Math.min(1.0, (turnsLeft - 1) / 3.0);

            score += makiWeight * maki[p] + (1 - makiWeight) * makiMean;
            score += puddingWeight * pudding[p] + (1 - puddingWeight) * puddingMean;
            scores[p] = score;
        }
        return scores;
    }

    /** Expected number of copies of a card type one player can still pick up this round. */
    private double expectedPicks(int copiesLeft, int nPlayers, int turnsLeft) {
        return Math.min(turnsLeft, pickShare * copiesLeft / nPlayers);
    }

    /** P(X >= k) for X ~ Poisson(mean). */
    private static double pAtLeast(int k, double mean) {
        double term = Math.exp(-mean), below = 0;
        for (int i = 0; i < k; i++) {
            below += term;
            term *= mean / (i + 1);
        }
        return 1 - below;
    }

    /** E[min(X, k)] for X ~ Poisson(mean). */
    private static double expectedMin(int k, double mean) {
        double sum = 0;
        for (int j = 1; j <= k; j++) sum += pAtLeast(j, mean);
        return sum;
    }

    /** Maki points each player would get if the round ended now (same rules as SGCard Maki.onRoundEnd). */
    private static double[] makiPoints(SGGameState s, SGParameters params) {
        int n = s.getNPlayers();
        int[] count = new int[n];
        int most = 0, second = 0;
        for (int p = 0; p < n; p++) {
            count[p] = s.getPlayedCardTypes(Maki, p).getValue();
            if (count[p] > most) {
                second = most;
                most = count[p];
            } else if (count[p] < most && count[p] > second) {
                second = count[p];
            }
        }
        int nMost = 0, nSecond = 0;
        for (int c : count) {
            if (most > 0 && c == most) nMost++;
            else if (second > 0 && c == second) nSecond++;
        }
        double[] points = new double[n];
        for (int p = 0; p < n; p++) {
            if (most > 0 && count[p] == most)
                points[p] = params.valueMakiMost / nMost;
            else if (nMost == 1 && second > 0 && count[p] == second)
                points[p] = params.valueMakiSecond / nSecond;
        }
        return points;
    }

    /** Pudding points each player would get if the game ended now (same rules as SGCard Pudding.onGameEnd). */
    private static double[] puddingPoints(SGGameState s, SGParameters params) {
        int n = s.getNPlayers();
        int[] count = new int[n];
        int best = Integer.MIN_VALUE, worst = Integer.MAX_VALUE;
        for (int p = 0; p < n; p++) {
            count[p] = s.getPlayedCardTypes(Pudding, p).getValue();
            best = Math.max(best, count[p]);
            worst = Math.min(worst, count[p]);
        }
        double[] points = new double[n];
        if (best == worst) return points;
        int nBest = 0, nWorst = 0;
        for (int c : count) {
            if (c == best) nBest++;
            if (c == worst) nWorst++;
        }
        for (int p = 0; p < n; p++) {
            if (count[p] == best) points[p] = params.valuePuddingMost / nBest;
            else if (count[p] == worst && n > 2) points[p] = params.valuePuddingLeast / nWorst;
        }
        return points;
    }
}