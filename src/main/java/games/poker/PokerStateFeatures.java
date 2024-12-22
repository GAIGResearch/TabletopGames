package games.poker;

import core.AbstractGameState;
import core.components.FrenchCard;
import evaluation.features.TunableStateFeatures;

import java.util.*;

import static java.util.stream.Collectors.*;

public class PokerStateFeatures extends TunableStateFeatures {

    static String[] allNames = new String[]{
            "PAIRS", "TRIPLES", "MAX_RUN",
            "ACES", "KINGS", "QUEENS", "JACKS", "TENS", "NINES", "EIGHTS", "SEVENS", "SIXES", "FIVES",
            "FOURS", "THREES", "TWOS",
            "OWN_BID", "OPPONENT_BID", "BID_DIFF", "PLAYERS_FOLDED", "TURN"
    };

    public PokerStateFeatures() {
        super(allNames);
    }

    @Override
    protected PokerStateFeatures _copy() {
        return new PokerStateFeatures();
    }

    @Override
    public double[] fullFeatureVector(AbstractGameState state, int playerID) {
        double[] data = new double[allNames.length];
        PokerGameState pgs = (PokerGameState) state;
        List<FrenchCard> cards = new ArrayList<>(pgs.getPlayerDecks().get(playerID).getComponents());
        cards.addAll(pgs.getCommunityCards().getComponents());
        Map<Integer, Long> countByNumber = cards.stream().collect(groupingBy(c -> c.number, counting()));

        // Pairs
        data[0] = countByNumber.values().stream().filter(c -> c == 2).count();
        // Triples
        data[1] = countByNumber.values().stream().filter(c -> c == 3).count();
        // Max run
        boolean inRun= false;
        int currentRun = 0;
        for (int i = 2; i <= 14; i++) {
            if (countByNumber.containsKey(i)) {
                if (inRun) {
                    currentRun++;
                } else {
                    inRun = true;
                    currentRun = 1;
                }
            } else {
                inRun = false;
                data[2] = Math.max(data[2], currentRun);
            }
        }
        // Aces to Twos
        for (int i = 3; i <= 15; i++) {
            data[i] = countByNumber.getOrDefault(17-i, 0L);
        }
        // Own Bid
        data[16] = pgs.getPlayerBet()[playerID].getValue();
        // Opponent Bid
        for (int i = 0; i < pgs.getNPlayers(); i++) {
            if (i != playerID) {
                data[17] = Math.max(pgs.getPlayerBet()[i].getValue(), data[17]);
            }
        }
        // Bid difference
        data[18] = data[16] - data[17];
        // Players folded
        for (int i = 0; i < pgs.getNPlayers(); i++) {
            if (pgs.playerFold[i]) {
                data[19]++;
            }
        }
        // Turn
        data[20] = pgs.getTurnCounter();

        return data;
    }
}
