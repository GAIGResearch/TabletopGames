package games.dominion.players;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dominion.DominionGameState;

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
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        DominionGameState state = (DominionGameState) gameState;
        if (state.getGamePhase() == DominionGameState.DominionGamePhase.Play) {
            return actions.get(rnd.nextInt(actions.size()));
        } else {
            return super._getAction(gameState, actions);
        }
    }

    @Override
    public String toString() {
        return "PlayActionCards";
    }
}
