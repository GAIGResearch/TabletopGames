package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.dominion.*;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

import java.util.*;

import static games.dominion.DominionConstants.*;
import static java.util.stream.Collectors.*;

public class Bureaucrat extends DominionAttackAction {

    public Bureaucrat(int playerId) {
        super(CardType.BUREAUCRAT, playerId);
    }

    public Bureaucrat(int playerId, boolean dummy) {
        super(CardType.BUREAUCRAT, playerId, dummy);
    }

    boolean victimHasResponded = false;

    @Override
    boolean _execute(DominionGameState state) {
        // first we check to see if there is any silver
        if (state.getCardsIncludedInGame().get(CardType.SILVER) > 0) {
            // first gain a silver onto drawpile
            (new GainCard(CardType.SILVER, player, DeckType.DRAW)).execute(state);
            // and now everyone knows this
            PartialObservableDeck<DominionCard> drawDeck = (PartialObservableDeck<DominionCard>) state.getDeck(DeckType.DRAW, player);
            for (int i = 0; i < state.getNPlayers(); i++) {
                drawDeck.setVisibilityOfComponent(0, i, true);
            }
        }
        // the rest is an attack, with decisions made by the victims
        initiateAttack(state);
        return true;
    }

    @Override
    public void executeAttack(DominionGameState state) {
        // nothing happens immediately...the victim now has a choice to make
        victimHasResponded = false;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if ((action instanceof MoveCard && ((MoveCard) action).playerFrom == currentTarget) ||
                (action instanceof RevealHand && ((RevealHand) action).player == currentTarget)) {
            victimHasResponded = true;
        }
    }

    @Override
    public boolean isAttackComplete(int currentTarget, DominionGameState state) {
        return victimHasResponded;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        DominionGameState state = (DominionGameState) gs;
        if (state.getDeck(DeckType.HAND, currentTarget).stream().noneMatch(DominionCard::isVictoryCard)) {
            // we have no victory cards in hand
            return Collections.singletonList(new RevealHand(currentTarget));
        }
        // we do have at least one victory card, so provide options as to which to move
        return state.getDeck(DeckType.HAND, currentTarget).stream()
                .filter(DominionCard::isVictoryCard)
                .map(c -> new MoveCard(c.cardType(), currentTarget, DeckType.HAND, currentTarget, DeckType.DRAW, true))
                .distinct()
                .collect(toList());
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return currentTarget;
    }

    /**
     * Delegates copying of the state of the subclass.
     * The returned value will then be updated with the copied state of DominionAttackAction (in copy())
     *
     * @return Instance of the sub-class with all local state copied
     */
    @Override
    public Bureaucrat _copy() {
        Bureaucrat retValue = new Bureaucrat(player, dummyAction);
        retValue.victimHasResponded = victimHasResponded;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Bureaucrat) {
            Bureaucrat other = (Bureaucrat) obj;
            return victimHasResponded == other.victimHasResponded && super.equals(other);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 67 * Objects.hash(victimHasResponded);
    }
}
