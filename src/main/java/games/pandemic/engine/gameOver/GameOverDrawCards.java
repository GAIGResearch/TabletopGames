package games.pandemic.engine.gameOver;

import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;
import games.pandemic.PandemicGameState;
import utilities.Utils;

import static games.pandemic.PandemicConstants.playerDeckHash;
import static utilities.Utils.GameResult.GAME_LOSE;
import static utilities.Utils.GameResult.GAME_ONGOING;

@SuppressWarnings("unchecked")
public class GameOverDrawCards extends GameOverCondition {
    @Override
    public Utils.GameResult test(AbstractGameState gs) {
        Deck<Card> cityDeck = (Deck<Card>) ((PandemicGameState)gs).getComponent(playerDeckHash);
        boolean canDraw = cityDeck.getSize() > 0;

        // if player cannot draw it means that the deck is empty -> GAME OVER
        if (!canDraw){
//            System.out.println("No more cards to draw");
            return GAME_LOSE;
        }
        return GAME_ONGOING;
    }
}
