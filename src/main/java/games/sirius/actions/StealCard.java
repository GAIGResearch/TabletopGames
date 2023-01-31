package games.sirius.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.sirius.*;

import java.util.*;
import java.util.stream.Collectors;

public class StealCard extends AbstractAction {

    private static final SiriusCard dummyCard = new SiriusCard("Dummy", SiriusConstants.SiriusCardType.AMMONIA, 86);

    final int targetPlayer;
    final SiriusCard targetCard;

    public StealCard(SiriusCard cardToSteal, int fromPlayer) {
        if (cardToSteal == null)
            targetCard = dummyCard;
        else
            targetCard = cardToSteal; // SiriusCards are immutable
        targetPlayer = fromPlayer;
        if (targetCard.cardType == SiriusConstants.SiriusCardType.FAVOUR)
            throw new IllegalArgumentException("Not able to steal Favour cards");
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SiriusGameState state = (SiriusGameState) gs;
        int thief = state.getCurrentPlayer();
        if (thief == targetPlayer)
            throw new AssertionError("Should not be able to steal from oneself");
        if (!targetCard.equals(dummyCard)) {
            Deck<SiriusCard> hand = state.getPlayerHand(targetPlayer);
            boolean found = hand.remove(targetCard);
            if (!found)
                throw new AssertionError("Card not found in hand when trying to steal : " + targetCard + " in " + hand);
            state.addCardToHand(thief, targetCard);
            // now we remove all the cards of the same type
            Map<Boolean, List<SiriusCard>> partitionedHand = hand.stream().collect(Collectors.partitioningBy(c -> c.cardType == targetCard.cardType));
            hand.removeAll(partitionedHand.get(true));

            // and add them to the relevant discard pile
            state.addToDeck(targetCard.cardType, true, partitionedHand.get(true));
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StealCard) {
            StealCard other = (StealCard) obj;
            return other.targetPlayer == targetPlayer &&
                    targetCard.cardType == other.targetCard.cardType &&
                    targetCard.value == other.targetCard.value;
            // deliberately exclude the full Card comparison to avoid componentID
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetPlayer, targetCard.cardType, targetCard.value) + 283;
    }

    @Override
    public String toString() {
        if (targetCard == dummyCard)
            return "No Card to Steal from " + targetPlayer;
        return String.format("Steal card %s from player %d", targetCard, targetPlayer);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
