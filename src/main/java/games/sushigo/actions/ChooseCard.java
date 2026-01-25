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
    public final int cardIdx;
    public final boolean useChopsticks;

    boolean chopstickChooseDone;

    public ChooseCard(int playerId, int cardIdx, boolean useChopsticks) {
        this.playerId = playerId;
        this.cardIdx = cardIdx;
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
        List<AbstractAction> actions = new ArrayList<>();

        Deck<SGCard> currentPlayerHand = sggs.getPlayerHands().get(playerId);
        for (int i = 0; i < currentPlayerHand.getSize(); i++) {
            // All players can do is choose a card in hand to play. Cannot chain chopsticks, only 1 per turn can be used.
            // So all of these actions can only be 'useChopsticks = false'
            if (idxSelected != i) {
                actions.add(new ChooseCard(playerId, i, false));
            }
        }
        if (actions.isEmpty())
            throw new AssertionError("No actions");
        return actions;
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
            ChooseCard retValue = new ChooseCard(playerId, cardIdx, useChopsticks);
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
        return playerId == that.playerId && useChopsticks == that.useChopsticks && chopstickChooseDone == that.chopstickChooseDone && cardIdx == that.cardIdx;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, cardIdx, useChopsticks, chopstickChooseDone);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Choose card " + getCard(gameState).getComponentName() + " [" + cardIdx + "] " + (useChopsticks ? " (+chopsticks)" : "");
    }

    @Override
    public String getString(AbstractGameState gameState, int perspective) {
        // Other players can only see that a card was chosen
        if (perspective == playerId)
            return getString(gameState);
        return "Chooses card";
    }

    public Card getCard(AbstractGameState gs) {
        SGGameState sggs = (SGGameState) gs;
        return sggs.getPlayerHands().get(playerId).get(cardIdx);
    }

    @Override
    public String toString() {
        return String.format("ChooseCard: player %d, cardIdx %d, %b, %b",
                playerId, cardIdx, useChopsticks, chopstickChooseDone);
    }

}
