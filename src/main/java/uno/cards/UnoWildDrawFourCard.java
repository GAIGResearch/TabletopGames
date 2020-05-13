package uno.cards;

import core.components.Deck;
import uno.UnoGameState;

public class UnoWildDrawFourCard extends UnoCard {

    public UnoWildDrawFourCard() {
        super(UnoCard.UnoCardColor.Wild, UnoCardType.WildDrawFour, -1);
    }

    // It is playable if the player has not playable Number cards on the hand
    @Override
    public boolean isPlayable(UnoGameState gameState) {
        /*int playerID = gameState.GetCurrentPLayerID();
        Deck<UnoCard> playerHand = gameState.playerDecks.get(playerID);
        boolean canPlay = true;
        for (UnoCard card : playerHand.getCards()) {
            if (card.isPlayable(gameState) && card instanceof UnoNumberCard) {
                canPlay = false;
                break;
            }
        }

        return canPlay;
        */
         return true;
    }

    @Override
    public String toString() {
        return "WildDrawFour";
    }
}
