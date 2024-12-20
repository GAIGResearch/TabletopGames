package games.monopolydeal.actions.informationcontainer;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import games.monopolydeal.cards.PropertySet;
import games.monopolydeal.cards.SetType;

import java.util.Objects;

/**
 * <p>A simple action which does not execute any command but acts as an information container for other EAS.</p>
 */
public class ChoosePropertySet extends AbstractAction {
    public final SetType setType;

    public ChoosePropertySet(SetType setType){
        this.setType = setType;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        return true;
    }
    @Override
    public ChoosePropertySet copy() {
        return this;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChoosePropertySet that = (ChoosePropertySet) o;
        return setType == that.setType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(setType);
    }

    @Override
    public String toString() {
        return "Steal set : " + setType.toString();
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
