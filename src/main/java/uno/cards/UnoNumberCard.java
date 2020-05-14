package uno.cards;

import uno.UnoGameState;

public class UnoNumberCard extends UnoCard {

     public UnoNumberCard(UnoCardColor color, int number) {
        super(color, UnoCardType.Number, number);
    }

    // To be playable, the card number or color must to be the same than the current one
    @Override
    public boolean isPlayable(UnoGameState gameState) {
        return this.number == gameState.currentCard.number || this.color == gameState.currentColor;
    }

    @Override
    public String toString() {
       return color.toString() + number;
    }
}
