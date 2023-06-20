package games.monopolydeal.cards;

import core.CoreConstants;
import core.components.Deck;

public class Set extends Deck<MonopolyDealCard> {
    SetType type;

    boolean isComplete;
    boolean hasWild; // Used to check for modifyBoard
    // Used to check rent
    boolean hasHouse;
    boolean hasHotel;
    public Set(String name, CoreConstants.VisibilityMode visibility,SetType type) {
        super(name, visibility);
        this.type = type;
        isComplete = false;
        hasHouse = false;
        hasHotel = false;
    }

    // Note to self
    // Add house/hotel only if complete set
    // modify properties only if no house/hotel present

    @Override
    public boolean add(MonopolyDealCard c) {
        if(c.type == CardType.House ) hasHouse = true;
        else if (c.type == CardType.Hotel ) hasHotel = true;
        else if(getSize()+1 == type.setSize) isComplete = true;
        return super.add(c);
    }
    @Override
    public boolean remove(MonopolyDealCard c) {
        if(c.type == CardType.House ) hasHouse = false;
        else if (c.type == CardType.Hotel ) hasHotel = false;
        else if(getSize() < type.setSize) isComplete = false;
        return super.remove(c);
    }
}
