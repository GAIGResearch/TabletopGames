package games.pitch;

import core.components.Deck;
import core.components.FrenchCard;
import games.tricktaking.PlayRule;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class PitchUtils {

    private PitchUtils() {
    }

    /**
     * The cards a player may play to the trick: any card when leading or when unable to follow suit; otherwise a card
     * of the suit led or a trump.
     *
     * @param trumps the trump suit, or null before the first card of the deal sets it
     */
    public static PlayRule playRule(FrenchCard.Suite trumps) {
        return (hand, trick) -> {
            List<FrenchCard> following = PlayRule.FOLLOW_SUIT.legalPlays(hand, trick);
            if (following.size() == hand.size())
                return following;
            return hand.stream().filter(c -> following.contains(c) || c.suite == trumps).toList();
        };
    }

    /**
     * The High, Low, Jack and Game points each team takes from the cards of the tricks it won.
     *
     * @param teamTricks the cards won by each team (index = team)
     * @param trumps     the trump suit
     * @param params     card values for Game, and how High, Low and Jack are counted
     * @return the points for each team (index = team)
     */
    public static int[] teamPoints(List<Deck<FrenchCard>> teamTricks, FrenchCard.Suite trumps, PitchParameters params) {
        int nTeams = teamTricks.size();
        // High and Low are the highest and lowest trumps played in the deal
        int high = Integer.MIN_VALUE, low = Integer.MAX_VALUE;
        for (Deck<FrenchCard> won : teamTricks)
            for (FrenchCard c : won.getComponents())
                if (c.suite == trumps) {
                    high = Math.max(high, c.number);
                    low = Math.min(low, c.number);
                }

        int[] points = new int[nTeams];
        int[] gameTotals = new int[nTeams];
        for (int t = 0; t < nTeams; t++) {
            for (FrenchCard c : teamTricks.get(t).getComponents()) {
                gameTotals[t] += params.gameValue(c);
                if (c.suite != trumps) continue;
                boolean isHigh = c.number == high, isLow = c.number == low;
                boolean isJack = c.type == FrenchCard.FrenchCardType.Jack;
                if (params.countHighLowSeparately)
                    points[t] += (isHigh ? 1 : 0) + (isLow ? 1 : 0) + (isJack ? 1 : 0);
                else if (isHigh || isLow || isJack)  // RECYCLE: 1 per card, however many of the three it is
                    points[t]++;
            }
        }

        // the Game point goes to the single team with the highest total; nobody on a tie
        int best = Arrays.stream(gameTotals).max().orElse(0);
        int[] leaders = IntStream.range(0, nTeams).filter(t -> gameTotals[t] == best).toArray();
        if (leaders.length == 1)
            points[leaders[0]]++;
        return points;
    }
}
