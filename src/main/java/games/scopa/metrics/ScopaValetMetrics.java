package games.scopa.metrics;

import core.AbstractGameState;
import core.components.TarotCard;
import evaluation.metrics.AbstractGameSummaryMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.scopa.PlayCard;
import games.scopa.ScopaGameState;
import games.scopa.ScopaParameters;
import games.scopa.ScopaUtils;

import java.util.*;

public class ScopaValetMetrics implements IMetricsCollection {

    /**
     * Per player, summed over the deals of the game: cards captured, Coins captured, the settebello, primiera value
     * and scopas, and the points scored for each of these. Also the number of deals.
     */
    public static class GameSummary extends AbstractGameSummaryMetric {

        static final String[] STATS = {"Cards", "Coins", "Settebello", "Primiera", "Scopas",
                "CardsPoints", "CoinsPoints", "SettebelloPoints", "PrimieraPoints", "ScopaPoints"};

        // [player][index into STATS]
        private int[][] totals;
        private int deals;

        @Override
        protected void resetGame(AbstractGameState state) {
            totals = new int[state.getNPlayers()][STATS.length];
            deals = 0;
        }

        /**
         * The captured piles are gathered up at the end of each deal, so each deal is totalled as its last card is
         * played.
         */
        @Override
        protected void onAction(Event e) {
            ScopaGameState state = (ScopaGameState) e.state;
            if (!(e.action instanceof PlayCard play) || state.getDrawDeck().getSize() > 0)
                return;
            int cardsInHands = 0;
            for (int p = 0; p < state.getNPlayers(); p++)
                cardsInHands += state.getPlayerHand(p).getSize();
            if (cardsInHands > 1)
                return;

            // the piles once this card is played, and the last capturer has taken the cards left on the table
            List<List<TarotCard>> piles = new ArrayList<>();
            for (int p = 0; p < state.getNPlayers(); p++)
                piles.add(new ArrayList<>(state.getCapturedCards(p).getComponents()));
            List<TarotCard> table = new ArrayList<>(state.getTable().getComponents());
            int lastCapturer = state.getLastCapturer();
            if (play.isCapture()) {
                table.removeAll(play.captured);
                piles.get(e.playerID).addAll(play.captured);
                piles.get(e.playerID).add(play.card);
                lastCapturer = e.playerID;
            } else {
                table.add(play.card);
            }
            if (lastCapturer >= 0)
                piles.get(lastCapturer).addAll(table);

            deals++;
            for (int p = 0; p < state.getNPlayers(); p++) {
                List<TarotCard> mine = piles.get(p);
                List<TarotCard> theirs = piles.get(1 - p);  // two players
                int settebello = mine.contains(ScopaParameters.SETTEBELLO) ? 1 : 0;
                int[] stats = {mine.size(), ScopaUtils.coins(mine), settebello, ScopaUtils.primiera(mine),
                        state.getScopas(p),  // the last card of a deal is never a scopa
                        mine.size() > theirs.size() ? 1 : 0,
                        ScopaUtils.coins(mine) > ScopaUtils.coins(theirs) ? 1 : 0,
                        settebello,
                        ScopaUtils.primiera(mine) > ScopaUtils.primiera(theirs) ? 1 : 0,
                        state.getScopas(p)};
                for (int i = 0; i < STATS.length; i++)
                    totals[p][i] += stats[i];
            }
        }

        @Override
        protected void recordGameOver(Event e, Map<String, Object> records) {
            for (int p = 0; p < totals.length; p++)
                for (int i = 0; i < STATS.length; i++)
                    records.put("Player-" + p + " " + STATS[i], totals[p][i]);
            records.put("Deals", deals);
        }

        @Override
        protected Map<String, Class<?>> getGameColumns(int nPlayersPerGame) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (int p = 0; p < nPlayersPerGame; p++)
                for (String stat : STATS)
                    columns.put("Player-" + p + " " + stat, Integer.class);
            columns.put("Deals", Integer.class);
            return columns;
        }
    }
}
