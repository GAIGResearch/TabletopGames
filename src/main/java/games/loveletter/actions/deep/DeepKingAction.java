package games.loveletter.actions.deep;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

import java.util.List;

/**
 * The King lets two players swap their hand cards.
 */
public class DeepKingAction extends PlayCardDeep {

    public DeepKingAction(int playerID) {
        super(LoveLetterCard.CardType.King, playerID);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DeepKingAction && super.equals(obj);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        return cardType.getFlatActions((LoveLetterGameState) state, playerID, false);
    }

    @Override
    public DeepKingAction copy() {
        DeepKingAction copy = new DeepKingAction(playerID);
        copyTo(copy);
        return copy;
    }

}
