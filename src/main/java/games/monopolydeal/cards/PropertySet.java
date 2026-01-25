package games.monopolydeal.cards;

import core.CoreConstants;
import core.components.Deck;

import java.util.Objects;

import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;

public class PropertySet extends Deck<MonopolyDealCard> {
    SetType type;
    public boolean isComplete;
    public boolean hasWild; // Used to check for modifyBoard
    // Used to check rent
    public boolean hasHouse;
    public boolean hasHotel;

    public PropertySet(String name, CoreConstants.VisibilityMode visibility, SetType type) {
        super(name, visibility);
        this.type = type;
        isComplete = false;
        hasHouse = false;
        hasHotel = false;
        hasWild = false;
    }

    private PropertySet(String name, CoreConstants.VisibilityMode visibility, SetType type, int ownerId, int id) {
        super(name, ownerId, id, visibility);
        this.type = type;
        isComplete = false;
        hasHouse = false;
        hasHotel = false;
        hasWild = false;
    }
    @Override
    public PropertySet copy(){
        SetType sType = getSetType();
        PropertySet newSet = new PropertySet(sType.toString(),VISIBLE_TO_ALL,sType, ownerId, componentID);
        copyTo(newSet);
        newSet.isComplete = isComplete;
        newSet.hasWild = hasWild;
        newSet.hasHouse = hasHouse;
        newSet.hasHotel = hasHotel;
        return newSet;
    }
    public int getPropertySetSize(){
        int count = 0;
        for (int i=0; i<this.getSize();i++) {
            if(this.get(i).isPropertyCard()) count = count+1;
        }
        return count;
    }
    public SetType getSetType(){return type;}
    public boolean getIsComplete(){return isComplete;}
    @Override
    public boolean add(MonopolyDealCard c) {
        if(c.type == CardType.House ) hasHouse = true;
        else if (c.type == CardType.Hotel ) hasHotel = true;
        else if(getPropertySetSize() >= getSetType().setSize-1)
            isComplete = true;
        if(c.isPropertyWildCard())hasWild = true;
        return super.add(c);
    }
    @Override
    public void remove(MonopolyDealCard c) {
        if(c.type == CardType.House ) hasHouse = false;
        else if (c.type == CardType.Hotel ) hasHotel = false;
        if(c.isPropertyCard() && getPropertySetSize() <= getSetType().setSize) isComplete = false;
        if(c.isPropertyWildCard()){
            int wildCount = 0;
            for (MonopolyDealCard dealCard: this.components) {
                if(dealCard.isPropertyWildCard())wildCount++;
            }
            if(wildCount==1)hasWild = false;
        }
        super.remove(c);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PropertySet that = (PropertySet) o;
        return isComplete == that.isComplete && hasWild == that.hasWild && hasHouse == that.hasHouse &&
                hasHotel == that.hasHotel && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), type.ordinal(), isComplete, hasWild, hasHouse, hasHotel);
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
