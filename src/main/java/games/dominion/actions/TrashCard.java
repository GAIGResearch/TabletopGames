package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

import java.util.*;

import static games.dominion.DominionConstants.*;
import static java.util.stream.Collectors.*;

public class TrashCard extends AbstractAction {

    public final int player;
    public final CardType trashedCard;
    public final DeckType fromDeck;

    public TrashCard(CardType trash, int playerId) {
        this(trash, playerId, DeckType.HAND);
    }

    public TrashCard(CardType trash, int playerId, DeckType from) {
        trashedCard = trash;
        player = playerId;
        fromDeck = from;
    }

    /**
     * Executes this action, applying its effect to the given game state. Can access any component IDs stored
     * through the AbstractGameState.getComponentById(int id) method.
     *
     * @param gs - game state which should be modified by this action.
     * @return - true if successfully executed, false otherwise.
     */
    @Override
    public boolean execute(AbstractGameState gs) {
        DominionGameState state = (DominionGameState) gs;
        List<DominionCard> matchingCards = state.getDeck(fromDeck, player).stream()
                .filter(c -> c.cardType() == trashedCard).collect(toList());
        if (matchingCards.isEmpty()) {
            throw new AssertionError("Cannot trash a card you do not have: " + trashedCard + " from " + fromDeck);
        }
        state.moveCard(matchingCards.get(0), player, fromDeck, -1, DeckType.TRASH);
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
        // immutable state
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TrashCard) {
            TrashCard other = (TrashCard) obj;
            return other.player == player && other.trashedCard == trashedCard && other.fromDeck == fromDeck;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(trashedCard, player, fromDeck);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return String.format("Player %d trashes a %s from %s", player, trashedCard, fromDeck);
    }
}
