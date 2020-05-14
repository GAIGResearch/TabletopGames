package games.uno.cards;

import core.components.Deck;
import games.uno.UnoGameState;

public class UnoWildDrawFourCard extends UnoCard {
    public UnoWildDrawFourCard() {
        super(UnoCard.UnoCardColor.Wild, UnoCardType.WildDrawFour, -1);
    }

    // It is playable if the player has not cards on the hand with the same color than the current one.
    @Override
    public boolean isPlayable(UnoGameState gameState) {
        int playerID = gameState.getCurrentPLayerID();
        Deck<UnoCard> playerHand = gameState.playerDecks.get(playerID);
        for (UnoCard card : playerHand.getCards()) {
            if (card.color == gameState.currentColor)
                 return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "WildDrawFour";
    }
}
