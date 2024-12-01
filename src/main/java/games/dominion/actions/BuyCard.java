package games.dominion.actions;

import core.AbstractGameState;
import games.dominion.DominionGameState;
import games.dominion.cards.*;

/**
 * An extension of GainCard that implements control over Buys and AvailableSpend
 */
public class BuyCard extends GainCard {

    public BuyCard(CardType cardToBuy, int playerID) {
        super(cardToBuy, playerID);
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
        if (state.getBuysLeft() > 0 && state.getAvailableSpend(buyingPlayer) >= cardType.cost) {
            boolean success = super.execute(state);
            if (success) {
                state.changeBuys(-1);
                state.spend(cardType.cost);
            }
            return success;
        }
        return false;
    }

    @Override
    public String toString() {
        return "BuyCard: " + cardType + " by player " + buyingPlayer;
    }
}
