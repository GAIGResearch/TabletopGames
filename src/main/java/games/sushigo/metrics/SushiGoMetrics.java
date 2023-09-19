package games.sushigo.metrics;

import core.CoreConstants;
import core.components.Deck;
import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import evaluation.summarisers.TAGOccurrenceStatSummary;
import evaluation.summarisers.TAGStatSummary;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.actions.ChooseCard;
import games.sushigo.cards.SGCard;
import utilities.Pair;

import java.util.*;


@SuppressWarnings("unused")
public class SushiGoMetrics implements IMetricsCollection {

    /**
     * How many times a card type is played during the game
     */
    public static class CardPlayedCount extends AbstractMetric {

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("Card played", String.class);
            return columns;
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {

            SGGameState gs = (SGGameState) e.state;
            ChooseCard action = (ChooseCard)e.action;
            SGCard c = gs.getPlayerHands().get(action.playerId).get(action.cardIdx);
            records.put("Card played", c.toString());

            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }

        // Proportion of occurrence relative to number of cards in the game
        public Map<String, Object> postProcessingGameOver(Event e, TAGStatSummary recordedData) {
            // Process the recorded data during the game and return game over summarised data
            Map<String, Object> toRecord = new HashMap<>();
            Map<String, Object> summaryData = recordedData.getSummary();
            SGParameters params = (SGParameters) e.state.getGameParameters();
            for (String k: summaryData.keySet()) {
                String[] split = k.split("-");
                SGCard.SGCardType type = SGCard.SGCardType.valueOf(split[0]);
                int count = 1;
                if (split.length > 1) count = Integer.parseInt(split[1]);
                toRecord.put(getClass().getSimpleName() + "(" + k + "):" + e.type, ((TAGOccurrenceStatSummary)recordedData).getElements().get(k) * 1.0 / params.nCardsPerType.get(new Pair<>(type, count)));
            }
            return toRecord;
        }
    }

    /**
     * How many times is the card of the given type chosen in favour of other cards in hand?
     * TODO: This metric can probably benefit from a custom plotter/summarized. Implemented to be ok for raw data export.
     *
     */
    public static class CardNotChosenInFavourOf extends AbstractMetric {
        String[] focusTypes;
        String[] allTypes;
        public CardNotChosenInFavourOf() {
            super();
            List<String> params = new ArrayList<>();
            for (SGCard.SGCardType type: SGCard.SGCardType.values()) {
                for (int count: type.getIconCountVariation()) {
                    params.add(type.name() + "-" + count);
                }
            }
            allTypes = params.toArray(new String[0]);
            focusTypes = allTypes.clone();
        }

        public CardNotChosenInFavourOf(String[] args) {
            super(args);
            focusTypes = args;
            List<String> params = new ArrayList<>();
            for (SGCard.SGCardType type: SGCard.SGCardType.values()) {
                for (int count: type.getIconCountVariation()) {
                    params.add(type.name() + "-" + count);
                }
            }
            allTypes = params.toArray(new String[0]);
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            for (String typeCount: focusTypes) {
                SGCard.SGCardType type = SGCard.SGCardType.valueOf(typeCount.split("-")[0]);
                int count = Integer.parseInt(typeCount.split("-")[1]);

                ArrayList<String> cardList = new ArrayList<>();

                SGGameState gs = (SGGameState) e.state;
                ChooseCard action = (ChooseCard) e.action;
                Deck<SGCard> playerHand = gs.getPlayerHands().get(action.playerId);
                SGCard chosenCard = playerHand.get(action.cardIdx);

                //Initialize all entries in the records Map to 0
                records.replaceAll((k, v) -> 0);

                if (chosenCard.type == type && chosenCard.count == count) {
                    for (int i = 0; i < playerHand.getSize(); i++) {
                        SGCard otherCard = playerHand.get(i);
                        if (!chosenCard.toString().equals(otherCard.toString())) {
                            cardList.add(otherCard.type.name() + "-" + otherCard.count);
                        }
                    }

                    if (cardList.size() > 0) {
                        for (String cardStr : cardList)
                            records.put(typeCount + " -- " + cardStr, 1);
                    }
                }
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (String typeCount: focusTypes) {
                for (String typeCount1 : allTypes) {
                    columns.put(typeCount + " -- " + typeCount1, Integer.class);
                }
            }
            return columns;
        }
    }

    /**
     * How many times each card type is played by the winning player
     {"class": "games.sushigo.metrics.SushiGoMetrics$CardPoints" }
     */
    public static class CardPlayedWin extends AbstractMetric {
        SGCard.SGCardType[] types = SGCard.SGCardType.values();
        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            for (SGCard.SGCardType type: types) {
                SGGameState gs = (SGGameState) e.state;
                records.put(type + " Count", 0);
                for (int i = 0; i < gs.getNPlayers(); i++) {
                    if (gs.getPlayerResults()[i] == CoreConstants.GameResult.WIN_GAME) {
                        records.put(type + " Count", gs.getPlayedCardTypesAllGame()[i].get(type).getValue());
                        break;
                    }
                }
            }
            return true;
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (SGCard.SGCardType type: types) {
                columns.put(type.name() + " Count", Integer.class);
            }
            return columns;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }
    }

    /**
     * How many points does each card type bring to a player on average
     */
    public static class CardPoints extends AbstractMetric {
        SGCard.SGCardType[] types = SGCard.SGCardType.values();

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            for (SGCard.SGCardType type: types) {
                SGGameState gs = (SGGameState) e.state;
                double sum = 0, sumPercentage = 0, sumDiff = 0;
                for (int i = 0; i < gs.getNPlayers(); i++) {
                    sum += gs.getPointsPerCardType()[i].get(type).getValue();
                    sumPercentage += gs.getPointsPerCardType()[i].get(type).getValue() * 1.0 / gs.getPlayerScore()[i].getValue();
                    if (i < gs.getNPlayers() - 1)
                        sumDiff += Math.abs(gs.getPointsPerCardType()[i].get(type).getValue() - gs.getPointsPerCardType()[i + 1].get(type).getValue());
                }
                records.put(type + " Average Points", sum / gs.getNPlayers());
                records.put(type + " Average Points (%)", sumPercentage / gs.getNPlayers());
                records.put(type + " Average Points (Diff)", sumDiff / (gs.getNPlayers() - 1));
            }
            return true;
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (SGCard.SGCardType type: types) {
                columns.put(type + " Average Points", Double.class);
                columns.put(type + " Average Points (%)", Double.class);
                columns.put(type + " Average Points (Diff)", Double.class);
            }
            return columns;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }
    }

}
