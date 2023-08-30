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

public class ResMissionVoting extends AbstractAction implements IExtendedSequence {
    public final int playerId;
    public final ResPlayerCards.CardType cardType;

    public ResMissionVoting(int playerId, ResPlayerCards.CardType cardType) {
        this.playerId = playerId;
        this.cardType = cardType;
    }

    public ResMissionVoting getHiddenChoice( int i) {
        Random rnd = new Random();
        if (rnd.nextInt(2) == 0){return new ResMissionVoting(i, ResPlayerCards.CardType.Yes);}
        else {return new ResMissionVoting(i, ResPlayerCards.CardType.No);}

    }

    @Override
    public boolean execute(AbstractGameState gs) {

        ((ResGameState)gs).addMissionChoice(this, gs.getCurrentPlayer());
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {

        ResGameState resgs = (ResGameState) state;
        List<AbstractAction> actions = new ArrayList<>();
        if(resgs.getFinalTeam().contains(playerId)){

            actions.add(new ResMissionVoting(playerId, ResPlayerCards.CardType.Yes));
            actions.add(new ResMissionVoting(playerId, ResPlayerCards.CardType.No));
        }
        else {
            actions.add(new ResWait(playerId));
        }

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
