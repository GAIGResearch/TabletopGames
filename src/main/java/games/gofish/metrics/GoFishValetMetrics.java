package games.gofish.metrics;

import core.AbstractGameState;
import core.components.FrenchCard;
import evaluation.metrics.AbstractGameSummaryMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.gofish.GoFishGameState;
import games.gofish.actions.GoFishAsk;

import java.util.*;

public class GoFishValetMetrics implements IMetricsCollection {

    /**
     * Per player: books laid down, asks made, asks that got cards, the cards received from asks, cards drawn after an
     * unsuccessful ask, and how many of those draws were of the rank asked for. Also why the game ended: DeckEmpty,
     * HandEmpty or Other.
     */
    public static class GameSummary extends AbstractGameSummaryMetric {

        private int[] asks, successfulAsks, cardsReceived, draws, drawsOfAskedRank;

        @Override
        protected void resetGame(AbstractGameState state) {
            int n = state.getNPlayers();
            asks = new int[n];
            successfulAsks = new int[n];
            cardsReceived = new int[n];
            draws = new int[n];
            drawsOfAskedRank = new int[n];
        }

        @Override
        protected void onAction(Event e) {
            GoFishGameState state = (GoFishGameState) e.state;
            if (!(e.action instanceof GoFishAsk ask))
                return;
            int asker = e.playerID;
            asks[asker]++;
            int received = (int) state.getPlayerHands().get(ask.targetPlayer).stream()
                    .filter(c -> c.number == ask.rank).count();
            if (received > 0) {
                successfulAsks[asker]++;
                cardsReceived[asker] += received;
            } else if (state.getDrawDeck().getSize() > 0) {
                // the action has not been applied yet, so the top card is the one the asker will draw
                draws[asker]++;
                FrenchCard drawn = state.getDrawDeck().peek();
                if (drawn.number == ask.rank)
                    drawsOfAskedRank[asker]++;
            }
        }

        @Override
        protected void recordGameOver(Event e, Map<String, Object> records) {
            GoFishGameState state = (GoFishGameState) e.state;
            for (int p = 0; p < state.getNPlayers(); p++) {
                records.put("Player-" + p + " Books", state.getBooks(p));
                records.put("Player-" + p + " Asks", asks[p]);
                records.put("Player-" + p + " SuccessfulAsks", successfulAsks[p]);
                records.put("Player-" + p + " CardsReceived", cardsReceived[p]);
                records.put("Player-" + p + " GoFishDraws", draws[p]);
                records.put("Player-" + p + " DrawsOfAskedRank", drawsOfAskedRank[p]);
            }
            String endReason = "Other";
            if (state.getDrawDeck().getSize() == 0)
                endReason = "DeckEmpty";
            else if (state.getPlayerHands().stream().anyMatch(h -> h.getSize() == 0))
                endReason = "HandEmpty";
            records.put("EndReason", endReason);
        }

        @Override
        protected Map<String, Class<?>> getGameColumns(int nPlayersPerGame) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (int p = 0; p < nPlayersPerGame; p++) {
                columns.put("Player-" + p + " Books", Integer.class);
                columns.put("Player-" + p + " Asks", Integer.class);
                columns.put("Player-" + p + " SuccessfulAsks", Integer.class);
                columns.put("Player-" + p + " CardsReceived", Integer.class);
                columns.put("Player-" + p + " GoFishDraws", Integer.class);
                columns.put("Player-" + p + " DrawsOfAskedRank", Integer.class);
            }
            columns.put("EndReason", String.class);
            return columns;
        }
    }
}
