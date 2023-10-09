package games.monopolydeal.actions.informationcontainer;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import games.monopolydeal.cards.PropertySet;
import games.monopolydeal.cards.SetType;

import java.util.Objects;

/**
 * <p>Actions are unit things players can do in the game (e.g. play a card, move a pawn, roll dice, attack etc.).</p>
 * <p>Actions in the game can (and should, if applicable) extend one of the other existing actions, in package {@link core.actions}.
 * Or, a game may simply reuse one of the existing core actions.</p>
 * <p>Actions may have parameters, so as not to duplicate actions for the same type of functionality,
 * e.g. playing card of different types (see {@link games.sushigo.actions.ChooseCard} action from SushiGo as an example).
 * Include these parameters in the class constructor.</p>
 * <p>They need to extend at a minimum the {@link AbstractAction} super class and implement the {@link AbstractAction#execute(AbstractGameState)} method.
 * This is where the main functionality of the action should be inserted, which modifies the given game state appropriately (e.g. if the action is to play a card,
 * then the card will be moved from the player's hand to the discard pile, and the card's effect will be applied).</p>
 * <p>They also need to include {@link Object#equals(Object)} and {@link Object#hashCode()} methods.</p>
 * <p>They <b>MUST NOT</b> keep references to game components. Instead, store the {@link Component#getComponentID()}
 * in variables for any components that must be referenced in the action. Then, in the execute() function,
 * use the {@link AbstractGameState#getComponentById(int)} function to retrieve the actual reference to the component,
 * given your componentID.</p>
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
            if(pSet.getPropertySetSize()==0) rent = 0;
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
