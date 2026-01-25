package games.descent2e.actions.searchcards;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;

public class UseWardingTalisman extends DescentAction {

    public UseWardingTalisman() {
        super(Triggers.ROLL_DEFENCE_DICE);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Use Warding Talisman";
    }

    @Override
    public boolean execute(DescentGameState gs) {
        // TODO:  add 2 (N) Shield to his defence pool when being a target of an attack.
        return false;
    }

    @Override
    public DescentAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof UseWardingTalisman;
    }

    @Override
    public int hashCode() {
        return 34890;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        return false;
    }
}
