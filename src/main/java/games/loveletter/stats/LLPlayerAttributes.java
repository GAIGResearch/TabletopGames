package games.loveletter.stats;

import core.components.Deck;
import core.interfaces.IGameMetric;
import evaluation.metrics.GameListener;
import evaluation.metrics.Event;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;
import utilities.Utils;

import java.util.ArrayList;
import java.util.function.BiFunction;

import static evaluation.metrics.Event.GameEvent.GAME_OVER;

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
//            String lastHistory = e.state.getHistoryAsText().get(e.state.getHistoryAsText().size() - 1);
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

    private final BiFunction<GameListener, Event, Object> lambda_sp;
    LLPlayerAttributes(BiFunction<GameListener, Event, Object> lambda) {
        this.lambda_sp = lambda;
    }

    @Override
    public Object get(GameListener listener, Event event) {
        return lambda_sp.apply(listener, event);
    }

    @Override
    public boolean listens(Event.GameEvent eventType) {
        return eventType == GAME_OVER;
    }

    @Override
    public boolean isRecordedPerPlayer() {
        return true;
    }

}
