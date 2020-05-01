package games.pandemic.engine.gameOver;

import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;
import core.components.IDeck;
import games.pandemic.Constants;
import games.pandemic.PandemicGameState;

import static games.pandemic.Constants.GameResult.GAME_LOSE;
import static games.pandemic.Constants.GameResult.GAME_ONGOING;
import static games.pandemic.Constants.playerDeckHash;

@SuppressWarnings("unchecked")
public class GameOverDrawCards extends GameOverCondition {
    @Override
    public Constants.GameResult test(AbstractGameState gs) {
        Deck<Card> cityDeck = (Deck<Card>) ((PandemicGameState)gs).getComponent(playerDeckHash);
        boolean canDraw = cityDeck.getCards().size() > 0;

        // if player cannot draw it means that the deck is empty -> GAME OVER
        if (!canDraw){
            System.out.println("No more cards to draw");
            return GAME_LOSE;
        }
        return GAME_ONGOING;
    }
}
