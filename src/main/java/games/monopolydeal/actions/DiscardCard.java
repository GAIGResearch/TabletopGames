package games.monopolydeal.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.MonopolyDealCard;

import java.util.Objects;
import java.util.Optional;

public class DiscardCard extends AbstractAction {

    final int player;
    final CardType type;

    public DiscardCard(CardType type, int playerId) {
        this.type = type;
        player = playerId;
    }

    @Override
    public boolean execute(AbstractGameState ags) {
        MonopolyDealGameState state = (MonopolyDealGameState) ags;
        Optional<MonopolyDealCard> cardToDiscard = state.getPlayerHand(player).stream()
                .filter(card -> card.cardType() == this.type).findFirst();

        if (cardToDiscard.isPresent()) {
            state.discardCard(type, player);
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
        return new DiscardCard(type, player);
        // no state
    }

    @Override
    public String getString(AbstractGameState state) {
        return String.format("Player %d discards %s", player, type);
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
