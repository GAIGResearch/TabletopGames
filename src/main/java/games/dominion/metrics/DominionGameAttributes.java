package games.dominion.metrics;

import core.interfaces.IGameMetric;
import evaluation.GameListener;
import evaluation.metrics.Event;
import games.dominion.DominionConstants;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;

import java.util.function.*;

public enum DominionGameAttributes implements IGameMetric {
    GAME_ID((l, e) -> e.state.getGameID()),
    GAME_ROUND((l, e) -> e.state.getTurnOrder().getRoundCounter()),
    PLAYER((l, e) -> e.state.getCurrentPlayer()),
    ACTION_TYPE((l, e) -> e.action == null ? "NONE" : e.action.getClass().getSimpleName()),
    ACTION_DESCRIPTION((l, e) ->  e.action == null ? "NONE" : e.action.getString(e.state)),
    PROVINCES_LEFT((l, e) -> ((DominionGameState)e.state).cardsOfType(CardType.PROVINCE, -1, DominionConstants.DeckType.SUPPLY)),
    DUCHIES_LEFT((l, e) -> ((DominionGameState)e.state).cardsOfType(CardType.DUCHY, -1, DominionConstants.DeckType.SUPPLY)),
    ESTATES_LEFT((l, e) -> ((DominionGameState)e.state).cardsOfType(CardType.ESTATE, -1, DominionConstants.DeckType.SUPPLY)),
    EMPTY_SUPPLY_SLOTS((l, e) -> ((DominionGameState)e.state).cardsIncludedInGame().stream()
            .filter(c -> ((DominionGameState)e.state).cardsOfType(c, -1, DominionConstants.DeckType.SUPPLY) == 0)
            .count());

    private final BiFunction<GameListener, Event, Object> lambda;

    DominionGameAttributes(BiFunction<GameListener, Event, Object> lambda) {
        this.lambda = lambda;
    }

    @Override
    public Object get(GameListener listener, Event event) {
        return lambda.apply(listener, event);
    }
}
