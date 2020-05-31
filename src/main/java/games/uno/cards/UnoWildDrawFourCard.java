package games.uno.cards;

import core.components.Deck;
import games.uno.UnoGameState;

public class UnoWildDrawFourCard extends UnoCard {
    public UnoWildDrawFourCard() {
        super("Wild", UnoCardType.WildDrawFour, -1);
    }

    @Override
    public UnoCard copy() {
        return new UnoWildDrawFourCard();
    }

    // It is playable if the player has not cards on the hand with the same color than the current one.
    @Override
    public boolean isPlayable(UnoGameState gameState) {
        int playerID = gameState.getCurrentPlayerID();
        Deck<UnoCard> playerHand = gameState.getPlayerDecks().get(playerID);
        for (UnoCard card : playerHand.getComponents()) {
            if (card.color.equals(gameState.getCurrentColor()))
                 return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "WildDrawFour";
    }
}
