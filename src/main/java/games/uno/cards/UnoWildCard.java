package games.uno.cards;

import games.uno.UnoGameState;

public class UnoWildCard extends UnoCard {

    public UnoWildCard() {
        super(UnoCardColor.Wild, UnoCardType.Wild, -1);
    }

    @Override
    public UnoCard copy() {
        return new UnoWildCard();
    }

    // It is always playable
    @Override
    public boolean isPlayable(UnoGameState gameState) {
        return true;
    }

    @Override
    public String toString() {
        return "Wild";
    }
}
