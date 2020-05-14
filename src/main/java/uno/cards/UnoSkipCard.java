package uno.cards;

import uno.UnoGameState;


public class UnoSkipCard extends UnoCard {

    public UnoSkipCard(UnoCardColor color) {
        super(color, UnoCardType.Skip, -1);
    }

    // It is playable if the color is the same of the currentCard color or the currentCard is a Skip one
    @Override
    public boolean isPlayable(UnoGameState gameState) {
        return this.color == gameState.currentColor || gameState.currentCard instanceof UnoSkipCard;
    }

    @Override
    public String toString() {
        return color.toString() + "Skip";
    }
}
