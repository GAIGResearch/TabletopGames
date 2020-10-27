package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import games.dominion.DominionConstants;
import games.dominion.DominionGameState;
import games.dominion.cards.*;

import java.util.Objects;

public class BuyCard extends AbstractAction {

    public final CardType cardType;
    public final int buyingPlayer;

    public BuyCard(CardType cardToBuy, int playerID) {
        buyingPlayer = playerID;
        cardType = cardToBuy;
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
        // We execute by:
        // i) Checking that we have enough money and buy actions
        // ii) Removing the card from the table and adding it to the player's discard pile
        // iii) Updating the available money and buy actions
        DominionGameState state = (DominionGameState) gs;
        if (state.buysLeft() > 0 && state.availableSpend(buyingPlayer) >= cardType.getCost()) {
            if (state.removeCardFromTable(cardType)) {
                state.changeBuys(-1);
                state.spend(cardType.getCost());
                state.addCard(cardType, buyingPlayer, DominionConstants.DeckType.DISCARD);
                return true;
            }
        }
        return false;
    }

    /**
     * Create a copy of this action, with all of its variables.
     * NO REFERENCES TO COMPONENTS TO BE KEPT IN ACTIONS, PRIMITIVE TYPES ONLY.
     *
     * @return - new AbstractAction object with the same properties.
     */
    @Override
    public AbstractAction copy() {
        // all state is immutable, so no need
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BuyCard) {
            BuyCard other = (BuyCard) obj;
            return other.cardType == cardType && other.buyingPlayer == buyingPlayer;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(buyingPlayer, cardType);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return this.toString();
    }

    @Override
    public String toString() {
        return "BuyCard: " + cardType + " by player " + buyingPlayer;
    }
}
