package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.dominion.*;
import games.dominion.cards.*;

import java.util.*;

import static games.dominion.DominionConstants.*;

public class PlaceOnDeck extends AbstractAction {

    final int player;
    final CardType type;

    public PlaceOnDeck(CardType type, int playerId) {
        this.type = type;
        player = playerId;
    }

    @Override
    public boolean execute(AbstractGameState ags) {
        DominionGameState state = (DominionGameState) ags;
        Optional<DominionCard> cardToMove = state.getDeck(DeckType.HAND, player).stream()
                .filter(card -> card.cardType() == this.type).findFirst();

        if (cardToMove.isPresent()) {
            DominionCard card = cardToMove.get();
            state.moveCard(card, player, DeckType.HAND, player, DeckType.DRAW);
            PartialObservableDeck<DominionCard> drawDeck = (PartialObservableDeck<DominionCard>) state.getDeck(DeckType.DRAW, player);
            boolean[] cardVisibility = new boolean[state.getNPlayers()];
            cardVisibility[player] = true;
            drawDeck.setVisibilityOfComponent(0, cardVisibility);
        } else {
            throw new AssertionError("Cannot move card that is not in hand : " + type);
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
        return String.format("Player %d moves %s to top of Draw deck", player, type);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof PlaceOnDeck) {
            PlaceOnDeck dc = (PlaceOnDeck) other;
            return dc.player == player && dc.type == type;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, type, DeckType.DRAW);
    }
}
