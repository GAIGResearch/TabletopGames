package games.dominion.actions;

import core.actions.AbstractAction;
import games.dominion.*;
import games.dominion.DominionConstants.*;
import games.dominion.cards.*;

import java.util.*;

import static java.util.stream.Collectors.*;

public class Militia extends DominionAction implements IExtendedSequence {

    public Militia(int playerId) {
        super(CardType.MILITIA, playerId);
    }

    int currentTarget;
    boolean executed;

    @Override
    boolean _execute(DominionGameState state) {
        state.spend(-2); // player gets +2 to spend
        state.setActionInProgress(this);
        currentTarget = (player + 1) % state.getNPlayers();
        checkCurrentTarget(state);
        return true;
    }

    private void checkCurrentTarget(DominionGameState state) {
        int prospectiveDiscardPlayer = currentTarget;
        do {
            if (state.getDeck(DominionConstants.DeckType.HAND, prospectiveDiscardPlayer).getSize() > 3) {
                currentTarget = prospectiveDiscardPlayer;
                return;
            }
            prospectiveDiscardPlayer = (prospectiveDiscardPlayer + 1) % state.getNPlayers();
        } while (prospectiveDiscardPlayer != player);
        executed = true;
    }

    @Override
    public List<AbstractAction> followOnActions(DominionGameState state) {
        // we can discard any card in hand, so create a DiscardCard action for each
        Set<DominionCard> uniqueCardsInHand = state.getDeck(DeckType.HAND, currentTarget).stream().collect(toSet());
        return uniqueCardsInHand.stream()
                .map(card -> new DiscardCard(card.cardType(), currentTarget))
                .collect(toList());
    }

    @Override
    public int getCurrentPlayer(DominionGameState state) {
        return currentTarget;
    }

    @Override
    public void registerActionTaken(DominionGameState state, AbstractAction action) {
        // the action does not matter here - we just check to see if cards need to be discarded
        // TODO: Once Moat is implemented, this will change to consider AttackReactions
        checkCurrentTarget(state);
    }

    @Override
    public boolean executionComplete() {
        return executed;
    }

    /**
     * Create a copy of this action, with all of its variables.
     * NO REFERENCES TO COMPONENTS TO BE KEPT IN ACTIONS, PRIMITIVE TYPES ONLY.
     *
     * @return - new AbstractAction object with the same properties.
     */
    @Override
    public Militia copy() {
        Militia retValue = new Militia(player);
        retValue.currentTarget = currentTarget;
        retValue.executed = executed;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Militia) {
            Militia other = (Militia) obj;
            return other.executed == executed && other.player == player && other.currentTarget == currentTarget;
        }
        return false;
    }
}
