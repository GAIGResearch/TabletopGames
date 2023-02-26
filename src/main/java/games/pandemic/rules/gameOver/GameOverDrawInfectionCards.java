package games.pandemic.rules.gameOver;

import core.AbstractGameState;
import core.CoreConstants;
import core.components.Card;
import core.components.Deck;
import core.rules.GameOverCondition;
import games.pandemic.PandemicGameState;

import static games.pandemic.PandemicConstants.infectionHash;
import static core.CoreConstants.GameResult.GAME_ONGOING;
import static core.CoreConstants.GameResult.LOSE_GAME;

@SuppressWarnings("unchecked")
public class GameOverDrawInfectionCards extends GameOverCondition {
    @Override
    public CoreConstants.GameResult test(AbstractGameState gs) {
        Deck<Card> deck = (Deck<Card>) ((PandemicGameState)gs).getComponent(infectionHash);
        boolean canDraw = deck.getSize() > 0;

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
