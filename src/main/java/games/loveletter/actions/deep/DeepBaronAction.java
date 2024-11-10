package games.loveletter.actions.deep;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.CardType;

import java.util.List;

/**
 * The Baron lets two players compare their hand card. The player with the lesser valued card is removed from the game.
 */
public class DeepBaronAction extends PlayCardDeep {

    public DeepBaronAction(int cardIdx, int playerID) {
        super(CardType.Baron, cardIdx, playerID);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DeepBaronAction && super.equals(obj);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        assert cardType != null;
        return cardType.flatActions((LoveLetterGameState) state, cardIdx, playerID, false);
    }

    @Override
    public DeepBaronAction copy() {
        DeepBaronAction copy = new DeepBaronAction(cardIdx, playerID);
        copyTo(copy);
        return copy;
    }
}
