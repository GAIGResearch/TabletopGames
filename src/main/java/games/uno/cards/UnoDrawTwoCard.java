package games.uno.cards;

import games.uno.UnoGameState;

public class UnoDrawTwoCard extends UnoCard {

    public UnoDrawTwoCard(UnoCardColor color) {
        super(color, UnoCardType.DrawTwo, -1);
    }

    // It is playable if the color is the same of the currentCard color or the currentCard is a DrawTwo one
    @Override
    public boolean isPlayable(UnoGameState gameState) {
        return this.color == gameState.currentColor || gameState.currentCard instanceof UnoDrawTwoCard;
    }

    @Override
    public String toString() {
        return color.toString() + "DrawTwo";
    }
}
