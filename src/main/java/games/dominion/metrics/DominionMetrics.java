package games.dominion.metrics;

import core.AbstractGameState;
import core.components.Card;
import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.GameMetrics;
import evaluation.metrics.IMetricsCollection;
import games.dominion.*;
import games.dominion.actions.DominionAction;
import games.dominion.actions.DominionAttackAction;
import games.dominion.actions.TrashCard;
import games.dominion.cards.CardType;
import org.apache.xmlbeans.impl.store.DomImpl;

import java.util.*;

@SuppressWarnings("unused")
public class DominionMetrics implements IMetricsCollection {

    /**
     * Records the paramters chosen for the game that was played
     */
    public static class ChosenParams extends AbstractMetric {

        public ChosenParams() {
            super();
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("HAND_SIZE", Integer.class);
            columns.put("PILES_EXHAUSTED_FOR_GAME_END", Integer.class);
            columns.put("KINGDOM_CARDS_OF_EACH_TYPE", Integer.class);
            columns.put("CURSE_CARDS_PER_PLAYER", Integer.class);
            columns.put("STARTING_COPPER", Integer.class);
            columns.put("STARTING_ESTATES", Integer.class);
            columns.put("COPPER_SUPPLY", Integer.class);
            columns.put("SILVER_SUPPLY", Integer.class);
            columns.put("GOLD_SUPPLY", Integer.class);

            // Cards in Play
            columns.put("ESTATE", Integer.class);
            columns.put("DUCHY", Integer.class);
            columns.put("PROVINCE", Integer.class);
            columns.put("CELLAR", Integer.class);
            columns.put("CHAPEL", Integer.class);
            columns.put("MOAT", Integer.class);
            columns.put("HARBINGER", Integer.class);
            columns.put("MERCHANT", Integer.class);
            columns.put("VASSAL", Integer.class);
            columns.put("VILLAGE", Integer.class);
            columns.put("WORKSHOP", Integer.class);
            columns.put("BUREAUCRAT", Integer.class);
            columns.put("GARDENS", Integer.class);
            columns.put("MILITIA", Integer.class);
            columns.put("MONEYLENDER", Integer.class);
            columns.put("POACHER", Integer.class);
            columns.put("REMODEL", Integer.class);
            columns.put("SMITHY", Integer.class);
            columns.put("THRONE_ROOM", Integer.class);
            columns.put("BANDIT", Integer.class);
            columns.put("COUNCIL_ROOM", Integer.class);
            columns.put("FESTIVAL", Integer.class);
            columns.put("LABORATORY", Integer.class);
            columns.put("LIBRARY", Integer.class);
            columns.put("MARKET", Integer.class);
            columns.put("MINE", Integer.class);
            columns.put("SENTRY", Integer.class);
            columns.put("WITCH", Integer.class);
            columns.put("ARTISAN", Integer.class);
            return columns;
        }

        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            DominionGameState state = (DominionGameState)e.state;
            DominionParameters params = (DominionParameters)state.getGameParameters();

            records.put("HAND_SIZE", params.HAND_SIZE);
            records.put("PILES_EXHAUSTED_FOR_GAME_END", params.PILES_EXHAUSTED_FOR_GAME_END);
            records.put("KINGDOM_CARDS_OF_EACH_TYPE", params.KINGDOM_CARDS_OF_EACH_TYPE);
            records.put("CURSE_CARDS_PER_PLAYER", params.CURSE_CARDS_PER_PLAYER);
            records.put("STARTING_COPPER", params.STARTING_COPPER);
            records.put("STARTING_ESTATES", params.STARTING_ESTATES);
            records.put("COPPER_SUPPLY", params.COPPER_SUPPLY);
            records.put("SILVER_SUPPLY", params.SILVER_SUPPLY);
            records.put("GOLD_SUPPLY", params.GOLD_SUPPLY);

