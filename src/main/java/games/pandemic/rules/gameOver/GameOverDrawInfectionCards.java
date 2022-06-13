package games.pandemic.rules.gameOver;

import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;
import core.rules.GameOverCondition;
import games.pandemic.PandemicGameState;
import utilities.Utils;

import static games.pandemic.PandemicConstants.infectionHash;
import static games.pandemic.PandemicConstants.playerDeckHash;
import static utilities.Utils.GameResult.GAME_ONGOING;
import static utilities.Utils.GameResult.LOSE;

@SuppressWarnings("unchecked")
public class GameOverDrawInfectionCards extends GameOverCondition {
    @Override
    public Utils.GameResult test(AbstractGameState gs) {
        Deck<Card> deck = (Deck<Card>) ((PandemicGameState)gs).getComponent(infectionHash);
        boolean canDraw = deck.getSize() > 0;

        // if the deck is empty -> GAME OVER
        if (!canDraw){
            if (gs.getCoreGameParameters().verbose) {
                System.out.println("No more cards to draw");
            }
            return LOSE;
        }
        return GAME_ONGOING;
    }
}
