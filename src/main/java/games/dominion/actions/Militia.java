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
        if (state.getDeck(DeckType.HAND, currentTarget).getSize() < 4 || state.isDefended(currentTarget))
            throw new AssertionError("Should not be here - there are no actions to be taken");
        Set<DominionCard> uniqueCardsInHand = state.getDeck(DeckType.HAND, currentTarget).stream().collect(toSet());
        return uniqueCardsInHand.stream()
                .map(card -> new DiscardCard(card.cardType(), currentTarget))
                .distinct()
                .collect(toList());
    }

}
