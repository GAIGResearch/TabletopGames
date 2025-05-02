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
    final boolean isPubliclyVisible;

    public MoveCard(CardType type, int fromPlayer, DeckType fromDeck, int toPlayer, DeckType toDeck, boolean revealed) {
        this.type = type;
        playerFrom = fromPlayer;
        playerTo = toPlayer;
        this.fromDeck = fromDeck;
        this.toDeck = toDeck;
        this.isPubliclyVisible = revealed;
    }

    @Override
    public boolean execute(AbstractGameState ags) {
        DominionGameState state = (DominionGameState) ags;
        Optional<DominionCard> cardToMove = state.getDeck(fromDeck, playerFrom).stream()
                .filter(card -> card.cardType() == this.type).findFirst();

        if (cardToMove.isPresent()) {

            // Moving to discard pile was causing a casting error Deck -> PartialObservableDeck
            if (toDeck == DeckType.DISCARD) {
                state.moveCard(cardToMove.get(), playerFrom, fromDeck, playerTo, toDeck);
                return true;
            }

            DominionCard card = cardToMove.get();
            state.moveCard(card, playerFrom, fromDeck, playerTo, toDeck);
            PartialObservableDeck<DominionCard> destination = (PartialObservableDeck<DominionCard>) state.getDeck(toDeck, playerTo);
            boolean[] cardVisibility = new boolean[state.getNPlayers()];
            cardVisibility[playerFrom] = true;
            if (isPubliclyVisible) {
                for (int i = 0; i < state.getNPlayers(); i++)
                    cardVisibility[i] = true;
            }
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
        return String.format("Player %d moves %s from %s to %s of player %d (visible: %s)",
                playerFrom, type, fromDeck, toDeck, playerTo, isPubliclyVisible);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof MoveCard) {
            MoveCard dc = (MoveCard) other;
            return dc.playerFrom == playerFrom && dc.type == type && dc.playerTo == playerFrom
                    && dc.toDeck == toDeck && dc.fromDeck == fromDeck && dc.isPubliclyVisible == isPubliclyVisible;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, playerFrom, fromDeck, playerTo, toDeck, isPubliclyVisible);
    }
}
