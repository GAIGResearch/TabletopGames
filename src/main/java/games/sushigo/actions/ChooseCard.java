package games.sushigo.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Deck;
import core.interfaces.IExtendedSequence;
import games.sushigo.SGGameState;
import games.sushigo.cards.SGCard;

import java.util.*;

public class ChooseCard extends AbstractAction implements IExtendedSequence {
    public final int playerId;
    public final SGCard.SGCardType cardType;
    public final int cardIdx;
    public final boolean useChopsticks;

    boolean chopstickChooseDone;

    public ChooseCard(int playerId, int idx, SGCard.SGCardType type, boolean useChopsticks) {
        this.playerId = playerId;
        this.cardIdx = idx;
        this.cardType = type;
        this.useChopsticks = useChopsticks;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ((SGGameState) gs).addCardChoice(this, gs.getCurrentPlayer());
        if (useChopsticks) {
            gs.setActionInProgress(this);
        }
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        // Chopsticks allowing to pick second card, different from that already selected
        SGGameState sggs = (SGGameState) state;
        int idxSelected = sggs.getCardChoices().get(playerId).get(0).cardIdx;
        Set<AbstractAction> actions = new HashSet<>();

        Deck<SGCard> currentPlayerHand = sggs.getPlayerHands().get(playerId);
        for (int i = 0; i < currentPlayerHand.getSize(); i++) {
            // All players can do is choose a card in hand to play. Cannot chain chopsticks, only 1 per turn can be used.
            // So all of these actions can only be 'useChopsticks = false'
            if (idxSelected != i) {
                actions.add(new ChooseCard(playerId, i, currentPlayerHand.get(i).type, false));
            }
        }
        if (actions.isEmpty())
            throw new AssertionError("No actions");
        return new ArrayList<>(actions);
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerId;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        chopstickChooseDone = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return chopstickChooseDone;
    }

    @Override
    public ChooseCard copy() {
        if (useChopsticks) {
            ChooseCard retValue = new ChooseCard(playerId, cardIdx, cardType, useChopsticks);
            retValue.chopstickChooseDone = chopstickChooseDone;
            return retValue;
        }
        return this; // immutable if not using chopsticks
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChooseCard)) return false;
        ChooseCard that = (ChooseCard) o;
        if (useChopsticks) {
            return playerId == that.playerId && chopstickChooseDone == that.chopstickChooseDone && cardType == that.cardType && cardIdx == that.cardIdx;
        } else {
            // ignore card idx
            return playerId == that.playerId && cardType == that.cardType;
        }
    }

    @Override
    public int hashCode() {
        if (useChopsticks) {
            return Objects.hash(playerId, cardIdx, cardType, useChopsticks, chopstickChooseDone);
        } else {
            return Objects.hash(playerId, cardType);
        }
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Choose card " + cardType + " [" + cardIdx + "] " + (useChopsticks ? " (+chopsticks)" : "");
    }

    public Card getCard(AbstractGameState gs) {
        SGGameState sggs = (SGGameState) gs;
        return sggs.getPlayerHands().get(playerId).get(cardIdx);
    }

    @Override
    public String toString() {
        return cardType + " (" + playerId + ")" + (useChopsticks ? " !!" : "");  // Compact version
//        return "ChooseCard{" +
//                "playerId=" + playerId +
//                "type=" + cardType +
//                ", cardIdx=" + cardIdx +
//                ", useChopsticks=" + useChopsticks +
//                ", chopstickChooseDone=" + chopstickChooseDone +
//                '}';
    }
}
