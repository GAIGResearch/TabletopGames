package uno.cards;

import uno.UnoGameState;

public class UnoWildDrawFourCard extends UnoCard {

    public UnoWildDrawFourCard() {
        super(UnoCard.UnoCardColor.Wild, UnoCardType.WildDrawFour, -1);
    }

    // It is playable if the color is the same of the currentColor
    @Override
    public boolean isPlayable(UnoGameState gameState) {
        return this.color == gameState.currentColor;
    }
}
