package games.monopolydeal.cards;

import core.CoreConstants;
import core.components.Deck;

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
    }

    // Copy constructor???
    public PropertySet copy(){
        PropertySet retValue = (PropertySet) super.copy();

        retValue.type = type;
        retValue.isComplete = isComplete;
        retValue.hasHouse = hasHouse;
        retValue.hasHotel = hasHotel;
        retValue.hasWild = hasWild;

        return retValue;
    }

    public SetType getSetType(){return type;}

    // Note to self
    // Add house/hotel only if complete set
    // modify properties only if no house/hotel present

    @Override
    public boolean add(MonopolyDealCard c) {
        if(c.type == CardType.House ) hasHouse = true;
        else if (c.type == CardType.Hotel ) hasHotel = true;
        else if(getSize()+1 == type.setSize) isComplete = true;

        if(c.isPropertyWildCard())hasWild = true;
        return super.add(c);
    }
    @Override
    public boolean remove(MonopolyDealCard c) {
        if(c.type == CardType.House ) hasHouse = false;
        else if (c.type == CardType.Hotel ) hasHotel = false;
        else if(getSize() < type.setSize) isComplete = false;
        if(c.isPropertyWildCard()){
            int wildCount = 0;
            for (MonopolyDealCard dealCard: this.components) {
                if(dealCard.isPropertyWildCard())wildCount++;
            }
            if(wildCount==1)hasWild = false;
        }
        return super.remove(c);
    }
}
