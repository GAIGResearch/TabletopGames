package games.loveletter.actions.deep;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.CardType;

import java.util.List;

/**
 * The King lets two players swap their hand cards.
 */
public class DeepKingAction extends PlayCardDeep {

    public DeepKingAction(int cardIdx, int playerID) {
        super(CardType.King, cardIdx, playerID);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DeepKingAction && super.equals(obj);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        assert cardType != null;
        return cardType.flatActions((LoveLetterGameState) state, cardIdx, playerID, false);
    }

    @Override
    public DeepKingAction copy() {
        DeepKingAction copy = new DeepKingAction(cardIdx, playerID);
        copyTo(copy);
        return copy;
    }

}
