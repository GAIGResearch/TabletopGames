package games.dominion.actions;

import core.actions.AbstractAction;
import games.dominion.*;
import games.dominion.DominionConstants.*;
import games.dominion.cards.*;

import java.util.*;

import static java.util.stream.Collectors.*;

public class Militia extends DominionAttackAction {

    public Militia(int playerId) {
        super(CardType.MILITIA, playerId);
    }

    @Override
    boolean _execute(DominionGameState state) {
        state.changeAdditionalSpend(2); // player gets +2 to spend as direct effects of card
        return true;
    }

    @Override
    public List<AbstractAction> followOnActions(DominionGameState state) {
        // we can discard any card in hand, so create a DiscardCard action for each
        if (isAttackComplete(currentTarget, state))
            throw new AssertionError("Should not be here - there are no actions to be taken");
        Set<DominionCard> uniqueCardsInHand = state.getDeck(DeckType.HAND, currentTarget).stream().collect(toSet());
        return uniqueCardsInHand.stream()
                .map(card -> new DiscardCard(card.cardType(), currentTarget))
                .distinct()
                .collect(toList());
    }

    @Override
    public int getCurrentPlayer(DominionGameState state) {
        return currentTarget;
    }

    @Override
    public void registerActionTaken(DominionGameState state, AbstractAction action) {
        // Do nothing
    }

    /**
     * Delegates copying of the state of the subclass.
     * The returned value will then be updated with the copied state of DominionAttackAction (in copy())
     *
     * @return Instance of the sub-class with all local state copied
     */
    @Override
    public DominionAttackAction _copy() {
        return new Militia(player);
    }

    @Override
    public void executeAttack(int victim, DominionGameState state) {
        // Does nothing directly
    }

    @Override
    public boolean isAttackComplete(int currentTarget, DominionGameState state) {
        // Does the victim now have 3 or fewer cards in hand?
        return state.getDeck(DeckType.HAND, currentTarget).getSize() < 4;
    }
}
