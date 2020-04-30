package updated_core.games.uno.cards;

import updated_core.games.uno.UnoGameState;

public class UnoNumberCard extends UnoCard {

    private final int number;

    public UnoNumberCard(UnoCardColor color, UnoCardType type, int number) {
        super(color, type);
        this.number = number;
    }

    @Override
    public boolean isPlayable(UnoGameState gameState) {
        if (gameState.currentCard instanceof  UnoNumberCard){
            if (this.number == ((UnoNumberCard) gameState.currentCard).number)
                return true;
        }

        return false;
    }

    @Override
    public String toString(){
       return color.toString() + number;
    }
}
