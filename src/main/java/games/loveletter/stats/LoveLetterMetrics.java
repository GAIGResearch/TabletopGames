package games.loveletter.stats;
import core.components.Deck;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.GameListener;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;
import utilities.Utils;

public class LoveLetterMetrics {

    public class ActionsPlayed extends AbstractMetric
    {
        public ActionsPlayed() {
            addEventType(Event.GameEvent.ACTION_TAKEN);
            recordPerPlayer = true;
        }

        @Override
        public Object run(GameListener listener, Event e) {
            Deck<LoveLetterCard> played = ((LoveLetterGameState)e.state).getPlayerDiscardCards().get(e.playerID);
            StringBuilder ss = new StringBuilder();
            for (LoveLetterCard card : played.getComponents()) {
                ss.append(card.cardType).append(",");
            }
            if (ss.toString().equals("")) return ss.toString();
            ss.append("]");
            return ss.toString().replace(",]", "");
        }
    }

    public class ActionsPlayedWin extends AbstractMetric
    {
        public ActionsPlayedWin() {
            addEventType(Event.GameEvent.ACTION_TAKEN);
            recordPerPlayer = true;
        }

        @Override
        public Object run(GameListener listener, Event e) {
            StringBuilder ss = new StringBuilder();
            if (e.state.getPlayerResults()[e.playerID] == Utils.GameResult.WIN) {
                Deck<LoveLetterCard> played = ((LoveLetterGameState)e.state).getPlayerDiscardCards().get(e.playerID);
                for (LoveLetterCard card : played.getComponents()) {
                    ss.append(card.cardType).append(",");
                }
                if (ss.toString().equals("")) return ss.toString();
                ss.append("]");
            }
            return ss.toString().replace(",]", "");
        }
    }

}
