package games.DiceMonastery;

import core.AbstractGameState;
import core.actions.AbstractAction;
import org.apache.commons.math3.analysis.function.Abs;

import java.util.*;

import static games.DiceMonastery.DiceMonasteryGameState.*;

public class PlaceMonk extends AbstractAction implements IExtendedSequence {

    public final actionArea destination;
    public int monkId = 0;

    public PlaceMonk(actionArea area) {
        destination = area;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // just a placeholder as a Move Group
        return true;
    }

    @Override
    public List<AbstractAction> followOnActions(DiceMonasteryGameState state) {
        return new ArrayList<>();
    }

    @Override
    public int getCurrentPlayer(DiceMonasteryGameState state) {
        return state.allMonks.get(monkId).getOwnerId();
    }

    @Override
    public void registerActionTaken(DiceMonasteryGameState state, AbstractAction action) {

    }

    @Override
    public boolean executionComplete(DiceMonasteryGameState state) {
        return monkId != 0;
    }

    @Override
    public PlaceMonk copy() {
        // no mutable state
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PlaceMonk))
            return false;
        PlaceMonk other = (PlaceMonk) obj;
        return other.monkId == monkId && other.destination == destination;
    }

    @Override
    public int hashCode() {
        return Objects.hash(monkId, destination);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Monk monk = ((DiceMonasteryGameState) gameState).allMonks.get(monkId);
        return String.format("Move monk %d (piety: %d, player: %d) to %s", monkId, monk.piety, monk.getOwnerId(), destination);
    }

    @Override
    public String toString() {
        return String.format("Move monk %d to %s", monkId, destination);
    }

}


