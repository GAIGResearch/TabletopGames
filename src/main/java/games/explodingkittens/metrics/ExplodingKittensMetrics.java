package games.explodingkittens.metrics;

import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.ExplodingKittensParameters;
import games.explodingkittens.cards.ExplodingKittensCard;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ExplodingKittensMetrics implements IMetricsCollection {
    /**
     * Records the paramters chosen for the game that was played
     */
    public static class ChosenParams extends AbstractMetric {
        public ChosenParams() {
            super();
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            ExplodingKittensGameState state = (ExplodingKittensGameState) e.state;
            ExplodingKittensParameters params = (ExplodingKittensParameters) state.getGameParameters();

            records.put("N_CARDS_PER_PLAYER", params.nCardsPerPlayer);
            records.put("NOPE_OWN_CARDS", params.nopeOwnCards);

            for (ExplodingKittensCard.CardType c : ExplodingKittensCard.CardType.values()) {
                records.put(c.name() + "_COUNT", params.cardCounts.get(c));
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ABOUT_TO_START);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("N_CARDS_PER_PLAYER", Integer.class);
            columns.put("NOPE_OWN_CARDS", Boolean.class);
            columns.put("EXPLODING_KITTEN_COUNT", Integer.class);
            columns.put("DEFUSE_COUNT", Integer.class);
            columns.put("NOPE_COUNT", Integer.class);
            columns.put("ATTACK_COUNT", Integer.class);
            columns.put("SKIP_COUNT", Integer.class);
            columns.put("FAVOR_COUNT", Integer.class);
            columns.put("SHUFFLE_COUNT", Integer.class);
            columns.put("SEETHEFUTURE_COUNT", Integer.class);
            columns.put("TACOCAT_COUNT", Integer.class);
            columns.put("MELONCAT_COUNT", Integer.class);
            columns.put("BEARDCAT_COUNT", Integer.class);
            columns.put("RAINBOWCAT_COUNT", Integer.class);
            columns.put("FURRYCAT_COUNT", Integer.class);
            return columns;
        }
    }
}
