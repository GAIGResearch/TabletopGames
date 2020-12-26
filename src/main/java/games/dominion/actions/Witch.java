package games.dominion.actions;

import core.actions.AbstractAction;
import games.dominion.DominionConstants;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;

import java.util.*;

import static games.dominion.DominionConstants.*;

public class Witch extends DominionAttackAction {

    public Witch( int playerId) {
        super(CardType.WITCH, playerId);
    }

    @Override
    boolean _execute(DominionGameState state) {
        state.drawCard(player);
        state.drawCard(player);
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
        return new Witch(player);
    }

    @Override
    public void executeAttack(int victim, DominionGameState state) {
        if (state.cardsOfType(CardType.CURSE, -1, DeckType.SUPPLY) > 0) {
            state.removeCardFromTable(CardType.CURSE);
            state.addCard(CardType.CURSE, victim, DeckType.DISCARD);
        }
    }

    @Override
    public boolean isAttackComplete(int currentTarget, DominionGameState state) {
        return true; // there are no ongoing actions for Witch
    }

    @Override
    public List<AbstractAction> followOnActions(DominionGameState state) {
        return Collections.emptyList();
    }

    @Override
    public int getCurrentPlayer(DominionGameState state) {
        return player;
    }

    @Override
    public void registerActionTaken(DominionGameState state, AbstractAction action) {
        // nothing to do
    }
}
