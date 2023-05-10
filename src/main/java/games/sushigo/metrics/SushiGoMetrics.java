package games.sushigo.metrics;

import core.CoreConstants;
import core.Game;
import core.components.Deck;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.*;
import evaluation.summarisers.TAGOccurrenceStatSummary;
import evaluation.summarisers.TAGStatSummary;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.actions.ChooseCard;
import games.sushigo.cards.SGCard;
import utilities.Group;
import utilities.Pair;

import java.util.*;


@SuppressWarnings("unused")
public class SushiGoMetrics implements IMetricsCollection {

    /**
     * How many times a card type is played during the game
     */
    public static class CardPlayedCount extends AbstractMetric {

        @Override
        public Map<String, Class<?>> getColumns(Game game) {
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
        public Set<Event.GameEvent> getDefaultEventTypes() {
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
    public static class CardNotChosenInFavourOf extends AbstractParameterizedMetric {
        public CardNotChosenInFavourOf(){super();}

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            String typeCount = (String) getParameterValue("typeCount");
            SGCard.SGCardType type = SGCard.SGCardType.valueOf(typeCount.split("-")[0]);
            int count = Integer.parseInt(typeCount.split("-")[1]);

            ArrayList<String> cardList = new ArrayList<>();

            SGGameState gs = (SGGameState) e.state;
            ChooseCard action = (ChooseCard)e.action;
            Deck<SGCard> playerHand = gs.getPlayerHands().get(action.playerId);
            SGCard chosenCard = playerHand.get(action.cardIdx);

            //Initialize all entries in the records Map to 0
            for (String k: records.keySet()) {
                records.put(k, 0);
            }

            if(chosenCard.type == type && chosenCard.count == count) {
                StringBuilder ss = new StringBuilder();
                for (int i = 0; i < playerHand.getSize(); i++) {
                    SGCard otherCard = playerHand.get(i);
                    if (!chosenCard.toString().equals(otherCard.toString())) {
                        ss.append(otherCard).append(",");
                        cardList.add(otherCard.type.name() + "-" + otherCard.count);
                    }
                }

                if(cardList.size() > 0)
                {
                    for(String cardStr : cardList)
                        records.put(cardStr, 1);
                    return true;
                }

            }
            return false;
        }

        public CardNotChosenInFavourOf(Object arg){super(arg);}
        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }

        @Override
        public Map<String, Class<?>> getColumns(Game game) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (SGCard.SGCardType type: SGCard.SGCardType.values()) {
                for (int count: type.getIconCountVariation()) {
                    columns.put(type.name() + "-" + count, Integer.class);
                }
            }
            return columns;
        }

        public List<Group<String, List<?>, ?>> getAllowedParameters() {
            List<String> params = new ArrayList<>();
            for (SGCard.SGCardType type: SGCard.SGCardType.values()) {
                for (int count: type.getIconCountVariation()) {
                    params.add(type.name() + "-" + count);
                }
            }
            return Collections.singletonList(new Group<>("typeCount", params, "Maki-1"));
        }
    }

    /**
     * How many times each card type is played by the winning player
     {"class": "games.sushigo.metrics.SushiGoMetrics$CardPoints" }
     */
    public static class CardPlayedWin extends AbstractParameterizedMetric {
        public CardPlayedWin(){super();}

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            SGCard.SGCardType type = (SGCard.SGCardType) getParameterValue("type");
            SGGameState gs = (SGGameState) e.state;
            for (int i = 0; i < gs.getNPlayers(); i++) {
                if (gs.getPlayerResults()[i] == CoreConstants.GameResult.WIN_GAME) {
                    records.put("Count", gs.getPlayedCardTypesAllGame()[i].get(type).getValue());
                    return true;
                }
            }
            records.put("Count", 0);
            return true;
        }

        public CardPlayedWin(Object arg){super(arg);}


        @Override
        public Map<String, Class<?>> getColumns(Game game) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("Count", Integer.class);
            return columns;
        }

        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }
        public List<Group<String, List<?>, ?>> getAllowedParameters() {
            return Collections.singletonList(new Group<>("type", Arrays.asList(SGCard.SGCardType.values()), SGCard.SGCardType.Maki));
        }
    }

    /**
     * How many points does each card type bring to a player on average
     */
    public static class CardPoints extends AbstractParameterizedMetric {
        public CardPoints(){super();}

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            SGCard.SGCardType type = (SGCard.SGCardType) getParameterValue("type");
            SGGameState gs = (SGGameState) e.state;
            double sum = 0, sumPercentage = 0, sumDiff = 0;
            for (int i = 0; i < gs.getNPlayers(); i++) {
                sum += gs.getPointsPerCardType()[i].get(type).getValue();
                sumPercentage += gs.getPointsPerCardType()[i].get(type).getValue() * 1.0 / gs.getPlayerScore()[i].getValue();
                if(i < gs.getNPlayers()-1)
                    sumDiff += Math.abs(gs.getPointsPerCardType()[i].get(type).getValue() - gs.getPointsPerCardType()[i+1].get(type).getValue());
            }
            records.put("Average Points", sum/gs.getNPlayers());
            records.put("Average Points (%)", sumPercentage/gs.getNPlayers());
            records.put("Average Points (Diff)", sumDiff/(gs.getNPlayers()-1));
            return true;
        }

        public CardPoints(Object arg){super(arg);}

        @Override
        public Map<String, Class<?>> getColumns(Game game) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("Average Points", Double.class);
            columns.put("Average Points (%)", Double.class);
            columns.put("Average Points (Diff)", Double.class);
            return columns;
        }

        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }
        public List<Group<String, List<?>, ?>> getAllowedParameters() {
            return Collections.singletonList(new Group<>("type", Arrays.asList(SGCard.SGCardType.values()), SGCard.SGCardType.Maki));
        }
    }

}
