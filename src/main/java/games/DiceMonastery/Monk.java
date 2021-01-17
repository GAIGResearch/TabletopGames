package games.DiceMonastery;

import core.components.*;
import utilities.Utils;

import java.util.Objects;

public class Monk extends Component {

    int piety;

    public Monk(int value, int ownerId) {
        super(Utils.ComponentType.TOKEN, "Monk");
        this.ownerId = ownerId;
        piety = value;
    }

    private Monk(int value, int ownerId, int componentId) {
        super(Utils.ComponentType.TOKEN, "Monk", componentId);
        this.ownerId = ownerId;
        piety = value;
    }

    @Override
    public Monk copy() {
        return new Monk(piety, getOwnerId(), getComponentID());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Monk))
            return false;
        Monk other = (Monk) o;
        return other.piety == piety && other.componentID == componentID && other.ownerId == ownerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(piety, ownerId, componentID);
    }

    public int getPiety() {
        return piety;
    }
}
