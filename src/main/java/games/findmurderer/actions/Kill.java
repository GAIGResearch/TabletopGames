package games.findmurderer.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.findmurderer.MurderGameState;
import games.findmurderer.components.Person;

import java.util.Objects;

public class Kill extends AbstractAction {
    public final int target; // component ID of target to kill with this action

    public Kill(int targetID) {
        this.target = targetID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // Get object representing person to kill from the game state
        Person toKill = (Person) gs.getComponentById(target);
        // Set status to dead, and mark the player killing this target
        toKill.status = Person.Status.Dead;
        toKill.killer = MurderGameState.PlayerMapping.getPlayerTypeByIdx(gs.getCurrentPlayer());
        return true;
    }

    @Override
    public Kill copy() {
        // Immutable, no need to copy
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Kill)) return false;
        Kill kill = (Kill) o;
        return target == kill.target;
    }

    @Override
    public int hashCode() {
        return Objects.hash(target);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return MurderGameState.PlayerMapping.getPlayerTypeByIdx(gameState.getCurrentPlayer()) + " kills " + target + " (" + ((Person)gameState.getComponentById(target)).personType + ")";
    }

    @Override
    public String toString() {
        return "Kill " + target;
    }
}
