package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.dominion.*;
import games.dominion.cards.*;

import java.util.*;

import static games.dominion.DominionConstants.*;

public class MoveCard extends AbstractAction {

    final int playerFrom;
    final int playerTo;
    final CardType type;
    final DeckType fromDeck, toDeck;

    public MoveCard(CardType type, int fromPlayer, DeckType fromDeck, int toPlayer, DeckType toDeck) {
        this.type = type;
        playerFrom = fromPlayer;
        playerTo = toPlayer;
        this.fromDeck = fromDeck;
        this.toDeck = toDeck;
    }

    @Override
    public boolean execute(AbstractGameState ags) {
        DominionGameState state = (DominionGameState) ags;
        Optional<DominionCard> cardToMove = state.getDeck(fromDeck, playerFrom).stream()
                .filter(card -> card.cardType() == this.type).findFirst();

        if (cardToMove.isPresent()) {
            DominionCard card = cardToMove.get();
            state.moveCard(card, playerFrom, fromDeck, playerTo, toDeck);
            PartialObservableDeck<DominionCard> destination = (PartialObservableDeck<DominionCard>) state.getDeck(toDeck, playerTo);
            boolean[] cardVisibility = new boolean[state.getNPlayers()];
            cardVisibility[playerFrom] = true;
            destination.setVisibilityOfComponent(0, cardVisibility);
        } else {
            throw new AssertionError("Cannot move card that is not in deck : " + type);
        }
        return true;
    }


    /**
     * Create a copy of this action, with all of its variables.
     * NO REFERENCES TO COMPONENTS TO BE KEPT IN ACTIONS, PRIMITIVE TYPES ONLY.
     *
     * @return - new AbstractAction object with the same properties.
     */
    @Override
    public AbstractAction copy() {
        return this;
        // no state
    }

    @Override
    public String getString(AbstractGameState state) {
        return toString();
    }

    @Override
    public String toString() {
        return String.format("Player %d moves %s from %s to %s of player %d", playerFrom, type, fromDeck, toDeck, playerTo);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof MoveCard) {
            MoveCard dc = (MoveCard) other;
            return dc.playerFrom == playerFrom && dc.type == type && dc.playerTo == playerFrom
                    && dc.toDeck == toDeck && dc.fromDeck == fromDeck;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, playerFrom, fromDeck, playerTo, toDeck);
    }
}
