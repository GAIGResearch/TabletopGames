package games.dominion;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IGameAttribute;
import games.dominion.cards.CardType;

import java.util.function.*;

public enum DominionGameAttributes implements IGameAttribute {
    GAME_ID((s, a) -> s.getGameID()),
    GAME_ROUND((s, a) -> s.getTurnOrder().getRoundCounter()),
    PLAYER((s, a) -> s.getCurrentPlayer()),
    ACTION_TYPE((s, a) -> a == null ? "NONE" : a.getClass().getSimpleName()),
    ACTION_DESCRIPTION((s, a) ->  a == null ? "NONE" : a.getString(s)),
    PROVINCES_LEFT((s, a) -> s.cardsOfType(CardType.PROVINCE, -1, DominionConstants.DeckType.SUPPLY)),
    DUCHIES_LEFT((s, a) -> s.cardsOfType(CardType.DUCHY, -1, DominionConstants.DeckType.SUPPLY)),
    ESTATES_LEFT((s, a) -> s.cardsOfType(CardType.ESTATE, -1, DominionConstants.DeckType.SUPPLY)),
    EMPTY_SUPPLY_SLOTS((s, a) -> s.cardsIncludedInGame().stream()
            .filter(c -> s.cardsOfType(c, -1, DominionConstants.DeckType.SUPPLY) == 0)
            .count());

    private final BiFunction<DominionGameState, AbstractAction, Object> lambda;

    DominionGameAttributes(BiFunction<DominionGameState, AbstractAction, Object> lambda) {
        this.lambda = lambda;
    }

    @Override
    public Object get(AbstractGameState state, AbstractAction action) {
        return lambda.apply((DominionGameState) state, action);
    }

}
