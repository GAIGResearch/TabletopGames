package games.dominion.actions;

import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.dominion.DominionConstants;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

import java.util.*;

import static java.util.stream.Collectors.toList;

public class AttackReaction implements IExtendedSequence {

    int attacker;
    int defender;
    boolean executed = false;
    List<CardType> cardsToPlay;

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
    public List<AbstractAction> followOnActions(DominionGameState state) {
        List<AbstractAction> reactions = cardsToPlay.stream().distinct()
                .map(c -> c.getAttackReaction(defender))
                .collect(toList());
        reactions.add(new DoNothing());
        return reactions;
    }

    @Override
    public int getCurrentPlayer(DominionGameState state) {
        return defender;
    }

    @Override
    public void registerActionTaken(DominionGameState state, AbstractAction action) {

    }

    @Override
    public boolean executionComplete() {
        return executed;
    }

    @Override
    public IExtendedSequence copy() {
        return null;
    }
}
