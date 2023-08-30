package games.resistance.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.resistance.ResGameState;
import utilities.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ResWait extends AbstractAction implements IExtendedSequence {
    public final int playerId;


    public ResWait(int playerId ) {
        this.playerId = playerId;

    }


    @Override
    public boolean execute(AbstractGameState gs) {

        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {

        ResGameState resgs = (ResGameState) state;

        List<AbstractAction> actions = new ArrayList<>();


        actions.add(new ResWait(playerId));

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
    public ResWait copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResWait)) return false;
        ResWait that = (ResWait) o;
        return playerId == that.playerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Is Waiting ";
    }


}
