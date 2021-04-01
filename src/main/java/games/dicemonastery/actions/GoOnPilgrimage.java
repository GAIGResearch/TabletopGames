package games.dicemonastery.actions;

import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.Monk;
import games.dicemonastery.Pilgrimage;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.GATEHOUSE;
import static java.util.stream.Collectors.toList;

public class GoOnPilgrimage extends UseMonk {

    public final Pilgrimage.DESTINATION destination;

    public GoOnPilgrimage(Pilgrimage.DESTINATION destination, int piety) {
        super(piety);
        this.destination = destination;
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
            return ((GoOnPilgrimage) obj).destination == destination;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return destination.hashCode() + 483;
    }

    @Override
    public String toString() {
        return "Pilgrimage to " + destination.name();
    }
}
