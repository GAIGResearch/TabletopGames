package games.dominion.actions;

import java.util.ArrayList;
import java.util.List;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IExtendedSequence;
import games.dominion.DominionGameState;
import games.dominion.DominionConstants.DeckType;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

/**
 * Action to play the Vassal card. This card allows the player to draw the top card of their deck, and if it is an action card, they can choose to play it.
 */
public class Vassal extends DominionAction implements IExtendedSequence {

    boolean executed = false;

    public Vassal(int playerId) {
        super(CardType.VASSAL, playerId);
    }

    public Vassal(int playerId, boolean dummy) {
        super(CardType.VASSAL, playerId, dummy);
    }

    @Override
    boolean _execute(DominionGameState state) {
        state.setActionInProgress(this);
        return true;
    }

    @Override
    public Vassal copy() {
        Vassal retValue = new Vassal(player, dummyAction);
        retValue.executed = executed;
        return retValue;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        DominionGameState dgs = (DominionGameState) state;

        List<AbstractAction> actions = new ArrayList<>();
        actions.add(new DoNothing());

        if (dgs.getDeck(DeckType.DRAW, player).getSize() == 0) {
            return actions;
        }

        // Draw the top card of the deck, and if action card, it can be played
        // This card is discarded afterwards
        DominionCard card = dgs.getDeck(DeckType.DRAW, player).draw();
        if (card.isActionCard()) {
            DominionAction action = card.getAction(player);
            action.dummyAction = true;
            actions.add(action);
        }

        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        executed = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }
    
}
