package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dominion.DominionConstants;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;

import java.util.Objects;

public class GainCard extends AbstractAction {

    public final CardType cardType;
    public final int buyingPlayer;
    public final DominionConstants.DeckType destinationDeck;

    public GainCard(CardType cardToBuy, int playerID, DominionConstants.DeckType deck) {
        buyingPlayer = playerID;
        cardType = cardToBuy;
        destinationDeck = deck;
    }
    public GainCard(CardType cardToBuy, int playerID) {
        this(cardToBuy, playerID, DominionConstants.DeckType.DISCARD);
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
        // We execute by removing the card from the table and adding it to the relevant deck
        DominionGameState state = (DominionGameState) gs;
        if (state.removeCardFromTable(cardType)) {
            state.addCard(cardType, buyingPlayer, destinationDeck);
            return true;
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
        if (obj instanceof GainCard) {
            GainCard other = (GainCard) obj;
            return other.cardType == cardType
                    && other.buyingPlayer == buyingPlayer
                    && other.destinationDeck == destinationDeck;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(buyingPlayer, cardType, destinationDeck);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return this.toString();
    }

    @Override
    public String toString() {
        return "GainCard: " + cardType + " by player " + buyingPlayer;
    }
}
