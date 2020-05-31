package games.uno.cards;

import games.uno.UnoGameState;


public class UnoSkipCard extends UnoCard {

    public UnoSkipCard(String color) {
        super(color, UnoCardType.Skip, -1);
    }

    @Override
    public UnoCard copy() {
        return new UnoSkipCard(color);
    }

    // It is playable if the color is the same of the currentCard color or the currentCard is a Skip one
    @Override
    public boolean isPlayable(UnoGameState gameState) {
        return this.color.equals(gameState.getCurrentColor()) || gameState.getCurrentCard() instanceof UnoSkipCard;
    }

    @Override
    public String toString() {
        return "Skip{" + color + "}";
    }
}
