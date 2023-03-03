package games.loveletter.actions.deep;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.loveletter.LoveLetterGameState;
import games.loveletter.actions.PrinceAction;
import games.loveletter.cards.LoveLetterCard;

import java.util.List;

/**
 * The targeted player discards its current and draws a new one.
 * In case the discarded card is a princess, the targeted player is removed from the game.
 */
public class DeepPrinceAction extends PlayCardDeep {

    public DeepPrinceAction(int playerId) {
        super(LoveLetterCard.CardType.Prince, playerId);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        return PrinceAction.generateActions((LoveLetterGameState) state, playerID, false);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DeepPrinceAction && super.equals(obj);
    }

    @Override
    public DeepPrinceAction copy() {
        DeepPrinceAction pa = new DeepPrinceAction(playerID);
        copyTo(pa);
        return pa;
    }

}
