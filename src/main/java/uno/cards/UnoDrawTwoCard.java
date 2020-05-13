package uno.cards;

import uno.UnoGameState;

public class UnoDrawTwoCard extends UnoCard {

    public UnoDrawTwoCard(UnoCardColor color) {
        super(color, UnoCardType.DrawTwo, -1);
    }

    // It is playable if the color is the same of the currentColor
    @Override
    public boolean isPlayable(UnoGameState gameState) {
        //return this.color == gameState.currentColor;
        return true;
    }


    @Override
    public String toString() {
        return color.toString() + "DrawTwo";
    }
}
