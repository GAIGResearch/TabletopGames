package games.dominion.actions;

import core.AbstractGameState;
import core.actions.*;
import games.dominion.*;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

import java.util.*;

import static games.dominion.DominionConstants.*;
import static java.util.stream.Collectors.*;

public class Bandit extends DominionAttackAction {

    public int CARDS_AFFECTED = 2;

    DominionCard[] topCards = new DominionCard[CARDS_AFFECTED];
    boolean cardTrashed = false;

    public Bandit(int playerId) {
        super(CardType.BANDIT, playerId);
    }
    public Bandit(int playerId, boolean dummy) {
        super(CardType.BANDIT, playerId, dummy);
    }

    @Override
    boolean _execute(DominionGameState state) {
        // first gain a gold
        (new GainCard(CardType.GOLD, player)).execute(state);
        // the rest is an attack, with decisions made by the victims
        initiateAttack(state);
        return true;
    }


    @Override
    public void executeAttack(DominionGameState state) {
        // put top two cards of deck into discard (and record what they are)
        // later we will trash them directly from the discard

        // We move to TABLE temporarily in case we shuffle the DISCARD into the DRAW
        for (int i = 0; i < CARDS_AFFECTED; i++) {
            state.drawCard(currentTarget, DeckType.DRAW, currentTarget, DeckType.TABLE);
            topCards[i] = state.getDeck(DeckType.TABLE, currentTarget).peek();
        }
        for (int i = 0; i < CARDS_AFFECTED; i++) {
            state.drawCard(currentTarget, DeckType.TABLE, currentTarget, DeckType.DISCARD);
        }
        cardTrashed = false;
    }

    @Override
    public boolean isAttackComplete(int currentTarget, DominionGameState state) {
        // we are done if there are no non-Copper treasure cards in the topTwo
        if (Arrays.stream(topCards).noneMatch(c -> c != null && c.isTreasureCard() && c.cardType() != CardType.COPPER))
            return true;
        // otherwise we are completed only once we have trashed a card
        return cardTrashed;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        return Arrays.stream(topCards)
                .filter(Objects::nonNull)
                .filter(DominionCard::isTreasureCard)
                .filter(c -> c.cardType() != CardType.COPPER)
                .map(c -> new TrashCard(c.cardType(), currentTarget, DeckType.DISCARD))
                .distinct()
                .collect(toList());
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        // decisions made by victims
        return super.currentTarget;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof TrashCard && ((TrashCard) action).player == currentTarget)
            cardTrashed = true;
    }

    @Override
    public Bandit _copy() {
        Bandit retValue = new Bandit(player, dummyAction);
        System.arraycopy(topCards, 0, retValue.topCards, 0, CARDS_AFFECTED);
        retValue.cardTrashed = cardTrashed;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Bandit) {
            Bandit other = (Bandit) obj;
            return other.cardTrashed == cardTrashed &&
                    Arrays.equals(topCards, other.topCards) &&
                    super.equals(other);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + (cardTrashed ? 13 : 0) + 313 * Arrays.hashCode(topCards);
    }
}
