package games.loveletter.actions.deep;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.CardType;

import java.util.List;

/**
 * The Priest allows a player to see another player's hand cards.
 * This has no effect in case the game is fully observable.
 */
public class DeepPriestAction extends PlayCardDeep {

    public DeepPriestAction(int cardIdx, int playerID) {
        super(CardType.Priest, cardIdx, playerID);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DeepPriestAction && super.equals(obj);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        assert cardType != null;
        return cardType.flatActions((LoveLetterGameState) state, cardIdx, playerID, false);
    }

    @Override
    public DeepPriestAction copy() {
        DeepPriestAction copy = new DeepPriestAction(cardIdx, playerID);
        copyTo(copy);
        return copy;
    }
}
