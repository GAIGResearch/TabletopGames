package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IExtendedSequence;
import games.dominion.DominionConstants;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

import java.util.*;

import static java.util.stream.Collectors.toList;

public class AttackReaction implements IExtendedSequence {

    int attacker;
    int defender;
    List<CardType> cardsToPlay; // we need to store either the cardType or the card component id for copying

    private AttackReaction(int attacker, int defender, List<CardType> cardsToPlay) {
        this.attacker = attacker;
        this.defender = defender;
        this.cardsToPlay = new ArrayList<>(cardsToPlay);
    }

    public AttackReaction(DominionGameState state, int attacker, int defender) {
        this.attacker = attacker;
        this.defender = defender;
        // we record what cards are in hand at the point of attack occurring
        cardsToPlay = state.getDeck(DominionConstants.DeckType.HAND, defender).stream()
                .filter(DominionCard::hasAttackReaction)
                .map(DominionCard::cardType)
                .collect(toList());
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        List<AbstractAction> reactions = cardsToPlay.stream().distinct()
                .map(c -> DominionCard.create(c).getAttackReaction(defender))
                .collect(toList());
        reactions.add(new DoNothing());
        return reactions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return defender;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof DoNothing)
            cardsToPlay.clear();
        if (action instanceof IDominionReaction actionTaken) {
            if (actionTaken.getPlayer() == defender) {
                cardsToPlay.remove(actionTaken.getCardType());
            }
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return cardsToPlay.isEmpty();
    }

    @Override
    public AttackReaction copy() {
        return new AttackReaction(attacker, defender, cardsToPlay);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof AttackReaction) {
            AttackReaction ar = (AttackReaction) other;
            return ar.defender == defender && ar.attacker == attacker && ar.cardsToPlay.equals(cardsToPlay);
        }
        return false;
    }

    @Override
    public int hashCode(){
        return Objects.hash(attacker, defender, cardsToPlay);
    }
}
