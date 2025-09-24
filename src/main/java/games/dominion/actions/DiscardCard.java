package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

import java.util.Objects;
import java.util.Optional;

import static games.dominion.DominionConstants.*;

public class DiscardCard extends AbstractAction {

    public final int player;
    public final CardType type;

    public DiscardCard(CardType type, int playerId) {
        this.type = type;
        player = playerId;
    }

    @Override
    public boolean execute(AbstractGameState ags) {
        DominionGameState state = (DominionGameState) ags;
        Optional<DominionCard> cardToDiscard = state.getDeck(DeckType.HAND, player).stream()
                .filter(card -> card.cardType() == this.type).findFirst();

        if (cardToDiscard.isPresent()) {
            DominionCard card = cardToDiscard.get();
            state.moveCard(card, player, DeckType.HAND, player, DeckType.DISCARD);
        } else {
            System.out.println(ags);
            throw new AssertionError("Cannot discard card that is not in hand : " + this);
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
    public String toString() {
        return String.format("Player %d discards %s", player, type);
    }

    @Override
    public String getString(AbstractGameState state) {
        return toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof DiscardCard) {
            DiscardCard dc = (DiscardCard) other;
            return dc.player == player && dc.type == type;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, type);
    }
}
