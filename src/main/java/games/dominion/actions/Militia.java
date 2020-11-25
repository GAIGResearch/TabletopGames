package games.dominion.actions;

import core.actions.AbstractAction;
import games.dominion.*;
import games.dominion.cards.*;

import java.util.*;

import static java.util.stream.Collectors.*;

public class Militia extends ExtendedDominionAction {

    public Militia(int playerId) {
        super(CardType.MILITIA, playerId);
    }

    int nextPlayerToDiscard = -1;
    boolean executed = false;

    @Override
    boolean _execute(DominionGameState state) {
        state.spend(-2); // player gets +2 to spend
        nextPlayerToDiscard = (player + 1) % state.getNPlayers();
        nextPlayerToDiscard = nextPlayerToDiscard(state);
        return true;
    }

    private int nextPlayerToDiscard(DominionGameState state) {
        int prospectiveDiscardPlayer = nextPlayerToDiscard;
        do {
            if (state.getDeck(DominionConstants.DeckType.HAND, prospectiveDiscardPlayer).getSize() > 3) {
                return prospectiveDiscardPlayer;
            }
            prospectiveDiscardPlayer = (prospectiveDiscardPlayer + 1) % state.getNPlayers();
        } while (prospectiveDiscardPlayer != player);
        executed = true;
        return -1;
    }

    @Override
    public List<AbstractAction> followOnActions(DominionGameState state) {
        // we can discard any card in hand, so create a DiscardCard action for each
        Set<DominionCard> uniqueCardsInHand = state.getDeck(DominionConstants.DeckType.HAND, nextPlayerToDiscard).stream().collect(toSet());
        return uniqueCardsInHand.stream()
                .map(card -> new DiscardCard(card.cardType(), nextPlayerToDiscard))
                .collect(toList());
    }

    @Override
    public int getCurrentPlayer(DominionGameState state) {
        return nextPlayerToDiscard;
    }

    @Override
    public void registerActionTaken(DominionGameState state, AbstractAction action) {
        // the action does not matter here - we just check to see if cards need to be discarded
        // TODO: Once Moat is implemented, this will change
        nextPlayerToDiscard = nextPlayerToDiscard(state);
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
    public AbstractAction copy() {
        Militia retValue = new Militia(player);
        retValue.nextPlayerToDiscard = nextPlayerToDiscard;
        retValue.executed = executed;
        return retValue;
    }
}
