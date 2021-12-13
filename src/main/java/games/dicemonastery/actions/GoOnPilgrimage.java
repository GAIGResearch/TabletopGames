package games.dicemonastery.actions;

import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.components.Monk;
import games.dicemonastery.components.Pilgrimage;

import java.util.Objects;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.GATEHOUSE;
import static java.util.stream.Collectors.toList;

public class GoOnPilgrimage extends UseMonk {

    public final Pilgrimage destination;

    public GoOnPilgrimage(Pilgrimage destination, int piety) {
        super(piety);
        this.destination = destination;
        if (destination.isActive())
            throw new AssertionError("We cannot start a pilgrimage that is already in progress");
    }

    @Override
    public boolean _execute(DiceMonasteryGameState state) {
        Monk pilgrim = state.monksIn(GATEHOUSE, state.getCurrentPlayer()).stream()
                .filter(m -> m.getPiety() == actionPoints).collect(toList()).get(0);
        state.startPilgrimage(destination, pilgrim);
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GoOnPilgrimage) {
            return ((GoOnPilgrimage) obj).destination.equals(destination) && ((GoOnPilgrimage) obj).actionPoints == actionPoints;
        }
        return false;
    }

    @Override
    public GoOnPilgrimage copy() {
        // no longer mutable due to Pilgrimage maintaining state
        return new GoOnPilgrimage(destination.copy(), actionPoints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actionPoints, destination) - 168729;
    }

    @Override
    public String toString() {
        return String.format("Pilgrimage to %s (piety %d)", destination.destination, actionPoints);
    }
}
