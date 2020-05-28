package games.uno.cards;

import games.uno.UnoGameState;

public class UnoReverseCard extends UnoCard {

    public UnoReverseCard(UnoCardColor color) {
        super(color, UnoCardType.Reverse, -1);
    }

    @Override
    public UnoCard copy() {
        return new UnoReverseCard(color);
    }

    // It is playable if the color is the same of the currentCard color or the currentCard is a Reverse one
    @Override
    public boolean isPlayable(UnoGameState gameState) {
       return this.color == gameState.getCurrentColor() || gameState.getCurrentCard() instanceof UnoReverseCard;
    }
    @Override
    public String toString() {
        return color.toString() + "Reverse";
    }
}
