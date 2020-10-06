package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dominion.DominionGameState;
import games.dominion.DominionPlayerState;
import games.dominion.cards.*;

public class BuyCard extends DominionAction {

    CardType cardType;
    int buyingPlayer;

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
        DominionPlayerState playerState = state.getPlayerState(buyingPlayer);
        if (playerState.currentBuys() > 0 && playerState.availableSpend() >= cardType.getCost()) {
            if (state.removeCardFromTable(cardType)) {
                playerState.changeBuys(-1);
                playerState.changeSpend(cardType.getCost());
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
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return null;
    }
}
