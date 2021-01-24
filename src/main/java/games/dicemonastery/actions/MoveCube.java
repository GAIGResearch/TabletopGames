package games.dicemonastery.actions;

import games.dicemonastery.DiceMonasteryGameState;

import java.util.Objects;

import static games.dicemonastery.DiceMonasteryConstants.*;

public class MoveCube extends UseMonk{

    Resource resource;
    ActionArea from, to;

    public MoveCube(int actionPoints, Resource resource, ActionArea from, ActionArea to) {
        super(actionPoints);
        this.from = from;
        this.to = to;
        this.resource = resource;
    }

    @Override
    public boolean _execute(DiceMonasteryGameState state) {
        state.moveCube(state.getCurrentPlayer(), resource, from, to);
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MoveCube) {
            MoveCube other = (MoveCube) obj;
            return other.resource == resource && other.from == from && other.to == to;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, from, to, actionPoints);
    }


}
