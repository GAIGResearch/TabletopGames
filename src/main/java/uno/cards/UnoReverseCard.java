package uno.cards;

import uno.UnoGameState;

public class UnoReverseCard extends UnoCard {

    public UnoReverseCard(UnoCardColor color) {
        super(color, UnoCardType.Reverse, -1);
    }

    // It is playable if the color is the same of the currentColor
    @Override
    public boolean isPlayable(UnoGameState gameState) {
        return this.color == gameState.currentColor || gameState.currentCard instanceof UnoReverseCard;
    }

    @Override
    public String toString() {
        return color.toString() + "Reverse";
    }
}
