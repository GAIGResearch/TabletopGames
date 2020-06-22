package games.descent.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.List;

//TODO: attack N targets with weapon
public class Attack extends AbstractAction {
    List<Integer> targets;
    int weaponCardId;

    public Attack(List<Integer> targets, int weaponCardId) {
        this.targets = targets;
        this.weaponCardId = weaponCardId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new Attack(targets, weaponCardId);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Attack;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Attack";
    }
}
