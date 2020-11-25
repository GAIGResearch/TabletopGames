package games.dominion;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import games.dominion.actions.BuyCard;
import games.dominion.cards.CardType;

import java.util.List;
import java.util.Random;

public class PlayActionCards extends BigMoney {

    Random rnd;

    public PlayActionCards(Random random) {
        rnd = random;
    }

    /**
     * Generate a valid action to play in the game. Valid actions can be found by accessing
     * AbstractGameState.getActions()
     *
     * @param gameState observation of the current game state
     */
    @Override
    public AbstractAction getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        DominionGameState state = (DominionGameState) gameState;
        if (state.getGamePhase() == DominionGameState.DominionGamePhase.Play) {
            return actions.get(rnd.nextInt(actions.size()));
        } else {
            return super.getAction(gameState, actions);
        }
    }

    @Override
    public String toString() {
        return "PlayActionCards";
    }
}
