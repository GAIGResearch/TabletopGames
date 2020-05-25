package games.uno.cards;

import games.uno.UnoGameState;

public class UnoNumberCard extends UnoCard {

    private final int number;

    public UnoNumberCard(UnoCardColor color, UnoCardType type, int number) {
        super(color, type);
        this.number = number;
    }

    @Override
    public boolean isPlayable(UnoGameState gameState) {
        if (gameState.getCurrentCard() instanceof UnoNumberCard) {
            return this.number == ((UnoNumberCard) gameState.getCurrentCard()).number;
        }

        return false;
    }

    @Override
    public String toString(){
       return color.toString() + number;
    }
}
