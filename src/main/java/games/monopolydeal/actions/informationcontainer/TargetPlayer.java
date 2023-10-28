package games.monopolydeal.actions.informationcontainer;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;

import java.util.Objects;

/**
 * <p>A simple action which does not execute any command but acts as an information container for other EAS.</p>
 */
public class TargetPlayer extends AbstractAction {
    public int target;
    public TargetPlayer(int target){
        this.target = target;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        return true;
    }
    @Override
    public TargetPlayer copy() {
        return new TargetPlayer(target);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TargetPlayer that = (TargetPlayer) o;
        return target == that.target;
    }
    @Override
    public int hashCode() {
        return Objects.hash(target);
    }
    @Override
    public String toString() {
        return "Target player "+ target;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
