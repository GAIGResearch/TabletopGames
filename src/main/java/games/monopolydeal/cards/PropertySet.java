package games.monopolydeal.cards;

import core.CoreConstants;
import core.components.Deck;

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
    // Copy constructor???
    public PropertySet copy(){
        Deck<MonopolyDealCard> cardDeck = super.copy();
        MonopolyDealCard card = cardDeck.get(0);
        SetType sType = card.getUseAs();
        PropertySet newSet = new PropertySet(sType.toString(),VISIBLE_TO_ALL,sType);
        for (int i=0; i<cardDeck.getSize();i++) {
            newSet.add(cardDeck.get(i));
        }
        return newSet;
    }
    public SetType getSetType(){return type;}
    public boolean getIsComplete(){return isComplete;}
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

    @Override
    public String toString() {
        return type.toString();
    }
}
