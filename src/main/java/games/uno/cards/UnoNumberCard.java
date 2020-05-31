package games.uno.cards;

import games.uno.UnoGameState;

public class UnoNumberCard extends UnoCard {

     public UnoNumberCard(String color, int number) {
        super(color, UnoCardType.Number, number);
    }

    @Override
    public UnoCard copy() {
        return new UnoNumberCard(color, number);
    }

    // To be playable, the card number or color must to be the same than the current one
    @Override
    public boolean isPlayable(UnoGameState gameState) {
        return this.number == gameState.getCurrentCard().number || this.color.equals(gameState.getCurrentColor());
    }

    @Override
    public String toString() {
       return "Card{" + color + " " + number + "}";
    }
}
