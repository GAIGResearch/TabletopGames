package uno.cards;

import uno.UnoGameState;

public class UnoWildCard extends UnoCard {

    public UnoWildCard() {
        super(UnoCardColor.Wild, UnoCardType.Wild, -1);
    }

    // It is always playable
    @Override
    public boolean isPlayable(UnoGameState gameState) {
        return true;
    }
}
