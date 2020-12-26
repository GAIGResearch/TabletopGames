package games.dominion.actions;

import core.actions.*;
import games.dominion.*;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

import java.util.*;

import static games.dominion.DominionConstants.*;
import static java.util.stream.Collectors.*;

public class Bandit extends DominionAttackAction {

    DominionCard[] topTwo = new DominionCard[2];
    boolean cardTrashed = false;

    public Bandit(int playerId) {
        super(CardType.BANDIT, playerId);
    }

    @Override
    boolean _execute(DominionGameState state) {
        // first gain a gold
        (new GainCard(CardType.GOLD, player)).execute(state);
        // the rest is an attack, with decisions made by the victims
        return true;
    }


    @Override
    public void executeAttack(int victim, DominionGameState state) {
        // put top two cards of deck into discard (and record what they are)
        // later we will trash them directly from the discard
        for (int i = 0; i < 2; i++) {
            topTwo[i] = state.getDeck(DeckType.DRAW, victim).peek();
            state.drawCard(player, DeckType.DRAW, player, DeckType.DISCARD);
        }
        cardTrashed = false;
    }

    @Override
    public boolean isAttackComplete(int currentTarget, DominionGameState state) {
        // we are done if there are no non-Copper treasure cards in the topTwo
        if (Arrays.stream(topTwo).noneMatch(c -> c.isTreasureCard() && c.cardType() != CardType.COPPER))
            return true;
        // otherwise we are completed only once we have trashed a card
        return cardTrashed;
    }

    @Override
    public List<AbstractAction> followOnActions(DominionGameState state) {
        return Arrays.stream(topTwo)
                .filter(DominionCard::isTreasureCard)
                .filter(c -> c.cardType() != CardType.COPPER)
                .map(c -> new TrashCard(c.cardType(), currentTarget))
                .distinct()
                .collect(toList());
    }

    @Override
    public int getCurrentPlayer(DominionGameState state) {
        // decisions made by victims
        return super.currentTarget;
    }

    @Override
    public void registerActionTaken(DominionGameState state, AbstractAction action) {
        if (action instanceof TrashCard && ((TrashCard) action).player == currentTarget)
            cardTrashed = true;
    }

    @Override
    public Bandit _copy() {
        Bandit retValue = new Bandit(player);
        retValue.topTwo[0] = topTwo[0];
        retValue.topTwo[1] = topTwo[1];
        retValue.cardTrashed = cardTrashed;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Bandit) {
            Bandit other = (Bandit) obj;
            return other.cardTrashed == cardTrashed &&
                    other.topTwo[0].equals(topTwo[0]) &&
                    other.topTwo[1].equals(topTwo[1]) &&
                    super.equals(other);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 13 * Objects.hash(topTwo[0], topTwo[1], cardTrashed);
    }
}