            for (CardType card : CardType.values()) {
                if (records.containsKey(card.toString())) {

                    if (params.cardsUsed.contains(card)) {
                        records.put(card.toString(), 1);
                    } else {
                        records.put(card.toString(), 0);
                    }
                }
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ABOUT_TO_START);
        }
    }

    public static class CardsInSupplyGameEnd extends AbstractMetric {
        CardType[] cardTypes;

        public CardsInSupplyGameEnd(){
            super();
            cardTypes = CardType.values();
        }
        public CardsInSupplyGameEnd(String[] args) {
            super(args);
            cardTypes = new CardType[args.length];
            for (int i = 0; i < args.length; i++) {
                cardTypes[i] = CardType.valueOf(args[i]);
            }
        }

        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            for (CardType type : cardTypes) {
                records.put(type.toString(), ((DominionGameState)e.state).cardsOfType(type, -1, DominionConstants.DeckType.SUPPLY));
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (CardType type : cardTypes) {
                columns.put(type.toString(), Integer.class);
            }
            return columns;
        }
    }

    public static class EmptySupplySlots extends AbstractMetric {
        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            records.put("EmptySupplySlots", (int)((DominionGameState)e.state).cardsIncludedInGame().stream()
                    .filter(c -> ((DominionGameState)e.state).cardsOfType(c, -1, DominionConstants.DeckType.SUPPLY) == 0)
                    .count());
            return true;
        }

        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            return Collections.singletonMap("EmptySupplySlots", Integer.class);
        }
    }


    public static class GameSeeds extends AbstractMetric {

        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("InitialShuffle", Integer.class);
            return columns;
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            DominionGameState state = (DominionGameState)e.state;
            DominionParameters params = (DominionParameters)state.getGameParameters();
            records.put("InitialShuffle", params.initialShuffleSeed);
            return true;
        }


    }

    public static class ActionFeatures extends AbstractMetric {

        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("Money", Integer.class);
            columns.put("Actions", Integer.class);
            columns.put("Buys", Integer.class);
            return columns;
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            DominionGameState state = (DominionGameState)e.state;
            records.put("Money", state.getAvailableSpend(e.playerID));
            records.put("Actions", state.getActionsLeft());
            records.put("Buys", state.getBuysLeft());
            return true;
        }
    }

    public static class DominionActionValue extends AbstractMetric {

        int plusActions = 0;
        int plusDraws = 0;
        int plusBuys = 0;
        int plusMoney = 0;
        int attackCount = 0;
        int trashCardCount = 0;
        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            if (e.type == Event.GameEvent.ACTION_CHOSEN) {
                if (e.action instanceof DominionAction dAction) {
                    plusActions += dAction.type.plusActions;
                    plusDraws += dAction.type.plusDraws;
                    plusBuys += dAction.type.plusBuys;
                    plusMoney += dAction.type.plusMoney;
                }
                if (e.action instanceof DominionAttackAction) {
                    attackCount++;
                }
                else if (e.action instanceof TrashCard) {
                    trashCardCount++;
                }
                return false;
            }
            if (e.type == Event.GameEvent.GAME_OVER) {
                records.put("plusActions", plusActions);
                records.put("plusDraws", plusDraws);
                records.put("plusBuys", plusBuys);
                records.put("plusMoney", plusMoney);
                records.put("attackCount", attackCount);
                records.put("trashCardCount", trashCardCount);
                return true;
            }
            return false;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Set.of(Event.GameEvent.ACTION_CHOSEN, Event.GameEvent.GAME_OVER);
        }

        @Override
        public void notifyGameOver() {
            super.notifyGameOver();
            plusActions = 0;
            plusDraws = 0;
            plusBuys = 0;
            plusMoney = 0;
            attackCount = 0;
            trashCardCount = 0;
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("plusActions", Integer.class);
            columns.put("plusDraws", Integer.class);
            columns.put("plusBuys", Integer.class);
            columns.put("plusMoney", Integer.class);
            columns.put("attackCount", Integer.class);
            columns.put("trashCardCount", Integer.class);
            return columns;
        }
    }

    public static class GameParams extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            DominionParameters params = (DominionParameters) e.state.getGameParameters();
            records.put("HAND_SIZE", params.HAND_SIZE);
            records.put("PILES_EXHAUSTED_FOR_GAME_END", params.PILES_EXHAUSTED_FOR_GAME_END);
            records.put("KINGDOM_CARDS_OF_EACH_TYPE", params.KINGDOM_CARDS_OF_EACH_TYPE);
            records.put("CURSE_CARDS_PER_PLAYER", params.CURSE_CARDS_PER_PLAYER);
            records.put("STARTING_COPPER", params.STARTING_COPPER);
            records.put("STARTING_ESTATES", params.STARTING_ESTATES);
            records.put("COPPER_SUPPLY", params.COPPER_SUPPLY);
            records.put("SILVER_SUPPLY", params.SILVER_SUPPLY);
            records.put("GOLD_SUPPLY", params.GOLD_SUPPLY);
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("HAND_SIZE", Integer.class);
            columns.put("PILES_EXHAUSTED_FOR_GAME_END", Integer.class);
            columns.put("KINGDOM_CARDS_OF_EACH_TYPE", Integer.class);
            columns.put("CURSE_CARDS_PER_PLAYER", Integer.class);
            columns.put("STARTING_COPPER", Integer.class);
            columns.put("STARTING_ESTATES", Integer.class);
            columns.put("COPPER_SUPPLY", Integer.class);
            columns.put("SILVER_SUPPLY", Integer.class);
            columns.put("GOLD_SUPPLY", Integer.class);
            return columns;
        }
    }
}
