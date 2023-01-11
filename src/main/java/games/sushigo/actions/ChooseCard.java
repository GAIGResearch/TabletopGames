package games.sushigo.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IExtendedSequence;
import games.sushigo.SGGameState;
import games.sushigo.cards.SGCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    public ChooseCard getHiddenChoice() {
        return new ChooseCard(playerId, -1, useChopsticks);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ((SGGameState)gs).addCardChoice(this, gs.getCurrentPlayer());
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
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerId;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        chopstickChooseDone = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return chopstickChooseDone;
    }

    @Override
    public ChooseCard copy() {
        return this; // immutable
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
        return "Choose card " + cardIdx + (useChopsticks? " (+chopsticks)" : "");
    }
}
