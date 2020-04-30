package pandemic.engine.gameOver;

import components.IDeck;
import core.GameState;

import static pandemic.Constants.*;

public class GameOverDrawCards extends GameOverCondition {
    @Override
    public int test(GameState gs) {
        IDeck cityDeck =  gs.findDeck("Player Deck");
        boolean canDraw = cityDeck.getCards().size() > 0;

        // if player cannot draw it means that the deck is empty -> GAME OVER
        if (!canDraw){
            System.out.println("No more cards to draw");
            return GAME_LOSE;
        }
        return GAME_ONGOING;
    }
}
