package games.sushigo.metrics;

import core.CoreConstants;
import core.components.Deck;
import evaluation.listeners.GameListener;
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
    public static class CardPlayability extends AbstractMetric {
        @Override
        public Object run(GameListener listener, Event e) {
            SGGameState gs = (SGGameState) e.state;
            ChooseCard action = (ChooseCard)e.action;
            SGCard c = gs.getPlayerHands().get(action.playerId).get(action.cardIdx);
            return c.toString();
        }
        @Override
        public Set<Event.GameEvent> getEventTypes() {
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
     */
    public static class CardChosenInFavourOf extends AbstractParameterizedMetric {
        public CardChosenInFavourOf(){super();}
        public CardChosenInFavourOf(Object arg){super(arg);}
        @Override
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }
        @Override
        public Object run(GameListener listener, Event e) {
            String typeCount = (String) getParameterValue("typeCount");
            SGCard.SGCardType type = SGCard.SGCardType.valueOf(typeCount.split("-")[0]);
            int count = Integer.parseInt(typeCount.split("-")[1]);

            SGGameState gs = (SGGameState) e.state;
            ChooseCard action = (ChooseCard)e.action;
            Deck<SGCard> playerHand = gs.getPlayerHands().get(action.playerId);
            SGCard chosenCard = playerHand.get(action.cardIdx);

            if(chosenCard.type == type && chosenCard.count == count) {
                StringBuilder ss = new StringBuilder();
                for (int i = 0; i < playerHand.getSize(); i++) {
                    SGCard otherCard = playerHand.get(i);
                    if (!chosenCard.toString().equals(otherCard.toString())) {
                        ss.append(otherCard).append(",");
                    }
                }
                if (ss.toString().equals("")) return ss.toString();
                ss.append("]");
                return ss.toString().replace(",]", "");
            }
            return "";
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
     */
    public static class CardPlayedWin extends AbstractParameterizedMetric {
        public CardPlayedWin(){super();}
        public CardPlayedWin(Object arg){super(arg);}
        @Override
        public Object run(GameListener listener, Event e) {
            SGCard.SGCardType type = (SGCard.SGCardType) getParameterValue("type");
            SGGameState gs = (SGGameState) e.state;
            for (int i = 0; i < gs.getNPlayers(); i++) {
                if (gs.getPlayerResults()[i] == CoreConstants.GameResult.WIN_GAME) {
                    return gs.getPlayedCardTypesAllGame()[i].get(type).getValue();
                }
            }
            return 0;
        }
        @Override
        public Set<Event.GameEvent> getEventTypes() {
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
        public CardPoints(Object arg){super(arg);}
        @Override
        public Object run(GameListener listener, Event e) {
            SGCard.SGCardType type = (SGCard.SGCardType) getParameterValue("type");
            SGGameState gs = (SGGameState) e.state;
            double sum = 0;
            for (int i = 0; i < gs.getNPlayers(); i++) {
                sum += gs.getPointsPerCardType()[i].get(type).getValue();
            }
            return sum/gs.getNPlayers();
        }
        @Override
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }
        public List<Group<String, List<?>, ?>> getAllowedParameters() {
            return Collections.singletonList(new Group<>("type", Arrays.asList(SGCard.SGCardType.values()), SGCard.SGCardType.Maki));
        }
    }

    /**
     * How many points does each card type bring to a player on average, as a percentage of the player's total points at the end     *
     */
    public static class CardPointsPercentage extends AbstractParameterizedMetric {
        public CardPointsPercentage(){super();}
        public CardPointsPercentage(Object arg){super(arg);}
        @Override
        public Object run(GameListener listener, Event e) {
            SGCard.SGCardType type = (SGCard.SGCardType) getParameterValue("type");
            SGGameState gs = (SGGameState) e.state;
            double sum = 0;
            for (int i = 0; i < gs.getNPlayers(); i++) {
                sum += gs.getPointsPerCardType()[i].get(type).getValue() * 1.0 / gs.getPlayerScore()[i].getValue();
            }
            return sum/gs.getNPlayers();
        }
        @Override
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }
        public List<Group<String, List<?>, ?>> getAllowedParameters() {
            return Collections.singletonList(new Group<>("type", Arrays.asList(SGCard.SGCardType.values()), SGCard.SGCardType.Maki));
        }
    }

    /**
     * What is the difference in points a card type brings to players on average
     */
    public static class CardPointsDifference extends AbstractParameterizedMetric {
        public CardPointsDifference(){super();}
        public CardPointsDifference(Object arg){super(arg);}
        @Override
        public Object run(GameListener listener, Event e) {
            SGCard.SGCardType type = (SGCard.SGCardType) getParameterValue("type");
            SGGameState gs = (SGGameState) e.state;
            double sum = 0;
            for (int i = 0; i < gs.getNPlayers()-1; i++) {
                sum += Math.abs(gs.getPointsPerCardType()[i].get(type).getValue() - gs.getPointsPerCardType()[i+1].get(type).getValue());
            }
            return sum/(gs.getNPlayers()-1);
        }
        @Override
        public Set<Event.GameEvent> getEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }
        public List<Group<String, List<?>, ?>> getAllowedParameters() {
            return Collections.singletonList(new Group<>("type", Arrays.asList(SGCard.SGCardType.values()), SGCard.SGCardType.Maki));
        }
    }
}
