package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;

import java.util.*;

import static games.dominion.DominionConstants.*;

public class Witch extends DominionAttackAction {

    public Witch( int playerId) {
        super(CardType.WITCH, playerId);
    }
    public Witch(int playerId, boolean dummy) {
        super(CardType.WITCH, playerId, dummy);
    }

    @Override
    boolean _execute(DominionGameState state) {
        initiateAttack(state);
        return true;
    }

    /**
     * Delegates copying of the state of the subclass.
     * The returned value will then be updated with the copied state of DominionAttackAction (in copy())
     *
     * @return Instance of the sub-class with all local state copied
     */
    @Override
    public DominionAttackAction _copy() {
        return new Witch(player, dummyAction);
    }

    @Override
    public void executeAttack(DominionGameState state) {
        if (state.cardsOfType(CardType.CURSE, -1, DeckType.SUPPLY) > 0) {
            state.removeCardFromTable(CardType.CURSE);
            state.addCard(CardType.CURSE, currentTarget, DeckType.DISCARD);
        }
    }

    @Override
    public boolean isAttackComplete(int currentTarget, DominionGameState state) {
        return true; // there are no ongoing actions for Witch
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        return Collections.emptyList();
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        // nothing to do
    }
}
