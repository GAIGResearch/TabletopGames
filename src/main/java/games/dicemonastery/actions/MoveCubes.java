package games.dicemonastery.actions;

import games.dicemonastery.DiceMonasteryGameState;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea;
import static games.dicemonastery.DiceMonasteryConstants.Resource;

public class MoveCubes extends UseMonk {

    Resource resource;
    ActionArea from, to;

    public MoveCubes(int cubes, Resource resource, ActionArea from, ActionArea to) {
        super(cubes);
        this.from = from;
        this.to = to;
        this.resource = resource;
    }

    @Override
    public boolean _execute(DiceMonasteryGameState state) {
        state.moveCubes(state.getCurrentPlayer(), resource, actionPoints, from, to);
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MoveCubes) {
            MoveCubes other = (MoveCubes) obj;
            return other.resource == resource && other.from == from && other.to == to && actionPoints == other.actionPoints;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return -1580077 + from.ordinal() * -1543 + to.ordinal() * 7883 + actionPoints * 7 + resource.ordinal() - 115249;
    }


}
