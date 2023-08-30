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

public class ResMissionVoting extends AbstractAction{
    public final int playerId;
    public final ResPlayerCards.CardType cardType;

    public ResMissionVoting(int playerId, ResPlayerCards.CardType cardType) {
        this.playerId = playerId;
        this.cardType = cardType;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ((ResGameState) gs).addMissionChoice(this, gs.getCurrentPlayer());
        return true;
    }

    @Override
    public ResMissionVoting copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResMissionVoting)) return false;
        ResMissionVoting that = (ResMissionVoting) o;
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
