package games.sushigo.metrics;

import evaluation.metrics.*;
import games.sushigo.SGGameState;
import games.sushigo.actions.ChooseCard;
import games.sushigo.cards.SGCard;
import utilities.Utils;

public class SushiGoMetrics implements IMetricsCollection {

    /**
     * How many times a card type is played during the game
     */
    public static class CardPlayability extends AbstractMetric {
        public CardPlayability() {
            addEventType(Event.GameEvent.ACTION_CHOSEN);
        }
        @Override
        public Object run(GameListener listener, Event e) {
            SGGameState gs = (SGGameState) e.state;
            ChooseCard action = (ChooseCard)e.action;
            SGCard c = gs.getPlayerHands().get(action.playerId).get(action.cardIdx);
            return c.type;
        }
    }

    /**
     * How many times each card type is played by the winning player
     */
    public static class CardPlayedWin extends AbstractParameterizedMetric {
        SGCard.SGCardType type;
        public CardPlayedWin() {
            this(SGCard.SGCardType.Maki.name());
        }
        public CardPlayedWin(String type) {
            addEventType(Event.GameEvent.GAME_OVER);
            this.type = SGCard.SGCardType.valueOf(type);
        }
        @Override
        public Object run(GameListener listener, Event e) {
            SGGameState gs = (SGGameState) e.state;
            for (int i = 0; i < gs.getNPlayers(); i++) {
                if (gs.getPlayerResults()[i] == Utils.GameResult.WIN) {
                    return gs.getPlayedCardTypesAllGame()[i].get(type).getValue();
                }
            }
            return 0;
        }
        public String name() {return getClass().getSimpleName() + " (" + type + ")";}
        @Override
        public Object[] getAllowedParameters() {
            return SGCard.SGCardType.values();
        }
    }

    /**
     * How many points does each card type bring to a player on average
     */
    public static class CardPoints extends AbstractParameterizedMetric {
        SGCard.SGCardType type;
        public CardPoints() {
            this(SGCard.SGCardType.Maki.name());
        }
        public CardPoints(String type) {
            addEventType(Event.GameEvent.GAME_OVER);
            this.type = SGCard.SGCardType.valueOf(type);
        }
        @Override
        public Object run(GameListener listener, Event e) {
            SGGameState gs = (SGGameState) e.state;
            double sum = 0;
            for (int i = 0; i < gs.getNPlayers(); i++) {
                sum += gs.getPointsPerCardType()[i].get(type).getValue();
            }
            return sum/gs.getNPlayers();
        }

        public String name() {return getClass().getSimpleName() + " (" + type + ")";}
        @Override
        public Object[] getAllowedParameters() {
            return SGCard.SGCardType.values();
        }
    }

    /**
     * How many points does each card type bring to a player on average, as a percentage of the player's total points at the end     *
     */
    public static class CardPointsPercentage extends AbstractParameterizedMetric {
        SGCard.SGCardType type;
        public CardPointsPercentage() {
            this(SGCard.SGCardType.Maki.name());
        }
        public CardPointsPercentage(String type) {
            addEventType(Event.GameEvent.GAME_OVER);
            this.type = SGCard.SGCardType.valueOf(type);
        }
        @Override
        public Object run(GameListener listener, Event e) {
            SGGameState gs = (SGGameState) e.state;
            double sum = 0;
            for (int i = 0; i < gs.getNPlayers(); i++) {
                sum += gs.getPointsPerCardType()[i].get(type).getValue() * 1.0 / gs.getPlayerScore()[i].getValue();
            }
            return sum/gs.getNPlayers();
        }

        public String name() {return getClass().getSimpleName() + " (" + type + ")";}
        @Override
        public Object[] getAllowedParameters() {
            return SGCard.SGCardType.values();
        }
    }

    /**
     * What is the difference in points a card type brings to players on average
     */
    public static class CardPointsDifference extends AbstractParameterizedMetric {
        SGCard.SGCardType type;
        public CardPointsDifference() {
            this(SGCard.SGCardType.Maki.name());
        }
        public CardPointsDifference(String type) {
            addEventType(Event.GameEvent.GAME_OVER);
            this.type = SGCard.SGCardType.valueOf(type);
        }
        @Override
        public Object run(GameListener listener, Event e) {
            SGGameState gs = (SGGameState) e.state;
            double sum = 0;
            for (int i = 0; i < gs.getNPlayers()-1; i++) {
                sum += Math.abs(gs.getPointsPerCardType()[i].get(type).getValue() - gs.getPointsPerCardType()[i+1].get(type).getValue());
            }
            return sum/(gs.getNPlayers()-1);
        }

        public String name() {return getClass().getSimpleName() + " (" + type + ")";}
        @Override
        public Object[] getAllowedParameters() {
            return SGCard.SGCardType.values();
        }
    }
}
