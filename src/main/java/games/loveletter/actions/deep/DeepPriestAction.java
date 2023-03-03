package games.loveletter.actions.deep;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.loveletter.LoveLetterGameState;
import games.loveletter.actions.PriestAction;
import games.loveletter.cards.LoveLetterCard;

import java.util.List;

/**
 * The Priest allows a player to see another player's hand cards.
 * This has no effect in case the game is fully observable.
 */
public class DeepPriestAction extends PlayCardDeep {

    public DeepPriestAction(int playerID) {
        super(LoveLetterCard.CardType.Priest, playerID);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DeepPriestAction && super.equals(obj);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        return PriestAction.generateActions((LoveLetterGameState) state, playerID, false);
    }

    @Override
    public DeepPriestAction copy() {
        DeepPriestAction copy = new DeepPriestAction(playerID);
        copyTo(copy);
        return copy;
    }
}
