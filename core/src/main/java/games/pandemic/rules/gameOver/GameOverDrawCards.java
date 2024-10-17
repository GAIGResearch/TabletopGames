package games.pandemic.rules.gameOver;

import core.AbstractGameState;
import core.CoreConstants;
import core.components.Card;
import core.components.Deck;
import core.rules.GameOverCondition;
import games.pandemic.PandemicGameState;

import static games.pandemic.PandemicConstants.playerDeckHash;
import static core.CoreConstants.GameResult.LOSE_GAME;
import static core.CoreConstants.GameResult.GAME_ONGOING;

@SuppressWarnings("unchecked")
public class GameOverDrawCards extends GameOverCondition {
    @Override
    public CoreConstants.GameResult test(AbstractGameState gs) {
        Deck<Card> cityDeck = (Deck<Card>) ((PandemicGameState)gs).getComponent(playerDeckHash);
        boolean canDraw = cityDeck.getSize() > 0;

        // if the deck is empty -> GAME OVER
        if (!canDraw){
            if (gs.getCoreGameParameters().verbose) {
                System.out.println("No more cards to draw");
            }
            return LOSE_GAME;
        }
        return GAME_ONGOING;
    }
}
