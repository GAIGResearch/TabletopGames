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
public class RentOf extends AbstractAction {
    final PropertySet pSet;
    public int rent;
    public RentOf(PropertySet pSet, int rent){
        this.pSet = pSet;
        this.rent = rent;
    }
    public RentOf(PropertySet pSet) {
        this.pSet = pSet;
        SetType setType = pSet.getSetType();
        if(pSet.isComplete){
            rent = setType.rent[setType.setSize-1];
            if(pSet.hasHouse) rent = rent + 3;
            if(pSet.hasHotel) rent = rent + 4;
        } else {
            if(pSet.getPropertySetSize()-1 >= setType.rent.length)
                throw new AssertionError("Another thing which should not happen");
            if(pSet.getPropertySetSize()==0)
                rent = 0;
            else rent = setType.rent[pSet.getPropertySetSize() - 1];
        }
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        return true;
    }
    @Override
    public RentOf copy() {
        return new RentOf(pSet,rent);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RentOf rentOf = (RentOf) o;
        return rent == rentOf.rent && Objects.equals(pSet, rentOf.pSet);
    }
    @Override
    public int hashCode() {
        return Objects.hash(pSet, rent);
    }
    @Override
    public String toString() {
        return pSet + " rent is " + rent;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
