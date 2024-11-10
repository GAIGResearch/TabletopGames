package games.loveletter.actions.deep;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.CardType;

import java.util.List;

/**
 * The targeted player discards its current and draws a new one.
 * In case the discarded card is a princess, the targeted player is removed from the game.
 */
public class DeepPrinceAction extends PlayCardDeep {

    public DeepPrinceAction(int cardIdx, int playerId) {
        super(CardType.Prince, cardIdx, playerId);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        assert cardType != null;
        return cardType.flatActions((LoveLetterGameState) state, cardIdx, playerID, false);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DeepPrinceAction && super.equals(obj);
    }

    @Override
    public DeepPrinceAction copy() {
        DeepPrinceAction pa = new DeepPrinceAction(cardIdx, playerID);
        copyTo(pa);
        return pa;
    }

}
