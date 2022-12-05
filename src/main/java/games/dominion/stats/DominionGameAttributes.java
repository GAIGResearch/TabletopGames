package games.dominion.stats;

import core.interfaces.IGameMetric;
import evaluation.metrics.GameListener;
import evaluation.metrics.Event;
import games.dominion.DominionConstants;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;

import java.util.function.*;

public enum DominionGameAttributes implements IGameMetric {
    GAME_ID((l, e) -> e.state.getGameID(), Event.GameEvent.ABOUT_TO_START),
    GAME_ROUND((l, e) -> e.state.getTurnOrder().getRoundCounter(), Event.GameEvent.ROUND_OVER),
    PLAYER((l, e) -> e.state.getCurrentPlayer(), Event.GameEvent.ACTION_TAKEN),
    ACTION_TYPE((l, e) -> e.action == null ? "NONE" : e.action.getClass().getSimpleName(), Event.GameEvent.ACTION_TAKEN),
    ACTION_DESCRIPTION((l, e) ->  e.action == null ? "NONE" : e.action.getString(e.state), Event.GameEvent.ACTION_TAKEN),
    PROVINCES_LEFT((l, e) -> ((DominionGameState)e.state).cardsOfType(CardType.PROVINCE, -1, DominionConstants.DeckType.SUPPLY), Event.GameEvent.GAME_OVER),
    DUCHIES_LEFT((l, e) -> ((DominionGameState)e.state).cardsOfType(CardType.DUCHY, -1, DominionConstants.DeckType.SUPPLY), Event.GameEvent.GAME_OVER),
    ESTATES_LEFT((l, e) -> ((DominionGameState)e.state).cardsOfType(CardType.ESTATE, -1, DominionConstants.DeckType.SUPPLY), Event.GameEvent.GAME_OVER),
    EMPTY_SUPPLY_SLOTS((l, e) -> ((DominionGameState)e.state).cardsIncludedInGame().stream()
            .filter(c -> ((DominionGameState)e.state).cardsOfType(c, -1, DominionConstants.DeckType.SUPPLY) == 0)
            .count(), Event.GameEvent.GAME_OVER);

    private final BiFunction<GameListener, Event, Object> lambda;
    private final Event.GameEvent eventType;

    DominionGameAttributes(BiFunction<GameListener, Event, Object> lambda, Event.GameEvent eventType) {
        this.lambda = lambda;
        this.eventType = eventType;
    }

    @Override
    public Object get(GameListener listener, Event event) {
        return lambda.apply(listener, event);
    }

    @Override
    public boolean listens(Event.GameEvent eventType) {
        return this.eventType == eventType;
    }

    @Override
    public boolean isRecordedPerPlayer() {
        return false;
    }
}
