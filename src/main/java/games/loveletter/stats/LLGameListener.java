package games.loveletter.stats;
import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGameAttribute;
import core.interfaces.IGameListener;
import core.interfaces.IStatisticLogger;
import games.loveletter.LoveLetterGameState;
import games.loveletter.actions.DrawCard;
import games.terraformingmars.TMGameState;
import games.terraformingmars.actions.TMAction;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
public class LLGameListener implements IGameListener {

    IStatisticLogger logger;
    public LLGameListener(IStatisticLogger logger) { this.logger = logger;}

    @Override
    public void onGameEvent(CoreConstants.GameEvents type, Game game) {
        if (type == CoreConstants.GameEvents.GAME_OVER) {
            AbstractGameState state = game.getGameState();
            Map<String, Object> data = Arrays.stream(LLGameAttributes.values())
                    .collect(Collectors.toMap(IGameAttribute::name, attr -> attr.get(state, null)));
            logger.record(data);
        }
    }

    @Override
    public void onEvent(CoreConstants.GameEvents type, AbstractGameState state, AbstractAction action) {
    }

    @Override
    public void allGamesFinished() {
        logger.processDataAndFinish();
    }

    public enum LLGameAttributes implements IGameAttribute {
        DISCARDED_CARDS((s, a) -> {
            int nCards = 0;
            for(int i = 0; i < s.getNPlayers(); i++) {
                nCards += s.getPlayerDiscardCards().get(i).getSize();
            }
            return nCards;
        });

        private final BiFunction<LoveLetterGameState, DrawCard, Object> lambda_sa;

        LLGameAttributes(BiFunction<LoveLetterGameState, DrawCard, Object> lambda) {
            this.lambda_sa = lambda;
        }

        @Override
        public Object get(AbstractGameState state, AbstractAction action) {
            return lambda_sa.apply((LoveLetterGameState) state, (DrawCard) action);
        }
    }
}
