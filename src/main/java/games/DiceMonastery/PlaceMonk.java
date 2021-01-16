package games.DiceMonastery;

import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.Objects;

import static games.DiceMonastery.DiceMonasteryGameState.*;

public class PlaceMonk extends AbstractAction {

    public final actionArea destination;
    public final int monkId;

    public PlaceMonk(int id, actionArea area) {
        destination = area;
        monkId = id;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        return false;
    }

    @Override
    public AbstractAction copy() {
        // no mutable state
        return this;
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
