package games.dicemonastery.components;

import core.components.Component;
import games.dicemonastery.DiceMonasteryGameState;
import utilities.Utils;

import java.util.Objects;

public class Monk extends Component {

    final static int MAX_PIETY = 6;

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

    public void promote(DiceMonasteryGameState state) {
        piety++;
        if (piety > MAX_PIETY)
            state.retireMonk(this);
    }

    public void demote() {
        if (piety > 1)
            piety--;
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

    @Override
    public String toString() {
        return String.format("Monk %d: Piety %d, Owner %d", componentID, piety, ownerId);
    }
}
