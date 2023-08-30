package games.resistance.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import core.interfaces.IExtendedSequence;
import games.resistance.ResGameState;
import games.resistance.components.ResPlayerCards;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class ResVoting extends AbstractAction implements IExtendedSequence {
    public final int playerId;
    public final ResPlayerCards.CardType cardType;

    public ResVoting(int playerId, ResPlayerCards.CardType cardType) {
        this.playerId = playerId;
        this.cardType = cardType;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ((ResGameState)gs).addCardChoice(this, gs.getCurrentPlayer());
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        List<AbstractAction> actions = new ArrayList<>();
        actions.add(new ResVoting(state.getCurrentPlayer(), ResPlayerCards.CardType.Yes));
        actions.add(new ResVoting(state.getCurrentPlayer(), ResPlayerCards.CardType.No));

        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerId;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {

    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return false;
    }

    @Override
    public ResVoting copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResVoting)) return false;
        ResVoting that = (ResVoting) o;
        return playerId == that.playerId && cardType == that.cardType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, cardType);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return cardType + ".";
    }
}


