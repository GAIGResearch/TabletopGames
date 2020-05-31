package games.uno.cards;

import games.uno.UnoGameState;

public class UnoDrawTwoCard extends UnoCard {

    public UnoDrawTwoCard(String color) {
        super(color, UnoCardType.DrawTwo, -1);
    }

    @Override
    public UnoCard copy() {
        return new UnoDrawTwoCard(color);
    }

    // It is playable if the color is the same of the currentCard color or the currentCard is a DrawTwo one
    @Override
    public boolean isPlayable(UnoGameState gameState) {
        return this.color.equals(gameState.getCurrentColor()) || gameState.getCurrentCard() instanceof UnoDrawTwoCard;
    }

    @Override
    public String toString() {
        return "DrawTwo{" + color + "}";
    }
}
