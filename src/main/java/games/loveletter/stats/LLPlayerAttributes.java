package games.loveletter.stats;
import core.AbstractGameState;
import core.components.Deck;
import core.interfaces.IGameMetric;
import evaluation.GameListener;
import evaluation.metrics.Event;
import evaluation.metrics.GameStatisticsListener;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;
import utilities.Utils;

import java.util.ArrayList;
import java.util.function.BiFunction;
public enum LLPlayerAttributes implements IGameMetric {
    RESULT((l, e) -> e.state.getPlayerResults()[e.playerID].value),
    ACTIONS_PLAYED((l, e) -> {
        Deck<LoveLetterCard> played = ((LoveLetterGameState)e.state).getPlayerDiscardCards().get(e.playerID);
        StringBuilder ss = new StringBuilder();
        for (LoveLetterCard card : played.getComponents()) {
            ss.append(card.cardType).append(",");
        }
        if (ss.toString().equals("")) return ss.toString();
        ss.append("]");
        return ss.toString().replace(",]", "");
    }),
    ACTIONS_PLAYED_WIN((l, e) -> {
        StringBuilder ss = new StringBuilder();
        if (e.state.getPlayerResults()[e.playerID] == Utils.GameResult.WIN) {
            String lastHistory = e.state.getHistoryAsText().get(((ArrayList) e.state.getHistoryAsText()).size() - 1);
            Deck<LoveLetterCard> played = ((LoveLetterGameState)e.state).getPlayerDiscardCards().get(e.playerID);
            if (played.getSize() == 0) {
                //Won by play of the opponent.
                ss.append("");
            }
            for (LoveLetterCard card : played.getComponents()) {
                ss.append(card.cardType).append(",");
            }
            if (ss.toString().equals("")) return ss.toString();
            ss.append("]");
        }
        return ss.toString().replace(",]", "");
    });
    private final BiFunction<LLGameListener, Event, Object> lambda_sp;
    LLPlayerAttributes(BiFunction<LLGameListener, Event, Object> lambda) {
        this.lambda_sp = lambda;
    }

    @Override
    public Object get(GameListener listener, Event event) {
        return lambda_sp.apply((LLGameListener)listener, event);
    }

}
