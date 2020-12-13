package games.dominion.actions;

import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

import java.util.*;

import static games.dominion.DominionConstants.*;
import static java.util.stream.Collectors.*;

public class Harbinger extends DominionAction implements IExtendedSequence {

    private boolean executed;

    public Harbinger(int playerId) {
        super(CardType.HARBINGER, playerId);
    }

    @Override
    boolean _execute(DominionGameState state) {
        state.drawCard(player);
        state.changeActions(1);
        if (state.getDeck(DeckType.DISCARD, player).getSize() > 0)
            state.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> followOnActions(DominionGameState state) {
        List<CardType> discardTypes = state.getDeck(DeckType.DISCARD, player).stream()
                .map(DominionCard::cardType).distinct().collect(toList());
        List<AbstractAction> retValue = discardTypes.stream().map(ct -> new MoveCard(ct, player, DeckType.DISCARD, player, DeckType.DRAW)).collect(toList());
        retValue.add(new DoNothing());
        return retValue;
    }

    @Override
    public int getCurrentPlayer(DominionGameState state) {
        return player;
    }

    @Override
    public void registerActionTaken(DominionGameState state, AbstractAction action) {
        if (action instanceof DoNothing || (action instanceof MoveCard && ((MoveCard) action).playerFrom == player)) {
            executed = true;
        }
    }

    @Override
    public boolean executionComplete(DominionGameState state) {
        return executed;
    }

    /**
     * Create a copy of this action, with all of its variables.
     * NO REFERENCES TO COMPONENTS TO BE KEPT IN ACTIONS, PRIMITIVE TYPES ONLY.
     *
     * @return - new AbstractAction object with the same properties.
     */
    @Override
    public Harbinger copy() {
        Harbinger retValue = new Harbinger(player);
        retValue.executed = executed;
        return retValue;
    }

}
