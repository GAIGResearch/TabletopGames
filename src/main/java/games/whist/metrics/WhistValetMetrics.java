package games.whist.metrics;

import core.AbstractGameState;
import core.components.FrenchCard;
import evaluation.metrics.AbstractGameSummaryMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.tricktaking.PlayCard;
import games.tricktaking.Trick;
import games.whist.WhistGameState;

import java.util.*;

public class WhistValetMetrics implements IMetricsCollection {

    /**
     * Per team: tricks won, tricks won by a trump card and points scored. Also the trump suit and dealer of each deal,
     * in order, separated by '|'.
     */
    public static class GameSummary extends AbstractGameSummaryMetric {

        private final int[] tricks = new int[2];
        private final int[] trumpTricks = new int[2];
        private final List<String> trumpSuits = new ArrayList<>();
        private final List<String> dealers = new ArrayList<>();
        private int deal;

        @Override
        protected void resetGame(AbstractGameState state) {
            Arrays.fill(tricks, 0);
            Arrays.fill(trumpTricks, 0);
            trumpSuits.clear();
            dealers.clear();
            deal = -1;
        }

        @Override
        protected void onAction(Event e) {
            WhistGameState state = (WhistGameState) e.state;
            if (state.getRoundCounter() != deal) {
                deal = state.getRoundCounter();
                trumpSuits.add(String.valueOf(state.getTrumpSuit()));
                dealers.add(String.valueOf(state.getDealer()));
            }
            if (!(e.action instanceof PlayCard<?> play))
                return;
            Trick<FrenchCard, FrenchCard.Suite> trick = state.getCurrentTrick().copy();
            trick.play((FrenchCard) play.card);
            if (!trick.isComplete())
                return;
            FrenchCard.Suite trumps = state.getTrumpSuit();
            int team = state.getTeam(trick.winner(trumps));
            tricks[team]++;
            // a trick containing a trump is won by the highest trump
            if (trumps != null && trick.stream().anyMatch(c -> c.suite == trumps))
                trumpTricks[team]++;
        }

        @Override
        protected void recordGameOver(Event e, Map<String, Object> records) {
            WhistGameState state = (WhistGameState) e.state;
            for (int t = 0; t < state.getNTeams(); t++) {
                records.put("Team-" + t + " Tricks", tricks[t]);
                records.put("Team-" + t + " TrumpTricks", trumpTricks[t]);
                records.put("Team-" + t + " Score", state.getTeamPoints(t));
            }
            records.put("TrumpSuits", String.join("|", trumpSuits));
            records.put("Dealers", String.join("|", dealers));
        }

        @Override
        protected Map<String, Class<?>> getGameColumns(int nPlayersPerGame) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (int t = 0; t < 2; t++) {
                columns.put("Team-" + t + " Tricks", Integer.class);
                columns.put("Team-" + t + " TrumpTricks", Integer.class);
                columns.put("Team-" + t + " Score", Integer.class);
            }
            columns.put("TrumpSuits", String.class);
            columns.put("Dealers", String.class);
            return columns;
        }
    }
}
