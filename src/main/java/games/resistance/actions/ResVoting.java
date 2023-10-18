package games.resistance.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.resistance.ResGameState;
import games.resistance.components.ResPlayerCards;

import java.util.Objects;

public class ResVoting extends AbstractAction {
    public final int playerId;
    public final ResPlayerCards.CardType cardType;

    public ResVoting(int playerId, ResPlayerCards.CardType cardType) {
        this.playerId = playerId;
        this.cardType = cardType;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ((ResGameState) gs).addVoteChoice(this, gs.getCurrentPlayer());
        return true;
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
    public String toString() {
        return "Player " + playerId + " voted " + cardType;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String getString(AbstractGameState gameState, int perspective) {
        if (perspective == playerId)
            return toString();
        return "Player " + playerId + " votes";
    }

}


